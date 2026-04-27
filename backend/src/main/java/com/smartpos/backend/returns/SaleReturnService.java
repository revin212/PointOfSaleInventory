package com.smartpos.backend.returns;

import com.smartpos.backend.audit.AuditAction;
import com.smartpos.backend.audit.AuditEntityType;
import com.smartpos.backend.audit.AuditService;
import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.domain.enums.SaleStatus;
import com.smartpos.backend.domain.enums.StockMovementType;
import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.CashMovementType;
import com.smartpos.backend.returns.dto.CreateReturnRequest;
import com.smartpos.backend.returns.dto.ReturnCreateResponse;
import com.smartpos.backend.returns.dto.ReturnDetailResponse;
import com.smartpos.backend.returns.dto.ReturnItemRequest;
import com.smartpos.backend.returns.dto.ReturnItemResponse;
import com.smartpos.backend.sales.SaleEntity;
import com.smartpos.backend.sales.SaleRepository;
import com.smartpos.backend.security.SecurityUtils;
import com.smartpos.backend.shifts.CashMovementEntity;
import com.smartpos.backend.shifts.CashMovementRepository;
import com.smartpos.backend.stock.StockLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SaleReturnService {

    private static final String REF_TYPE = "SALE_RETURN";

    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final StockLedgerService stockLedger;
    private final AuditService auditService;
    private final CashMovementRepository cashMovementRepository;

    public SaleReturnService(SaleRepository saleRepository,
                             SaleReturnRepository saleReturnRepository,
                             StockLedgerService stockLedger,
                             AuditService auditService,
                             CashMovementRepository cashMovementRepository) {
        this.saleRepository = saleRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.stockLedger = stockLedger;
        this.auditService = auditService;
        this.cashMovementRepository = cashMovementRepository;
    }

    @Transactional
    public ReturnCreateResponse create(UUID saleId, CreateReturnRequest req) {
        SaleEntity sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new BusinessRuleException("Only COMPLETED sales can be returned");
        }

        // aggregate duplicates defensively
        Map<UUID, Integer> requested = new LinkedHashMap<>();
        for (ReturnItemRequest it : req.items()) {
            requested.merge(it.productId(), it.qty(), Integer::sum);
        }

        // sold qty per product
        Map<UUID, Integer> sold = sale.getItems().stream()
                .collect(Collectors.toMap(
                        si -> si.getProductId(),
                        si -> si.getQty(),
                        Integer::sum
                ));

        // already returned qty per product
        Map<UUID, Integer> returned = new HashMap<>();
        for (Object[] row : saleReturnRepository.sumReturnedQtyByProduct(saleId)) {
            returned.put((UUID) row[0], ((Number) row[1]).intValue());
        }

        for (Map.Entry<UUID, Integer> e : requested.entrySet()) {
            UUID productId = e.getKey();
            int reqQty = e.getValue();
            int soldQty = sold.getOrDefault(productId, 0);
            int retQty = returned.getOrDefault(productId, 0);
            int remaining = soldQty - retQty;
            if (soldQty <= 0) {
                throw new BusinessRuleException("Product " + productId + " is not part of this sale");
            }
            if (reqQty > remaining) {
                throw new BusinessRuleException("Cannot return " + reqQty + " for product " + productId
                        + "; only " + remaining + " remaining to return");
            }
        }

        BigDecimal refundableMax = computeRefundableMax(sale, requested);
        BigDecimal alreadyRefunded = saleReturnRepository.sumRefundedAmount(saleId);
        if (alreadyRefunded == null) alreadyRefunded = BigDecimal.ZERO;
        BigDecimal remainingRefundable = refundableMax.subtract(alreadyRefunded);
        if (remainingRefundable.compareTo(BigDecimal.ZERO) < 0) remainingRefundable = BigDecimal.ZERO;

        PaymentMethod refundMethod = req.refundMethod();
        BigDecimal refundAmount = req.refundAmount() == null ? BigDecimal.ZERO : req.refundAmount();
        if (refundMethod != null && refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("refundAmount must be > 0 when refundMethod is provided");
        }
        if (refundMethod == null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("refundMethod is required when refundAmount is provided");
        }
        if (refundAmount.compareTo(remainingRefundable) > 0) {
            throw new BusinessRuleException("Refund amount exceeds refundable remaining (max=" + remainingRefundable + ")");
        }

        UUID actor = SecurityUtils.currentUserId();
        SaleReturnEntity r = new SaleReturnEntity();
        r.setSaleId(saleId);
        r.setCreatedBy(actor);
        r.setReason(req.reason());
        r.setRefundMethod(refundMethod == null ? null : refundMethod.name());
        r.setRefundAmount(refundAmount.setScale(2, RoundingMode.HALF_UP));
        for (Map.Entry<UUID, Integer> e : requested.entrySet()) {
            SaleReturnItemEntity item = new SaleReturnItemEntity();
            item.setProductId(e.getKey());
            item.setQty(e.getValue());
            r.addItem(item);
        }
        SaleReturnEntity saved = saleReturnRepository.save(r);

        // stock movements
        for (SaleReturnItemEntity it : saved.getItems()) {
            stockLedger.record(
                    StockMovementType.RETURN,
                    it.getProductId(),
                    it.getQty(),
                    null,
                    REF_TYPE,
                    saved.getId(),
                    "Return for sale " + sale.getInvoiceNo(),
                    actor
            );
        }

        auditService.record(AuditAction.SALE_RETURN, AuditEntityType.SALE, saleId,
                Map.of("saleReturnId", saved.getId().toString(),
                        "invoiceNo", sale.getInvoiceNo(),
                        "itemCount", saved.getItems().size(),
                        "refundMethod", saved.getRefundMethod() == null ? "" : saved.getRefundMethod(),
                        "refundAmount", saved.getRefundAmount().toPlainString(),
                        "reason", saved.getReason() == null ? "" : saved.getReason()));

        if (refundMethod == PaymentMethod.CASH && refundAmount.compareTo(BigDecimal.ZERO) > 0 && sale.getShiftId() != null) {
            CashMovementEntity m = new CashMovementEntity();
            m.setShiftId(sale.getShiftId());
            m.setType(CashMovementType.OUT);
            m.setAmount(refundAmount.setScale(2, RoundingMode.HALF_UP));
            m.setNote("Refund for return " + saved.getId() + " (sale " + sale.getInvoiceNo() + ")");
            m.setCreatedBy(actor);
            cashMovementRepository.save(m);
        }

        return new ReturnCreateResponse(
                saved.getId(),
                saleId,
                saved.getItems().size(),
                refundableMax,
                saved.getRefundAmount(),
                refundMethod,
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ReturnDetailResponse> list(UUID saleId) {
        SaleEntity sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        BigDecimal saleRefundableMax = computeRefundableMax(sale, sale.getItems().stream()
                .collect(Collectors.toMap(
                        si -> si.getProductId(),
                        si -> si.getQty(),
                        Integer::sum
                )));

        return saleReturnRepository.findAllBySaleIdOrderByCreatedAtDesc(saleId).stream()
                .map(r -> toDetailResponse(r, saleRefundableMax, sale))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReturnDetailResponse get(UUID saleId, UUID returnId) {
        SaleEntity sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        SaleReturnEntity r = saleReturnRepository.findById(returnId)
                .orElseThrow(() -> new NotFoundException("Return not found"));
        if (!saleId.equals(r.getSaleId())) {
            throw new NotFoundException("Return not found");
        }
        BigDecimal saleRefundableMax = computeRefundableMax(sale, sale.getItems().stream()
                .collect(Collectors.toMap(
                        si -> si.getProductId(),
                        si -> si.getQty(),
                        Integer::sum
                )));
        return toDetailResponse(r, saleRefundableMax, sale);
    }

    private ReturnDetailResponse toDetailResponse(SaleReturnEntity r, BigDecimal saleRefundableMax, SaleEntity sale) {
        PaymentMethod method = r.getRefundMethod() == null ? null : PaymentMethod.valueOf(r.getRefundMethod());
        BigDecimal refunded = r.getRefundAmount() == null ? BigDecimal.ZERO : r.getRefundAmount();
        List<ReturnItemResponse> items = r.getItems().stream()
                .map(i -> new ReturnItemResponse(i.getProductId(), i.getQty()))
                .toList();

        // For now, expose sale total as the upper bound. A future enhancement can compute remaining per-item.
        return new ReturnDetailResponse(
                r.getId(),
                r.getSaleId(),
                r.getReason(),
                saleRefundableMax,
                refunded,
                method,
                r.getCreatedAt(),
                items
        );
    }

    private BigDecimal computeRefundableMax(SaleEntity sale, Map<UUID, Integer> requestedByProduct) {
        // Compute net return amount from item lineTotal prorated by qty, then allocate tax proportionally.
        BigDecimal netReturn = BigDecimal.ZERO;
        for (var si : sale.getItems()) {
            Integer qtyRet = requestedByProduct.get(si.getProductId());
            if (qtyRet == null || qtyRet <= 0) continue;
            BigDecimal unitNet = si.getLineTotal().divide(BigDecimal.valueOf(si.getQty()), 8, RoundingMode.HALF_UP);
            netReturn = netReturn.add(unitNet.multiply(BigDecimal.valueOf(qtyRet)));
        }
        netReturn = roundIdr(netReturn);

        BigDecimal taxReturn = BigDecimal.ZERO;
        if (sale.getNetAmount() != null && sale.getNetAmount().compareTo(BigDecimal.ZERO) > 0) {
            taxReturn = sale.getTaxAmount()
                    .multiply(netReturn)
                    .divide(sale.getNetAmount(), 8, RoundingMode.HALF_UP);
            taxReturn = roundIdr(taxReturn);
        }
        return netReturn.add(taxReturn).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal roundIdr(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return amount.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }
}


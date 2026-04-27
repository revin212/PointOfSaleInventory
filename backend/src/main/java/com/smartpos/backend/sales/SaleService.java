package com.smartpos.backend.sales;

import com.smartpos.backend.audit.AuditAction;
import com.smartpos.backend.audit.AuditEntityType;
import com.smartpos.backend.audit.AuditService;
import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.domain.enums.SaleStatus;
import com.smartpos.backend.domain.enums.StockMovementType;
import com.smartpos.backend.products.ProductEntity;
import com.smartpos.backend.products.ProductRepository;
import com.smartpos.backend.sales.dto.*;
import com.smartpos.backend.security.SecurityUtils;
import com.smartpos.backend.stock.StockLedgerService;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import com.smartpos.backend.config.AppProperties;
import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.ShiftStatus;
import com.smartpos.backend.shifts.ShiftRepository;
import com.smartpos.backend.paymenttypes.PaymentTypeEntity;
import com.smartpos.backend.paymenttypes.PaymentTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private static final String REF_TYPE = "SALE";
    private static final DateTimeFormatter INV_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockLedgerService stockLedger;
    private final AuditService auditService;
    private final AppProperties props;
    private final ShiftRepository shiftRepository;
    private final PaymentTypeService paymentTypeService;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       StockLedgerService stockLedger,
                       AuditService auditService,
                       AppProperties props,
                       ShiftRepository shiftRepository,
                       PaymentTypeService paymentTypeService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.stockLedger = stockLedger;
        this.auditService = auditService;
        this.props = props;
        this.shiftRepository = shiftRepository;
        this.paymentTypeService = paymentTypeService;
    }

    @Transactional
    public SaleCreateResponse create(CreateSaleRequest req) {
        UUID cashierId = SecurityUtils.currentUserId();

        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessRuleException("Sale must contain at least one item");
        }

        // Aggregate duplicate productId entries defensively
        Map<UUID, SaleItemRequest> aggregated = new LinkedHashMap<>();
        for (SaleItemRequest ir : req.items()) {
            if (aggregated.containsKey(ir.productId())) {
                throw new BusinessRuleException("Duplicate productId in items");
            }
            aggregated.put(ir.productId(), ir);
        }

        Map<UUID, ProductEntity> products = loadProducts(aggregated.keySet());
        for (ProductEntity p : products.values()) {
            if (!p.isActive()) {
                throw new BusinessRuleException("Product is not active: " + p.getSku());
            }
        }

        SaleEntity sale = new SaleEntity();

        // Compute totals
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        BigDecimal adminFee = BigDecimal.ZERO;
        PaymentMethod paymentMethod = req.paymentMethod();
        if (req.paymentTypeId() != null) {
            PaymentTypeEntity pt = paymentTypeService.require(req.paymentTypeId());
            if (!pt.isActive()) {
                throw new BusinessRuleException("Payment type is inactive");
            }
            paymentMethod = pt.getMethod();
            adminFee = pt.getAdminFee() == null ? BigDecimal.ZERO : pt.getAdminFee();
            sale.setPaymentTypeId(pt.getId());
        }
        for (SaleItemRequest ir : req.items()) {
            BigDecimal lineDiscount = ir.lineDiscount() == null ? BigDecimal.ZERO : ir.lineDiscount();
            BigDecimal gross = ir.unitPrice().multiply(BigDecimal.valueOf(ir.qty()));
            if (lineDiscount.compareTo(gross) > 0) {
                throw new BusinessRuleException("Line discount exceeds line gross for product " + ir.productId());
            }
            BigDecimal lineTotal = gross.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            SaleItemEntity item = new SaleItemEntity();
            item.setProductId(ir.productId());
            item.setQty(ir.qty());
            item.setUnitPrice(ir.unitPrice());
            item.setLineDiscount(lineDiscount);
            item.setLineTotal(lineTotal);
            sale.addItem(item);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Discount exceeds subtotal");
        }
        BigDecimal net = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        TaxCalc tax = computeTax(net);
        BigDecimal total = net.add(tax.taxAmount()).add(adminFee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal paid = req.paidAmount().setScale(2, RoundingMode.HALF_UP);
        if (paid.compareTo(total) < 0) {
            throw new BusinessRuleException("Paid amount is less than total");
        }
        BigDecimal change = paid.subtract(total);

        sale.setCashierId(cashierId);
        sale.setCustomerId(req.customerId());
        if (req.paymentMethod() == PaymentMethod.CASH) {
            shiftRepository.findFirstByOpenedByAndStatusOrderByOpenedAtDesc(cashierId, ShiftStatus.OPEN)
                    .map(com.smartpos.backend.shifts.ShiftEntity::getId)
                    .ifPresent(sale::setShiftId);
            boolean require = props.shift() != null && props.shift().requireOpenForCashSales();
            if (require && sale.getShiftId() == null) {
                throw new BusinessRuleException("CASH sales require an open shift");
            }
        }
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setNetAmount(net);
        sale.setTaxRate(tax.taxRate());
        sale.setTaxAmount(tax.taxAmount());
        sale.setTotal(total);
        sale.setAdminFee(adminFee);
        sale.setPaymentMethod(paymentMethod);
        sale.setPaidAmount(paid);
        sale.setChangeAmount(change);

        SaleEntity saved = persistWithUniqueInvoice(sale);

        // Write stock movements (will throw INSUFFICIENT_STOCK and rollback if any fails)
        for (SaleItemEntity it : saved.getItems()) {
            stockLedger.record(
                    StockMovementType.SALE,
                    it.getProductId(),
                    -it.getQty(),
                    null,
                    REF_TYPE,
                    saved.getId(),
                    null,
                    cashierId
            );
        }

        auditService.record(AuditAction.SALE_CREATE, AuditEntityType.SALE, saved.getId(),
                Map.of("invoiceNo", saved.getInvoiceNo(),
                        "total", saved.getTotal().toPlainString(),
                        "taxAmount", saved.getTaxAmount().toPlainString(),
                        "adminFee", saved.getAdminFee().toPlainString(),
                        "paymentMethod", saved.getPaymentMethod().name(),
                        "itemCount", saved.getItems().size()));

        return new SaleCreateResponse(
                saved.getId(),
                saved.getInvoiceNo(),
                saved.getStatus(),
                new SaleTotalsResponse(saved.getSubtotal(), saved.getDiscount(),
                        saved.getNetAmount(), saved.getTaxRate(), saved.getTaxAmount(),
                        saved.getTotal(), saved.getPaidAmount(), saved.getChangeAmount(), saved.getAdminFee())
        );
    }

    @Transactional
    public SaleCancelResponse cancel(UUID saleId, CancelSaleRequest req) {
        SaleEntity sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessRuleException("Sale already cancelled");
        }

        UUID actorId = SecurityUtils.currentUserId();
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(Instant.now());
        sale.setCancelledBy(actorId);
        sale.setCancelReason(req == null ? null : req.reason());

        for (SaleItemEntity it : sale.getItems()) {
            stockLedger.record(
                    StockMovementType.SALE_CANCEL,
                    it.getProductId(),
                    it.getQty(),
                    null,
                    REF_TYPE,
                    sale.getId(),
                    "Reversal of sale " + sale.getInvoiceNo(),
                    actorId
            );
        }
        saleRepository.save(sale);
        auditService.record(AuditAction.SALE_CANCEL, AuditEntityType.SALE, sale.getId(),
                Map.of("invoiceNo", sale.getInvoiceNo(),
                        "reason", sale.getCancelReason() == null ? "" : sale.getCancelReason()));
        return new SaleCancelResponse(true, sale.getStatus());
    }

    @Transactional(readOnly = true)
    public Page<SaleSummaryResponse> list(Instant from, Instant to, UUID cashierId,
                                          com.smartpos.backend.domain.enums.PaymentMethod paymentMethod,
                                          Pageable pageable) {
        Specification<SaleEntity> spec = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (from != null)          predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null)            predicates.add(cb.lessThan(root.get("createdAt"), to));
            if (cashierId != null)     predicates.add(cb.equal(root.get("cashierId"), cashierId));
            if (paymentMethod != null) predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<SaleEntity> page = saleRepository.findAll(spec, pageable);
        Map<UUID, String> cashierNames = resolveCashierNames(
                page.getContent().stream().map(SaleEntity::getCashierId).collect(Collectors.toSet()));
        return page.map(s -> new SaleSummaryResponse(
                s.getId(), s.getInvoiceNo(), s.getCashierId(), cashierNames.get(s.getCashierId()),
                s.getStatus(), s.getPaymentMethod(),
                s.getSubtotal(), s.getDiscount(), s.getTotal(),
                s.getItems().size(), s.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public SaleDetailResponse get(UUID id) {
        SaleEntity s = saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        String cashierName = userRepository.findById(s.getCashierId())
                .map(UserEntity::getName).orElse(null);
        Set<UUID> productIds = s.getItems().stream().map(SaleItemEntity::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductEntity> products = new HashMap<>();
        productRepository.findAllById(productIds).forEach(p -> products.put(p.getId(), p));
        List<SaleItemResponse> items = s.getItems().stream().map(it -> {
            ProductEntity p = products.get(it.getProductId());
            return SaleItemResponse.from(it, p == null ? null : p.getSku(), p == null ? null : p.getName());
        }).toList();
        return new SaleDetailResponse(
                s.getId(), s.getInvoiceNo(), s.getCashierId(), cashierName, s.getStatus(), s.getPaymentMethod(),
                new SaleTotalsResponse(s.getSubtotal(), s.getDiscount(),
                        s.getNetAmount(), s.getTaxRate(), s.getTaxAmount(),
                        s.getTotal(), s.getPaidAmount(), s.getChangeAmount(), s.getAdminFee()),
                s.getCreatedAt(), s.getCancelledAt(), s.getCancelledBy(), s.getCancelReason(), items);
    }

    private Map<UUID, ProductEntity> loadProducts(Set<UUID> ids) {
        Map<UUID, ProductEntity> map = new HashMap<>();
        productRepository.findAllById(ids).forEach(p -> map.put(p.getId(), p));
        if (map.size() != ids.size()) {
            Set<UUID> missing = ids.stream().filter(id -> !map.containsKey(id)).collect(Collectors.toSet());
            throw new NotFoundException("Product not found: " + missing);
        }
        return map;
    }

    private Map<UUID, String> resolveCashierNames(Set<UUID> ids) {
        Map<UUID, String> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        userRepository.findAllById(ids).forEach(u -> map.put(u.getId(), u.getName()));
        return map;
    }

    private String generateInvoiceNumber() {
        String prefix = "INV-" + INV_DATE.format(Instant.now()) + "-";
        int suffix = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return prefix + String.format("%06d", suffix);
    }

    private SaleEntity persistWithUniqueInvoice(SaleEntity sale) {
        final int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sale.setInvoiceNo(generateInvoiceNumber());
            try {
                return saleRepository.saveAndFlush(sale);
            } catch (DataIntegrityViolationException ex) {
                // Retry when invoice number hits unique constraint; otherwise rethrow.
                if (attempt == maxAttempts || !looksLikeInvoiceUniqueViolation(ex)) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unable to generate a unique invoice number after retries");
    }

    private boolean looksLikeInvoiceUniqueViolation(DataIntegrityViolationException ex) {
        Throwable t = ex;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("uq_sales_invoice") || m.contains("invoice_no") || m.contains("invoice")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private TaxCalc computeTax(BigDecimal netAmount) {
        if (props.tax() == null || !props.tax().enabled()) {
            return new TaxCalc(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal rate = props.tax().vatRate() == null ? BigDecimal.ZERO : props.tax().vatRate();
        BigDecimal rawTax = netAmount.multiply(rate);
        BigDecimal tax = roundIdr(rawTax);
        return new TaxCalc(rate, tax);
    }

    private BigDecimal roundIdr(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        // UI uses IDR with no decimals; we round to whole Rupiah but store with scale(2).
        return amount.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    private record TaxCalc(BigDecimal taxRate, BigDecimal taxAmount) {}
}

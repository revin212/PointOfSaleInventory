package com.smartpos.backend.purchases;

import com.smartpos.backend.audit.AuditAction;
import com.smartpos.backend.audit.AuditEntityType;
import com.smartpos.backend.audit.AuditService;
import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.domain.enums.PurchaseStatus;
import com.smartpos.backend.domain.enums.StockMovementType;
import com.smartpos.backend.products.ProductEntity;
import com.smartpos.backend.products.ProductRepository;
import com.smartpos.backend.purchases.dto.*;
import com.smartpos.backend.security.SecurityUtils;
import com.smartpos.backend.stock.StockLedgerService;
import com.smartpos.backend.suppliers.SupplierEntity;
import com.smartpos.backend.suppliers.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    private static final String REF_TYPE = "PURCHASE_RECEIPT";

    private final PurchaseRepository purchaseRepository;
    private final PurchaseReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockLedgerService stockLedger;
    private final AuditService auditService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           PurchaseReceiptRepository receiptRepository,
                           SupplierRepository supplierRepository,
                           ProductRepository productRepository,
                           StockLedgerService stockLedger,
                           AuditService auditService) {
        this.purchaseRepository = purchaseRepository;
        this.receiptRepository = receiptRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockLedger = stockLedger;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<PurchaseSummaryResponse> list(PurchaseStatus status, UUID supplierId, Pageable pageable) {
        Page<PurchaseEntity> page = purchaseRepository.search(status, supplierId, pageable);
        Map<UUID, String> supplierNames = resolveSupplierNames(
                page.getContent().stream().map(PurchaseEntity::getSupplierId).collect(Collectors.toSet()));
        return page.map(p -> toSummary(p, supplierNames.get(p.getSupplierId())));
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(UUID id) {
        PurchaseEntity p = findOrThrow(id);
        return toDetail(p);
    }

    @Transactional
    public PurchaseResponse create(CreatePurchaseRequest req) {
        SupplierEntity supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found"));

        Set<UUID> productIds = req.items().stream()
                .map(PurchaseItemRequest::productId).collect(Collectors.toSet());
        if (productIds.size() != req.items().size()) {
            throw new BusinessRuleException("Duplicate productId in items");
        }
        Map<UUID, ProductEntity> products = loadProducts(productIds);

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setSupplierId(supplier.getId());
        purchase.setStatus(PurchaseStatus.OPEN);
        purchase.setCreatedBy(SecurityUtils.currentUserId());

        for (PurchaseItemRequest ir : req.items()) {
            if (!products.containsKey(ir.productId())) {
                throw new NotFoundException("Product not found: " + ir.productId());
            }
            PurchaseItemEntity item = new PurchaseItemEntity();
            item.setProductId(ir.productId());
            item.setQtyOrdered(ir.qtyOrdered());
            item.setCost(ir.cost());
            item.setQtyReceivedTotal(0);
            purchase.addItem(item);
        }

        PurchaseEntity saved = purchaseRepository.save(purchase);
        auditService.record(AuditAction.PURCHASE_CREATE, AuditEntityType.PURCHASE, saved.getId(),
                Map.of("supplierId", saved.getSupplierId().toString(), "itemCount", saved.getItems().size()));
        return toDetail(saved);
    }

    @Transactional
    public ReceiveResponse receive(UUID purchaseId, ReceivePurchaseRequest req) {
        PurchaseEntity p = findOrThrow(purchaseId);
        if (p.getStatus() == PurchaseStatus.CANCELLED || p.getStatus() == PurchaseStatus.RECEIVED) {
            throw new BusinessRuleException("Purchase in status " + p.getStatus() + " cannot be received");
        }

        Map<UUID, PurchaseItemEntity> byProduct = p.getItems().stream()
                .collect(Collectors.toMap(PurchaseItemEntity::getProductId, it -> it));
        Map<UUID, Integer> aggregated = new LinkedHashMap<>();
        Map<UUID, BigDecimal> costOverrides = new LinkedHashMap<>();

        for (ReceiveItemRequest ir : req.items()) {
            aggregated.merge(ir.productId(), ir.qtyReceived(), Integer::sum);
            costOverrides.put(ir.productId(), ir.cost());
        }

        for (Map.Entry<UUID, Integer> e : aggregated.entrySet()) {
            PurchaseItemEntity item = byProduct.get(e.getKey());
            if (item == null) {
                throw new BusinessRuleException("Product " + e.getKey() + " is not in this purchase");
            }
            int remaining = item.getQtyOrdered() - item.getQtyReceivedTotal();
            if (e.getValue() > remaining) {
                throw new BusinessRuleException("Cannot receive " + e.getValue()
                        + " for product " + e.getKey() + "; only " + remaining + " remaining");
            }
        }

        PurchaseReceiptEntity receipt = new PurchaseReceiptEntity();
        receipt.setPurchase(p);
        receipt.setReceivedBy(SecurityUtils.currentUserId());
        for (ReceiveItemRequest ir : req.items()) {
            PurchaseReceiptItemEntity ri = new PurchaseReceiptItemEntity();
            ri.setProductId(ir.productId());
            ri.setQtyReceived(ir.qtyReceived());
            ri.setCost(ir.cost());
            receipt.addItem(ri);
        }
        PurchaseReceiptEntity savedReceipt = receiptRepository.save(receipt);

        for (Map.Entry<UUID, Integer> e : aggregated.entrySet()) {
            PurchaseItemEntity item = byProduct.get(e.getKey());
            item.setQtyReceivedTotal(item.getQtyReceivedTotal() + e.getValue());

            stockLedger.record(
                    StockMovementType.PURCHASE_RECEIVE,
                    e.getKey(),
                    e.getValue(),
                    costOverrides.get(e.getKey()),
                    REF_TYPE,
                    savedReceipt.getId(),
                    null,
                    SecurityUtils.currentUserId()
            );
        }

        p.setStatus(computeStatus(p));
        purchaseRepository.save(p);

        auditService.record(AuditAction.PURCHASE_RECEIVE, AuditEntityType.PURCHASE, p.getId(),
                Map.of("receiptId", savedReceipt.getId().toString(),
                        "lineCount", req.items().size(),
                        "newStatus", p.getStatus().name()));

        return new ReceiveResponse(true, p.getStatus());
    }

    private PurchaseStatus computeStatus(PurchaseEntity p) {
        boolean anyReceived = false;
        boolean allFull = true;
        for (PurchaseItemEntity it : p.getItems()) {
            if (it.getQtyReceivedTotal() > 0) anyReceived = true;
            if (it.getQtyReceivedTotal() < it.getQtyOrdered()) allFull = false;
        }
        if (allFull) return PurchaseStatus.RECEIVED;
        if (anyReceived) return PurchaseStatus.PARTIALLY_RECEIVED;
        return PurchaseStatus.OPEN;
    }

    private PurchaseEntity findOrThrow(UUID id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase not found"));
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

    private Map<UUID, String> resolveSupplierNames(Set<UUID> ids) {
        Map<UUID, String> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        supplierRepository.findAllById(ids).forEach(s -> map.put(s.getId(), s.getName()));
        return map;
    }

    private PurchaseSummaryResponse toSummary(PurchaseEntity p, String supplierName) {
        int itemCount = p.getItems().size();
        BigDecimal total = p.getItems().stream()
                .map(it -> it.getCost().multiply(BigDecimal.valueOf(it.getQtyOrdered())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseSummaryResponse(p.getId(), p.getSupplierId(), supplierName,
                p.getStatus(), itemCount, total, p.getCreatedAt(), p.getUpdatedAt());
    }

    private PurchaseResponse toDetail(PurchaseEntity p) {
        String supplierName = supplierRepository.findById(p.getSupplierId())
                .map(SupplierEntity::getName).orElse(null);
        Set<UUID> productIds = p.getItems().stream()
                .map(PurchaseItemEntity::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductEntity> products = new HashMap<>();
        productRepository.findAllById(productIds).forEach(prod -> products.put(prod.getId(), prod));

        List<PurchaseItemResponse> items = p.getItems().stream()
                .map(it -> {
                    ProductEntity prod = products.get(it.getProductId());
                    String sku = prod == null ? null : prod.getSku();
                    String name = prod == null ? null : prod.getName();
                    return PurchaseItemResponse.from(it, sku, name);
                })
                .toList();

        return new PurchaseResponse(p.getId(), p.getSupplierId(), supplierName,
                p.getStatus(), p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt(), items);
    }
}

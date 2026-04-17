package com.smartpos.backend.purchases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_receipt_items")
public class PurchaseReceiptItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private PurchaseReceiptEntity receipt;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "qty_received", nullable = false)
    private int qtyReceived;

    @Column(name = "cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal cost;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PurchaseReceiptEntity getReceipt() { return receipt; }
    public void setReceipt(PurchaseReceiptEntity receipt) { this.receipt = receipt; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public int getQtyReceived() { return qtyReceived; }
    public void setQtyReceived(int qtyReceived) { this.qtyReceived = qtyReceived; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
}

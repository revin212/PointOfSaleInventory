package com.smartpos.backend.purchases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_items")
public class PurchaseItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private PurchaseEntity purchase;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "qty_ordered", nullable = false)
    private int qtyOrdered;

    @Column(name = "cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal cost;

    @Column(name = "qty_received_total", nullable = false)
    private int qtyReceivedTotal = 0;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PurchaseEntity getPurchase() { return purchase; }
    public void setPurchase(PurchaseEntity purchase) { this.purchase = purchase; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public int getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(int qtyOrdered) { this.qtyOrdered = qtyOrdered; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public int getQtyReceivedTotal() { return qtyReceivedTotal; }
    public void setQtyReceivedTotal(int qtyReceivedTotal) { this.qtyReceivedTotal = qtyReceivedTotal; }
}

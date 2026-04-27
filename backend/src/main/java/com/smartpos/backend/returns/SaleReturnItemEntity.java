package com.smartpos.backend.returns;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sale_return_items")
public class SaleReturnItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_return_id", nullable = false)
    private SaleReturnEntity saleReturn;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "qty", nullable = false)
    private int qty;

    public UUID getId() { return id; }

    public SaleReturnEntity getSaleReturn() { return saleReturn; }
    public void setSaleReturn(SaleReturnEntity saleReturn) { this.saleReturn = saleReturn; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}


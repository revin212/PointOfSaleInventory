package com.smartpos.backend.returns;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sale_returns")
@EntityListeners(AuditingEntityListener.class)
public class SaleReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "refund_method", length = 20)
    private String refundMethod;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SaleReturnItemEntity> items = new ArrayList<>();

    public UUID getId() { return id; }

    public UUID getSaleId() { return saleId; }
    public void setSaleId(UUID saleId) { this.saleId = saleId; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRefundMethod() { return refundMethod; }
    public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public Instant getCreatedAt() { return createdAt; }

    public List<SaleReturnItemEntity> getItems() { return items; }
    public void addItem(SaleReturnItemEntity item) {
        item.setSaleReturn(this);
        items.add(item);
    }
}


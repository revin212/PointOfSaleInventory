package com.smartpos.backend.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_stocks")
@EntityListeners(AuditingEntityListener.class)
@IdClass(ProductStockId.class)
public class ProductStockEntity {

    @Id
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Id
    @Column(name = "location_id", updatable = false, nullable = false)
    private UUID locationId;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }

    public int getOnHand() { return onHand; }
    public void setOnHand(int onHand) { this.onHand = onHand; }

    public Instant getUpdatedAt() { return updatedAt; }
}


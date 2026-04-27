package com.smartpos.backend.stock;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProductStockId implements Serializable {
    private UUID productId;
    private UUID locationId;

    public ProductStockId() {}
    public ProductStockId(UUID productId, UUID locationId) {
        this.productId = productId;
        this.locationId = locationId;
    }

    public UUID getProductId() { return productId; }
    public UUID getLocationId() { return locationId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStockId that = (ProductStockId) o;
        return Objects.equals(productId, that.productId) && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, locationId);
    }
}


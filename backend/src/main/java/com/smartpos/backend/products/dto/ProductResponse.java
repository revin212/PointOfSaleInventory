package com.smartpos.backend.products.dto;

import com.smartpos.backend.products.ProductEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        UUID categoryId,
        String categoryName,
        UUID supplierId,
        String supplierName,
        String unit,
        BigDecimal cost,
        BigDecimal price,
        String barcode,
        int lowStockThreshold,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(ProductEntity p, String categoryName, String supplierName) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getCategoryId(),
                categoryName,
                p.getSupplierId(),
                supplierName,
                p.getUnit(),
                p.getCost(),
                p.getPrice(),
                p.getBarcode(),
                p.getLowStockThreshold(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

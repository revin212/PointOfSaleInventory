package com.smartpos.backend.sales.dto;

import com.smartpos.backend.sales.SaleItemEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        int qty,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal lineTotal
) {
    public static SaleItemResponse from(SaleItemEntity it, String sku, String name) {
        return new SaleItemResponse(it.getId(), it.getProductId(), sku, name,
                it.getQty(), it.getUnitPrice(), it.getLineDiscount(), it.getLineTotal());
    }
}

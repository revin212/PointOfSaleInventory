package com.smartpos.backend.purchases.dto;

import com.smartpos.backend.purchases.PurchaseItemEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        int qtyOrdered,
        int qtyReceivedTotal,
        int qtyOutstanding,
        BigDecimal cost
) {
    public static PurchaseItemResponse from(PurchaseItemEntity it, String sku, String name) {
        return new PurchaseItemResponse(
                it.getId(),
                it.getProductId(),
                sku,
                name,
                it.getQtyOrdered(),
                it.getQtyReceivedTotal(),
                it.getQtyOrdered() - it.getQtyReceivedTotal(),
                it.getCost()
        );
    }
}

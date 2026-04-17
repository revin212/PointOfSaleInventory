package com.smartpos.backend.stock.dto;

import java.util.UUID;

public record OnHandResponse(
        UUID productId,
        String sku,
        String name,
        UUID categoryId,
        String categoryName,
        String unit,
        int onHand,
        int lowStockThreshold,
        boolean lowStock,
        boolean active
) {}

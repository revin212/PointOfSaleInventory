package com.smartpos.backend.stock.dto;

import com.smartpos.backend.domain.enums.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        StockMovementType type,
        int qtyDelta,
        BigDecimal unitCost,
        String refType,
        UUID refId,
        String note,
        UUID createdBy,
        String createdByName,
        Instant createdAt
) {}

package com.smartpos.backend.stock.dto;

import com.smartpos.backend.domain.enums.StockMovementType;

import java.time.Instant;
import java.util.UUID;

public record AdjustmentResponse(
        UUID id,
        UUID productId,
        StockMovementType type,
        int qtyDelta,
        int onHandAfter,
        Instant createdAt
) {}

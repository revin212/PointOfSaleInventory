package com.smartpos.backend.purchases.dto;

import com.smartpos.backend.domain.enums.PurchaseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseSummaryResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        PurchaseStatus status,
        int itemCount,
        BigDecimal totalCost,
        Instant createdAt,
        Instant updatedAt
) {}

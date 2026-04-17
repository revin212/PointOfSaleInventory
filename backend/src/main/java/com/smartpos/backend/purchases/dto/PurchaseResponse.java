package com.smartpos.backend.purchases.dto;

import com.smartpos.backend.domain.enums.PurchaseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        PurchaseStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<PurchaseItemResponse> items
) {}

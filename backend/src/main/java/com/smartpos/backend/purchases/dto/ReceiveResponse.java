package com.smartpos.backend.purchases.dto;

import com.smartpos.backend.domain.enums.PurchaseStatus;

public record ReceiveResponse(
        boolean success,
        PurchaseStatus status
) {}

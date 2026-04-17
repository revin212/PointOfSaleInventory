package com.smartpos.backend.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdjustmentRequest(
        @NotNull UUID productId,
        @NotNull Integer qtyDelta,
        @Size(max = 500) String note
) {}

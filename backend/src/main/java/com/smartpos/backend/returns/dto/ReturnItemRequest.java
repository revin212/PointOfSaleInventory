package com.smartpos.backend.returns.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReturnItemRequest(
        @NotNull UUID productId,
        @Min(1) int qty
) {}


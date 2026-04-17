package com.smartpos.backend.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePurchaseRequest(
        @NotNull UUID supplierId,
        @NotEmpty @Size(max = 100) @Valid List<PurchaseItemRequest> items
) {}

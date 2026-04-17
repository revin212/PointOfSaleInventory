package com.smartpos.backend.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReceivePurchaseRequest(
        @NotEmpty @Size(max = 100) @Valid List<ReceiveItemRequest> items
) {}

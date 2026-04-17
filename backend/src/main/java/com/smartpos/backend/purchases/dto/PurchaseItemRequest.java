package com.smartpos.backend.purchases.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer qtyOrdered,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal cost
) {}

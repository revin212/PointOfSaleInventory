package com.smartpos.backend.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer qty,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal unitPrice,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal lineDiscount
) {}

package com.smartpos.backend.sales.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        @NotEmpty @Size(max = 100) @Valid List<SaleItemRequest> items,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal discount,
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal paidAmount
) {}

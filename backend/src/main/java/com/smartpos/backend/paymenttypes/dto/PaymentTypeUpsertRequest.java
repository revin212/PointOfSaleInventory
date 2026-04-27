package com.smartpos.backend.paymenttypes.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentTypeUpsertRequest(
        @NotNull PaymentMethod method,
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal adminFee,
        @NotNull Boolean active
) {}


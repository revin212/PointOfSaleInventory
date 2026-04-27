package com.smartpos.backend.shifts.dto;

import com.smartpos.backend.domain.enums.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CashMovementRequest(
        @NotNull CashMovementType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @Size(max = 500) String note
) {}


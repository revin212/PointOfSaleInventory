package com.smartpos.backend.shifts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record OpenShiftRequest(
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal openingCash,
        String note
) {}


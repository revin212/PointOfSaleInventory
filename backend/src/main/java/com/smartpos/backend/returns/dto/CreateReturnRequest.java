package com.smartpos.backend.returns.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateReturnRequest(
        @NotEmpty @Size(max = 100) @Valid List<ReturnItemRequest> items,
        @Size(max = 500) String reason,
        PaymentMethod refundMethod,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal refundAmount
) {}


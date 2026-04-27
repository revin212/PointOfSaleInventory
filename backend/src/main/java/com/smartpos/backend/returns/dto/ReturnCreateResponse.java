package com.smartpos.backend.returns.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReturnCreateResponse(
        UUID id,
        UUID saleId,
        int itemCount,
        BigDecimal refundableMax,
        BigDecimal refundedAmount,
        PaymentMethod refundMethod,
        Instant createdAt
) {}


package com.smartpos.backend.returns.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReturnDetailResponse(
        UUID id,
        UUID saleId,
        String reason,
        BigDecimal refundableMax,
        BigDecimal refundedAmount,
        PaymentMethod refundMethod,
        Instant createdAt,
        List<ReturnItemResponse> items
) {}


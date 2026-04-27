package com.smartpos.backend.paymenttypes.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentTypeResponse(
        UUID id,
        PaymentMethod method,
        String name,
        BigDecimal adminFee,
        boolean active
) {}


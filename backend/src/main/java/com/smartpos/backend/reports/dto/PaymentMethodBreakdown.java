package com.smartpos.backend.reports.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodBreakdown(
        PaymentMethod paymentMethod,
        long count,
        BigDecimal total
) {}

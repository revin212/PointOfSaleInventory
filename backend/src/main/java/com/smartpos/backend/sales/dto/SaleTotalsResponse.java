package com.smartpos.backend.sales.dto;

import java.math.BigDecimal;

public record SaleTotalsResponse(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal netAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total,
        BigDecimal paidAmount,
        BigDecimal changeAmount,
        BigDecimal adminFee
) {}

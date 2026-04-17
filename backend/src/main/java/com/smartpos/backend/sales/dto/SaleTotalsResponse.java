package com.smartpos.backend.sales.dto;

import java.math.BigDecimal;

public record SaleTotalsResponse(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal paidAmount,
        BigDecimal changeAmount
) {}

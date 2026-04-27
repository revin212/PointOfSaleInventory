package com.smartpos.backend.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailySummaryResponse(
        LocalDate date,
        long salesCount,
        BigDecimal netRevenue,
        BigDecimal taxRevenue,
        BigDecimal totalRevenue,
        long totalItemsSold,
        long cancelledCount,
        BigDecimal cancelledAmount,
        List<PaymentMethodBreakdown> byPaymentMethod
) {}

package com.smartpos.backend.sales.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleSummaryResponse(
        UUID id,
        String invoiceNo,
        UUID cashierId,
        String cashierName,
        SaleStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        int itemCount,
        Instant createdAt
) {}

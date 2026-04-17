package com.smartpos.backend.sales.dto;

import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.SaleStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleDetailResponse(
        UUID id,
        String invoiceNo,
        UUID cashierId,
        String cashierName,
        SaleStatus status,
        PaymentMethod paymentMethod,
        SaleTotalsResponse totals,
        Instant createdAt,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancelReason,
        List<SaleItemResponse> items
) {}

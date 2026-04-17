package com.smartpos.backend.sales.dto;

import com.smartpos.backend.domain.enums.SaleStatus;

import java.util.UUID;

public record SaleCreateResponse(
        UUID id,
        String invoiceNo,
        SaleStatus status,
        SaleTotalsResponse totals
) {}

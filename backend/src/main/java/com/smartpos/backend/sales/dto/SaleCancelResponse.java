package com.smartpos.backend.sales.dto;

import com.smartpos.backend.domain.enums.SaleStatus;

public record SaleCancelResponse(
        boolean success,
        SaleStatus status
) {}

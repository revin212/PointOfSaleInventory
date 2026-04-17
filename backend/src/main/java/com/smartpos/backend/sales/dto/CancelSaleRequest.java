package com.smartpos.backend.sales.dto;

import jakarta.validation.constraints.Size;

public record CancelSaleRequest(
        @Size(max = 500) String reason
) {}

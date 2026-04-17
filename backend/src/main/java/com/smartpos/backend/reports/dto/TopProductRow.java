package com.smartpos.backend.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductRow(
        UUID productId,
        String sku,
        String name,
        long qtySold,
        BigDecimal totalRevenue
) {}

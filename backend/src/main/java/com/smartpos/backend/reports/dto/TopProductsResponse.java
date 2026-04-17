package com.smartpos.backend.reports.dto;

import java.time.LocalDate;
import java.util.List;

public record TopProductsResponse(
        LocalDate from,
        LocalDate to,
        int limit,
        List<TopProductRow> rows
) {}

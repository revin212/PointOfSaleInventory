package com.smartpos.backend.shifts.dto;

import com.smartpos.backend.domain.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID openedBy,
        Instant openedAt,
        BigDecimal openingCash,
        ShiftStatus status,
        UUID closedBy,
        Instant closedAt,
        BigDecimal closingCash,
        BigDecimal expectedCash,
        BigDecimal cashDifference,
        String note
) {}


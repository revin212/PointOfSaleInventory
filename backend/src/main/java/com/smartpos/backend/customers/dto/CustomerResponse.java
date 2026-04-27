package com.smartpos.backend.customers.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String phone,
        String email,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}


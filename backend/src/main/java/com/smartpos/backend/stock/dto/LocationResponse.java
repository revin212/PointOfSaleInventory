package com.smartpos.backend.stock.dto;

import java.util.UUID;

public record LocationResponse(
        UUID id,
        String code,
        String name,
        boolean isDefault
) {}


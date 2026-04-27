package com.smartpos.backend.returns.dto;

import java.util.UUID;

public record ReturnItemResponse(
        UUID productId,
        int qty
) {}


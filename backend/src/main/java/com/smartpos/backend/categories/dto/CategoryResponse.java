package com.smartpos.backend.categories.dto;

import com.smartpos.backend.categories.CategoryEntity;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse from(CategoryEntity c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
    }
}

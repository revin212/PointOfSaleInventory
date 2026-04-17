package com.smartpos.backend.users.dto;

import com.smartpos.backend.domain.enums.Role;
import com.smartpos.backend.users.UserEntity;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserEntity u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.isActive(), u.getCreatedAt(), u.getUpdatedAt());
    }
}

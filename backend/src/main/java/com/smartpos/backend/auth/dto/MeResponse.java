package com.smartpos.backend.auth.dto;

import com.smartpos.backend.domain.enums.Role;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active
) {}

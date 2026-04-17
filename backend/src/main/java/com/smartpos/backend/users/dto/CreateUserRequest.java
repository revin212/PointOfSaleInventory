package com.smartpos.backend.users.dto;

import com.smartpos.backend.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @Email @NotBlank @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Role role,
        Boolean active
) {}

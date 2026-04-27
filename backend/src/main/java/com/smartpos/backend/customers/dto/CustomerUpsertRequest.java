package com.smartpos.backend.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerUpsertRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 40) String phone,
        @Email @Size(max = 180) String email,
        @Size(max = 500) String notes
) {}


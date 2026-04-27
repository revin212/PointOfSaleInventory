package com.smartpos.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLocationRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name
) {}


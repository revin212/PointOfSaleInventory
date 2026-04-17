package com.smartpos.backend.suppliers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierUpsertRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 40)  String phone,
        @Size(max = 500) String address
) {}

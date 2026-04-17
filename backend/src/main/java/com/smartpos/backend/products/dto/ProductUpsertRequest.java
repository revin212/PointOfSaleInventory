package com.smartpos.backend.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpsertRequest(
        @NotBlank @Size(max = 60)  String sku,
        @NotBlank @Size(max = 200) String name,
        UUID categoryId,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal cost,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal price,
        @Size(max = 80) String barcode,
        @NotNull @Min(0) Integer lowStockThreshold,
        @NotNull Boolean active
) {}

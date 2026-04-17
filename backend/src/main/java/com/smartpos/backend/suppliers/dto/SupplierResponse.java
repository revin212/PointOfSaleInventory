package com.smartpos.backend.suppliers.dto;

import com.smartpos.backend.suppliers.SupplierEntity;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String phone,
        String address,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierResponse from(SupplierEntity s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getAddress(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}

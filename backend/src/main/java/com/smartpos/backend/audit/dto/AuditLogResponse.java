package com.smartpos.backend.audit.dto;

import com.smartpos.backend.audit.AuditLogEntity;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String userName,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLogEntity a, String userName) {
        return new AuditLogResponse(
                a.getId(), a.getUserId(), userName,
                a.getAction(), a.getEntityType(), a.getEntityId(),
                a.getMetadata(), a.getCreatedAt()
        );
    }
}

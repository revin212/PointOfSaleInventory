package com.smartpos.backend.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpos.backend.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records an audit entry inside the caller's transaction.
     * Callers can pass metadata as a Map; it is serialized to JSON string.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, UUID entityId, Map<String, ?> metadata) {
        record(action, entityType, entityId, SecurityUtils.currentUserIdOrNull(), serialize(metadata));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, UUID entityId, UUID userId, String metadataJson) {
        AuditLogEntity a = new AuditLogEntity();
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setUserId(userId);
        a.setMetadata(metadataJson);
        repository.save(a);
    }

    private String serialize(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Audit metadata serialization failed: {}", e.getMessage());
            return null;
        }
    }
}

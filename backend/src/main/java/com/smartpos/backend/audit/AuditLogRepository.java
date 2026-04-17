package com.smartpos.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:action IS NULL OR a.action = :action)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to   IS NULL OR a.createdAt <  :to)
            """)
    Page<AuditLogEntity> search(@Param("entityType") String entityType,
                                @Param("action") String action,
                                @Param("userId") UUID userId,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                Pageable pageable);
}

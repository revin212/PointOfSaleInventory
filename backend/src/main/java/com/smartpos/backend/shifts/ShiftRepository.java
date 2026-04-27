package com.smartpos.backend.shifts;

import com.smartpos.backend.domain.enums.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShiftRepository extends JpaRepository<ShiftEntity, UUID> {

    @Query("""
            SELECT s FROM ShiftEntity s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:openedBy IS NULL OR s.openedBy = :openedBy)
            """)
    Page<ShiftEntity> search(@Param("status") ShiftStatus status,
                             @Param("openedBy") UUID openedBy,
                             Pageable pageable);

    Optional<ShiftEntity> findFirstByOpenedByAndStatusOrderByOpenedAtDesc(UUID openedBy, ShiftStatus status);
}


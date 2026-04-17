package com.smartpos.backend.purchases;

import com.smartpos.backend.domain.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {

    boolean existsBySupplierId(UUID supplierId);

    @Query("""
            SELECT p FROM PurchaseEntity p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:supplierId IS NULL OR p.supplierId = :supplierId)
            """)
    Page<PurchaseEntity> search(@Param("status") PurchaseStatus status,
                                @Param("supplierId") UUID supplierId,
                                Pageable pageable);
}

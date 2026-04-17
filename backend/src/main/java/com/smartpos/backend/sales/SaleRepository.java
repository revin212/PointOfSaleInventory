package com.smartpos.backend.sales;

import com.smartpos.backend.domain.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<SaleEntity, UUID> {

    @Query("""
            SELECT s FROM SaleEntity s
            WHERE (:from IS NULL OR s.createdAt >= :from)
              AND (:to   IS NULL OR s.createdAt <  :to)
              AND (:cashierId IS NULL OR s.cashierId = :cashierId)
              AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)
            """)
    Page<SaleEntity> search(@Param("from") Instant from,
                            @Param("to") Instant to,
                            @Param("cashierId") UUID cashierId,
                            @Param("paymentMethod") PaymentMethod paymentMethod,
                            Pageable pageable);
}

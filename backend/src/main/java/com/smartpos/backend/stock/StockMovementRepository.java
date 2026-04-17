package com.smartpos.backend.stock;

import com.smartpos.backend.domain.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    @Query("SELECT COALESCE(SUM(m.qtyDelta), 0) FROM StockMovementEntity m WHERE m.productId = :productId")
    int sumQtyByProduct(@Param("productId") UUID productId);

    @Query("""
            SELECT m.productId, COALESCE(SUM(m.qtyDelta), 0)
              FROM StockMovementEntity m
             WHERE m.productId IN :productIds
             GROUP BY m.productId
            """)
    List<Object[]> sumQtyByProducts(@Param("productIds") Collection<UUID> productIds);

    @Query("""
            SELECT m FROM StockMovementEntity m
            WHERE (:productId IS NULL OR m.productId = :productId)
              AND (:type IS NULL OR m.type = :type)
              AND (:from IS NULL OR m.createdAt >= :from)
              AND (:to   IS NULL OR m.createdAt <  :to)
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    Page<StockMovementEntity> search(@Param("productId") UUID productId,
                                     @Param("type") StockMovementType type,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}

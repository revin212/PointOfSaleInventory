package com.smartpos.backend.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository
        extends JpaRepository<StockMovementEntity, UUID>,
                JpaSpecificationExecutor<StockMovementEntity> {

    @Query("SELECT COALESCE(SUM(m.qtyDelta), 0) FROM StockMovementEntity m WHERE m.productId = :productId")
    int sumQtyByProduct(@Param("productId") UUID productId);

    @Query("""
            SELECT m.productId, COALESCE(SUM(m.qtyDelta), 0)
              FROM StockMovementEntity m
             WHERE m.productId IN :productIds
             GROUP BY m.productId
            """)
    List<Object[]> sumQtyByProducts(@Param("productIds") Collection<UUID> productIds);
}

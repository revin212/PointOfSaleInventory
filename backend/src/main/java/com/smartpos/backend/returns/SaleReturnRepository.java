package com.smartpos.backend.returns;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SaleReturnRepository extends JpaRepository<SaleReturnEntity, UUID> {

    @Query("""
            SELECT i.productId, COALESCE(SUM(i.qty), 0)
            FROM SaleReturnItemEntity i
            WHERE i.saleReturn.saleId = :saleId
            GROUP BY i.productId
            """)
    List<Object[]> sumReturnedQtyByProduct(@Param("saleId") UUID saleId);

    @Query("""
            SELECT COALESCE(SUM(r.refundAmount), 0)
            FROM SaleReturnEntity r
            WHERE r.saleId = :saleId
            """)
    BigDecimal sumRefundedAmount(@Param("saleId") UUID saleId);

    List<SaleReturnEntity> findAllBySaleIdOrderByCreatedAtDesc(UUID saleId);
}


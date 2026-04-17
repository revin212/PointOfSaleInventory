package com.smartpos.backend.reports;

import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.SaleStatus;
import com.smartpos.backend.sales.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<SaleEntity, UUID> {

    @Query("""
            SELECT s.paymentMethod, COUNT(s), COALESCE(SUM(s.total), 0)
            FROM SaleEntity s
            WHERE s.status = :status
              AND s.createdAt >= :from
              AND s.createdAt <  :to
            GROUP BY s.paymentMethod
            """)
    List<Object[]> salesAggregateByPaymentMethod(@Param("status") SaleStatus status,
                                                 @Param("from") Instant from,
                                                 @Param("to") Instant to);

    @Query("""
            SELECT COUNT(s), COALESCE(SUM(s.total), 0)
            FROM SaleEntity s
            WHERE s.status = :status
              AND s.createdAt >= :from
              AND s.createdAt <  :to
            """)
    List<Object[]> salesTotals(@Param("status") SaleStatus status,
                               @Param("from") Instant from,
                               @Param("to") Instant to);

    @Query("""
            SELECT COUNT(s), COALESCE(SUM(s.total), 0)
            FROM SaleEntity s
            WHERE s.status = :status
              AND s.cancelledAt >= :from
              AND s.cancelledAt <  :to
            """)
    List<Object[]> cancelledTotals(@Param("status") SaleStatus status,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query("""
            SELECT COALESCE(SUM(i.qty), 0)
            FROM SaleItemEntity i
            WHERE i.sale.status = :status
              AND i.sale.createdAt >= :from
              AND i.sale.createdAt <  :to
            """)
    Long totalItemsSold(@Param("status") SaleStatus status,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

    @Query("""
            SELECT i.productId, SUM(i.qty), COALESCE(SUM(i.lineTotal), 0)
            FROM SaleItemEntity i
            WHERE i.sale.status = :status
              AND i.sale.createdAt >= :from
              AND i.sale.createdAt <  :to
            GROUP BY i.productId
            ORDER BY SUM(i.qty) DESC
            """)
    List<Object[]> topProducts(@Param("status") SaleStatus status,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               org.springframework.data.domain.Pageable pageable);
}

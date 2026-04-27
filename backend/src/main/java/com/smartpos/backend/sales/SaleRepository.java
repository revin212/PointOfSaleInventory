package com.smartpos.backend.sales;

import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<SaleEntity, UUID>, JpaSpecificationExecutor<SaleEntity> {

    @Query("""
            SELECT COALESCE(SUM(s.total), 0)
            FROM SaleEntity s
            WHERE s.shiftId = :shiftId
              AND s.status = :status
              AND s.paymentMethod = :method
            """)
    BigDecimal sumTotalByShiftAndPayment(@Param("shiftId") UUID shiftId,
                                        @Param("status") SaleStatus status,
                                        @Param("method") PaymentMethod method);
}

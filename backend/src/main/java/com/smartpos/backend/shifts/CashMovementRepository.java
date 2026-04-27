package com.smartpos.backend.shifts;

import com.smartpos.backend.domain.enums.CashMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CashMovementRepository extends JpaRepository<CashMovementEntity, UUID> {

    @Query("""
            SELECT m.type, COALESCE(SUM(m.amount), 0)
            FROM CashMovementEntity m
            WHERE m.shiftId = :shiftId
            GROUP BY m.type
            """)
    List<Object[]> totalsByType(@Param("shiftId") UUID shiftId);

    default BigDecimal netCashDelta(UUID shiftId) {
        BigDecimal inTotal = BigDecimal.ZERO;
        BigDecimal outTotal = BigDecimal.ZERO;
        for (Object[] r : totalsByType(shiftId)) {
            CashMovementType t = (CashMovementType) r[0];
            BigDecimal amt = (BigDecimal) r[1];
            if (t == CashMovementType.IN) inTotal = amt;
            if (t == CashMovementType.OUT) outTotal = amt;
        }
        return inTotal.subtract(outTotal);
    }
}


package com.smartpos.backend.stock;

import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.common.error.ErrorCode;
import com.smartpos.backend.domain.enums.StockMovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Single write-path for the append-only stock ledger.
 * Never mutates historical rows; cancellations emit compensating rows.
 */
@Service
public class StockLedgerService {

    private final StockMovementRepository repository;

    public StockLedgerService(StockMovementRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public int onHand(UUID productId) {
        return repository.sumQtyByProduct(productId);
    }

    @Transactional
    public StockMovementEntity record(StockMovementType type,
                                      UUID productId,
                                      int qtyDelta,
                                      BigDecimal unitCost,
                                      String refType,
                                      UUID refId,
                                      String note,
                                      UUID createdBy) {
        if (qtyDelta == 0) {
            throw new BusinessRuleException("qty_delta must be non-zero");
        }
        // Validate sign by movement type
        switch (type) {
            case PURCHASE_RECEIVE, SALE_CANCEL -> {
                if (qtyDelta <= 0) {
                    throw new BusinessRuleException(type + " must produce a positive qty_delta");
                }
            }
            case SALE -> {
                if (qtyDelta >= 0) {
                    throw new BusinessRuleException("SALE must produce a negative qty_delta");
                }
            }
            case ADJUSTMENT -> {
                // sign free; sufficient-stock check done by caller
            }
        }

        if (qtyDelta < 0) {
            int current = repository.sumQtyByProduct(productId);
            if (current + qtyDelta < 0) {
                throw new BusinessRuleException(ErrorCode.INSUFFICIENT_STOCK,
                        "Insufficient stock for product " + productId
                                + " (on-hand=" + current + ", requested=" + (-qtyDelta) + ")");
            }
        }

        StockMovementEntity m = new StockMovementEntity();
        m.setProductId(productId);
        m.setType(type);
        m.setQtyDelta(qtyDelta);
        m.setUnitCost(unitCost);
        m.setRefType(refType);
        m.setRefId(refId);
        m.setNote(note);
        m.setCreatedBy(createdBy);
        return repository.save(m);
    }
}

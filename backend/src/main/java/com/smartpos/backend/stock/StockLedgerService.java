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

    public static final UUID DEFAULT_LOCATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final StockMovementRepository repository;
    private final ProductStockRepository productStockRepository;

    public StockLedgerService(StockMovementRepository repository,
                              ProductStockRepository productStockRepository) {
        this.repository = repository;
        this.productStockRepository = productStockRepository;
    }

    @Transactional(readOnly = true)
    public int onHand(UUID productId) {
        return onHand(DEFAULT_LOCATION_ID, productId);
    }

    @Transactional(readOnly = true)
    public int onHand(UUID locationId, UUID productId) {
        return productStockRepository.onHand(productId, locationId).orElse(0);
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
        return record(type, DEFAULT_LOCATION_ID, productId, qtyDelta, unitCost, refType, refId, note, createdBy);
    }

    @Transactional
    public StockMovementEntity record(StockMovementType type,
                                      UUID locationId,
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
            case RETURN -> {
                if (qtyDelta <= 0) {
                    throw new BusinessRuleException("RETURN must produce a positive qty_delta");
                }
            }
            case ADJUSTMENT -> {
                // sign free; sufficient-stock check done by caller
            }
        }

        ProductStockEntity stock = productStockRepository.findForUpdate(productId, locationId)
                .orElseGet(() -> {
                    ProductStockEntity s = new ProductStockEntity();
                    s.setProductId(productId);
                    s.setLocationId(locationId);
                    s.setOnHand(0);
                    return productStockRepository.save(s);
                });
        int after = stock.getOnHand() + qtyDelta;
        if (after < 0) {
            throw new BusinessRuleException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock for product " + productId
                            + " (on-hand=" + stock.getOnHand() + ", requested=" + (-qtyDelta) + ")");
        }

        StockMovementEntity m = new StockMovementEntity();
        m.setProductId(productId);
        m.setLocationId(locationId);
        m.setType(type);
        m.setQtyDelta(qtyDelta);
        m.setUnitCost(unitCost);
        m.setRefType(refType);
        m.setRefId(refId);
        m.setNote(note);
        m.setCreatedBy(createdBy);
        StockMovementEntity saved = repository.save(m);

        stock.setOnHand(after);
        productStockRepository.save(stock);

        return saved;
    }
}

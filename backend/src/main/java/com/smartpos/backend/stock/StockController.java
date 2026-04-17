package com.smartpos.backend.stock;

import com.smartpos.backend.audit.AuditAction;
import com.smartpos.backend.audit.AuditEntityType;
import com.smartpos.backend.audit.AuditService;
import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.domain.enums.StockMovementType;
import com.smartpos.backend.products.ProductEntity;
import com.smartpos.backend.security.SecurityUtils;
import com.smartpos.backend.stock.dto.AdjustmentRequest;
import com.smartpos.backend.stock.dto.AdjustmentResponse;
import com.smartpos.backend.stock.dto.OnHandResponse;
import com.smartpos.backend.stock.dto.StockMovementResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockQueryService stockQueryService;
    private final StockLedgerService stockLedger;
    private final AuditService auditService;

    public StockController(StockQueryService stockQueryService,
                           StockLedgerService stockLedger,
                           AuditService auditService) {
        this.stockQueryService = stockQueryService;
        this.stockLedger = stockLedger;
        this.auditService = auditService;
    }

    @GetMapping("/on-hand")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE','CASHIER')")
    public PageResponse<OnHandResponse> onHand(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean lowOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return PageResponse.from(stockQueryService.onHand(query, categoryId, lowOnly,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.ASC, "name"))));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public PageResponse<StockMovementResponse> movements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Instant fromI = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toI   = to   == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return PageResponse.from(stockQueryService.movements(productId, type, fromI, toI,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public AdjustmentResponse adjust(@Valid @RequestBody AdjustmentRequest req) {
        if (req.qtyDelta() == 0) {
            throw new BusinessRuleException("qtyDelta must be non-zero");
        }
        ProductEntity product = stockQueryService.requireProduct(req.productId());
        UUID actor = SecurityUtils.currentUserId();
        StockMovementEntity movement = stockLedger.record(
                StockMovementType.ADJUSTMENT,
                product.getId(),
                req.qtyDelta(),
                null,
                "ADJUSTMENT",
                null,
                req.note(),
                actor
        );
        int after = stockLedger.onHand(product.getId());
        auditService.record(AuditAction.STOCK_ADJUSTMENT, AuditEntityType.STOCK, movement.getId(),
                Map.of("productId", product.getId().toString(),
                        "qtyDelta", req.qtyDelta(),
                        "onHandAfter", after,
                        "note", req.note() == null ? "" : req.note()));
        return new AdjustmentResponse(
                movement.getId(), product.getId(), movement.getType(),
                movement.getQtyDelta(), after, movement.getCreatedAt()
        );
    }
}

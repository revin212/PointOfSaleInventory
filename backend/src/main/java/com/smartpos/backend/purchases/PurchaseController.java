package com.smartpos.backend.purchases;

import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.domain.enums.PurchaseStatus;
import com.smartpos.backend.purchases.dto.CreatePurchaseRequest;
import com.smartpos.backend.purchases.dto.PurchaseResponse;
import com.smartpos.backend.purchases.dto.PurchaseSummaryResponse;
import com.smartpos.backend.purchases.dto.ReceivePurchaseRequest;
import com.smartpos.backend.purchases.dto.ReceiveResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public PageResponse<PurchaseSummaryResponse> list(
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return PageResponse.from(purchaseService.list(status, supplierId,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public PurchaseResponse get(@PathVariable UUID id) {
        return purchaseService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody CreatePurchaseRequest request) {
        PurchaseResponse created = purchaseService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/purchases/" + created.id())).body(created);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public ReceiveResponse receive(@PathVariable UUID id, @Valid @RequestBody ReceivePurchaseRequest request) {
        return purchaseService.receive(id, request);
    }
}

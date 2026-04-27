package com.smartpos.backend.returns;

import com.smartpos.backend.returns.dto.CreateReturnRequest;
import com.smartpos.backend.returns.dto.ReturnCreateResponse;
import com.smartpos.backend.returns.dto.ReturnDetailResponse;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/{saleId}/returns")
public class SaleReturnController {

    private final SaleReturnService service;

    public SaleReturnController(SaleReturnService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public ResponseEntity<ReturnCreateResponse> create(@PathVariable UUID saleId,
                                                       @Valid @RequestBody CreateReturnRequest request) {
        ReturnCreateResponse created = service.create(saleId, request);
        return ResponseEntity.created(URI.create("/api/v1/sales/" + saleId + "/returns/" + created.id()))
                .body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    @Transactional(readOnly = true)
    public List<ReturnDetailResponse> list(@PathVariable UUID saleId) {
        return service.list(saleId);
    }

    @GetMapping("/{returnId}")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    @Transactional(readOnly = true)
    public ReturnDetailResponse get(@PathVariable UUID saleId, @PathVariable UUID returnId) {
        return service.get(saleId, returnId);
    }
}


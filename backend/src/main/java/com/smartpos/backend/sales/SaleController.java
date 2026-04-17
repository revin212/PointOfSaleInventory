package com.smartpos.backend.sales;

import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.sales.dto.CancelSaleRequest;
import com.smartpos.backend.sales.dto.CreateSaleRequest;
import com.smartpos.backend.sales.dto.SaleCancelResponse;
import com.smartpos.backend.sales.dto.SaleCreateResponse;
import com.smartpos.backend.sales.dto.SaleDetailResponse;
import com.smartpos.backend.sales.dto.SaleSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public PageResponse<SaleSummaryResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID cashierId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Instant fromInstant = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant   = to   == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return PageResponse.from(saleService.list(fromInstant, toInstant, cashierId, paymentMethod,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public SaleDetailResponse get(@PathVariable UUID id) {
        return saleService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public ResponseEntity<SaleCreateResponse> create(@Valid @RequestBody CreateSaleRequest request) {
        SaleCreateResponse created = saleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sales/" + created.id())).body(created);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('OWNER')")
    public SaleCancelResponse cancel(@PathVariable UUID id, @Valid @RequestBody(required = false) CancelSaleRequest request) {
        return saleService.cancel(id, request);
    }
}

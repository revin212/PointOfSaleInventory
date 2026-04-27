package com.smartpos.backend.paymenttypes;

import com.smartpos.backend.paymenttypes.dto.PaymentTypeResponse;
import com.smartpos.backend.paymenttypes.dto.PaymentTypeUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-types")
public class PaymentTypeController {

    private final PaymentTypeService service;

    public PaymentTypeController(PaymentTypeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER','WAREHOUSE')")
    public List<PaymentTypeResponse> listForPos() {
        return service.listActiveForPos();
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('OWNER')")
    public List<PaymentTypeResponse> listManage() {
        return service.listAll();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER')")
    public PaymentTypeResponse update(@PathVariable UUID id, @Valid @RequestBody PaymentTypeUpsertRequest req) {
        return service.update(id, req);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER')")
    public PaymentTypeResponse upsert(@Valid @RequestBody PaymentTypeUpsertRequest req) {
        return service.upsert(req);
    }
}


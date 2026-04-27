package com.smartpos.backend.customers;

import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.customers.dto.CustomerResponse;
import com.smartpos.backend.customers.dto.CustomerUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('OWNER','CASHIER')")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return PageResponse.from(service.list(query,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.ASC, "name"))));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerUpsertRequest request) {
        CustomerResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerUpsertRequest request) {
        return service.update(id, request);
    }
}


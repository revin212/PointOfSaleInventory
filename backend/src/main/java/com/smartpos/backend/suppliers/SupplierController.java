package com.smartpos.backend.suppliers;

import com.smartpos.backend.auth.dto.SimpleResponse;
import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.suppliers.dto.SupplierResponse;
import com.smartpos.backend.suppliers.dto.SupplierUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public PageResponse<SupplierResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return PageResponse.from(supplierService.list(query,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.ASC, "name"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public SupplierResponse get(@PathVariable UUID id) {
        return supplierService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierUpsertRequest request) {
        SupplierResponse created = supplierService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierUpsertRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public SimpleResponse delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return SimpleResponse.ok();
    }
}

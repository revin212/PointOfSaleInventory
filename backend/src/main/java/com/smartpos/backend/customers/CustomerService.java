package com.smartpos.backend.customers;

import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.customers.dto.CustomerResponse;
import com.smartpos.backend.customers.dto.CustomerUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? "" : query.trim().toLowerCase();
        return repository.search(q, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return toResponse(repository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found")));
    }

    @Transactional
    public CustomerResponse create(CustomerUpsertRequest req) {
        CustomerEntity c = new CustomerEntity();
        apply(c, req);
        return toResponse(repository.save(c));
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpsertRequest req) {
        CustomerEntity c = repository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
        apply(c, req);
        return toResponse(repository.save(c));
    }

    private void apply(CustomerEntity c, CustomerUpsertRequest req) {
        c.setName(req.name().trim());
        c.setPhone(req.phone() == null ? null : req.phone().trim());
        c.setEmail(req.email() == null ? null : req.email().trim());
        c.setNotes(req.notes() == null ? null : req.notes().trim());
    }

    private CustomerResponse toResponse(CustomerEntity c) {
        return new CustomerResponse(
                c.getId(),
                c.getName(),
                c.getPhone(),
                c.getEmail(),
                c.getNotes(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}


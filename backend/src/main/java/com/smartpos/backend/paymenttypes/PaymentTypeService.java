package com.smartpos.backend.paymenttypes;

import com.smartpos.backend.common.error.ConflictException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.paymenttypes.dto.PaymentTypeResponse;
import com.smartpos.backend.paymenttypes.dto.PaymentTypeUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentTypeService {

    private final PaymentTypeRepository repo;

    public PaymentTypeService(PaymentTypeRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<PaymentTypeResponse> listActiveForPos() {
        return repo.findAll().stream()
                .filter(PaymentTypeEntity::isActive)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentTypeResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentTypeEntity require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Payment type not found"));
    }

    @Transactional
    public PaymentTypeResponse upsert(PaymentTypeUpsertRequest req) {
        PaymentMethod method = req.method();
        PaymentTypeEntity entity = repo.findByMethod(method).orElseGet(PaymentTypeEntity::new);
        entity.setMethod(method);
        entity.setName(req.name());
        entity.setAdminFee(req.adminFee() == null ? BigDecimal.ZERO : req.adminFee());
        entity.setActive(Boolean.TRUE.equals(req.active()));
        PaymentTypeEntity saved = repo.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public PaymentTypeResponse update(UUID id, PaymentTypeUpsertRequest req) {
        PaymentTypeEntity entity = require(id);
        if (req.method() != null && req.method() != entity.getMethod()) {
            repo.findByMethod(req.method()).ifPresent(existing -> {
                if (!existing.getId().equals(entity.getId())) {
                    throw new ConflictException("Payment method already exists: " + req.method());
                }
            });
            entity.setMethod(req.method());
        }
        entity.setName(req.name());
        entity.setAdminFee(req.adminFee() == null ? BigDecimal.ZERO : req.adminFee());
        entity.setActive(Boolean.TRUE.equals(req.active()));
        PaymentTypeEntity saved = repo.save(entity);
        return toResponse(saved);
    }

    private PaymentTypeResponse toResponse(PaymentTypeEntity e) {
        return new PaymentTypeResponse(e.getId(), e.getMethod(), e.getName(), e.getAdminFee(), e.isActive());
    }
}


package com.smartpos.backend.suppliers;

import com.smartpos.backend.common.error.ConflictException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.purchases.PurchaseRepository;
import com.smartpos.backend.suppliers.dto.SupplierResponse;
import com.smartpos.backend.suppliers.dto.SupplierUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;

    public SupplierService(SupplierRepository supplierRepository, PurchaseRepository purchaseRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> list(String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return supplierRepository.search(q, pageable).map(SupplierResponse::from);
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(UUID id) {
        return SupplierResponse.from(findOrThrow(id));
    }

    @Transactional
    public SupplierResponse create(SupplierUpsertRequest req) {
        SupplierEntity s = new SupplierEntity();
        apply(s, req);
        return SupplierResponse.from(supplierRepository.save(s));
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierUpsertRequest req) {
        SupplierEntity s = findOrThrow(id);
        apply(s, req);
        return SupplierResponse.from(supplierRepository.save(s));
    }

    @Transactional
    public void delete(UUID id) {
        SupplierEntity s = findOrThrow(id);
        if (purchaseRepository.existsBySupplierId(s.getId())) {
            throw new ConflictException("Supplier is referenced by one or more purchases");
        }
        supplierRepository.delete(s);
    }

    private void apply(SupplierEntity s, SupplierUpsertRequest req) {
        s.setName(req.name().trim());
        s.setPhone(normalize(req.phone()));
        s.setAddress(normalize(req.address()));
    }

    private String normalize(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SupplierEntity findOrThrow(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
    }
}

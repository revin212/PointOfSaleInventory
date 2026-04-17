package com.smartpos.backend.purchases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, UUID>, JpaSpecificationExecutor<PurchaseEntity> {

    boolean existsBySupplierId(UUID supplierId);
}

package com.smartpos.backend.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SaleRepository extends JpaRepository<SaleEntity, UUID>, JpaSpecificationExecutor<SaleEntity> {
}

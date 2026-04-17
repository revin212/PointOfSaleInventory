package com.smartpos.backend.purchases;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceiptEntity, UUID> {
}

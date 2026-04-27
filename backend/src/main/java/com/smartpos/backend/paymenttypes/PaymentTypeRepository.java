package com.smartpos.backend.paymenttypes;

import com.smartpos.backend.domain.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTypeRepository extends JpaRepository<PaymentTypeEntity, UUID> {
    Optional<PaymentTypeEntity> findByMethod(PaymentMethod method);
}


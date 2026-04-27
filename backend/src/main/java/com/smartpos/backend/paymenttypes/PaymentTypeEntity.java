package com.smartpos.backend.paymenttypes;

import com.smartpos.backend.domain.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_types")
public class PaymentTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20, unique = true)
    private PaymentMethod method;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "admin_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal adminFee = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAdminFee() { return adminFee; }
    public void setAdminFee(BigDecimal adminFee) { this.adminFee = adminFee; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}


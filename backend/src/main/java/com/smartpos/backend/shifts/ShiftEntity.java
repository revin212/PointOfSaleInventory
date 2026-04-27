package com.smartpos.backend.shifts;

import com.smartpos.backend.domain.enums.ShiftStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shifts")
@EntityListeners(AuditingEntityListener.class)
public class ShiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @CreatedDate
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "opening_cash", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closing_cash", precision = 18, scale = 2)
    private BigDecimal closingCash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(name = "note", length = 500)
    private String note;

    public UUID getId() { return id; }

    public UUID getOpenedBy() { return openedBy; }
    public void setOpenedBy(UUID openedBy) { this.openedBy = openedBy; }

    public Instant getOpenedAt() { return openedAt; }

    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }

    public UUID getClosedBy() { return closedBy; }
    public void setClosedBy(UUID closedBy) { this.closedBy = closedBy; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public BigDecimal getClosingCash() { return closingCash; }
    public void setClosingCash(BigDecimal closingCash) { this.closingCash = closingCash; }

    public ShiftStatus getStatus() { return status; }
    public void setStatus(ShiftStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}


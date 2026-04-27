package com.smartpos.backend.shifts;

import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.ShiftStatus;
import com.smartpos.backend.domain.enums.SaleStatus;
import com.smartpos.backend.security.SecurityUtils;
import com.smartpos.backend.sales.SaleRepository;
import com.smartpos.backend.shifts.dto.CashMovementRequest;
import com.smartpos.backend.shifts.dto.CloseShiftRequest;
import com.smartpos.backend.shifts.dto.OpenShiftRequest;
import com.smartpos.backend.shifts.dto.ShiftResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final CashMovementRepository cashMovementRepository;
    private final SaleRepository saleRepository;

    public ShiftService(ShiftRepository shiftRepository,
                        CashMovementRepository cashMovementRepository,
                        SaleRepository saleRepository) {
        this.shiftRepository = shiftRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.saleRepository = saleRepository;
    }

    @Transactional
    public ShiftResponse open(OpenShiftRequest req) {
        UUID actor = SecurityUtils.currentUserId();
        shiftRepository.findFirstByOpenedByAndStatusOrderByOpenedAtDesc(actor, ShiftStatus.OPEN)
                .ifPresent(s -> { throw new BusinessRuleException("You already have an open shift"); });

        ShiftEntity s = new ShiftEntity();
        s.setOpenedBy(actor);
        s.setStatus(ShiftStatus.OPEN);
        s.setOpeningCash(req.openingCash() == null ? BigDecimal.ZERO : req.openingCash());
        s.setNote(req.note());
        ShiftEntity saved = shiftRepository.save(s);
        return toResponse(saved);
    }

    @Transactional
    public ShiftResponse close(UUID shiftId, CloseShiftRequest req, boolean ownerOverride) {
        ShiftEntity s = shiftRepository.findById(shiftId).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (s.getStatus() == ShiftStatus.CLOSED) {
            throw new BusinessRuleException("Shift already closed");
        }
        UUID actor = SecurityUtils.currentUserId();
        if (!ownerOverride && !actor.equals(s.getOpenedBy())) {
            throw new BusinessRuleException("Only the shift owner can close this shift");
        }
        s.setStatus(ShiftStatus.CLOSED);
        s.setClosedBy(actor);
        s.setClosedAt(Instant.now());
        s.setClosingCash(req.closingCash());
        s.setNote(req.note());
        ShiftEntity saved = shiftRepository.save(s);
        return toResponse(saved);
    }

    @Transactional
    public void addCashMovement(UUID shiftId, CashMovementRequest req, boolean ownerOverride) {
        ShiftEntity s = shiftRepository.findById(shiftId).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (s.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessRuleException("Cash movements are only allowed on OPEN shifts");
        }
        UUID actor = SecurityUtils.currentUserId();
        if (!ownerOverride && !actor.equals(s.getOpenedBy())) {
            throw new BusinessRuleException("Only the shift owner can add cash movements");
        }
        CashMovementEntity m = new CashMovementEntity();
        m.setShiftId(shiftId);
        m.setType(req.type());
        m.setAmount(req.amount());
        m.setNote(req.note());
        m.setCreatedBy(actor);
        cashMovementRepository.save(m);
    }

    @Transactional(readOnly = true)
    public Page<ShiftResponse> list(ShiftStatus status, UUID openedBy, Pageable pageable) {
        return shiftRepository.search(status, openedBy, pageable).map(this::toResponse);
    }

    private ShiftResponse toResponse(ShiftEntity s) {
        BigDecimal cashSales = saleRepository.sumTotalByShiftAndPayment(s.getId(), SaleStatus.COMPLETED, PaymentMethod.CASH);
        BigDecimal expected = s.getOpeningCash()
                .add(cashMovementRepository.netCashDelta(s.getId()))
                .add(cashSales);
        BigDecimal diff = s.getClosingCash() == null ? null : s.getClosingCash().subtract(expected);
        return new ShiftResponse(
                s.getId(),
                s.getOpenedBy(),
                s.getOpenedAt(),
                s.getOpeningCash(),
                s.getStatus(),
                s.getClosedBy(),
                s.getClosedAt(),
                s.getClosingCash(),
                expected,
                diff,
                s.getNote()
        );
    }
}


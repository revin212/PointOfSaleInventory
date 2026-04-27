package com.smartpos.backend.shifts;

import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.domain.enums.ShiftStatus;
import com.smartpos.backend.security.SecurityUtils;
import com.smartpos.backend.shifts.dto.CashMovementRequest;
import com.smartpos.backend.shifts.dto.CloseShiftRequest;
import com.smartpos.backend.shifts.dto.OpenShiftRequest;
import com.smartpos.backend.shifts.dto.ShiftResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService service;

    public ShiftController(ShiftService service) {
        this.service = service;
    }

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public ResponseEntity<ShiftResponse> open(@Valid @RequestBody OpenShiftRequest request) {
        ShiftResponse created = service.open(request);
        return ResponseEntity.created(URI.create("/api/v1/shifts/" + created.id())).body(created);
    }

    @PostMapping("/{id}/cash-movements")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public ResponseEntity<Void> addCashMovement(@PathVariable UUID id, @Valid @RequestBody CashMovementRequest request) {
        boolean ownerOverride = SecurityUtils.requirePrincipal().getRole().name().equals("OWNER");
        service.addCashMovement(id, request, ownerOverride);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public ShiftResponse close(@PathVariable UUID id, @Valid @RequestBody CloseShiftRequest request) {
        boolean ownerOverride = SecurityUtils.requirePrincipal().getRole().name().equals("OWNER");
        return service.close(id, request, ownerOverride);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER')")
    public PageResponse<ShiftResponse> list(
            @RequestParam(required = false) ShiftStatus status,
            @RequestParam(required = false) UUID openedBy,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        boolean isOwner = SecurityUtils.requirePrincipal().getRole().name().equals("OWNER");
        UUID openedByFilter = isOwner ? openedBy : SecurityUtils.currentUserId();
        return PageResponse.from(service.list(status, openedByFilter,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.DESC, "openedAt"))));
    }
}


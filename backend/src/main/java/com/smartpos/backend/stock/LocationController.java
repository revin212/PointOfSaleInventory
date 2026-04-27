package com.smartpos.backend.stock;

import com.smartpos.backend.common.error.ConflictException;
import com.smartpos.backend.stock.dto.CreateLocationRequest;
import com.smartpos.backend.stock.dto.LocationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationRepository repository;

    public LocationController(LocationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    @Transactional(readOnly = true)
    public List<LocationResponse> list() {
        return repository.findAll().stream()
                .map(l -> new LocationResponse(l.getId(), l.getCode(), l.getName(), l.isDefault()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody CreateLocationRequest req) {
        String code = req.code().trim();
        String name = req.name().trim();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Location code already exists");
        }
        LocationEntity l = new LocationEntity();
        l.setId(UUID.randomUUID());
        l.setCode(code);
        l.setName(name);
        l.setDefault(false);
        LocationEntity saved = repository.save(l);
        LocationResponse body = new LocationResponse(saved.getId(), saved.getCode(), saved.getName(), saved.isDefault());
        return ResponseEntity.created(URI.create("/api/v1/locations/" + saved.getId())).body(body);
    }
}


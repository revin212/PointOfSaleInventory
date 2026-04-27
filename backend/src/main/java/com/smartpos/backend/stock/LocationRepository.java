package com.smartpos.backend.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    @Query("SELECT l FROM LocationEntity l WHERE l.isDefault = true")
    Optional<LocationEntity> findDefault();

    boolean existsByCodeIgnoreCase(String code);
}


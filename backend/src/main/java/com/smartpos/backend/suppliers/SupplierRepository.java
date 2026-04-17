package com.smartpos.backend.suppliers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID> {

    @Query("""
            SELECT s FROM SupplierEntity s
            WHERE (LOWER(s.name) LIKE CONCAT('%', :query, '%')
                    OR LOWER(COALESCE(s.phone, '')) LIKE CONCAT('%', :query, '%'))
            """)
    Page<SupplierEntity> search(@Param("query") String query, Pageable pageable);
}

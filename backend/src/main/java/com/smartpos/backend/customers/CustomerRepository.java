package com.smartpos.backend.customers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    @Query("""
            SELECT c FROM CustomerEntity c
            WHERE (:query = '' OR
                   LOWER(c.name) LIKE CONCAT('%', :query, '%') OR
                   LOWER(COALESCE(c.phone, '')) LIKE CONCAT('%', :query, '%') OR
                   LOWER(COALESCE(c.email, '')) LIKE CONCAT('%', :query, '%'))
            """)
    Page<CustomerEntity> search(@Param("query") String query, Pageable pageable);
}


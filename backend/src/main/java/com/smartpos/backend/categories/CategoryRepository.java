package com.smartpos.backend.categories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT c FROM CategoryEntity c
            WHERE (:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CategoryEntity> search(@Param("query") String query, Pageable pageable);
}

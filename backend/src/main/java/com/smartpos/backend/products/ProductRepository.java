package com.smartpos.backend.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByCategoryId(UUID categoryId);

    @Query("""
            SELECT p FROM ProductEntity p
            WHERE (LOWER(p.name) LIKE CONCAT('%', :query, '%')
                    OR LOWER(p.sku) LIKE CONCAT('%', :query, '%')
                    OR LOWER(COALESCE(p.barcode, '')) LIKE CONCAT('%', :query, '%'))
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<ProductEntity> search(@Param("query") String query,
                               @Param("categoryId") UUID categoryId,
                               @Param("active") Boolean active,
                               Pageable pageable);

    @Query("""
            SELECT p FROM ProductEntity p
            WHERE (LOWER(p.name) LIKE CONCAT('%', :query, '%')
                    OR LOWER(p.sku) LIKE CONCAT('%', :query, '%')
                    OR LOWER(COALESCE(p.barcode, '')) LIKE CONCAT('%', :query, '%'))
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:lowOnly = FALSE OR
                   COALESCE((SELECT s.onHand
                               FROM com.smartpos.backend.stock.ProductStockEntity s
                              WHERE s.productId = p.id
                                AND s.locationId = com.smartpos.backend.stock.StockLedgerService.DEFAULT_LOCATION_ID), 0) <= p.lowStockThreshold)
            """)
    Page<ProductEntity> searchForStock(@Param("query") String query,
                                       @Param("categoryId") UUID categoryId,
                                       @Param("lowOnly") boolean lowOnly,
                                       Pageable pageable);
}

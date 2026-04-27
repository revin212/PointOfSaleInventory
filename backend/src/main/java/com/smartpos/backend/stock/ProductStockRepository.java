package com.smartpos.backend.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStockEntity, ProductStockId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductStockEntity s WHERE s.productId = :productId")
    Optional<ProductStockEntity> findForUpdate(@Param("productId") UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductStockEntity s WHERE s.productId = :productId AND s.locationId = :locationId")
    Optional<ProductStockEntity> findForUpdate(@Param("productId") UUID productId, @Param("locationId") UUID locationId);

    @Query("SELECT s FROM ProductStockEntity s WHERE s.locationId = :locationId AND s.productId IN :productIds")
    List<ProductStockEntity> findByProductIds(@Param("locationId") UUID locationId,
                                              @Param("productIds") Collection<UUID> productIds);

    @Query("SELECT COALESCE(s.onHand, 0) FROM ProductStockEntity s WHERE s.productId = :productId AND s.locationId = :locationId")
    Optional<Integer> onHand(@Param("productId") UUID productId, @Param("locationId") UUID locationId);
}


package com.smartpos.backend.stock;

import com.smartpos.backend.categories.CategoryEntity;
import com.smartpos.backend.categories.CategoryRepository;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.domain.enums.StockMovementType;
import com.smartpos.backend.products.ProductEntity;
import com.smartpos.backend.products.ProductRepository;
import com.smartpos.backend.stock.dto.OnHandResponse;
import com.smartpos.backend.stock.dto.StockMovementResponse;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockQueryService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;

    public StockQueryService(ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             StockMovementRepository movementRepository,
                             UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<OnHandResponse> onHand(String query, UUID categoryId, boolean lowOnly, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? "" : query.trim().toLowerCase();
        Page<ProductEntity> page = productRepository.searchForStock(q, categoryId, lowOnly, pageable);

        List<UUID> productIds = page.getContent().stream().map(ProductEntity::getId).toList();
        Map<UUID, Integer> onHandByProduct = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] row : movementRepository.sumQtyByProducts(productIds)) {
                onHandByProduct.put((UUID) row[0], ((Number) row[1]).intValue());
            }
        }

        Set<UUID> catIds = page.getContent().stream()
                .map(ProductEntity::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> categoryNames = new HashMap<>();
        if (!catIds.isEmpty()) {
            categoryRepository.findAllById(catIds).forEach(c -> categoryNames.put(c.getId(), c.getName()));
        }

        return page.map(p -> {
            int onHand = onHandByProduct.getOrDefault(p.getId(), 0);
            return new OnHandResponse(
                    p.getId(), p.getSku(), p.getName(),
                    p.getCategoryId(),
                    p.getCategoryId() == null ? null : categoryNames.get(p.getCategoryId()),
                    p.getUnit(),
                    onHand,
                    p.getLowStockThreshold(),
                    onHand <= p.getLowStockThreshold(),
                    p.isActive()
            );
        });
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> movements(UUID productId, StockMovementType type,
                                                 Instant from, Instant to, Pageable pageable) {
        Specification<StockMovementEntity> spec = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (productId != null) predicates.add(cb.equal(root.get("productId"), productId));
            if (type != null)      predicates.add(cb.equal(root.get("type"), type));
            if (from != null)      predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null)        predicates.add(cb.lessThan(root.get("createdAt"), to));
            q.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<StockMovementEntity> page = movementRepository.findAll(spec, pageable);

        Set<UUID> productIds = page.getContent().stream().map(StockMovementEntity::getProductId).collect(Collectors.toSet());
        Set<UUID> userIds = page.getContent().stream()
                .map(StockMovementEntity::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, ProductEntity> products = new HashMap<>();
        if (!productIds.isEmpty()) {
            productRepository.findAllById(productIds).forEach(p -> products.put(p.getId(), p));
        }
        Map<UUID, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userNames.put(u.getId(), u.getName()));
        }

        return page.map(m -> {
            ProductEntity p = products.get(m.getProductId());
            return new StockMovementResponse(
                    m.getId(), m.getProductId(),
                    p == null ? null : p.getSku(),
                    p == null ? null : p.getName(),
                    m.getType(), m.getQtyDelta(), m.getUnitCost(),
                    m.getRefType(), m.getRefId(), m.getNote(),
                    m.getCreatedBy(),
                    m.getCreatedBy() == null ? null : userNames.get(m.getCreatedBy()),
                    m.getCreatedAt()
            );
        });
    }

    @Transactional(readOnly = true)
    public ProductEntity requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }
}

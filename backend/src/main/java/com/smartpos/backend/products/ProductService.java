package com.smartpos.backend.products;

import com.smartpos.backend.categories.CategoryEntity;
import com.smartpos.backend.categories.CategoryRepository;
import com.smartpos.backend.common.error.ConflictException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.products.dto.ProductResponse;
import com.smartpos.backend.products.dto.ProductUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String query, UUID categoryId, Boolean active, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        Page<ProductEntity> page = productRepository.search(q, categoryId, active, pageable);

        Map<UUID, String> categoryNames = resolveCategoryNames(
                page.getContent().stream().map(ProductEntity::getCategoryId).collect(Collectors.toSet()));

        return page.map(p -> ProductResponse.from(p, categoryNames.get(p.getCategoryId())));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        ProductEntity p = findOrThrow(id);
        String name = p.getCategoryId() == null ? null :
                categoryRepository.findById(p.getCategoryId()).map(CategoryEntity::getName).orElse(null);
        return ProductResponse.from(p, name);
    }

    @Transactional
    public ProductResponse create(ProductUpsertRequest req) {
        String sku = req.sku().trim();
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("SKU already exists");
        }
        ProductEntity p = new ProductEntity();
        p.setSku(sku);
        apply(p, req);
        ProductEntity saved = productRepository.save(p);
        String categoryName = saved.getCategoryId() == null ? null :
                categoryRepository.findById(saved.getCategoryId()).map(CategoryEntity::getName).orElse(null);
        return ProductResponse.from(saved, categoryName);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpsertRequest req) {
        ProductEntity p = findOrThrow(id);
        String newSku = req.sku().trim();
        if (!p.getSku().equalsIgnoreCase(newSku) && productRepository.existsBySkuIgnoreCase(newSku)) {
            throw new ConflictException("SKU already exists");
        }
        p.setSku(newSku);
        apply(p, req);
        ProductEntity saved = productRepository.save(p);
        String categoryName = saved.getCategoryId() == null ? null :
                categoryRepository.findById(saved.getCategoryId()).map(CategoryEntity::getName).orElse(null);
        return ProductResponse.from(saved, categoryName);
    }

    @Transactional
    public void delete(UUID id) {
        ProductEntity p = findOrThrow(id);
        // Soft-retirement: keep FK integrity for historical references (sale_items, purchase_items, stock_movements).
        p.setActive(false);
        productRepository.save(p);
    }

    private void apply(ProductEntity p, ProductUpsertRequest req) {
        p.setName(req.name().trim());
        if (req.categoryId() != null && !categoryRepository.existsById(req.categoryId())) {
            throw new NotFoundException("Category not found");
        }
        p.setCategoryId(req.categoryId());
        p.setUnit(req.unit().trim());
        p.setCost(req.cost());
        p.setPrice(req.price());
        p.setBarcode(normalize(req.barcode()));
        p.setLowStockThreshold(req.lowStockThreshold());
        p.setActive(req.active());
    }

    private Map<UUID, String> resolveCategoryNames(Set<UUID> ids) {
        Map<UUID, String> map = new HashMap<>();
        List<UUID> nonNull = ids.stream().filter(java.util.Objects::nonNull).toList();
        if (nonNull.isEmpty()) return map;
        categoryRepository.findAllById(nonNull).forEach(c -> map.put(c.getId(), c.getName()));
        return map;
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ProductEntity findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }
}

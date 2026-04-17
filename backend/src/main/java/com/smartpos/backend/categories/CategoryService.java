package com.smartpos.backend.categories;

import com.smartpos.backend.categories.dto.CategoryResponse;
import com.smartpos.backend.categories.dto.CategoryUpsertRequest;
import com.smartpos.backend.common.error.ConflictException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.products.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? "" : query.trim().toLowerCase();
        return categoryRepository.search(q, pageable).map(CategoryResponse::from);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryUpsertRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category name already exists");
        }
        CategoryEntity c = new CategoryEntity();
        c.setName(name);
        return CategoryResponse.from(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryUpsertRequest req) {
        CategoryEntity c = findOrThrow(id);
        String name = req.name().trim();
        if (!c.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category name already exists");
        }
        c.setName(name);
        return CategoryResponse.from(categoryRepository.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        CategoryEntity c = findOrThrow(id);
        if (productRepository.existsByCategoryId(c.getId())) {
            throw new ConflictException("Category is referenced by one or more products");
        }
        categoryRepository.delete(c);
    }

    private CategoryEntity findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}

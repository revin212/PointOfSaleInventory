package com.smartpos.backend.categories;

import com.smartpos.backend.auth.dto.SimpleResponse;
import com.smartpos.backend.categories.dto.CategoryResponse;
import com.smartpos.backend.categories.dto.CategoryUpsertRequest;
import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE','CASHIER')")
    public PageResponse<CategoryResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return PageResponse.from(categoryService.list(query,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.ASC, "name"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE','CASHIER')")
    public CategoryResponse get(@PathVariable UUID id) {
        return categoryService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryUpsertRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryUpsertRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','WAREHOUSE')")
    public SimpleResponse delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return SimpleResponse.ok();
    }
}

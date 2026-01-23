package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.CategoryRequest;
import com.techno.backend.dto.warehouse.CategoryResponse;
import com.techno.backend.dto.warehouse.CategorySummary;
import com.techno.backend.service.ItemCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing item categories in the warehouse system.
 * Provides endpoints for CRUD operations on item categories.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@RestController
@RequestMapping("/warehouse/categories")
@RequiredArgsConstructor
@Slf4j
public class ItemCategoryController {

    private final ItemCategoryService categoryService;

    /**
     * Create a new item category
     * POST /api/warehouse/categories
     */
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("REST request to create item category: {}", request.getCategoryName());

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ ÙØ¦Ø© Ø§Ù„ØµÙ†Ù Ø¨Ù†Ø¬Ø§Ø­", response));
    }

    /**
     * Get all item categories
     * GET /api/warehouse/categories
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<CategorySummary>>> getAllCategories() {
        log.info("REST request to get all item categories");

        List<CategorySummary> categories = categoryService.getAllCategories();

        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Get item category by code
     * GET /api/warehouse/categories/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        log.info("REST request to get item category: {}", id);

        CategoryResponse response = categoryService.getCategoryById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update an existing item category
     * PUT /api/warehouse/categories/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("REST request to update item category: {}", id);

        CategoryResponse response = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(ApiResponse.success("ØªÙ… ØªØ­Ø¯ÙŠØ« ÙØ¦Ø© Ø§Ù„ØµÙ†Ù Ø¨Ù†Ø¬Ø§Ø­", response));
    }

    /**
     * Delete (deactivate) an item category
     * DELETE /api/warehouse/categories/{id}?force=false
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(
            @PathVariable Long id,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        log.info("REST request to deactivate item category: {} (force: {})", id, force);

        if (force) {
            categoryService.forceDeactivateCategory(id);
            return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø­Ø°Ù Ø§Ù„ÙØ¦Ø© Ù‚Ø³Ø±ÙŠØ§Ù‹ Ø¨Ù†Ø¬Ø§Ø­ (ØªÙ… ØªØ¬Ø§Ù‡Ù„ ÙØ­Øµ Ø§Ù„Ø£ØµÙ†Ø§Ù)", null));
        } else {
            categoryService.deactivateCategory(id);
            return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ ÙØ¦Ø© Ø§Ù„ØµÙ†Ù Ø¨Ù†Ø¬Ø§Ø­", null));
        }
    }
}


package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.ItemRequest;
import com.techno.backend.dto.warehouse.ItemResponse;
import com.techno.backend.dto.warehouse.ItemSummary;
import com.techno.backend.service.StoreItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing store items in the warehouse system.
 * Provides endpoints for CRUD operations on store items.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@RestController
@RequestMapping("/warehouse/items")
@RequiredArgsConstructor
@Slf4j
public class StoreItemController {

    private final StoreItemService itemService;

    /**
     * Create a new store item
     * POST /api/warehouse/items
     */
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(@Valid @RequestBody ItemRequest request) {
        log.info("REST request to create store item: {}", request.getItemName());

        ItemResponse response = itemService.createItem(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† Ø¨Ù†Ø¬Ø§Ø­", response));
    }

    /**
     * Get all store items (paginated)
     * GET /api/warehouse/items?page=0&size=10&sort=itemEnName,asc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<ItemSummary>>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "itemName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        log.info("REST request to get all store items (page: {}, size: {}, sort: {} {})", page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ItemSummary> items = itemService.getAllItems(pageable);

        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Get store item by code
     * GET /api/warehouse/items/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ItemResponse>> getItemById(@PathVariable Long id) {
        log.info("REST request to get store item: {}", id);

        ItemResponse response = itemService.getItemById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get items by category (paginated)
     * GET /api/warehouse/items/category/{categoryCode}?page=0&size=10
     */
    @GetMapping("/category/{categoryCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<ItemSummary>>> getItemsByCategory(
            @PathVariable Long categoryCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "itemName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        log.info("REST request to get items by category: {} (page: {}, size: {})", categoryCode, page, size);

        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ItemSummary> items = itemService.getItemsByCategory(categoryCode, pageable);

        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Update an existing store item
     * PUT /api/warehouse/items/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        log.info("REST request to update store item: {}", id);

        ItemResponse response = itemService.updateItem(id, request);

        return ResponseEntity.ok(ApiResponse.success("ØªÙ… ØªØ­Ø¯ÙŠØ« ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† Ø¨Ù†Ø¬Ø§Ø­", response));
    }

    /**
     * Delete (deactivate) a store item
     * DELETE /api/warehouse/items/{id}?force=false
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateItem(
            @PathVariable Long id,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        log.info("REST request to deactivate store item: {} (force: {})", id, force);

        if (force) {
            itemService.forceDeactivateItem(id);
            return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø­Ø°Ù Ø§Ù„ØµÙ†Ù Ù‚Ø³Ø±ÙŠØ§Ù‹ Ø¨Ù†Ø¬Ø§Ø­ (ØªÙ… ØªØ¬Ø§Ù‡Ù„ Ø§Ù„Ù…Ø®Ø²ÙˆÙ†)", null));
        } else {
            itemService.deactivateItem(id);
            return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† Ø¨Ù†Ø¬Ø§Ø­", null));
        }
    }
}


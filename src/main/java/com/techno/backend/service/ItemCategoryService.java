package com.techno.backend.service;

import com.techno.backend.dto.warehouse.CategoryRequest;
import com.techno.backend.dto.warehouse.CategoryResponse;
import com.techno.backend.dto.warehouse.CategorySummary;
import com.techno.backend.entity.ItemCategory;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.ItemCategoryRepository;
import com.techno.backend.repository.StoreItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing item categories in the warehouse system.
 * Handles creation, updates, retrieval, and deactivation of categories.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItemCategoryService {

    private final ItemCategoryRepository categoryRepository;
    private final StoreItemRepository itemRepository;

    /**
     * Create a new item category
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating new item category: {}", request.getCategoryName());

        // Validate unique name
        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new BadRequestException("اسم الفئة موجود بالفعل: " + request.getCategoryName());
        }

        // Create entity
        ItemCategory category = ItemCategory.builder()
                .categoryName(request.getCategoryName())
                .categoryDescription(request.getCategoryDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        category = categoryRepository.save(category);
        log.info("Item category created successfully with code: {}", category.getCategoryCode());

        return mapToResponse(category);
    }

    /**
     * Update an existing item category
     */
    @Transactional
    public CategoryResponse updateCategory(Long categoryCode, CategoryRequest request) {
        log.info("Updating item category with code: {}", categoryCode);

        ItemCategory category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("فئة الصنف غير موجودة برقم: " + categoryCode));

        // Validate unique names (excluding current category)
        if (categoryRepository.existsByNameExcludingId(request.getCategoryName(), categoryCode)) {
            throw new BadRequestException("اسم الفئة موجود بالفعل");
        }

        // Update fields
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        category = categoryRepository.save(category);
        log.info("Item category updated successfully: {}", categoryCode);

        return mapToResponse(category);
    }

    /**
     * Get category by code
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryCode) {
        log.info("Retrieving category with code: {}", categoryCode);

        ItemCategory category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ÙØ¦Ø© Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + categoryCode));

        return mapToResponse(category);
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<CategorySummary> getAllCategories() {
        log.info("Retrieving all item categories");

        return categoryRepository.findAll().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get all active categories
     */
    @Transactional(readOnly = true)
    public List<CategorySummary> getActiveCategories() {
        log.info("Retrieving all active item categories");

        return categoryRepository.findAllActive().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate a category (soft delete)
     */
    @Transactional
    public void deactivateCategory(Long categoryCode) {
        log.info("Deactivating item category with code: {}", categoryCode);

        ItemCategory category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ÙØ¦Ø© Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + categoryCode));

        // Check if category has non-deleted items (using direct query to exclude
        // deleted items)
        long activeItemCount = itemRepository.countByCategoryCodeAndIsDeletedFalse(categoryCode);
        if (activeItemCount > 0) {
            throw new BadRequestException(
                    "Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ ÙØ¦Ø© Ø¨Ù‡Ø§ Ø¹Ù†Ø§ØµØ± Ù…ÙˆØ¬ÙˆØ¯Ø©. ÙŠØ±Ø¬Ù‰ Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ Ø£Ùˆ Ù†Ù‚Ù„ Ø§Ù„Ø¹Ù†Ø§ØµØ± Ø£ÙˆÙ„Ø§Ù‹.");
        }

        category.setIsDeleted(true);
        categoryRepository.save(category);

        log.info("Item category deactivated successfully: {}", categoryCode);
    }

    /**
     * Force deactivate a category (soft delete) even if it has items
     * This will delete the category regardless of items - use with caution
     */
    @Transactional
    public void forceDeactivateCategory(Long categoryCode) {
        log.warn("FORCE deactivating item category with code: {} (bypassing item check)", categoryCode);

        ItemCategory category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ÙØ¦Ø© Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + categoryCode));

        // No item check - force delete
        category.setIsDeleted(true);
        categoryRepository.save(category);

        log.warn("Item category force deactivated successfully: {} (items check was bypassed)", categoryCode);
    }

    /**
     * Map entity to response DTO
     */
    private CategoryResponse mapToResponse(ItemCategory category) {
        // Use repository query to count items (more reliable than lazy loading)
        long itemCount = itemRepository.countByCategoryCodeAndIsDeletedFalse(category.getCategoryCode());
        return CategoryResponse.builder()
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .categoryDescription(category.getCategoryDescription())
                .isActive(category.getIsActive())
                .itemCount((int) itemCount)
                .createdDate(category.getCreatedDate())
                .createdBy(category.getCreatedBy())
                .modifiedDate(category.getModifiedDate())
                .modifiedBy(category.getModifiedBy())
                .build();
    }

    /**
     * Map entity to summary DTO
     */
    private CategorySummary mapToSummary(ItemCategory category) {
        // Use repository query to count items (more reliable than lazy loading)
        long itemCount = itemRepository.countByCategoryCodeAndIsDeletedFalse(category.getCategoryCode());
        return CategorySummary.builder()
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .isActive(category.getIsActive())
                .itemCount((int) itemCount)
                .build();
    }
}

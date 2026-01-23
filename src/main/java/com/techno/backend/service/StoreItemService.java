package com.techno.backend.service;

import com.techno.backend.dto.warehouse.ItemRequest;
import com.techno.backend.dto.warehouse.ItemResponse;
import com.techno.backend.dto.warehouse.ItemSummary;
import com.techno.backend.entity.ItemCategory;
import com.techno.backend.entity.StoreItem;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.entity.ProjectStore;
import com.techno.backend.entity.StoreBalance;
import com.techno.backend.repository.ItemCategoryRepository;
import com.techno.backend.repository.StoreItemRepository;
import com.techno.backend.repository.StoreBalanceRepository;
import com.techno.backend.repository.ProjectStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing store items in the warehouse system.
 * Handles creation, updates, retrieval, and deactivation of items.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 11 - Warehouse Management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StoreItemService {

    private final StoreItemRepository itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final StoreBalanceRepository balanceRepository;
    private final ProjectStoreRepository storeRepository;

    /**
     * Create a new store item
     */
    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        log.info("Creating new store item: {}", request.getItemName());

        // Validate category exists
        ItemCategory category = categoryRepository.findById(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ÙØ¦Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + request.getCategoryCode()));

        // Validate unique names
        if (itemRepository.existsByItemName(request.getItemName())) {
            throw new BadRequestException("Ø§Ø³Ù… Ø§Ù„Ø¹Ù†ØµØ± Ø¨Ø§Ù„Ø¹Ø±Ø¨ÙŠØ© Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getItemName());
        }
        if (itemRepository.existsByItemName(request.getItemName())) {
            throw new BadRequestException("Ø§Ø³Ù… Ø§Ù„Ø¹Ù†ØµØ± Ø¨Ø§Ù„Ø¥Ù†Ø¬Ù„ÙŠØ²ÙŠØ© Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getItemName());
        }

        // Create entity
        StoreItem item = StoreItem.builder()
                .category(category)
                .itemName(request.getItemName())
                .itemName(request.getItemName())
                .unitOfMeasure(request.getUnitOfMeasure())
                .itemDescription(request.getItemDescription())
                .reorderLevel(request.getReorderLevel())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        item = itemRepository.save(item);
        log.info("Store item created successfully with code: {}", item.getItemCode());

        // Handle initial quantity if provided
        if (request.getInitialQuantity() != null && request.getInitialQuantity().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getStoreCode() == null) {
                throw new BadRequestException("Ø±Ù…Ø² Ø§Ù„Ù…ØªØ¬Ø± Ù…Ø·Ù„ÙˆØ¨ Ø¹Ù†Ø¯ ØªÙˆÙÙŠØ± Ø§Ù„ÙƒÙ…ÙŠØ© Ø§Ù„Ø£ÙˆÙ„ÙŠØ©");
            }
            createOrUpdateInitialBalance(item, request.getStoreCode(), request.getInitialQuantity());
        }

        return mapToResponse(item);
    }

    /**
     * Update an existing store item
     */
    @Transactional
    public ItemResponse updateItem(Long itemCode, ItemRequest request) {
        log.info("Updating store item with code: {}", itemCode);

        StoreItem item = itemRepository.findById(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + itemCode));

        // Validate category exists
        ItemCategory category = categoryRepository.findById(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ÙØ¦Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + request.getCategoryCode()));

        // Validate unique names (excluding current item)
        if (itemRepository.existsByNameExcludingId(request.getItemName(), request.getItemName(), itemCode)) {
            throw new BadRequestException("Ø§Ø³Ù… Ø§Ù„Ø¹Ù†ØµØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Update fields
        item.setCategory(category);
        item.setItemName(request.getItemName());
        item.setItemName(request.getItemName());
        item.setUnitOfMeasure(request.getUnitOfMeasure());
        item.setItemDescription(request.getItemDescription());
        item.setReorderLevel(request.getReorderLevel());
        if (request.getIsActive() != null) {
            item.setIsActive(request.getIsActive());
        }

        item = itemRepository.save(item);
        log.info("Store item updated successfully: {}", itemCode);

        // Handle initial quantity if provided
        if (request.getInitialQuantity() != null && request.getInitialQuantity().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getStoreCode() == null) {
                throw new BadRequestException("Ø±Ù…Ø² Ø§Ù„Ù…ØªØ¬Ø± Ù…Ø·Ù„ÙˆØ¨ Ø¹Ù†Ø¯ ØªÙˆÙÙŠØ± Ø§Ù„ÙƒÙ…ÙŠØ© Ø§Ù„Ø£ÙˆÙ„ÙŠØ©");
            }
            log.info("Processing initial quantity: {} for store: {}", request.getInitialQuantity(), request.getStoreCode());
            createOrUpdateInitialBalance(item, request.getStoreCode(), request.getInitialQuantity());
        } else {
            log.debug("No initial quantity provided or quantity is zero");
        }

        return mapToResponse(item);
    }

    /**
     * Get item by code
     */
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long itemCode) {
        log.info("Retrieving item with code: {}", itemCode);

        StoreItem item = itemRepository.findById(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + itemCode));

        return mapToResponse(item);
    }

    /**
     * Get all items with pagination
     */
    @Transactional(readOnly = true)
    public Page<ItemSummary> getAllItems(Pageable pageable) {
        log.info("Retrieving all store items with pagination");

        Page<StoreItem> itemsPage = itemRepository.findAll(pageable);
        
        // Fetch all item codes from the current page
        List<Long> itemCodes = itemsPage.getContent().stream()
                .map(StoreItem::getItemCode)
                .collect(Collectors.toList());
        
        // Fetch all balances for these items in one query
        Map<Long, BigDecimal> totalQuantitiesMap = calculateTotalQuantitiesForItems(itemCodes);
        
        // Map items with pre-calculated quantities
        return itemsPage.map(item -> mapToSummaryWithQuantity(item, totalQuantitiesMap.getOrDefault(item.getItemCode(), BigDecimal.ZERO)));
    }

    /**
     * Get items by category with pagination
     */
    @Transactional(readOnly = true)
    public Page<ItemSummary> getItemsByCategory(Long categoryCode, Pageable pageable) {
        log.info("Retrieving items for category: {}", categoryCode);

        // Validate category exists
        categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ÙØ¦Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯Ø© Ø¨Ø±Ù‚Ù…: " + categoryCode));

        return itemRepository.findByCategoryCode(categoryCode, pageable)
                .map(this::mapToSummary);
    }

    /**
     * Get all active items
     */
    @Transactional(readOnly = true)
    public List<ItemSummary> getActiveItems() {
        log.info("Retrieving all active store items");

        return itemRepository.findAllActive().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate an item (soft delete)
     */
    @Transactional
    public void deactivateItem(Long itemCode) {
        log.info("Deactivating store item with code: {}", itemCode);

        StoreItem item = itemRepository.findById(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + itemCode));

        // Check if item has balances
        if (item.getTotalQuantity().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ Ø¹Ù†ØµØ± Ø¨Ù‡ Ù…Ø®Ø²ÙˆÙ† Ù…ÙˆØ¬ÙˆØ¯. ÙŠØ±Ø¬Ù‰ Ù†Ù‚Ù„ Ø£Ùˆ Ø¥ØµØ¯Ø§Ø± Ø¬Ù…ÙŠØ¹ Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø£ÙˆÙ„Ø§Ù‹.");
        }

        item.setIsDeleted(true);
        itemRepository.save(item);

        log.info("Store item deactivated successfully: {}", itemCode);
    }

    /**
     * Force deactivate an item (soft delete) even if it has stock
     * This will delete the item regardless of stock levels - use with caution
     */
    @Transactional
    public void forceDeactivateItem(Long itemCode) {
        log.warn("FORCE deactivating store item with code: {} (bypassing stock check)", itemCode);

        StoreItem item = itemRepository.findById(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("ØµÙ†Ù Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + itemCode));

        // No stock check - force delete
        item.setIsDeleted(true);
        itemRepository.save(item);

        log.warn("Store item force deactivated successfully: {} (stock was not checked)", itemCode);
    }

    /**
     * Map entity to response DTO
     */
    private ItemResponse mapToResponse(StoreItem item) {
        return ItemResponse.builder()
                .itemCode(item.getItemCode())
                .categoryCode(item.getCategory().getCategoryCode())
                .categoryName(item.getCategory().getCategoryName())
                .categoryName(item.getCategory().getCategoryName())
                .itemName(item.getItemName())
                .itemName(item.getItemName())
                .unitOfMeasure(item.getUnitOfMeasure())
                .itemDescription(item.getItemDescription())
                .reorderLevel(item.getReorderLevel())
                .isActive(item.getIsActive())
                .totalQuantity(item.getTotalQuantity())
                .needsReorder(item.needsReorder())
                .createdDate(item.getCreatedDate())
                .createdBy(item.getCreatedBy())
                .modifiedDate(item.getModifiedDate())
                .modifiedBy(item.getModifiedBy())
                .build();
    }

    /**
     * Map entity to summary DTO
     */
    private ItemSummary mapToSummary(StoreItem item) {
        // Calculate totalQuantity from database directly instead of relying on lazy-loaded balances
        BigDecimal totalQuantity = calculateTotalQuantityFromDatabase(item.getItemCode());
        
        return mapToSummaryWithQuantity(item, totalQuantity);
    }
    
    /**
     * Map entity to summary DTO with pre-calculated quantity
     */
    private ItemSummary mapToSummaryWithQuantity(StoreItem item, BigDecimal totalQuantity) {
        return ItemSummary.builder()
                .itemCode(item.getItemCode())
                .categoryCode(item.getCategory().getCategoryCode())
                .categoryName(item.getCategory().getCategoryName())
                .categoryName(item.getCategory().getCategoryName())
                .itemName(item.getItemName())
                .itemName(item.getItemName())
                .unitOfMeasure(item.getUnitOfMeasure())
                .reorderLevel(item.getReorderLevel())
                .isActive(item.getIsActive())
                .totalQuantity(totalQuantity)
                .build();
    }
    
    /**
     * Calculate total quantities for multiple items in one query (optimized)
     */
    private Map<Long, BigDecimal> calculateTotalQuantitiesForItems(List<Long> itemCodes) {
        if (itemCodes.isEmpty()) {
            return new HashMap<>();
        }
        
        // Fetch all balances for these items in one query
        List<com.techno.backend.entity.StoreBalance> balances = balanceRepository.findByItemCodeIn(itemCodes);
        
        log.debug("Found {} balances for {} items", balances.size(), itemCodes.size());
        
        // Group by item code and sum quantities
        Map<Long, BigDecimal> result = new HashMap<>();
        for (com.techno.backend.entity.StoreBalance balance : balances) {
            if (balance.getQuantityOnHand() != null && balance.getItem() != null) {
                Long itemCode = balance.getItem().getItemCode();
                BigDecimal currentQty = result.getOrDefault(itemCode, BigDecimal.ZERO);
                result.put(itemCode, currentQty.add(balance.getQuantityOnHand()));
                log.debug("Item {}: adding {} to total (current: {})", itemCode, balance.getQuantityOnHand(), currentQty);
            }
        }
        
        // Initialize all items with 0 if they have no balances
        for (Long itemCode : itemCodes) {
            result.putIfAbsent(itemCode, BigDecimal.ZERO);
        }
        
        log.debug("Total quantities map: {}", result);
        return result;
    }
    
    /**
     * Calculate total quantity for an item by querying store_balances directly
     */
    private BigDecimal calculateTotalQuantityFromDatabase(Long itemCode) {
        return balanceRepository.findByItemCode(itemCode).stream()
                .map(balance -> balance.getQuantityOnHand() != null ? balance.getQuantityOnHand() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Create or update balance record with initial quantity
     */
    private void createOrUpdateInitialBalance(StoreItem item, Long storeCode, BigDecimal quantity) {
        log.info("Setting initial balance for item={}, store={}, quantity={}", 
                item.getItemCode(), storeCode, quantity);

        // Validate store exists
        ProjectStore store = storeRepository.findById(storeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + storeCode));

        // Find or create balance
        StoreBalance balance = balanceRepository
                .findByStoreAndItem(storeCode, item.getItemCode())
                .orElseGet(() -> {
                    log.info("Creating new balance record for store={}, item={}", storeCode, item.getItemCode());
                    return StoreBalance.builder()
                            .store(store)
                            .item(item)
                            .quantityOnHand(BigDecimal.ZERO)
                            .quantityReserved(BigDecimal.ZERO)
                            .build();
                });

        // Set the initial quantity
        balance.setQuantityOnHand(quantity);
        balance.setLastTransactionDate(java.time.LocalDateTime.now());
        
        balanceRepository.save(balance);
        log.info("Balance record created/updated: store={}, item={}, quantity={}", 
                storeCode, item.getItemCode(), quantity);
    }
}


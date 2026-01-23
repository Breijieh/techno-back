package com.techno.backend.service;

import com.techno.backend.dto.warehouse.GoodsIssueRequest;
import com.techno.backend.dto.warehouse.GoodsIssueResponse;
import com.techno.backend.dto.warehouse.GoodsIssueLineRequest;
import com.techno.backend.dto.warehouse.GoodsIssueLineResponse;
import com.techno.backend.entity.*;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoodsIssueService {

    private final GoodsIssueRepository issueRepository;
    private final ProjectStoreRepository storeRepository;
    private final ProjectRepository projectRepository;
    private final StoreItemRepository itemRepository;
    private final StoreBalanceRepository balanceRepository;
    private final StoreTransactionRepository transactionRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * CRITICAL: Validate and decrease balance
     * This method ensures we NEVER allow negative balances
     */
    private void updateStoreBalance(ProjectStore store, StoreItem item, BigDecimal quantity,
                                     String transactionType, Long referenceId) {
        log.info("Decreasing store balance: store={}, item={}, quantity={}",
                store.getStoreCode(), item.getItemCode(), quantity);

        StoreBalance balance = balanceRepository
                .findByStoreAndItem(store.getStoreCode(), item.getItemCode())
                .orElseThrow(() -> new BadRequestException("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ù„Ù„ØµÙ†Ù ÙÙŠ Ù‡Ø°Ø§ Ø§Ù„Ù…Ø®Ø²Ù†"));

        BigDecimal oldBalance = balance.getQuantityOnHand();
        BigDecimal newBalance = oldBalance.add(quantity); // quantity is negative for issues

        // CRITICAL: Prevent negative balances
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    String.format("Ø§Ù„ÙƒÙ…ÙŠØ© ØºÙŠØ± ÙƒØ§ÙÙŠØ© Ù„Ù„Ø¹Ù†ØµØ± %s. Ø§Ù„Ù…ØªØ§Ø­: %sØŒ Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %s",
                            item.getItemName(),
                            balance.getAvailableQuantity(),
                            quantity.abs()));
        }

        balance.setQuantityOnHand(newBalance);
        balance.setLastTransactionDate(LocalDateTime.now());
        balanceRepository.save(balance);

        // Create transaction log
        StoreTransaction transaction = StoreTransaction.builder()
                .store(store)
                .item(item)
                .transactionType(transactionType)
                .transactionDate(LocalDateTime.now())
                .quantity(quantity)
                .referenceType("GOODS_ISSUE")
                .referenceId(referenceId)
                .balanceAfter(newBalance)
                .build();
        transactionRepository.save(transaction);

        log.info("Balance decreased successfully: oldBalance={}, newBalance={}", oldBalance, newBalance);
    }

    @Transactional
    public GoodsIssueResponse createGoodsIssue(GoodsIssueRequest request) {
        log.info("Creating goods issue for store: {}, project: {}", request.getStoreCode(), request.getProjectCode());

        // Validate store exists
        ProjectStore store = storeRepository.findById(request.getStoreCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Validate project exists
        Project project = projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Generate issue number
        String issueNumber = generateIssueNumber();

        // Build issue entity
        GoodsIssue issue = GoodsIssue.builder()
                .issueNumber(issueNumber)
                .store(store)
                .project(project)
                .issueDate(request.getIssueDate())
                .issuedTo(request.getIssuedTo())
                .purpose(request.getPurpose())
                .notes(request.getNotes())
                .isDeleted(false)
                .build();

        // Add issue lines
        for (GoodsIssueLineRequest lineRequest : request.getIssueLines()) {
            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            GoodsIssueLine line = GoodsIssueLine.builder()
                    .goodsIssue(issue)
                    .item(item)
                    .quantity(lineRequest.getQuantity())
                    .notes(lineRequest.getNotes())
                    .isDeleted(false)
                    .build();

            issue.addIssueLine(line);
        }

        // STEP 1: VALIDATE SUFFICIENT BALANCES FIRST (before saving anything)
        for (GoodsIssueLine line : issue.getIssueLines()) {
            if (line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† ÙƒÙ…ÙŠØ© Ø§Ù„Ø¥ØµØ¯Ø§Ø± Ø£ÙƒØ¨Ø± Ù…Ù† Ø§Ù„ØµÙØ±");
            }

            StoreItem item = line.getItem();

            StoreBalance balance = balanceRepository
                    .findByStoreAndItem(store.getStoreCode(), item.getItemCode())
                    .orElseThrow(() -> new BadRequestException(
                            String.format("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ù„Ù„ØµÙ†Ù %s ÙÙŠ Ø§Ù„Ù…Ø®Ø²Ù† %s",
                                    item.getItemName(), store.getStoreName())));

            if (!balance.hasSufficientQuantity(line.getQuantity())) {
                throw new BadRequestException(
                        String.format("Ø§Ù„ÙƒÙ…ÙŠØ© ØºÙŠØ± ÙƒØ§ÙÙŠØ© Ù„Ù„ØµÙ†Ù %s. Ø§Ù„Ù…ØªØ§Ø­: %sØŒ Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %s",
                                item.getItemName(),
                                balance.getAvailableQuantity(),
                                line.getQuantity()));
            }
        }

        // STEP 2: Set createdBy (issuedBy) from authenticated user
        Long currentEmployeeNo = getCurrentEmployeeNo();
        if (currentEmployeeNo != null) {
            issue.setCreatedBy(currentEmployeeNo);
        }

        // STEP 3: Save issue (all validations passed)
        issue = issueRepository.save(issue);

        // STEP 4: Process each line - DECREASE BALANCES
        for (GoodsIssueLine line : issue.getIssueLines()) {
            StoreItem item = line.getItem();

            // CRITICAL: Update balance (ISSUE = SUBTRACT quantity, so pass negative)
            updateStoreBalance(store, item, line.getQuantity().negate(), "ISSUE", issue.getIssueId());
        }

        log.info("Goods issue created successfully with {} lines", issue.getIssueLines().size());
        return mapToResponse(issue);
    }

    private String generateIssueNumber() {
        // Generate issue number in format: GI-YYYYMMDD-XXXX
        String prefix = "GI-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long maxId = issueRepository.count();
        return prefix + String.format("%04d", maxId + 1);
    }

    @Transactional(readOnly = true)
    public GoodsIssueResponse getGoodsIssueById(Long issueId) {
        GoodsIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¥ÙŠØµØ§Ù„ Ø¥ØµØ¯Ø§Ø± Ø§Ù„Ø¨Ø¶Ø§Ø¦Ø¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
        return mapToResponse(issue);
    }

    @Transactional(readOnly = true)
    public List<GoodsIssueResponse> getAllGoodsIssues() {
        List<GoodsIssue> issues = issueRepository.findAll().stream()
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .collect(Collectors.toList());
        return issues.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GoodsIssueResponse updateGoodsIssue(Long issueId, GoodsIssueRequest request) {
        log.info("Updating goods issue: {}", issueId);

        GoodsIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¥ÙŠØµØ§Ù„ Ø¥ØµØ¯Ø§Ø± Ø§Ù„Ø¨Ø¶Ø§Ø¦Ø¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Validate store exists
        ProjectStore store = storeRepository.findById(request.getStoreCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // Validate project exists
        Project project = projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        // STEP 1: Store original quantities and current balances BEFORE restoration
        // Use the ORIGINAL store from the issue, not the store from the request (in case store changed)
        ProjectStore originalStore = issue.getStore();
        java.util.Map<Long, BigDecimal> originalQuantities = new java.util.HashMap<>();
        java.util.Map<Long, BigDecimal> balancesBeforeRestore = new java.util.HashMap<>();
        
        for (GoodsIssueLine originalLine : issue.getIssueLines()) {
            if (!Boolean.TRUE.equals(originalLine.getIsDeleted())) {
                StoreItem item = originalLine.getItem();
                originalQuantities.put(item.getItemCode(), originalLine.getQuantity());
                
                // Get current balance BEFORE restoration using the ORIGINAL store
                // Use native query to bypass JPA cache and get actual DB value
                BigDecimal balanceBefore = balanceRepository
                        .getAvailableQuantityNative(originalStore.getStoreCode(), item.getItemCode());
                if (balanceBefore == null) {
                    throw new BadRequestException(
                            String.format("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ù„Ù„ØµÙ†Ù %s ÙÙŠ Ø§Ù„Ù…Ø®Ø²Ù† %s",
                                    item.getItemName(), originalStore.getStoreName()));
                }
                balancesBeforeRestore.put(item.getItemCode(), balanceBefore);
                
                // Add back the original quantity (restore stock) - use original store
                updateStoreBalance(originalStore, item, originalLine.getQuantity(), "ISSUE_REVERSAL", issueId);
                log.info("Restored {} units of item {} to stock in store {}. Balance before: {}, after restore should be: {}",
                        originalLine.getQuantity(), item.getItemName(), originalStore.getStoreCode(),
                        balanceBefore, balanceBefore.add(originalLine.getQuantity()));
            }
        }

        // STEP 2: Validate new quantities against adjusted stock
        // Flush to ensure balance updates are persisted
        balanceRepository.flush();
        for (GoodsIssueLineRequest lineRequest : request.getIssueLines()) {
            if (lineRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† ÙƒÙ…ÙŠØ© Ø§Ù„Ø¥ØµØ¯Ø§Ø± Ø£ÙƒØ¨Ø± Ù…Ù† Ø§Ù„ØµÙØ±");
            }

            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            // Calculate adjusted available quantity manually
            // If item was in original issue: balance before restore + restored quantity
            // If item is new: use current balance from DB (bypass JPA cache)
            // NOTE: If store changed, we restore to original store but validate against new store
            BigDecimal adjustedAvailable;
            if (balancesBeforeRestore.containsKey(item.getItemCode())) {
                // Item was in original issue
                // If store changed, we need to check balance in NEW store (not original)
                // If store is same, use balance before restore + restored quantity
                if (originalStore.getStoreCode().equals(store.getStoreCode())) {
                    // Same store - use balance before restore + restored quantity
                    BigDecimal balanceBeforeRestore = balancesBeforeRestore.get(item.getItemCode());
                    BigDecimal restoredQty = originalQuantities.get(item.getItemCode());
                    adjustedAvailable = balanceBeforeRestore.add(restoredQty);
                    log.info("Validating item {} (was in original issue, same store): balance before restore={}, restored={}, adjusted available={}, requested={}",
                            item.getItemName(), balanceBeforeRestore, restoredQty, adjustedAvailable, lineRequest.getQuantity());
                } else {
                    // Store changed - get current balance in NEW store (bypass JPA cache)
                    BigDecimal currentAvailable = balanceRepository.getAvailableQuantityNative(store.getStoreCode(), item.getItemCode());
                    if (currentAvailable == null) {
                        throw new BadRequestException(
                                String.format("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ù„Ù„ØµÙ†Ù %s ÙÙŠ Ø§Ù„Ù…Ø®Ø²Ù† %s",
                                        item.getItemName(), store.getStoreName()));
                    }
                    adjustedAvailable = currentAvailable;
                    log.info("Validating item {} (was in original issue, store changed from {} to {}): current available in new store={}, requested={}",
                            item.getItemName(), originalStore.getStoreCode(), store.getStoreCode(), adjustedAvailable, lineRequest.getQuantity());
                }
            } else {
                // Item is new - get current balance using native query to bypass JPA cache
                BigDecimal currentAvailable = balanceRepository.getAvailableQuantityNative(store.getStoreCode(), item.getItemCode());
                if (currentAvailable == null) {
                    throw new BadRequestException(
                            String.format("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ù„Ù„Ø¹Ù†ØµØ± %s ÙÙŠ Ø§Ù„Ù…ØªØ¬Ø± %s",
                                    item.getItemName(), store.getStoreName()));
                }
                adjustedAvailable = currentAvailable;
                log.info("Validating item {} (new item): current available={}, requested={}",
                        item.getItemName(), adjustedAvailable, lineRequest.getQuantity());
            }

            if (lineRequest.getQuantity().compareTo(adjustedAvailable) > 0) {
                throw new BadRequestException(
                        String.format("Ø§Ù„ÙƒÙ…ÙŠØ© ØºÙŠØ± ÙƒØ§ÙÙŠØ© Ù„Ù„ØµÙ†Ù %s. Ø§Ù„Ù…ØªØ§Ø­: %sØŒ Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %s",
                                item.getItemName(),
                                adjustedAvailable,
                                lineRequest.getQuantity()));
            }
        }

        // STEP 3: Update issue details
        issue.setStore(store);
        issue.setProject(project);
        issue.setIssueDate(request.getIssueDate());
        issue.setIssuedTo(request.getIssuedTo());
        issue.setPurpose(request.getPurpose());
        issue.setNotes(request.getNotes());

        // STEP 4: Clear and update issue lines
        issue.getIssueLines().clear();
        for (GoodsIssueLineRequest lineRequest : request.getIssueLines()) {
            StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

            GoodsIssueLine line = GoodsIssueLine.builder()
                    .goodsIssue(issue)
                    .item(item)
                    .quantity(lineRequest.getQuantity())
                    .notes(lineRequest.getNotes())
                    .isDeleted(false)
                    .build();

            issue.addIssueLine(line);
        }

        // STEP 5: Save issue
        issue = issueRepository.save(issue);

        // STEP 6: Process new issue lines - DECREASE BALANCES
        for (GoodsIssueLine line : issue.getIssueLines()) {
            StoreItem item = line.getItem();
            // CRITICAL: Update balance (ISSUE = SUBTRACT quantity, so pass negative)
            updateStoreBalance(store, item, line.getQuantity().negate(), "ISSUE", issue.getIssueId());
        }

        log.info("Goods issue updated successfully with {} lines", issue.getIssueLines().size());
        return mapToResponse(issue);
    }

    @Transactional
    public void deleteGoodsIssue(Long issueId) {
        log.info("Deleting goods issue: {}", issueId);

        GoodsIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Ø¥ÙŠØµØ§Ù„ Ø¥ØµØ¯Ø§Ø± Ø§Ù„Ø¨Ø¶Ø§Ø¦Ø¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

        issue.setIsDeleted(true);
        issueRepository.save(issue);
        log.info("Goods issue deleted successfully");
    }

    private GoodsIssueResponse mapToResponse(GoodsIssue issue) {
        GoodsIssueResponse.GoodsIssueResponseBuilder builder = GoodsIssueResponse.builder()
                .issueId(issue.getIssueId())
                .issueNumber(issue.getIssueNumber())
                .issueDate(issue.getIssueDate())
                .issuedTo(issue.getIssuedTo())
                .purpose(issue.getPurpose())
                .notes(issue.getNotes())
                .createdDate(issue.getCreatedDate())
                .createdBy(issue.getCreatedBy())
                .modifiedDate(issue.getModifiedDate())
                .modifiedBy(issue.getModifiedBy());

        // Map createdBy to issuedBy and fetch issuedByName
        if (issue.getCreatedBy() != null) {
            builder.issuedBy(issue.getCreatedBy());
            employeeRepository.findById(issue.getCreatedBy())
                    .ifPresent(employee -> builder.issuedByName(employee.getEmployeeName()));
        }

        if (issue.getStore() != null) {
            builder.storeCode(issue.getStore().getStoreCode())
                    .storeName(issue.getStore().getStoreName())
                    .storeName(issue.getStore().getStoreName())
                    .storeName(issue.getStore().getStoreName());
        }

        if (issue.getProject() != null) {
            builder.projectCode(issue.getProject().getProjectCode())
                    .projectName(issue.getProject().getProjectName())
                    .projectName(issue.getProject().getProjectName())
                    .projectName(issue.getProject().getProjectName());
        }

        if (issue.getIssueLines() != null && !issue.getIssueLines().isEmpty()) {
            List<GoodsIssueLineResponse> lineResponses = issue.getIssueLines().stream()
                    .filter(line -> !Boolean.TRUE.equals(line.getIsDeleted()))
                    .map(this::mapLineToResponse)
                    .collect(Collectors.toList());
            builder.issueLines(lineResponses);
        }

        return builder.build();
    }

    private GoodsIssueLineResponse mapLineToResponse(GoodsIssueLine line) {
        GoodsIssueLineResponse.GoodsIssueLineResponseBuilder builder = GoodsIssueLineResponse.builder()
                .lineId(line.getLineId())
                .quantity(line.getQuantity())
                .notes(line.getNotes());

        if (line.getItem() != null) {
            builder.itemCode(line.getItem().getItemCode())
                    .itemName(line.getItem().getItemName())
                    .itemName(line.getItem().getItemName())
                    .itemName(line.getItem().getItemName());
        }

        return builder.build();
    }

    /**
     * Get current authenticated user's employee number
     */
    private Long getCurrentEmployeeNo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    // Assuming the principal is the employee number as a string
                    return Long.parseLong((String) principal);
                } else if (principal instanceof Long) {
                    return (Long) principal;
                }
            }
        } catch (Exception e) {
            log.warn("Could not get current employee number: {}", e.getMessage());
        }
        return null;
    }
}


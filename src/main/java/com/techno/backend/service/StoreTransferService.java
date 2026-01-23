package com.techno.backend.service;

import com.techno.backend.dto.warehouse.StoreTransferRequest;
import com.techno.backend.dto.warehouse.StoreTransferResponse;
import com.techno.backend.dto.warehouse.StoreTransferLineRequest;
import com.techno.backend.dto.warehouse.StoreTransferLineResponse;
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
public class StoreTransferService {

        private final StoreTransferRepository transferRepository;
        private final ProjectStoreRepository storeRepository;
        private final StoreItemRepository itemRepository;
        private final StoreBalanceRepository balanceRepository;
        private final StoreTransactionRepository transactionRepository;
        private final EmployeeRepository employeeRepository;

        /**
         * CRITICAL: Atomic balance update for transfers
         * Updates balance and creates transaction log in a single operation
         */
        private void updateStoreBalance(ProjectStore store, StoreItem item, BigDecimal quantity,
                        String transactionType, Long referenceId) {
                log.info("Transfer balance update: store={}, item={}, quantity={}, type={}",
                                store.getStoreCode(), item.getItemCode(), quantity, transactionType);

                // Find or create balance
                StoreBalance balance = balanceRepository
                                .findByStoreAndItem(store.getStoreCode(), item.getItemCode())
                                .orElseGet(() -> {
                                        log.info("Creating new balance for destination store={}, item={}",
                                                        store.getStoreCode(), item.getItemCode());
                                        StoreBalance newBalance = StoreBalance.builder()
                                                        .store(store)
                                                        .item(item)
                                                        .quantityOnHand(BigDecimal.ZERO)
                                                        .quantityReserved(BigDecimal.ZERO)
                                                        .build();
                                        return balanceRepository.save(newBalance);
                                });

                BigDecimal oldBalance = balance.getQuantityOnHand();
                BigDecimal newBalance = oldBalance.add(quantity);

                // CRITICAL: Prevent negative balances
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        throw new BadRequestException(
                                        String.format("Ø§Ù„ÙƒÙ…ÙŠØ© ØºÙŠØ± ÙƒØ§ÙÙŠØ© ÙÙŠ Ø§Ù„Ù…ØªØ¬Ø± Ø§Ù„Ù…ØµØ¯Ø±. Ø§Ù„Ù…ØªØ§Ø­: %sØŒ Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %s",
                                                        oldBalance, quantity.abs()));
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
                                .referenceType("TRANSFER")
                                .referenceId(referenceId)
                                .balanceAfter(newBalance)
                                .build();
                transactionRepository.save(transaction);

                log.info("Transfer balance updated: oldBalance={}, newBalance={}", oldBalance, newBalance);
        }

        @Transactional
        public StoreTransferResponse createTransfer(StoreTransferRequest request) {
                log.info("Creating store transfer from store: {} to store: {}", request.getFromStoreCode(),
                                request.getToStoreCode());

                // Validate stores are different
                if (request.getFromStoreCode().equals(request.getToStoreCode())) {
                        throw new BadRequestException(
                                        "ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø§Ù„Ù…ØªØ¬Ø± Ø§Ù„Ù…ØµØ¯Ø± ÙˆØ§Ù„ÙˆØ¬Ù‡Ø© Ù…Ø®ØªÙ„ÙÙŠÙ†");
                }

                // Validate stores exist
                ProjectStore fromStore = storeRepository.findById(request.getFromStoreCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ø§Ù„Ù…Ø®Ø²Ù† Ø§Ù„Ù…ØµØ¯Ø± ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                ProjectStore toStore = storeRepository.findById(request.getToStoreCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ø§Ù„Ù…Ø®Ø²Ù† Ø§Ù„ÙˆØ¬Ù‡Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                // Generate transfer number
                String transferNumber = generateTransferNumber();

                // Build transfer entity
                StoreTransfer transfer = StoreTransfer.builder()
                                .transferNumber(transferNumber)
                                .fromStore(fromStore)
                                .toStore(toStore)
                                .transferDate(request.getTransferDate())
                                .notes(request.getNotes())
                                .transferStatus("PENDING")
                                .isDeleted(false)
                                .build();

                // Add transfer lines
                for (StoreTransferLineRequest lineRequest : request.getTransferLines()) {
                        StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

                        StoreTransferLine line = StoreTransferLine.builder()
                                        .storeTransfer(transfer)
                                        .item(item)
                                        .quantity(lineRequest.getQuantity())
                                        .notes(lineRequest.getNotes())
                                        .isDeleted(false)
                                        .build();

                        transfer.addTransferLine(line);
                }

                // STEP 1: VALIDATE SUFFICIENT BALANCES IN SOURCE STORE
                for (StoreTransferLine line : transfer.getTransferLines()) {
                        if (line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                                throw new BadRequestException(
                                                "ÙŠØ¬Ø¨ Ø£Ù† ØªÙƒÙˆÙ† ÙƒÙ…ÙŠØ© Ø§Ù„Ù†Ù‚Ù„ Ø£ÙƒØ¨Ø± Ù…Ù† Ø§Ù„ØµÙØ±");
                        }

                        StoreItem item = line.getItem();

                        StoreBalance sourceBalance = balanceRepository
                                        .findByStoreAndItem(fromStore.getStoreCode(), item.getItemCode())
                                        .orElseThrow(() -> new BadRequestException(
                                                        String.format("Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ù„Ù„ØµÙ†Ù %s ÙÙŠ Ø§Ù„Ù…Ø®Ø²Ù† Ø§Ù„Ù…ØµØ¯Ø±",
                                                                        item.getItemName())));

                        if (!sourceBalance.hasSufficientQuantity(line.getQuantity())) {
                                throw new BadRequestException(
                                                String.format("Ø§Ù„ÙƒÙ…ÙŠØ© ØºÙŠØ± ÙƒØ§ÙÙŠØ© Ù„Ù„Ù†Ù‚Ù„. Ø§Ù„Ø¹Ù†ØµØ±: %sØŒ Ø§Ù„Ù…ØªØ§Ø­: %sØŒ Ø§Ù„Ù…Ø·Ù„ÙˆØ¨: %s",
                                                                item.getItemName(),
                                                                sourceBalance.getAvailableQuantity(),
                                                                line.getQuantity()));
                        }
                }

                // STEP 2: Set createdBy (transferredBy) from authenticated user
                Long currentEmployeeNo = getCurrentEmployeeNo();
                if (currentEmployeeNo != null) {
                        transfer.setCreatedBy(currentEmployeeNo);
                }

                // STEP 3: Save transfer in PENDING status
                transfer = transferRepository.save(transfer);

                log.info("Store transfer created in PENDING status with {} lines", transfer.getTransferLines().size());
                return mapToResponse(transfer);
        }

        private String generateTransferNumber() {
                // Generate transfer number in format: TR-YYYYMMDD-XXXX
                String prefix = "TR-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                                + "-";
                Long maxId = transferRepository.count();
                return prefix + String.format("%04d", maxId + 1);
        }

        @Transactional(readOnly = true)
        public StoreTransferResponse getStoreTransferById(Long transferId) {
                StoreTransfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ù†Ù‚Ù„ Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));
                return mapToResponse(transfer);
        }

        @Transactional(readOnly = true)
        public List<StoreTransferResponse> getAllStoreTransfers() {
                List<StoreTransfer> transfers = transferRepository.findAll().stream()
                                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                                .collect(Collectors.toList());
                return transfers.stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public StoreTransferResponse updateStoreTransfer(Long transferId, StoreTransferRequest request) {
                log.info("Updating store transfer: {}", transferId);

                StoreTransfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ù†Ù‚Ù„ Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                // Only allow updating PENDING transfers
                if (!"PENDING".equals(transfer.getTransferStatus())) {
                        throw new BadRequestException(
                                        "ÙŠÙ…ÙƒÙ† ØªØ­Ø¯ÙŠØ« Ø¹Ù…Ù„ÙŠØ§Øª Ø§Ù„Ù†Ù‚Ù„ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø± ÙÙ‚Ø·");
                }

                // Validate stores are different
                if (request.getFromStoreCode().equals(request.getToStoreCode())) {
                        throw new BadRequestException(
                                        "ÙŠØ¬Ø¨ Ø£Ù† ÙŠÙƒÙˆÙ† Ø§Ù„Ù…ØªØ¬Ø± Ø§Ù„Ù…ØµØ¯Ø± ÙˆØ§Ù„ÙˆØ¬Ù‡Ø© Ù…Ø®ØªÙ„ÙÙŠÙ†");
                }

                // Validate stores exist
                ProjectStore fromStore = storeRepository.findById(request.getFromStoreCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ø§Ù„Ù…Ø®Ø²Ù† Ø§Ù„Ù…ØµØ¯Ø± ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                ProjectStore toStore = storeRepository.findById(request.getToStoreCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ø§Ù„Ù…Ø®Ø²Ù† Ø§Ù„ÙˆØ¬Ù‡Ø© ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                transfer.setFromStore(fromStore);
                transfer.setToStore(toStore);
                transfer.setTransferDate(request.getTransferDate());
                transfer.setNotes(request.getNotes());

                // Clear and update transfer lines
                transfer.getTransferLines().clear();
                for (StoreTransferLineRequest lineRequest : request.getTransferLines()) {
                        StoreItem item = itemRepository.findById(lineRequest.getItemCode())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯: " + lineRequest.getItemCode()));

                        StoreTransferLine line = StoreTransferLine.builder()
                                        .storeTransfer(transfer)
                                        .item(item)
                                        .quantity(lineRequest.getQuantity())
                                        .notes(lineRequest.getNotes())
                                        .isDeleted(false)
                                        .build();

                        transfer.addTransferLine(line);
                }

                transfer = transferRepository.save(transfer);
                log.info("Store transfer updated successfully");
                return mapToResponse(transfer);
        }

        @Transactional
        public void deleteStoreTransfer(Long transferId) {
                log.info("Deleting store transfer: {}", transferId);

                StoreTransfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Ù†Ù‚Ù„ Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                transfer.setIsDeleted(true);
                transferRepository.save(transfer);
                log.info("Store transfer deleted successfully");
        }

        /**
         * CRITICAL: Complete transfer with ATOMIC operation
         * This ensures BOTH source decrease and destination increase happen together
         * If either fails, the entire transaction is rolled back
         */
        @Transactional
        public StoreTransferResponse completeTransfer(Long transferId) {
                log.info("Completing store transfer: {}", transferId);

                StoreTransfer transfer = transferRepository.findById(transferId)
                                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù†Ù‚Ù„ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                if (!"PENDING".equals(transfer.getTransferStatus())) {
                        throw new BadRequestException("Ø§Ù„Ù†Ù‚Ù„ Ù„ÙŠØ³ ÙÙŠ Ø­Ø§Ù„Ø© Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø±");
                }

                ProjectStore fromStore = transfer.getFromStore();
                ProjectStore toStore = transfer.getToStore();

                // ATOMIC OPERATION: Process all lines in a single transaction
                for (StoreTransferLine line : transfer.getTransferLines()) {
                        StoreItem item = itemRepository.findById(line.getItem().getItemCode())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Ø§Ù„ØµÙ†Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯"));

                        // Re-validate source balance (prevent race conditions)
                        StoreBalance sourceBalance = balanceRepository
                                        .findByStoreAndItem(fromStore.getStoreCode(), item.getItemCode())
                                        .orElseThrow(() -> new BadRequestException(
                                                        "Ù„Ù… ÙŠØªÙ… Ø§Ù„Ø¹Ø«ÙˆØ± Ø¹Ù„Ù‰ Ø±ØµÙŠØ¯ Ø§Ù„Ù…ØµØ¯Ø±"));

                        if (!sourceBalance.hasSufficientQuantity(line.getQuantity())) {
                                throw new BadRequestException(
                                                String.format("Ø§Ù„ÙƒÙ…ÙŠØ© ØºÙŠØ± ÙƒØ§ÙÙŠØ© Ø¹Ù†Ø¯ Ø§Ù„Ø¥ÙƒÙ…Ø§Ù„. Ø§Ù„Ø¹Ù†ØµØ±: %sØŒ Ø§Ù„Ù…ØªØ§Ø­: %s",
                                                                item.getItemName(),
                                                                sourceBalance.getAvailableQuantity()));
                        }

                        // CRITICAL ATOMIC OPERATION:
                        // Step 1: Decrease source store (negative quantity)
                        updateStoreBalance(fromStore, item, line.getQuantity().negate(), "TRANSFER_OUT", transferId);

                        // Step 2: Increase destination store (positive quantity)
                        updateStoreBalance(toStore, item, line.getQuantity(), "TRANSFER_IN", transferId);

                        log.info("Transfer line completed: item={}, quantity={}, from={}, to={}",
                                        item.getItemName(), line.getQuantity(),
                                        fromStore.getStoreName(), toStore.getStoreName());
                }

                // Update transfer status
                transfer.setTransferStatus("RECEIVED");
                transfer = transferRepository.save(transfer);

                log.info("Store transfer completed successfully: {} lines transferred",
                                transfer.getTransferLines().size());
                return mapToResponse(transfer);
        }

        private StoreTransferResponse mapToResponse(StoreTransfer transfer) {
                StoreTransferResponse.StoreTransferResponseBuilder builder = StoreTransferResponse.builder()
                                .transferId(transfer.getTransferId())
                                .transferNumber(transfer.getTransferNumber())
                                .transferDate(transfer.getTransferDate())
                                .transferStatus(transfer.getTransferStatus())
                                .notes(transfer.getNotes())
                                .createdDate(transfer.getCreatedDate())
                                .createdBy(transfer.getCreatedBy())
                                .modifiedDate(transfer.getModifiedDate())
                                .modifiedBy(transfer.getModifiedBy());

                if (transfer.getFromStore() != null) {
                        builder.fromStoreCode(transfer.getFromStore().getStoreCode())
                                        .fromStoreName(transfer.getFromStore().getStoreName());

                        if (transfer.getFromStore().getProject() != null) {
                                builder.fromProjectCode(transfer.getFromStore().getProject().getProjectCode())
                                                .fromProjectName(transfer.getFromStore().getProject().getProjectName());
                        }
                }

                if (transfer.getToStore() != null) {
                        builder.toStoreCode(transfer.getToStore().getStoreCode())
                                        .toStoreName(transfer.getToStore().getStoreName());

                        if (transfer.getToStore().getProject() != null) {
                                builder.toProjectCode(transfer.getToStore().getProject().getProjectCode())
                                                .toProjectName(transfer.getToStore().getProject().getProjectName());
                        }
                }

                if (transfer.getTransferLines() != null && !transfer.getTransferLines().isEmpty()) {
                        List<StoreTransferLineResponse> lineResponses = transfer.getTransferLines().stream()
                                        .filter(line -> !Boolean.TRUE.equals(line.getIsDeleted()))
                                        .map(this::mapLineToResponse)
                                        .collect(Collectors.toList());
                        builder.transferLines(lineResponses);
                }

                // Map createdBy to transferredBy and fetch transferredByName
                if (transfer.getCreatedBy() != null) {
                        builder.transferredBy(transfer.getCreatedBy());
                        employeeRepository.findById(transfer.getCreatedBy())
                                        .ifPresent(employee -> builder.transferredByName(employee.getEmployeeName()));
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

        private StoreTransferLineResponse mapLineToResponse(StoreTransferLine line) {
                StoreTransferLineResponse.StoreTransferLineResponseBuilder builder = StoreTransferLineResponse.builder()
                                .lineId(line.getLineId())
                                .quantity(line.getQuantity())
                                .notes(line.getNotes());

                if (line.getItem() != null) {
                        builder.itemCode(line.getItem().getItemCode())
                                        .itemName(line.getItem().getItemName());
                }

                return builder.build();
        }
}

package com.techno.backend.service;

import com.techno.backend.dto.warehouse.BalanceResponse;
import com.techno.backend.entity.StoreBalance;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.StoreBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreBalanceService {

    private final StoreBalanceRepository balanceRepository;

    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByStoreAndItem(Long storeCode, Long itemCode) {
        log.info("Retrieving balance for store: {} and item: {}", storeCode, itemCode);

        StoreBalance balance = balanceRepository.findByStoreAndItem(storeCode, itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ø±ØµÙŠØ¯ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ù„Ù„Ù…Ø®Ø²Ù†: " + storeCode + " ÙˆØ§Ù„ØµÙ†Ù: " + itemCode));

        return mapToResponse(balance);
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalancesByStore(Long storeCode) {
        log.info("Retrieving all balances for store: {}", storeCode);

        return balanceRepository.findByStoreCode(storeCode).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalancesByItem(Long itemCode) {
        log.info("Retrieving all balances for item: {}", itemCode);

        return balanceRepository.findByItemCode(itemCode).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getLowStockItems(BigDecimal threshold) {
        log.info("Retrieving low stock items with threshold: {}", threshold);

        return balanceRepository.findLowStockItems(threshold).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getItemsBelowReorderLevel() {
        log.info("Retrieving items below reorder level");

        return balanceRepository.findItemsBelowReorderLevel().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getAllBalances(Long storeCode) {
        log.info("Retrieving all balances - storeCode: {}", storeCode);

        if (storeCode != null) {
            return getBalancesByStore(storeCode);
        }

        return balanceRepository.findAllWithStock().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BalanceResponse mapToResponse(StoreBalance balance) {
        return BalanceResponse.builder()
                .balanceId(balance.getBalanceId())
                .storeCode(balance.getStore().getStoreCode())
                .storeName(balance.getStore().getStoreName())
                .storeName(balance.getStore().getStoreName())
                .itemCode(balance.getItem().getItemCode())
                .itemName(balance.getItem().getItemName())
                .itemName(balance.getItem().getItemName())
                .unitOfMeasure(balance.getItem().getUnitOfMeasure())
                .quantityOnHand(balance.getQuantityOnHand())
                .quantityReserved(balance.getQuantityReserved())
                .availableQuantity(balance.getAvailableQuantity())
                .lastTransactionDate(balance.getLastTransactionDate())
                .isBelowReorderLevel(balance.isBelowReorderLevel())
                .reorderLevel(balance.getItem().getReorderLevel())
                .build();
    }
}


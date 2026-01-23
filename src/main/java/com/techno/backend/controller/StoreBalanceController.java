package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.BalanceResponse;
import com.techno.backend.service.StoreBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/warehouse/balances")
@RequiredArgsConstructor
@Slf4j
public class StoreBalanceController {

    private final StoreBalanceService balanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> getAllBalances(
            @RequestParam(required = false) Long storeCode) {
        log.info("REST request to get all balances - storeCode: {}", storeCode);

        List<BalanceResponse> balances = balanceService.getAllBalances(storeCode);

        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/store/{storeCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> getBalancesByStore(@PathVariable Long storeCode) {
        log.info("REST request to get balances for store: {}", storeCode);

        List<BalanceResponse> balances = balanceService.getBalancesByStore(storeCode);

        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/item/{itemCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> getBalancesByItem(@PathVariable Long itemCode) {
        log.info("REST request to get balances for item: {}", itemCode);

        List<BalanceResponse> balances = balanceService.getBalancesByItem(itemCode);

        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @GetMapping("/store/{storeCode}/item/{itemCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalanceByStoreAndItem(
            @PathVariable Long storeCode,
            @PathVariable Long itemCode) {
        log.info("REST request to get balance for store: {} and item: {}", storeCode, itemCode);

        BalanceResponse balance = balanceService.getBalanceByStoreAndItem(storeCode, itemCode);

        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> getLowStockItems(
            @RequestParam(defaultValue = "10") BigDecimal threshold) {
        log.info("REST request to get low stock items with threshold: {}", threshold);

        List<BalanceResponse> items = balanceService.getLowStockItems(threshold);

        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/below-reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> getItemsBelowReorderLevel() {
        log.info("REST request to get items below reorder level");

        List<BalanceResponse> items = balanceService.getItemsBelowReorderLevel();

        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.StoreRequest;
import com.techno.backend.dto.warehouse.StoreResponse;
import com.techno.backend.dto.warehouse.StoreSummary;
import com.techno.backend.service.ProjectStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse/stores")
@RequiredArgsConstructor
@Slf4j
public class ProjectStoreController {

    private final ProjectStoreService storeService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(@Valid @RequestBody StoreRequest request) {
        log.info("REST request to create project store: {}", request.getStoreName());

        StoreResponse response = storeService.createStore(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("ØªÙ… Ø¥Ù†Ø´Ø§Ø¡ Ù…Ø®Ø²Ù† Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø¨Ù†Ø¬Ø§Ø­", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<StoreSummary>>> getAllStores() {
        log.info("REST request to get all project stores");

        List<StoreSummary> stores = storeService.getAllStores();

        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable Long id) {
        log.info("REST request to get project store: {}", id);

        StoreResponse response = storeService.getStoreById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/project/{projectCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getStoresByProject(@PathVariable Long projectCode) {
        log.info("REST request to get stores for project: {}", projectCode);

        List<StoreResponse> stores = storeService.getStoresByProject(projectCode);

        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody StoreRequest request) {
        log.info("REST request to update project store: {}", id);

        StoreResponse response = storeService.updateStore(id, request);

        return ResponseEntity.ok(ApiResponse.success("ØªÙ… ØªØ­Ø¯ÙŠØ« Ù…Ø®Ø²Ù† Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø¨Ù†Ø¬Ø§Ø­", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateStore(
            @PathVariable Long id,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        log.info("REST request to deactivate project store: {} (force: {})", id, force);

        if (force) {
            storeService.forceDeactivateStore(id);
            return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø­Ø°Ù Ø§Ù„Ù…Ø®Ø²Ù† Ù‚Ø³Ø±ÙŠØ§Ù‹ Ø¨Ù†Ø¬Ø§Ø­ (ØªÙ… ØªØ¬Ø§Ù‡Ù„ ÙØ­Øµ Ø§Ù„Ø£Ø±ØµØ¯Ø©)", null));
        } else {
            storeService.deactivateStore(id);
            return ResponseEntity.ok(ApiResponse.success("ØªÙ… Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ Ù…Ø®Ø²Ù† Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ Ø¨Ù†Ø¬Ø§Ø­", null));
        }
    }
}


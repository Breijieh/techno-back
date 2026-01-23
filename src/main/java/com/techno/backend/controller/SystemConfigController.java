package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.config.*;
import com.techno.backend.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {

    private final SystemConfigService configService;

    /**
     * GET /api/config - Get all configurations
     * Security: MANAGER, ADMIN
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConfigResponse>>> getAllConfigs() {
        log.info("GET /api/config - Fetching all configs");

        List<ConfigResponse> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * GET /api/config/{key} - Get config by key
     * Security: MANAGER, ADMIN
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigResponse>> getConfigByKey(
            @PathVariable("key") String configKey) {
        log.info("GET /api/config/{} - Fetching config by key", configKey);

        ConfigResponse config = configService.getConfigByKey(configKey);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * PUT /api/config/{key} - Update config value
     * Security: ADMIN only
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigResponse>> updateConfig(
            @PathVariable("key") String configKey,
            @Valid @RequestBody ConfigUpdateRequest request) {
        log.info("PUT /api/config/{} - Updating config", configKey);

        ConfigResponse updated = configService.updateConfig(configKey, request);
        return ResponseEntity.ok(ApiResponse.success("تم تحديث الإعدادات بنجاح", updated));
    }

    /**
     * POST /api/config/bulk - Update multiple configs
     * Security: ADMIN only
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConfigResponse>>> bulkUpdateConfigs(
            @Valid @RequestBody BulkConfigUpdateRequest request) {
        log.info("POST /api/config/bulk - Bulk updating {} configs", request.getConfigs().size());

        List<ConfigResponse> updated = configService.bulkUpdateConfigs(request);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("تم تحديث %d إعدادات بنجاح", updated.size()),
                updated));
    }

    /**
     * GET /api/config/categories - Group configs by category
     * Security: MANAGER, ADMIN
     */
    @GetMapping("/categories")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ConfigCategoryResponse>>> getConfigsByCategories() {
        log.info("GET /api/config/categories - Fetching configs grouped by categories");

        List<ConfigCategoryResponse> categories = configService.getConfigsByCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}

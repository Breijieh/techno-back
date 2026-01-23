package com.techno.backend.service;

import com.techno.backend.dto.config.*;
import com.techno.backend.entity.SystemConfig;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    /**
     * CRITICAL: Cached config retrieval
     * Cache key: "systemConfig::{configKey}"
     * This prevents database hits for every config access
     */
    @Cacheable(value = "systemConfig", key = "#configKey")
    @Transactional(readOnly = true)
    public String getConfigValue(String configKey) {
        log.info("Fetching config value for key: {}", configKey);

        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("الإعداد غير موجود: " + configKey));

        if (!"Y".equals(config.getIsActive())) {
            throw new BadRequestException("الإعداد غير نشط: " + configKey);
        }

        return config.getConfigValue();
    }

    @Cacheable(value = "systemConfig", key = "#configKey")
    @Transactional(readOnly = true)
    public ConfigResponse getConfigByKey(String configKey) {
        log.info("Fetching config by key: {}", configKey);

        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("الإعداد غير موجود: " + configKey));

        return mapToResponse(config);
    }

    /**
     * Get HR Manager employee number from system configuration.
     * Cached to prevent repeated database hits.
     *
     * @return Employee number of HR Manager
     */
    @Cacheable(value = "systemConfig", key = "'HR_MANAGER_EMPLOYEE_NO'")
    @Transactional(readOnly = true)
    public Long getHRManagerEmployeeNo() {
        log.debug("Fetching HR Manager employee number from config");
        try {
            String value = getConfigValue("HR_MANAGER_EMPLOYEE_NO");
            return Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to get HR Manager from config, using default fallback: 2", e);
            return 2L; // Fallback to default
        }
    }

    /**
     * Get Finance Manager employee number from system configuration.
     * Cached to prevent repeated database hits.
     *
     * @return Employee number of Finance Manager
     */
    @Cacheable(value = "systemConfig", key = "'FINANCE_MANAGER_EMPLOYEE_NO'")
    @Transactional(readOnly = true)
    public Long getFinanceManagerEmployeeNo() {
        log.debug("Fetching Finance Manager employee number from config");
        try {
            String value = getConfigValue("FINANCE_MANAGER_EMPLOYEE_NO");
            return Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to get Finance Manager from config, using default fallback: 3", e);
            return 3L; // Fallback to default
        }
    }

    /**
     * Get General Manager employee number from system configuration.
     * Cached to prevent repeated database hits.
     *
     * @return Employee number of General Manager
     */
    @Cacheable(value = "systemConfig", key = "'GENERAL_MANAGER_EMPLOYEE_NO'")
    @Transactional(readOnly = true)
    public Long getGeneralManagerEmployeeNo() {
        log.debug("Fetching General Manager employee number from config");
        try {
            String value = getConfigValue("GENERAL_MANAGER_EMPLOYEE_NO");
            return Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Failed to get General Manager from config, using default fallback: 1", e);
            return 1L; // Fallback to default
        }
    }

    @Transactional(readOnly = true)
    public List<ConfigResponse> getAllConfigs() {
        log.info("Fetching all active configs");

        List<SystemConfig> configs = configRepository.findAllActive();
        return configs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConfigCategoryResponse> getConfigsByCategories() {
        log.info("Fetching configs grouped by categories");

        List<SystemConfig> allConfigs = configRepository.findAllActive();

        Map<String, List<SystemConfig>> groupedByCategory = allConfigs.stream()
                .collect(Collectors.groupingBy(
                        config -> config.getConfigCategory() != null ? config.getConfigCategory() : "GENERAL"
                ));

        return groupedByCategory.entrySet().stream()
                .map(entry -> ConfigCategoryResponse.builder()
                        .category(entry.getKey())
                        .configs(entry.getValue().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * CRITICAL: Cache eviction on update
     * Clears cache for the specific config key
     */
    @CacheEvict(value = "systemConfig", key = "#configKey")
    @Transactional
    public ConfigResponse updateConfig(String configKey, ConfigUpdateRequest request) {
        log.info("Updating config: {}", configKey);

        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("الإعداد غير موجود: " + configKey));

        if (!"Y".equals(config.getIsEditable())) {
            throw new BadRequestException("الإعداد غير قابل للتعديل: " + configKey);
        }

        // Validate value based on type
        validateConfigValue(config.getConfigType(), request.getConfigValue());

        config.setConfigValue(request.getConfigValue());
        config = configRepository.save(config);

        log.info("Config updated successfully: {}", configKey);
        return mapToResponse(config);
    }

    /**
     * CRITICAL: Bulk update with full cache eviction
     * Clears entire cache to ensure consistency
     */
    @CacheEvict(value = "systemConfig", allEntries = true)
    @Transactional
    public List<ConfigResponse> bulkUpdateConfigs(BulkConfigUpdateRequest request) {
        log.info("Bulk updating {} configs", request.getConfigs().size());

        List<ConfigResponse> updated = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.getConfigs().entrySet()) {
            String configKey = entry.getKey();
            String configValue = entry.getValue();

            SystemConfig config = configRepository.findByConfigKey(configKey)
                    .orElseThrow(() -> new ResourceNotFoundException("الإعداد غير موجود: " + configKey));

            if (!"Y".equals(config.getIsEditable())) {
                log.warn("Skipping non-editable config: {}", configKey);
                continue;
            }

            validateConfigValue(config.getConfigType(), configValue);

            config.setConfigValue(configValue);
            config = configRepository.save(config);
            updated.add(mapToResponse(config));
        }

        log.info("Bulk update completed: {} configs updated", updated.size());
        return updated;
    }

    /**
     * CRITICAL: Type validation
     * Ensures config values match their declared type
     */
    private void validateConfigValue(String configType, String configValue) {
        try {
            switch (configType) {
                case "NUMBER":
                    Double.parseDouble(configValue);
                    break;
                case "BOOLEAN":
                    if (!configValue.matches("^(Y|N|TRUE|FALSE|true|false)$")) {
                        throw new BadRequestException("القيمة المنطقية يجب أن تكون Y/N أو TRUE/FALSE");
                    }
                    break;
                case "JSON":
                    // Basic JSON validation (starts with { or [)
                    if (!configValue.trim().matches("^[\\{\\[].*")) {
                        throw new BadRequestException("قيمة JSON يجب أن تبدأ بـ { أو [");
                    }
                    break;
                case "STRING":
                    // No validation needed
                    break;
                default:
                    throw new BadRequestException("نوع الإعداد غير معروف: " + configType);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("قيمة NUMBER غير صالحة: " + configValue);
        }
    }

    private ConfigResponse mapToResponse(SystemConfig config) {
        return ConfigResponse.builder()
                .configId(config.getConfigId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType())
                .configCategory(config.getConfigCategory())
                .configDescription(config.getConfigDescription())
                .isActive(config.getIsActive())
                .isEditable(config.getIsEditable())
                .defaultValue(config.getDefaultValue())
                .createdDate(config.getCreatedDate())
                .createdBy(config.getCreatedBy())
                .modifiedDate(config.getModifiedDate())
                .modifiedBy(config.getModifiedBy())
                .build();
    }
}

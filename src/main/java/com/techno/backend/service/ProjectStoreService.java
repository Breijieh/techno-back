package com.techno.backend.service;

import com.techno.backend.dto.warehouse.StoreRequest;
import com.techno.backend.dto.warehouse.StoreResponse;
import com.techno.backend.dto.warehouse.StoreSummary;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.ProjectStore;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.entity.Employee;
import com.techno.backend.repository.ProjectRepository;
import com.techno.backend.repository.ProjectStoreRepository;
import com.techno.backend.repository.EmployeeRepository;
import com.techno.backend.repository.StoreBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectStoreService {

    private final ProjectStoreRepository storeRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreBalanceRepository balanceRepository;

    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        log.info("Creating new project store: {}", request.getStoreName());

        Project project = projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getProjectCode()));

        if (storeRepository.existsByName(request.getStoreName(), request.getStoreName())) {
            throw new BadRequestException("Ø§Ø³Ù… Ø§Ù„Ù…ØªØ¬Ø± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Validate store manager if provided
        if (request.getStoreManagerId() != null) {
            employeeRepository.findById(request.getStoreManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getStoreManagerId()));
        }

        ProjectStore store = ProjectStore.builder()
                .project(project)
                .storeName(request.getStoreName())
                .storeName(request.getStoreName())
                .storeLocation(request.getStoreLocation())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .storeManagerId(request.getStoreManagerId())
                .build();

        store = storeRepository.save(store);
        log.info("Project store created successfully with code: {}", store.getStoreCode());

        return mapToResponse(store);
    }

    @Transactional
    public StoreResponse updateStore(Long storeCode, StoreRequest request) {
        log.info("Updating project store with code: {}", storeCode);

        ProjectStore store = storeRepository.findById(storeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + storeCode));

        Project project = projectRepository.findById(request.getProjectCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getProjectCode()));

        if (storeRepository.existsByNameExcludingId(request.getStoreName(), request.getStoreName(), storeCode)) {
            throw new BadRequestException("Ø§Ø³Ù… Ø§Ù„Ù…ØªØ¬Ø± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„");
        }

        // Validate store manager if provided
        if (request.getStoreManagerId() != null) {
            employeeRepository.findById(request.getStoreManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + request.getStoreManagerId()));
        }

        store.setProject(project);
        store.setStoreName(request.getStoreName());
        store.setStoreName(request.getStoreName());
        store.setStoreLocation(request.getStoreLocation());
        store.setStoreManagerId(request.getStoreManagerId());
        if (request.getIsActive() != null) {
            store.setIsActive(request.getIsActive());
        }

        store = storeRepository.save(store);
        log.info("Project store updated successfully: {}", storeCode);

        return mapToResponse(store);
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long storeCode) {
        log.info("Retrieving store with code: {}", storeCode);

        ProjectStore store = storeRepository.findById(storeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + storeCode));

        return mapToResponse(store);
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresByProject(Long projectCode) {
        log.info("Retrieving stores for project: {}", projectCode);

        projectRepository.findById(projectCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + projectCode));

        return storeRepository.findByProjectCode(projectCode).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreSummary> getAllStores() {
        log.info("Retrieving all project stores");

        return storeRepository.findAll().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateStore(Long storeCode) {
        log.info("Deactivating project store with code: {}", storeCode);

        ProjectStore store = storeRepository.findById(storeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + storeCode));

        // Check if store has non-deleted balances (using direct query to exclude deleted balances)
        long activeBalanceCount = balanceRepository.countByStoreCodeAndIsDeletedFalse(storeCode);
        if (activeBalanceCount > 0) {
            throw new BadRequestException("Ù„Ø§ ÙŠÙ…ÙƒÙ† Ø¥Ù„ØºØ§Ø¡ ØªÙØ¹ÙŠÙ„ Ù…ØªØ¬Ø± Ø¨Ù‡ Ø£Ø±ØµØ¯Ø© Ù…ÙˆØ¬ÙˆØ¯Ø©. ÙŠØ±Ø¬Ù‰ Ù†Ù‚Ù„ Ø¬Ù…ÙŠØ¹ Ø§Ù„Ù…Ø®Ø²ÙˆÙ† Ø£ÙˆÙ„Ø§Ù‹.");
        }

        store.setIsDeleted(true);
        storeRepository.save(store);

        log.info("Project store deactivated successfully: {}", storeCode);
    }

    /**
     * Force deactivate a store (soft delete) even if it has balances
     * This will delete the store regardless of balances - use with caution
     */
    @Transactional
    public void forceDeactivateStore(Long storeCode) {
        log.warn("FORCE deactivating project store with code: {} (bypassing balances check)", storeCode);

        ProjectStore store = storeRepository.findById(storeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ø§Ù„Ù…Ø®Ø²Ù† ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + storeCode));

        // No balance check - force delete
        store.setIsDeleted(true);
        storeRepository.save(store);

        log.warn("Project store force deactivated successfully: {} (balances check was bypassed)", storeCode);
    }

    private StoreResponse mapToResponse(ProjectStore store) {
        String managerName = null;
        if (store.getStoreManagerId() != null) {
            managerName = employeeRepository.findById(store.getStoreManagerId())
                    .map(Employee::getEmployeeName)
                    .orElse(null);
        }

        return StoreResponse.builder()
                .storeCode(store.getStoreCode())
                .projectCode(store.getProject().getProjectCode())
                .projectName(store.getProject().getProjectName())
                .projectName(store.getProject().getProjectName())
                .storeName(store.getStoreName())
                .storeName(store.getStoreName())
                .storeLocation(store.getStoreLocation())
                .isActive(store.getIsActive())
                .itemCount(store.getItemCount())
                .storeManagerId(store.getStoreManagerId())
                .storeManagerName(managerName)
                .createdDate(store.getCreatedDate())
                .createdBy(store.getCreatedBy())
                .modifiedDate(store.getModifiedDate())
                .modifiedBy(store.getModifiedBy())
                .build();
    }

    private StoreSummary mapToSummary(ProjectStore store) {
        String managerName = null;
        if (store.getStoreManagerId() != null) {
            managerName = employeeRepository.findById(store.getStoreManagerId())
                    .map(Employee::getEmployeeName)
                    .orElse(null);
        }

        return StoreSummary.builder()
                .storeCode(store.getStoreCode())
                .projectCode(store.getProject().getProjectCode())
                .projectName(store.getProject().getProjectName())
                .storeName(store.getStoreName())
                .storeName(store.getStoreName())
                .storeLocation(store.getStoreLocation())
                .isActive(store.getIsActive())
                .itemCount(store.getItemCount())
                .storeManagerId(store.getStoreManagerId())
                .storeManagerName(managerName)
                .build();
    }
}


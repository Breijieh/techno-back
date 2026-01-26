package com.techno.backend.service;

import com.techno.backend.dto.*;
import com.techno.backend.entity.*;
import com.techno.backend.exception.BadRequestException;
import com.techno.backend.exception.ResourceNotFoundException;
import com.techno.backend.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for Employee management.
 * Handles business logic for employee CRUD operations, search, and document
 * expiry tracking.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final ProjectRepository projectRepository;
    private final SalaryBreakdownPercentageRepository salaryBreakdownRepository;
    private final EmpPayrollTransactionRepository empPayrollTransactionRepository;
    private final UserService userService;

    private static final int DEFAULT_DOCUMENT_EXPIRY_THRESHOLD_DAYS = 14;

    /**
     * Get all employees with pagination
     */
    @Transactional(readOnly = true)
    public EmployeeListResponse getAllEmployees(int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching all employees - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Employee> employeePage = employeeRepository.findAll(pageable);
        List<EmployeeResponse> employeeResponses = employeePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return EmployeeListResponse.builder()
                .employees(employeeResponses)
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .currentPage(employeePage.getNumber())
                .pageSize(employeePage.getSize())
                .hasNext(employeePage.hasNext())
                .hasPrevious(employeePage.hasPrevious())
                .build();
    }

    /**
     * Search employees with multiple filters
     */
    @Transactional(readOnly = true)
    public EmployeeListResponse searchEmployees(EmployeeSearchRequest searchRequest) {
        log.info("Searching employees with criteria: {}", searchRequest);

        Specification<Employee> spec = buildSearchSpecification(searchRequest);

        Sort sort = searchRequest.getSortDirection().equalsIgnoreCase("desc")
                ? Sort.by(searchRequest.getSortBy()).descending()
                : Sort.by(searchRequest.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);
        List<EmployeeResponse> employeeResponses = employeePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return EmployeeListResponse.builder()
                .employees(employeeResponses)
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .currentPage(employeePage.getNumber())
                .pageSize(employeePage.getSize())
                .hasNext(employeePage.hasNext())
                .hasPrevious(employeePage.hasPrevious())
                .build();
    }

    /**
     * Get employee by ID
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long employeeNo) {
        log.info("Fetching employee by ID: {}", employeeNo);
        Employee employee = findEmployeeByIdOrThrow(employeeNo);
        return mapToResponse(employee);
    }

    /**
     * Create new employee
     */
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.info("Creating new employee: {}", request.getEmployeeName());

        // Resolve National ID if missing (for foreign employees)
        resolveNationalId(request);

        // Validate national ID uniqueness
        if (employeeRepository.existsByNationalId(request.getNationalId())) {
            throw new BadRequestException(
                    "Ø±Ù‚Ù… Ø§Ù„Ù‡ÙˆÙŠØ© Ø§Ù„ÙˆØ·Ù†ÙŠØ© Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getNationalId());
        }

        // Validate contract type exists
        validateContractType(request.getEmpContractType());

        // Validate department if provided
        if (request.getPrimaryDeptCode() != null) {
            validateDepartment(request.getPrimaryDeptCode());
        }

        // Validate project if provided
        if (request.getPrimaryProjectCode() != null) {
            validateProject(request.getPrimaryProjectCode());
        }

        // Create employee entity
        Employee employee = mapToEntity(request);
        employee = employeeRepository.save(employee);

        log.info("Employee created successfully with ID: {}", employee.getEmployeeNo());

        // Initialize salary breakdown transactions
        initializeSalaryBreakdown(employee);

        // Create User Account if credentials provided
        if (request.getUsername() != null && request.getPassword() != null) {
            log.info("Automatically creating user account for employee: {}", employee.getEmployeeNo());
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername(request.getUsername());
            registerRequest.setPassword(request.getPassword());
            registerRequest.setNationalId(request.getNationalId());

            // Set user type if provided, otherwise default to EMPLOYEE
            if (request.getUserType() != null && !request.getUserType().isEmpty()) {
                try {
                    registerRequest.setUserType(UserAccount.UserType.valueOf(request.getUserType()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid user type provided: {}. Defaulting to EMPLOYEE.", request.getUserType());
                    registerRequest.setUserType(UserAccount.UserType.EMPLOYEE);
                }
            } else {
                registerRequest.setUserType(UserAccount.UserType.EMPLOYEE);
            }

            registerRequest.setEmployeeNo(employee.getEmployeeNo());
            userService.createUser(registerRequest);
        }

        return mapToResponse(employee);
    }

    /**
     * Update existing employee
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long employeeNo, EmployeeRequest request) {
        log.info("Updating employee ID: {}", employeeNo);

        Employee employee = findEmployeeByIdOrThrow(employeeNo);

        // Resolve National ID if missing (for foreign employees)
        resolveNationalId(request);

        // Validate national ID uniqueness (excluding current employee)
        if (employeeRepository.existsByNationalIdAndNotEmployeeNo(request.getNationalId(), employeeNo)) {
            throw new BadRequestException(
                    "Ø±Ù‚Ù… Ø§Ù„Ù‡ÙˆÙŠØ© Ø§Ù„ÙˆØ·Ù†ÙŠØ© Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø§Ù„ÙØ¹Ù„: " + request.getNationalId());
        }

        // Validate contract type
        validateContractType(request.getEmpContractType());

        // Validate department if provided
        if (request.getPrimaryDeptCode() != null) {
            validateDepartment(request.getPrimaryDeptCode());
        }

        // Validate project if provided
        if (request.getPrimaryProjectCode() != null) {
            validateProject(request.getPrimaryProjectCode());
        }

        // Check if salary or category changed - need to recalculate breakdown
        boolean salaryChanged = !employee.getMonthlySalary().equals(request.getMonthlySalary());
        boolean categoryChanged = !employee.getEmployeeCategory().equals(request.getEmployeeCategory());

        // Update employee fields
        updateEmployeeFields(employee, request);
        employee = employeeRepository.save(employee);

        // Recalculate salary breakdown if salary or category changed
        if (salaryChanged || categoryChanged) {
            log.info("Salary or category changed, recalculating salary breakdown for employee: {}", employeeNo);
            recalculateSalaryBreakdown(employee);
        }

        log.info("Employee updated successfully: {}", employeeNo);
        return mapToResponse(employee);
    }

    /**
     * Delete employee (soft delete by changing status to TERMINATED)
     */
    @Transactional
    public void deleteEmployee(Long employeeNo) {
        log.info("Deleting employee ID: {}", employeeNo);

        Employee employee = findEmployeeByIdOrThrow(employeeNo);
        employee.setEmploymentStatus("TERMINATED");
        employee.setTerminationDate(LocalDate.now());
        employee.setTerminationReason("Deleted by system administrator");

        employeeRepository.save(employee);
        log.info("Employee soft-deleted successfully: {}", employeeNo);
    }

    /**
     * Get employees by department
     */
    @Transactional(readOnly = true)
    public EmployeeListResponse getEmployeesByDepartment(Long deptCode, int page, int size) {
        log.info("Fetching employees for department: {}", deptCode);

        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeNo").ascending());
        Page<Employee> employeePage = employeeRepository.findByPrimaryDeptCode(deptCode, pageable);

        List<EmployeeResponse> employeeResponses = employeePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return buildEmployeeListResponse(employeePage, employeeResponses);
    }

    /**
     * Get employees by project
     */
    @Transactional(readOnly = true)
    public EmployeeListResponse getEmployeesByProject(Long projectCode, int page, int size) {
        log.info("Fetching employees for project: {}", projectCode);

        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeNo").ascending());
        Page<Employee> employeePage = employeeRepository.findByPrimaryProjectCode(projectCode, pageable);

        List<EmployeeResponse> employeeResponses = employeePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return buildEmployeeListResponse(employeePage, employeeResponses);
    }

    /**
     * Get employees with expiring documents (passport or residency)
     */
    @Transactional(readOnly = true)
    public List<DocumentExpiryResponse> getEmployeesWithExpiringDocuments(Integer daysThreshold) {
        return getEmployeesWithExpiringDocuments(daysThreshold, null);
    }

    @Transactional(readOnly = true)
    public List<DocumentExpiryResponse> getEmployeesWithExpiringDocuments(Integer daysThreshold, Long employeeNo) {
        log.info("Fetching employees with documents expiring within {} days{}",
                daysThreshold, employeeNo != null ? " for employee: " + employeeNo : "");

        int threshold = daysThreshold != null ? daysThreshold : DEFAULT_DOCUMENT_EXPIRY_THRESHOLD_DAYS;
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = today.plusDays(threshold);

        List<Employee> employees;
        if (employeeNo != null) {
            employees = employeeRepository.findEmployeeWithExpiringDocuments(employeeNo, today, expiryDate);
        } else {
            employees = employeeRepository.findEmployeesWithExpiringDocuments(today, expiryDate);
        }

        return employees.stream()
                .map(this::mapToDocumentExpiryResponse)
                .collect(Collectors.toList());
    }

    // ===== Private Helper Methods =====

    /**
     * Find employee by ID or throw exception
     */
    private Employee findEmployeeByIdOrThrow(Long employeeNo) {
        return employeeRepository.findById(employeeNo)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Ø§Ù„Ù…ÙˆØ¸Ù ØºÙŠØ± Ù…ÙˆØ¬ÙˆØ¯ Ø¨Ø±Ù‚Ù…: " + employeeNo));
    }

    /**
     * Validate contract type exists
     */
    private void validateContractType(String contractTypeCode) {
        if (!contractTypeRepository.existsById(contractTypeCode)) {
            throw new BadRequestException("Ù†ÙˆØ¹ Ø§Ù„Ø¹Ù‚Ø¯ ØºÙŠØ± ØµØ§Ù„Ø­: " + contractTypeCode);
        }
    }

    /**
     * Validate department exists
     */
    private void validateDepartment(Long deptCode) {
        if (!departmentRepository.existsById(deptCode)) {
            throw new BadRequestException("Ø±Ù…Ø² Ø§Ù„Ù‚Ø³Ù… ØºÙŠØ± ØµØ§Ù„Ø­: " + deptCode);
        }
    }

    /**
     * Validate project exists
     */
    private void validateProject(Long projectCode) {
        if (!projectRepository.existsById(projectCode)) {
            throw new BadRequestException("Ø±Ù…Ø² Ø§Ù„Ù…Ø´Ø±ÙˆØ¹ ØºÙŠØ± ØµØ§Ù„Ø­: " + projectCode);
        }
    }

    /**
     * Initialize salary breakdown transactions when creating new employee
     */
    private void initializeSalaryBreakdown(Employee employee) {
        log.info("Initializing salary breakdown for employee: {} ({})",
                employee.getEmployeeNo(), employee.getEmployeeCategory());

        List<SalaryBreakdownPercentage> breakdowns = salaryBreakdownRepository
                .findByEmployeeCategory(employee.getEmployeeCategory());

        if (breakdowns.isEmpty()) {
            log.warn("No salary breakdown percentages found for category: {}", employee.getEmployeeCategory());
            return;
        }

        List<EmpPayrollTransaction> transactions = new ArrayList<>();
        for (SalaryBreakdownPercentage breakdown : breakdowns) {
            BigDecimal amount = breakdown.calculateAmount(employee.getMonthlySalary());

            EmpPayrollTransaction transaction = EmpPayrollTransaction.builder()
                    .employeeNo(employee.getEmployeeNo())
                    .transTypeCode(breakdown.getTransTypeCode())
                    .transAmount(amount)
                    .transPercentage(breakdown.getSalaryPercentage())
                    .effectiveDate(employee.getHireDate())
                    .isActive("Y")
                    .build();

            transactions.add(transaction);
        }

        empPayrollTransactionRepository.saveAll(transactions);
        log.info("Created {} salary breakdown transactions for employee: {}",
                transactions.size(), employee.getEmployeeNo());
    }

    /**
     * Recalculate salary breakdown when salary or category changes
     */
    private void recalculateSalaryBreakdown(Employee employee) {
        // Deactivate existing transactions
        List<EmpPayrollTransaction> existingTransactions = empPayrollTransactionRepository
                .findActiveByEmployeeNo(employee.getEmployeeNo());
        existingTransactions.forEach(t -> t.setIsActive("N"));
        empPayrollTransactionRepository.saveAll(existingTransactions);

        // Create new transactions
        initializeSalaryBreakdown(employee);
    }

    /**
     * Build search specification from search criteria
     */
    private Specification<Employee> buildSearchSpecification(EmployeeSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search text (name or national ID)
            if (searchRequest.getSearchText() != null && !searchRequest.getSearchText().isBlank()) {
                String originalSearchText = searchRequest.getSearchText().toLowerCase();
                String normalizedSearchText = normalizeArabicText(originalSearchText);
                String likePattern = "%" + normalizedSearchText + "%";

                // Normalize DB columns for comparison
                jakarta.persistence.criteria.Expression<String> employeeNameExp = root.get("employeeName");
                jakarta.persistence.criteria.Expression<String> normalizedNameExp = applyArabicNormalization(
                        employeeNameExp, criteriaBuilder);

                // For National ID, we just check original and normalized text just in case,
                // though it's usually numeric
                // But sometimes people search name in the "search text" field which covers both
                jakarta.persistence.criteria.Expression<String> nationalIdExp = root.get("nationalId");

                Predicate namePredicate = criteriaBuilder.or(
                        // Match normalized name against normalized query
                        criteriaBuilder.like(normalizedNameExp, likePattern),
                        // Also match original (non-normalized) just in case
                        criteriaBuilder.like(criteriaBuilder.lower(employeeNameExp), "%" + originalSearchText + "%"),
                        // Match national ID
                        criteriaBuilder.like(criteriaBuilder.lower(nationalIdExp), "%" + originalSearchText + "%"));
                predicates.add(namePredicate);
            }

            // Filter by employee category
            if (searchRequest.getEmployeeCategory() != null && !searchRequest.getEmployeeCategory().isBlank()) {
                predicates
                        .add(criteriaBuilder.equal(root.get("employeeCategory"), searchRequest.getEmployeeCategory()));
            }

            // Filter by employment status
            if (searchRequest.getEmploymentStatus() != null && !searchRequest.getEmploymentStatus().isBlank()) {
                predicates
                        .add(criteriaBuilder.equal(root.get("employmentStatus"), searchRequest.getEmploymentStatus()));
            }

            // Filter by contract type
            if (searchRequest.getContractType() != null && !searchRequest.getContractType().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("empContractType"), searchRequest.getContractType()));
            }

            // Filter by department
            if (searchRequest.getDepartmentCode() != null) {
                predicates.add(criteriaBuilder.equal(root.get("primaryDeptCode"), searchRequest.getDepartmentCode()));
            }

            // Filter by project
            if (searchRequest.getProjectCode() != null) {
                predicates.add(criteriaBuilder.equal(root.get("primaryProjectCode"), searchRequest.getProjectCode()));
            }

            // Filter by nationality
            if (searchRequest.getNationality() != null && !searchRequest.getNationality().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("nationality"), searchRequest.getNationality()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Normalize Arabic text for search.
     * Unifies Alef forms, Ta Marbuta/Ha, and Ya/Alef Maqsura.
     */
    /**
     * Normalize Arabic text for search.
     * Unifies Alef forms, Ta Marbuta/Ha, Ya/Alef Maqsura, and removes Tashkeel
     * (Diacritics).
     */
    private String normalizeArabicText(String text) {
        if (text == null)
            return null;
        text = text.replaceAll("[\\u064B-\\u065F]", ""); // Remove all Tashkeel/Diacritics
        return text
                .replaceAll("[أإآ]", "ا") // Normalize Alefs
                .replaceAll("ة", "ه") // Normalize Ta Marbuta to Ha
                .replaceAll("ى", "ي"); // Normalize Alef Maqsura to Ya
    }

    /**
     * Build a CriteriaBuilder expression to normalize Arabic text in the database.
     * Applies nested REPLACE functions for chars and diacritics.
     */
    private jakarta.persistence.criteria.Expression<String> applyArabicNormalization(
            jakarta.persistence.criteria.Expression<String> expression,
            jakarta.persistence.criteria.CriteriaBuilder cb) {

        jakarta.persistence.criteria.Expression<String> exp = expression;

        // Strip Diacritics (Tashkeel) - Range 064B to 0652 usually
        // Common ones: Fathatan, Dammatan, Kasratan, Fatha, Damma, Kasra, Shadda, Sukun
        char[] diacritics = {
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652'
        };

        for (char d : diacritics) {
            exp = cb.function("REPLACE", String.class, exp, cb.literal(String.valueOf(d)), cb.literal(""));
        }

        // Normalize Alefs (أ, إ, آ -> ا)
        exp = cb.function("REPLACE", String.class, exp, cb.literal("أ"), cb.literal("ا"));
        exp = cb.function("REPLACE", String.class, exp, cb.literal("إ"), cb.literal("ا"));
        exp = cb.function("REPLACE", String.class, exp, cb.literal("آ"), cb.literal("ا"));

        // Normalize Ta Marbuta (ة -> ه)
        exp = cb.function("REPLACE", String.class, exp, cb.literal("ة"), cb.literal("ه"));

        // Normalize Alef Maqsura (ى -> ي)
        exp = cb.function("REPLACE", String.class, exp, cb.literal("ى"), cb.literal("ي"));

        return exp;
    }

    /**
     * Map Employee entity to EmployeeResponse DTO
     */
    private EmployeeResponse mapToResponse(Employee employee) {
        EmployeeResponse response = EmployeeResponse.builder()
                .employeeNo(employee.getEmployeeNo())
                .employeeName(employee.getEmployeeName())
                .nationalId(employee.getNationalId())
                .nationality(employee.getNationality())
                .employeeCategory(employee.getEmployeeCategory())
                .passportNo(employee.getPassportNo())
                .passportExpiryDate(employee.getPassportExpiryDate())
                .residencyNo(employee.getResidencyNo())
                .residencyExpiryDate(employee.getResidencyExpiryDate())
                .hireDate(employee.getHireDate())
                .terminationDate(employee.getTerminationDate())
                .employmentStatus(employee.getEmploymentStatus())
                .terminationReason(employee.getTerminationReason())
                .socialInsuranceNo(employee.getSocialInsuranceNo())
                .empContractType(employee.getEmpContractType())
                .primaryDeptCode(employee.getPrimaryDeptCode())
                .primaryProjectCode(employee.getPrimaryProjectCode())
                .monthlySalary(employee.getMonthlySalary())
                .leaveBalanceDays(employee.getLeaveBalanceDays())
                .email(employee.getEmail())
                .mobile(employee.getMobile())
                .yearsOfService(employee.getYearsOfService())
                .monthsOfService(employee.getMonthsOfService())
                .daysUntilPassportExpiry(employee.getDaysUntilPassportExpiry())
                .daysUntilResidencyExpiry(employee.getDaysUntilResidencyExpiry())
                .passportExpiringSoon(employee.isPassportExpiringSoon(DEFAULT_DOCUMENT_EXPIRY_THRESHOLD_DAYS))
                .residencyExpiringSoon(employee.isResidencyExpiringSoon(DEFAULT_DOCUMENT_EXPIRY_THRESHOLD_DAYS))
                .createdDate(employee.getCreatedDate())
                .modifiedDate(employee.getModifiedDate())
                .build();

        // Add department info if available
        if (employee.getPrimaryDepartment() != null) {
            response.setPrimaryDeptName(employee.getPrimaryDepartment().getDeptName());
        }

        return response;
    }

    /**
     * Map EmployeeRequest DTO to Employee entity
     */
    private Employee mapToEntity(EmployeeRequest request) {
        return Employee.builder()
                .employeeName(request.getEmployeeName())
                .nationalId(request.getNationalId())
                .nationality(request.getNationality())
                .employeeCategory(request.getEmployeeCategory())
                .passportNo(request.getPassportNo())
                .passportExpiryDate(request.getPassportExpiryDate())
                .residencyNo(request.getResidencyNo())
                .residencyExpiryDate(request.getResidencyExpiryDate())
                .hireDate(request.getHireDate())
                .terminationDate(request.getTerminationDate())
                .employmentStatus(request.getEmploymentStatus() != null ? request.getEmploymentStatus() : "ACTIVE")
                .terminationReason(request.getTerminationReason())
                .socialInsuranceNo(request.getSocialInsuranceNo())
                .empContractType(request.getEmpContractType())
                .primaryDeptCode(request.getPrimaryDeptCode())
                .primaryProjectCode(request.getPrimaryProjectCode())
                .monthlySalary(request.getMonthlySalary())
                .leaveBalanceDays(
                        request.getLeaveBalanceDays() != null ? request.getLeaveBalanceDays()
                                : BigDecimal.valueOf(30.0))
                .email(request.getEmail())
                .mobile(request.getMobile())
                .build();
    }

    /**
     * Update employee fields from request
     */
    private void updateEmployeeFields(Employee employee, EmployeeRequest request) {
        employee.setEmployeeName(request.getEmployeeName());
        employee.setNationalId(request.getNationalId());
        employee.setNationality(request.getNationality());
        employee.setEmployeeCategory(request.getEmployeeCategory());
        employee.setPassportNo(request.getPassportNo());
        employee.setPassportExpiryDate(request.getPassportExpiryDate());
        employee.setResidencyNo(request.getResidencyNo());
        employee.setResidencyExpiryDate(request.getResidencyExpiryDate());
        employee.setHireDate(request.getHireDate());
        employee.setTerminationDate(request.getTerminationDate());
        employee.setEmploymentStatus(request.getEmploymentStatus());
        employee.setTerminationReason(request.getTerminationReason());
        employee.setSocialInsuranceNo(request.getSocialInsuranceNo());
        employee.setEmpContractType(request.getEmpContractType());
        employee.setPrimaryDeptCode(request.getPrimaryDeptCode());
        employee.setPrimaryProjectCode(request.getPrimaryProjectCode());
        employee.setMonthlySalary(request.getMonthlySalary());
        employee.setLeaveBalanceDays(request.getLeaveBalanceDays());
        employee.setEmail(request.getEmail());
        employee.setMobile(request.getMobile());
    }

    /**
     * Map Employee to DocumentExpiryResponse
     */
    private DocumentExpiryResponse mapToDocumentExpiryResponse(Employee employee) {
        LocalDate today = LocalDate.now();

        return DocumentExpiryResponse.builder()
                .employeeNo(employee.getEmployeeNo())
                .employeeName(employee.getEmployeeName())
                .nationalId(employee.getNationalId())
                .employeeCategory(employee.getEmployeeCategory())
                .passportNo(employee.getPassportNo())
                .passportExpiryDate(employee.getPassportExpiryDate())
                .daysUntilPassportExpiry(employee.getDaysUntilPassportExpiry())
                .passportExpired(employee.getPassportExpiryDate() != null &&
                        employee.getPassportExpiryDate().isBefore(today))
                .passportExpiringSoon(employee.isPassportExpiringSoon(DEFAULT_DOCUMENT_EXPIRY_THRESHOLD_DAYS))
                .residencyNo(employee.getResidencyNo())
                .residencyExpiryDate(employee.getResidencyExpiryDate())
                .daysUntilResidencyExpiry(employee.getDaysUntilResidencyExpiry())
                .residencyExpired(employee.getResidencyExpiryDate() != null &&
                        employee.getResidencyExpiryDate().isBefore(today))
                .residencyExpiringSoon(employee.isResidencyExpiringSoon(DEFAULT_DOCUMENT_EXPIRY_THRESHOLD_DAYS))
                .email(employee.getEmail())
                .mobile(employee.getMobile())
                .primaryDeptCode(employee.getPrimaryDeptCode())
                .primaryDeptName(
                        employee.getPrimaryDepartment() != null ? employee.getPrimaryDepartment().getDeptName()
                                : null)
                .primaryProjectCode(employee.getPrimaryProjectCode())
                .build();
    }

    /**
     * Build EmployeeListResponse from Page and employee responses
     */
    private EmployeeListResponse buildEmployeeListResponse(Page<Employee> employeePage,
            List<EmployeeResponse> employeeResponses) {
        return EmployeeListResponse.builder()
                .employees(employeeResponses)
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .currentPage(employeePage.getNumber())
                .pageSize(employeePage.getSize())
                .hasNext(employeePage.hasNext())
                .hasPrevious(employeePage.hasPrevious())
                .build();
    }

    /**
     * Helper to resolve National ID from Residency or Passport if not provided.
     */
    private void resolveNationalId(EmployeeRequest request) {
        if (request.getNationalId() == null || request.getNationalId().trim().isEmpty()) {
            if ("F".equals(request.getEmployeeCategory())) {
                if (request.getResidencyNo() != null && !request.getResidencyNo().trim().isEmpty()) {
                    request.setNationalId(request.getResidencyNo());
                } else if (request.getPassportNo() != null && !request.getPassportNo().trim().isEmpty()) {
                    request.setNationalId(request.getPassportNo());
                } else {
                    throw new BadRequestException("رقم الهوية (أو الإقامة/الجواز للمقيمين) مطلوب");
                }
            } else {
                throw new BadRequestException("رقم الهوية الوطنية مطلوب للسعوديين");
            }
        }
    }
}

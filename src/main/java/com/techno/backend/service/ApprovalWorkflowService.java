package com.techno.backend.service;

import com.techno.backend.entity.*;
import com.techno.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing multi-level approval workflows.
 *
 * This service provides reusable approval workflow logic for all request types:
 * - Leave requests (VAC)
 * - Loan requests (LOAN)
 * - Salary raise requests (INCR)
 * - Loan postponement (POSTLOAN)
 * - Payroll approval (PAYROLL)
 * - Project payment requests (PROJ_PAYMENT)
 * - Project employee transfers (PROJ_TRANSFER)
 * - Manual attendance requests (MANUAL_ATTENDANCE)
 *
 * Features:
 * - Dynamic approver resolution based on function calls
 * - Multi-level approval chains
 * - Department/Project specific approval flows
 * - Automatic next approver calculation
 *
 * Approval Status Codes:
 * - N: New (needs approval)
 * - A: Approved
 * - R: Rejected
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 3 - Approval System
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final RequestsApprovalSetRepository approvalSetRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Initialize approval workflow for a new request.
     *
     * Sets the next approver and next approval level based on the approval chain
     * configuration.
     *
     * @param requestType Request type (VAC, LOAN, INCR, POSTLOAN, PAYROLL,
     *                    PROJ_PAYMENT, PROJ_TRANSFER)
     * @param employeeNo  Employee submitting the request
     * @param deptCode    Employee's department
     * @param projectCode Employee's project
     * @return ApprovalInfo containing next approver and level
     */
    @Transactional(readOnly = true)
    public ApprovalInfo initializeApproval(String requestType, Long employeeNo,
            Long deptCode, Long projectCode) {
        log.info("Initializing approval for request type: {}, employee: {}", requestType, employeeNo);

        // Get the first approval level for this request type
        List<RequestsApprovalSet> approvalChain = getApprovalChain(requestType, deptCode, projectCode);

        if (approvalChain.isEmpty()) {
            throw new RuntimeException("لم يتم تكوين سلسلة الموافقة لنوع الطلب: " + requestType);
        }

        RequestsApprovalSet firstLevel = approvalChain.get(0);
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("الموظف غير موجود: " + employeeNo));

        Long nextApprover = resolveApprover(firstLevel.getFunctionCall(), employee, deptCode, projectCode);

        log.info("Approval initialized: Level {} → Approver {}", firstLevel.getLevelNo(), nextApprover);

        return ApprovalInfo.builder()
                .nextApproval(nextApprover)
                .nextAppLevel(firstLevel.getLevelNo())
                .nextAppLevelName(getFriendlyLevelName(firstLevel.getFunctionCall()))
                .transStatus("N") // Needs approval
                .build();
    }

    /**
     * Move request to next approval level.
     *
     * Called when current level approves the request.
     *
     * @param requestType  Request type
     * @param currentLevel Current approval level
     * @param employeeNo   Employee who submitted the request
     * @param deptCode     Department
     * @param projectCode  Project
     * @return ApprovalInfo for next level, or null if this was the final level
     */
    @Transactional(readOnly = true)
    public ApprovalInfo moveToNextLevel(String requestType, Integer currentLevel,
            Long employeeNo, Long deptCode, Long projectCode) {
        log.info("Moving to next approval level for type: {}, current level: {}", requestType, currentLevel);

        // Check if current user is ADMIN - Immediate Approval Bypass
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.info("Admin user bypassing remaining approval levels. Request fully approved.");
            return ApprovalInfo.builder()
                    .nextApproval(null)
                    .nextAppLevel(null)
                    .transStatus("A") // Fully approved
                    .build();
        }

        List<RequestsApprovalSet> approvalChain = getApprovalChain(requestType, deptCode, projectCode);

        // Find current level in chain
        Optional<RequestsApprovalSet> currentLevelOpt = approvalChain.stream()
                .filter(a -> a.getLevelNo().equals(currentLevel))
                .findFirst();

        if (currentLevelOpt.isEmpty()) {
            throw new RuntimeException("مستوى الموافقة الحالي غير موجود: " + currentLevel);
        }

        RequestsApprovalSet currentLevelConfig = currentLevelOpt.get();

        // Check if this is the final level
        if (currentLevelConfig.isFinalLevel()) {
            log.info("Final approval level reached. Request will be approved.");
            return ApprovalInfo.builder()
                    .nextApproval(null)
                    .nextAppLevel(null)
                    .transStatus("A") // Fully approved
                    .build();
        }

        // Find next level
        Optional<RequestsApprovalSet> nextLevelOpt = approvalChain.stream()
                .filter(a -> a.getLevelNo() > currentLevel)
                .min((a, b) -> a.getLevelNo().compareTo(b.getLevelNo()));

        if (nextLevelOpt.isEmpty()) {
            log.warn("No next level found after level {}. Approving request.", currentLevel);
            return ApprovalInfo.builder()
                    .nextApproval(null)
                    .nextAppLevel(null)
                    .transStatus("A")
                    .build();
        }

        RequestsApprovalSet nextLevel = nextLevelOpt.get();
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("الموظف غير موجود: " + employeeNo));

        Long nextApprover = resolveApprover(nextLevel.getFunctionCall(), employee, deptCode, projectCode);

        log.info("Next approval level: {} → Approver {}", nextLevel.getLevelNo(), nextApprover);

        return ApprovalInfo.builder()
                .nextApproval(nextApprover)
                .nextAppLevel(nextLevel.getLevelNo())
                .nextAppLevelName(getFriendlyLevelName(nextLevel.getFunctionCall()))
                .transStatus("N") // Still needs approval
                .build();
    }

    /**
     * Get the approval chain for a specific request type.
     *
     * Uses existing repository method that handles department/project specific
     * chains.
     *
     * @param requestType Request type
     * @param deptCode    Department code (can be null)
     * @param projectCode Project code (can be null)
     * @return Ordered list of approval levels
     */
    @Transactional(readOnly = true)
    public List<RequestsApprovalSet> getApprovalChain(String requestType, Long deptCode, Long projectCode) {
        // Try department specific first
        if (deptCode != null) {
            List<RequestsApprovalSet> chain = approvalSetRepository
                    .findApprovalFlowByRequestTypeAndDepartment(requestType, deptCode);
            if (!chain.isEmpty()) {
                log.debug("Using department specific approval chain for {}", requestType);
                return chain;
            }
        }

        // Try project specific
        if (projectCode != null) {
            List<RequestsApprovalSet> chain = approvalSetRepository
                    .findApprovalFlowByRequestTypeAndProject(requestType, projectCode);
            if (!chain.isEmpty()) {
                log.debug("Using project specific approval chain for {}", requestType);
                return chain;
            }
        }

        // Use global chain
        log.debug("Using global approval chain for {}", requestType);
        return approvalSetRepository.findActiveApprovalFlowByRequestType(requestType);
    }

    /**
     * Resolve approver employee number based on function call.
     *
     * Supported functions:
     * - GetProjectManager: Project manager
     * - GetHRManager: HR manager from system config
     * - GetFinManager: Finance manager from system config
     * - GetGeneralManager: General manager from system config
     *
     * @param functionCall Function to execute
     * @param employee     Employee submitting the request
     * @param deptCode     Department code
     * @param projectCode  Project code
     * @return Approver employee number
     */
    private Long resolveApprover(String functionCall, Employee employee, Long deptCode, Long projectCode) {
        log.debug("Resolving approver using function: {}", functionCall);

        return switch (functionCall) {
            case "GetDirectManager" -> {
                // Resolve direct manager from employee's department
                if (deptCode == null) {
                    log.warn("No department code provided for GetDirectManager, falling back to HR Manager");
                    yield systemConfigService.getHRManagerEmployeeNo();
                }

                Department department = departmentRepository.findById(deptCode)
                        .orElseThrow(() -> new RuntimeException("القسم غير موجود: " + deptCode));

                if (department.getDeptMgrCode() == null) {
                    log.warn("Department {} has no manager assigned, falling back to HR Manager", deptCode);
                    yield systemConfigService.getHRManagerEmployeeNo();
                }

                log.debug("Resolved direct manager {} for department {}", department.getDeptMgrCode(), deptCode);
                yield department.getDeptMgrCode();
            }
            case "GetProjectManager" -> {
                if (projectCode == null) {
                    log.warn("No project code provided for GetProjectManager, falling back to HR Manager");
                    yield systemConfigService.getHRManagerEmployeeNo();
                }
                Project project = projectRepository.findById(projectCode)
                        .orElseThrow(() -> new RuntimeException("المشروع غير موجود: " + projectCode));
                if (project.getProjectMgr() == null) {
                    log.warn("Project {} has no manager assigned, falling back to HR Manager", projectCode);
                    yield systemConfigService.getHRManagerEmployeeNo();
                }
                yield project.getProjectMgr();
            }
            case "GetHRManager" -> systemConfigService.getHRManagerEmployeeNo();
            case "GetFinManager" -> systemConfigService.getFinanceManagerEmployeeNo();
            case "GetGeneralManager" -> systemConfigService.getGeneralManagerEmployeeNo();
            case "GetRegionalManager" -> {
                if (projectCode == null) {
                    log.warn("No project code provided for GetRegionalManager, falling back to HR Manager");
                    yield systemConfigService.getHRManagerEmployeeNo();
                }
                Project project = projectRepository.findById(projectCode)
                        .orElseThrow(() -> new RuntimeException("المشروع غير موجود: " + projectCode));
                if (project.getRegionalMgr() == null) {
                    log.warn("Project {} has no regional manager assigned, falling back to HR Manager", projectCode);
                    yield systemConfigService.getHRManagerEmployeeNo();
                }
                yield project.getRegionalMgr();
            }
            default -> throw new RuntimeException("استدعاء دالة غير معروف: " + functionCall);
        };
    }

    /**
     * Get a human-readable name for an approval level based on its function call.
     */
    private String getFriendlyLevelName(String functionCall) {
        return switch (functionCall) {
            case "GetDirectManager" -> "المدير المباشر";
            case "GetProjectManager" -> "مدير المشروع";
            case "GetRegionalManager" -> "مدير المشروع الإقليمي";
            case "GetHRManager" -> "مدير الموارد البشرية";
            case "GetFinManager" -> "مدير المالية";
            case "GetGeneralManager" -> "المدير العام";
            case "SpecificEmployee" -> "موافق محدد";
            default -> functionCall;
        };
    }

    /**
     * Validate that an employee can approve a request at a specific level.
     *
     * @param requestType        Request type
     * @param levelNo            Approval level
     * @param approverNo         Employee attempting to approve
     * @param expectedApproverNo Expected approver for this level
     * @return true if valid, false otherwise
     */
    public boolean canApprove(String requestType, Integer levelNo, Long approverNo, Long expectedApproverNo) {
        // Check if current user is ADMIN
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.info("Admin user bypassing approver check");
            return true;
        }

        if (!approverNo.equals(expectedApproverNo)) {
            log.warn("Approver {} is not authorized to approve at level {}. Expected: {}",
                    approverNo, levelNo, expectedApproverNo);
            return false;
        }
        return true;
    }

    /**
     * Get the projected approval timeline for a request.
     *
     * Calculates the approval steps based on the workflow configuration
     * and the current status of the request.
     *
     * @param requestType  Request type
     * @param employeeNo   Employee who submitted the request
     * @param deptCode     Department code
     * @param projectCode  Project code
     * @param currentLevel Current approval level of the request (null if closed)
     * @param transStatus  Transaction status (N/A/R)
     * @return List of approval steps with status and approver details
     */
    @Transactional(readOnly = true)
    public List<ApprovalStep> getApprovalTimeline(String requestType, Long employeeNo,
            Long deptCode, Long projectCode, Integer currentLevel, String transStatus) {

        List<RequestsApprovalSet> approvalChain = getApprovalChain(requestType, deptCode, projectCode);
        Employee employee = employeeRepository.findById(employeeNo)
                .orElseThrow(() -> new RuntimeException("الموظف غير موجود: " + employeeNo));

        return approvalChain.stream().map(level -> {
            Long approverNo = resolveApprover(level.getFunctionCall(), employee, deptCode, projectCode);
            String approverName = employeeRepository.findById(approverNo)
                    .map(Employee::getEmployeeName)
                    .orElse("موافق غير معروف");

            String stepStatus = "FUTURE";

            // If request is Rejected, mark all steps as REJECTED or handle appropriately?
            // Usually, if rejected, the flow stops. Unique handling might be needed.
            // For now, let's keep it simple based on level comparisons.

            if ("R".equals(transStatus)) {
                // If rejected, we might not know exactly at which level it was rejected without
                // history.
                // But valid assumption: if level < currentLevel it was passed. If level ==
                // currentLevel it was rejected.
                if (currentLevel != null) {
                    if (level.getLevelNo() < currentLevel) {
                        stepStatus = "COMPLETED";
                    } else if (level.getLevelNo().equals(currentLevel)) {
                        stepStatus = "REJECTED";
                    } else {
                        stepStatus = "SKIPPED";
                    }
                } else {
                    // If currentLevel is null and status is R, it means it's fully rejected/closed.
                    // But usually we keep the level it was rejected at?
                    // If we don't have it, we assume completed flow? No.
                    stepStatus = "REJECTED"; // Fallback
                }
            } else if ("A".equals(transStatus) && currentLevel == null) {
                // Fully approved
                stepStatus = "COMPLETED";
            } else if (currentLevel != null) {
                if (level.getLevelNo() < currentLevel) {
                    stepStatus = "COMPLETED";
                } else if (level.getLevelNo().equals(currentLevel)) {
                    stepStatus = "PENDING";
                }
            }

            return ApprovalStep.builder()
                    .levelNo(level.getLevelNo())
                    .levelName(getFriendlyLevelName(level.getFunctionCall()))
                    .approverNo(approverNo)
                    .approverName(approverName)
                    .status(stepStatus)
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * DTO for approval step in timeline
     */
    @lombok.Data
    @lombok.Builder
    public static class ApprovalStep {
        private Integer levelNo;
        private String levelName;
        private Long approverNo;
        private String approverName;
        private String status; // COMPLETED, PENDING, FUTURE, REJECTED, SKIPPED
    }

    /**
     * DTO for approval information
     */
    @lombok.Data
    @lombok.Builder
    public static class ApprovalInfo {
        private Long nextApproval; // Next approver employee number (null if final)
        private Integer nextAppLevel; // Next approval level (null if final)
        private String nextAppLevelName; // Human-readable name (e.g., HR Manager)
        private String transStatus; // N=Needs approval, A=Approved, R=Rejected
    }
}

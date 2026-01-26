package com.techno.backend.service;

import com.techno.backend.entity.Department;
import com.techno.backend.entity.Employee;
import com.techno.backend.entity.Project;
import com.techno.backend.entity.RequestsApprovalSet;
import com.techno.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ApprovalWorkflowService.
 * Tests sections 5.1-5.3: Approval workflow, chain resolution, and auto-approval.
 *
 * @author Techno HR System
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Approval Workflow Service Tests")
class ApprovalWorkflowServiceTest {

    @Mock
    private RequestsApprovalSetRepository approvalSetRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private ApprovalWorkflowService approvalWorkflowService;

    private Employee testEmployee;
    private Department testDepartment;
    private Project testProject;
    private final Long EMPLOYEE_NO = 1001L;
    private final Long DEPT_CODE = 1L;
    private final Long PROJECT_CODE = 101L;

    @BeforeEach
    void setUp() {
        testEmployee = Employee.builder()
                .employeeNo(EMPLOYEE_NO)
                .employeeName("Test Employee")
                .primaryDeptCode(DEPT_CODE)
                .primaryProjectCode(PROJECT_CODE)
                .build();

        testDepartment = Department.builder()
                .deptCode(DEPT_CODE)
                .deptName("IT Department")
                .deptMgrCode(2001L)
                .build();

        testProject = Project.builder()
                .projectCode(PROJECT_CODE)
                .projectName("Test Project")
                .projectMgr(3001L)
                .build();
    }

    // ==================== Section 5.2: Approval Chain Resolution ====================

    @Nested
    @DisplayName("5.2 Approval Chain Resolution")
    class ApprovalChainResolution {

        @Test
        @DisplayName("Department-specific approval chain should be used when configured")
        void testDepartmentSpecificApprovalChain_UsedWhenConfigured() {
            RequestsApprovalSet deptLevel1 = RequestsApprovalSet.builder()
                    .requestType("PAYROLL")
                    .departmentCode(DEPT_CODE)
                    .levelNo(1)
                    .functionCall("GetDirectManager")
                    .isActive("Y")
                    .build();

            RequestsApprovalSet deptLevel2 = RequestsApprovalSet.builder()
                    .requestType("PAYROLL")
                    .departmentCode(DEPT_CODE)
                    .levelNo(2)
                    .functionCall("GetHRManager")
                    .isActive("Y")
                    .build();

            when(approvalSetRepository.findApprovalFlowByRequestTypeAndDepartment("PAYROLL", DEPT_CODE))
                    .thenReturn(List.of(deptLevel1, deptLevel2));

            List<RequestsApprovalSet> chain = approvalWorkflowService.getApprovalChain("PAYROLL", DEPT_CODE, PROJECT_CODE);

            assertThat(chain).isNotEmpty();
            assertThat(chain.size()).isEqualTo(2);
            assertThat(chain.get(0).getDepartmentCode()).isEqualTo(DEPT_CODE);
            verify(approvalSetRepository).findApprovalFlowByRequestTypeAndDepartment("PAYROLL", DEPT_CODE);
        }

        @Test
        @DisplayName("Project-specific approval chain should be used when configured")
        void testProjectSpecificApprovalChain_UsedWhenConfigured() {
            RequestsApprovalSet projLevel1 = RequestsApprovalSet.builder()
                    .requestType("PAYROLL")
                    .projectCode(PROJECT_CODE)
                    .levelNo(1)
                    .functionCall("GetProjectManager")
                    .isActive("Y")
                    .build();

            when(approvalSetRepository.findApprovalFlowByRequestTypeAndDepartment("PAYROLL", DEPT_CODE))
                    .thenReturn(Collections.emptyList());
            when(approvalSetRepository.findApprovalFlowByRequestTypeAndProject("PAYROLL", PROJECT_CODE))
                    .thenReturn(List.of(projLevel1));

            List<RequestsApprovalSet> chain = approvalWorkflowService.getApprovalChain("PAYROLL", DEPT_CODE, PROJECT_CODE);

            assertThat(chain).isNotEmpty();
            assertThat(chain.get(0).getProjectCode()).isEqualTo(PROJECT_CODE);
            verify(approvalSetRepository).findApprovalFlowByRequestTypeAndProject("PAYROLL", PROJECT_CODE);
        }

        @Test
        @DisplayName("Global approval chain fallback when no department/project specific")
        void testGlobalApprovalChain_FallbackWhenNoSpecific() {
            RequestsApprovalSet globalLevel1 = RequestsApprovalSet.builder()
                    .requestType("PAYROLL")
                    .levelNo(1)
                    .functionCall("GetHRManager")
                    .isActive("Y")
                    .build();

            RequestsApprovalSet globalLevel2 = RequestsApprovalSet.builder()
                    .requestType("PAYROLL")
                    .levelNo(2)
                    .functionCall("GetFinManager")
                    .isActive("Y")
                    .build();

            RequestsApprovalSet globalLevel3 = RequestsApprovalSet.builder()
                    .requestType("PAYROLL")
                    .levelNo(3)
                    .functionCall("GetGeneralManager")
                    .isActive("Y")
                    .build();

            when(approvalSetRepository.findApprovalFlowByRequestTypeAndDepartment("PAYROLL", DEPT_CODE))
                    .thenReturn(Collections.emptyList());
            when(approvalSetRepository.findApprovalFlowByRequestTypeAndProject("PAYROLL", PROJECT_CODE))
                    .thenReturn(Collections.emptyList());
            when(approvalSetRepository.findActiveApprovalFlowByRequestType("PAYROLL"))
                    .thenReturn(List.of(globalLevel1, globalLevel2, globalLevel3));

            List<RequestsApprovalSet> chain = approvalWorkflowService.getApprovalChain("PAYROLL", DEPT_CODE, PROJECT_CODE);

            assertThat(chain).isNotEmpty();
            assertThat(chain.size()).isEqualTo(3);
            verify(approvalSetRepository).findActiveApprovalFlowByRequestType("PAYROLL");
        }
    }
}

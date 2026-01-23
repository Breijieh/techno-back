package com.techno.backend.dto.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for transfer response data.
 * Returned from transfer API endpoints.
 *
 * @author Techno HR System
 * @version 1.0
 * @since Phase 10 - Projects
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferResponse {

    // Transfer fields
    private Long transferNo;
    private Long employeeNo;
    private String employeeName;
    private Long fromProjectCode;
    private String fromProjectName;
    private Long toProjectCode;
    private String toProjectName;
    private LocalDate transferDate;
    private String transferReason;
    private String remarks;

    // Approval fields
    private String transStatus;
    private Long nextApproval;
    private String nextApprovalName;
    private Integer nextAppLevel;
    private LocalDate requestDate;
    private Long requestedBy;
    private String requestedByName;
    private LocalDate approvedDate;
    private Long approvedBy;
    private String approvedByName;
    private String rejectionReason;

    // Execution fields
    private String executionStatus;
    private LocalDate executionDate;
    private Long executedBy;
    private String executedByName;

    // Audit fields
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

    /**
     * Get status display name
     */
    public String getStatusDisplay() {
        if (transStatus == null) {
            return "غير معروف";
        }
        return switch (transStatus) {
            case "P" -> "في انتظار الموافقة";
            case "A" -> "موافق عليه";
            case "R" -> "مرفوض";
            default -> transStatus;
        };
    }

    /**
     * Get execution status display name
     */
    public String getExecutionStatusDisplay() {
        if (executionStatus == null) {
            return "معلق";
        }
        return switch (executionStatus) {
            case "PENDING" -> "نقل معلق";
            case "EXECUTED" -> "تم النقل";
            case "CANCELLED" -> "تم إلغاء النقل";
            default -> executionStatus;
        };
    }

    /**
     * Check if transfer is pending (approved but not executed)
     */
    public boolean isPendingExecution() {
        return "A".equals(transStatus) && "PENDING".equals(executionStatus);
    }

    /**
     * Check if transfer is completed
     */
    public boolean isCompleted() {
        return "A".equals(transStatus) && "EXECUTED".equals(executionStatus);
    }

    /**
     * Get status badge color for UI
     */
    public String getStatusColor() {
        if (transStatus == null) {
            return "gray";
        }
        return switch (transStatus) {
            case "P" -> "yellow";
            case "A" -> "green";
            case "R" -> "red";
            default -> "gray";
        };
    }
}

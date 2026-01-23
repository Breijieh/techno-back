package com.techno.backend.dto.paymentrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for payment request response data.
 * Returned from payment request API endpoints.
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
public class PaymentRequestResponse {

    private Long requestNo;
    private Long projectCode;
    private String projectName;
    private Long supplierCode;
    private String supplierName;
    private LocalDate requestDate;
    private BigDecimal paymentAmount;
    private String paymentPurpose;
    private String transStatus;
    private Long nextApproval;
    private String nextApproverName;
    private Integer nextAppLevel;
    private Long approvedBy;
    private String approverName;
    private LocalDateTime approvedDate;
    private String rejectionReason;
    private Long requestedBy;
    private String requesterName;
    private String attachmentPath;
    private String isProcessed;
    private String isDeleted;

    // Calculated fields
    private Boolean isPending;
    private Boolean isApproved;
    private Boolean isRejected;
    private Boolean isProcessedFlag;

    // Audit fields
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

    /**
     * Get transaction status display name
     */
    public String getTransStatusDisplay() {
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
     * Get approval level display
     */
    public String getApprovalLevelDisplay() {
        if (nextAppLevel == null) {
            return "غير متاح";
        }
        return switch (nextAppLevel) {
            case 1 -> "مدير المشروع";
            case 2 -> "المدير الإقليمي";
            case 3 -> "مدير المالية";
            default -> "المستوى " + nextAppLevel;
        };
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

    /**
     * Get formatted amount
     */
    public String getFormattedAmount() {
        if (paymentAmount == null) {
            return "0.00 ريال";
        }
        return String.format("%,.2f ريال", paymentAmount);
    }
}

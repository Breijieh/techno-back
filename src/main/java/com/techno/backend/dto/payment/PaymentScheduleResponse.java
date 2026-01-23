package com.techno.backend.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for payment schedule response data.
 * Returned from payment schedule API endpoints.
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
public class PaymentScheduleResponse {

    private Long paymentId;
    private Long projectCode;
    private String projectName;
    private Integer sequenceNo;
    private LocalDate dueDate;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private String paymentStatus;
    private LocalDate paymentDate;
    private String notes;

    // Calculated fields
    private BigDecimal remainingAmount;
    private BigDecimal paymentPercentage;
    private Long daysUntilDue;
    private Boolean isOverdue;
    private Boolean isPending;
    private Boolean isPartial;
    private Boolean isPaid;

    // Audit fields
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

    /**
     * Get payment status display name
     */
    public String getPaymentStatusDisplay() {
        if (paymentStatus == null) {
            return "غير معروف";
        }
        return switch (paymentStatus) {
            case "PENDING" -> "معلق";
            case "PARTIAL" -> "مدفوع جزئياً";
            case "PAID" -> "مدفوع بالكامل";
            default -> paymentStatus;
        };
    }

    /**
     * Get status badge color for UI
     */
    public String getStatusColor() {
        if (paymentStatus == null) {
            return "gray";
        }
        return switch (paymentStatus) {
            case "PENDING" -> "red";
            case "PARTIAL" -> "yellow";
            case "PAID" -> "green";
            default -> "gray";
        };
    }

    /**
     * Get formatted amounts
     */
    public String getFormattedDueAmount() {
        if (dueAmount == null) {
            return "0.00 ريال";
        }
        return String.format("%,.2f ريال", dueAmount);
    }

    public String getFormattedPaidAmount() {
        if (paidAmount == null) {
            return "0.00 ريال";
        }
        return String.format("%,.2f ريال", paidAmount);
    }

    public String getFormattedRemainingAmount() {
        if (remainingAmount == null) {
            return "0.00 ريال";
        }
        return String.format("%,.2f ريال", remainingAmount);
    }
}

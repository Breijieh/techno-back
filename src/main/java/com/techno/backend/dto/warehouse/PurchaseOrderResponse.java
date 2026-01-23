package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderResponse {

    private Long poId;
    private String poNumber;
    private Long storeCode;
    private String storeName;
    private Long projectCode;
    private String projectName;
    private LocalDate poDate;
    private LocalDate expectedDeliveryDate;
    private String supplierName;
    private BigDecimal totalAmount;
    private String poStatus; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED
    private String approvalNotes;
    private List<PurchaseOrderLineResponse> orderLines;
    private Long requestedBy;
    private String requestedByName;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}


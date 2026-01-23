package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoodsReceiptResponse {

    private Long receiptId;
    private String receiptNumber;
    private Long storeCode;
    private String storeName;
    private Long projectCode;
    private String projectName;
    private LocalDate receiptDate;
    private String receiptType;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private String notes;
    private List<GoodsReceiptLineResponse> receiptLines;
    private Long receivedBy;
    private String receivedByName;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}


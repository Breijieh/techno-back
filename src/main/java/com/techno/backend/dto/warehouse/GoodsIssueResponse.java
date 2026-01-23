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
public class GoodsIssueResponse {

    private Long issueId;
    private String issueNumber;
    private Long storeCode;
    private String storeName;
    private Long projectCode;
    private String projectName;
    private LocalDate issueDate;
    private String issuedTo;
    private String purpose;
    private String notes;
    private List<GoodsIssueLineResponse> issueLines;
    private Long issuedBy;
    private String issuedByName;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}


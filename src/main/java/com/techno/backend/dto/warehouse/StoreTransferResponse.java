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
public class StoreTransferResponse {

    private Long transferId;
    private String transferNumber;
    private Long fromStoreCode;
    private String fromStoreName;
    private Long fromProjectCode;
    private String fromProjectName;
    private Long toStoreCode;
    private String toStoreName;
    private Long toProjectCode;
    private String toProjectName;
    private LocalDate transferDate;
    private String transferStatus; // PENDING, RECEIVED
    private String notes;
    private List<StoreTransferLineResponse> transferLines;
    private Long transferredBy;
    private String transferredByName;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}


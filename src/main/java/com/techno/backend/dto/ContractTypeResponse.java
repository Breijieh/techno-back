package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Contract Type Response DTO
 * Used for returning contract type information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTypeResponse {

    private String contractTypeCode;
    private String typeName;
    private Character calculateSalary;
    private Character allowSelfService;
    private Character isActive;
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}



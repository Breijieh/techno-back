package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Supplier Response DTO
 * Used for returning supplier information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {

    private Long supplierId;
    private String supplierName;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private Boolean isActive; // Converted from Character for frontend
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;
}


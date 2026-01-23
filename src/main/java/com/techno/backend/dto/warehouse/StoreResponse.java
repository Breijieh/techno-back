package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreResponse {

    private Long storeCode;
    private Long projectCode;
    private String projectName;
    private String storeName;
    private String storeLocation;
    private Boolean isActive;
    private Integer itemCount;
    private Long storeManagerId;
    private String storeManagerName;

    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

    public String getStoreName() {
        return storeName;
    }

    public String getProjectName() {
        return projectName;
    }
}


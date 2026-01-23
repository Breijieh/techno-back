package com.techno.backend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSummary {

    private Long storeCode;
    private Long projectCode;
    private String projectName;
    private String storeName;
    private String storeLocation;
    private Boolean isActive;
    private Integer itemCount;
    private Long storeManagerId;
    private String storeManagerName;
}

package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRequest {

    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    @NotBlank(message = "اسم المخزن بالعربية مطلوب")
    @Size(max = 200, message = "اسم المخزن بالعربية لا يجب أن يتجاوز 200 حرف")
    private String storeName;

    @NotBlank(message = "اسم المخزن بالإنجليزية مطلوب")
    @Size(max = 200, message = "اسم المخزن بالإنجليزية لا يجب أن يتجاوز 200 حرف")

    @Size(max = 500, message = "موقع المخزن لا يجب أن يتجاوز 500 حرف")
    private String storeLocation;

    private Boolean isActive;

    /**
     * Store manager employee ID (optional)
     */
    private Long storeManagerId;
}

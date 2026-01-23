package com.techno.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contract Type Request DTO
 * Used for creating and updating contract types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTypeRequest {

    @NotBlank(message = "رمز نوع العقد مطلوب")
    @Size(max = 20, message = "رمز نوع العقد لا يجب أن يتجاوز 20 حرفاً")
    private String contractTypeCode;

    @NotBlank(message = "اسم النوع بالعربية مطلوب")
    @Size(max = 100, message = "اسم النوع بالعربية لا يجب أن يتجاوز 100 حرف")
    private String typeName;

    @NotBlank(message = "اسم النوع بالإنجليزية مطلوب")
    @Size(max = 100, message = "اسم النوع بالإنجليزية لا يجب أن يتجاوز 100 حرف")

    private Character calculateSalary;
    private Character allowSelfService;
    private Character isActive;
}



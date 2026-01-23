package com.techno.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Supplier Request DTO
 * Used for creating and updating suppliers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {

    @NotBlank(message = "اسم المورد مطلوب")
    @Size(max = 250, message = "اسم المورد لا يجب أن يتجاوز 250 حرفاً")
    private String supplierName;

    @Size(max = 250, message = "اسم المورد بالعربية لا يجب أن يتجاوز 250 حرفاً")

    @Size(max = 100, message = "اسم الشخص المسؤول لا يجب أن يتجاوز 100 حرف")
    private String contactPerson;

    @Email(message = "يجب أن يكون البريد الإلكتروني عنوان بريد إلكتروني صالح")
    @Size(max = 100, message = "البريد الإلكتروني لا يجب أن يتجاوز 100 حرف")
    private String email;

    @Size(max = 20, message = "رقم الهاتف لا يجب أن يتجاوز 20 حرفاً")
    private String phone;

    @Size(max = 500, message = "العنوان لا يجب أن يتجاوز 500 حرف")
    private String address;

    private Boolean isActive; // Boolean for frontend mapping
}


package com.techno.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating employee information.
 * Used in POST /api/employees and PUT /api/employees/{id} endpoints.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRequest {

    @NotBlank(message = "اسم الموظف بالعربية مطلوب")
    @Size(max = 250, message = "الاسم العربي لا يجب أن يتجاوز 250 حرفاً")
    private String employeeName;

    @Size(max = 20, message = "رقم الهوية الوطنية لا يجب أن يتجاوز 20 حرفاً")
    private String nationalId;

    @NotBlank(message = "الجنسية مطلوبة")
    @Size(max = 50, message = "الجنسية لا يجب أن تتجاوز 50 حرفاً")
    private String nationality;

    @NotNull(message = "فئة الموظف مطلوبة")
    @Pattern(regexp = "^[SF]$", message = "فئة الموظف يجب أن تكون 'S' (سعودي) أو 'F' (أجنبي)")
    private String employeeCategory;

    @Size(max = 50, message = "رقم جواز السفر لا يجب أن يتجاوز 50 حرفاً")
    private String passportNo;

    private LocalDate passportExpiryDate;

    @Size(max = 50, message = "رقم الإقامة لا يجب أن يتجاوز 50 حرفاً")
    private String residencyNo;

    private LocalDate residencyExpiryDate;

    @NotNull(message = "تاريخ التوظيف مطلوب")
    private LocalDate hireDate;

    private LocalDate terminationDate;

    @Size(max = 20, message = "حالة التوظيف لا يجب أن تتجاوز 20 حرفاً")
    private String employmentStatus;

    @Size(max = 500, message = "سبب الإنهاء لا يجب أن يتجاوز 500 حرف")
    private String terminationReason;

    @Size(max = 50, message = "رقم الاشتراك في التامينات لا يجب أن يتجاوز 50 حرفاً")
    private String socialInsuranceNo;

    @NotBlank(message = "نوع العقد مطلوب")
    @Size(max = 20, message = "نوع العقد لا يجب أن يتجاوز 20 حرفاً")
    private String empContractType;

    private Long primaryDeptCode;

    private Long primaryProjectCode;

    @NotNull(message = "الراتب الشهري مطلوب")
    @DecimalMin(value = "0.0", inclusive = false, message = "الراتب يجب أن يكون أكبر من 0")
    private BigDecimal monthlySalary;

    @DecimalMin(value = "0.0", message = "رصيد الإجازة لا يمكن أن يكون سالباً")
    private BigDecimal leaveBalanceDays;

    @Email(message = "تنسيق البريد الإلكتروني غير صالح")
    @Size(max = 100, message = "البريد الإلكتروني لا يجب أن يتجاوز 100 حرف")
    private String email;

    @Size(max = 20, message = "رقم الجوال لا يجب أن يتجاوز 20 حرفاً")
    private String mobile;

    private String username;

    private String password;

    private String userType;
}

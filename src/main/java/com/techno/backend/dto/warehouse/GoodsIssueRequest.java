package com.techno.backend.dto.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueRequest {

    @NotNull(message = "رمز المخزن مطلوب")
    private Long storeCode;

    @NotNull(message = "رمز المشروع مطلوب")
    private Long projectCode;

    @NotNull(message = "تاريخ الصرف مطلوب")
    private LocalDate issueDate;

    @Size(max = 250, message = "المصروف إليه لا يجب أن يتجاوز 250 حرفاً")
    private String issuedTo;

    @Size(max = 500, message = "الغرض لا يجب أن يتجاوز 500 حرف")
    private String purpose;

    @Size(max = 1000, message = "الملاحظات لا يجب أن تتجاوز 1000 حرف")
    private String notes;

    @NotEmpty(message = "سطور الصرف مطلوبة")
    @Valid
    private List<GoodsIssueLineRequest> issueLines;
}


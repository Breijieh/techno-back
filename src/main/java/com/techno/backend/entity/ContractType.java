package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Contract Type Entity
 * Represents employee contract types (TECHNO, CLIENT, CONTRACTOR)
 * Based on CONTRACT_TYPES table from DOCUMNET.MD
 */
@Entity
@Table(name = "contract_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractType extends BaseEntity {

    @Id
    @Size(max = 20, message = "رمز نوع العقد لا يجب أن يتجاوز 20 حرفاً")
    @Column(name = "contract_type_code", nullable = false, length = 20)
    private String contractTypeCode;

    @NotBlank(message = "اسم النوع مطلوب")
    @Size(max = 100, message = "اسم النوع لا يجب أن يتجاوز 100 حرف")
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Builder.Default
    @Column(name = "calculate_salary", length = 1)
    private Character calculateSalary = 'Y';

    @Builder.Default
    @Column(name = "allow_self_service", length = 1)
    private Character allowSelfService = 'Y';

    @Builder.Default
    @Column(name = "is_active", length = 1)
    private Character isActive = 'Y';
}

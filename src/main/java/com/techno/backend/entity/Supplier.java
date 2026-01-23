package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Supplier Entity
 * Represents suppliers/vendors in the system
 */
@Entity
@Table(name = "suppliers", uniqueConstraints = {
        @UniqueConstraint(columnNames = "supplier_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @NotBlank(message = "اسم المورد مطلوب")
    @Size(max = 250, message = "اسم المورد لا يجب أن يتجاوز 250 حرفاً")
    @Column(name = "supplier_name", nullable = false, unique = true, length = 250)
    private String supplierName;

    @Size(max = 100, message = "اسم الشخص المسؤول لا يجب أن يتجاوز 100 حرف")
    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Email(message = "يجب أن يكون البريد الإلكتروني عنوان بريد إلكتروني صالح")
    @Size(max = 100, message = "البريد الإلكتروني لا يجب أن يتجاوز 100 حرف")
    @Column(name = "email", length = 100)
    private String email;

    @Size(max = 20, message = "رقم الهاتف لا يجب أن يتجاوز 20 حرفاً")
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 500, message = "العنوان لا يجب أن يتجاوز 500 حرف")
    @Column(name = "address", length = 500)
    private String address;

    @Builder.Default
    @Column(name = "is_active", length = 1)
    private Character isActive = 'Y';
}

package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Role Entity
 * Represents user roles in the system with dynamic permissions
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = "role_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @NotBlank(message = "اسم الدور مطلوب")
    @Size(max = 100, message = "اسم الدور لا يجب أن يتجاوز 100 حرف")
    @Column(name = "role_name", nullable = false, unique = true, length = 100)
    private String roleName;

    @Size(max = 500, message = "الوصف لا يجب أن يتجاوز 500 حرف")
    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_active", length = 1)
    private Character isActive = 'Y';
}


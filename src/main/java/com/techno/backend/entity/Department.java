package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Department Entity
 * Represents organizational departments with hierarchical structure
 * Based on DEPARTMENTS table from DOCUMNET.MD
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_code")
    private Long deptCode;

    @NotBlank(message = "اسم القسم مطلوب")
    @Size(max = 250, message = "اسم القسم لا يجب أن يتجاوز 250 حرفاً")
    @Column(name = "dept_name", nullable = false, length = 250)
    private String deptName;

    @Column(name = "parent_dept_code")
    private Long parentDeptCode;

    @Column(name = "dept_mgr_code")
    private Long deptMgrCode;

    @Builder.Default
    @Column(name = "is_active", length = 1)
    private Character isActive = 'Y';
}

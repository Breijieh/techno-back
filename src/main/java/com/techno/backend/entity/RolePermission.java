package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role Permission Entity
 * Represents permissions associated with a role
 * One role has one set of permissions (OneToOne relationship)
 */
@Entity
@Table(name = "role_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = "role_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, unique = true)
    private Role role;

    @Builder.Default
    @Column(name = "can_manage_employees", length = 1)
    private Character canManageEmployees = 'N';

    @Builder.Default
    @Column(name = "can_manage_attendance", length = 1)
    private Character canManageAttendance = 'N';

    @Builder.Default
    @Column(name = "can_manage_leave", length = 1)
    private Character canManageLeave = 'N';

    @Builder.Default
    @Column(name = "can_manage_loans", length = 1)
    private Character canManageLoans = 'N';

    @Builder.Default
    @Column(name = "can_manage_payroll", length = 1)
    private Character canManagePayroll = 'N';

    @Builder.Default
    @Column(name = "can_manage_projects", length = 1)
    private Character canManageProjects = 'N';

    @Builder.Default
    @Column(name = "can_manage_warehouse", length = 1)
    private Character canManageWarehouse = 'N';

    @Builder.Default
    @Column(name = "can_view_reports", length = 1)
    private Character canViewReports = 'N';

    @Builder.Default
    @Column(name = "can_approve", length = 1)
    private Character canApprove = 'N';

    @Builder.Default
    @Column(name = "can_manage_settings", length = 1)
    private Character canManageSettings = 'N';
}


package com.techno.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User Permission Entity
 * Maps users to menu items with specific permissions
 * Based on USER_PERMISSIONS table from DOCUMNET.MD
 */
@Entity
@Table(name = "user_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Builder.Default
    @Column(name = "can_view", length = 1)
    private Character canView = 'N';

    @Builder.Default
    @Column(name = "can_add", length = 1)
    private Character canAdd = 'N';

    @Builder.Default
    @Column(name = "can_edit", length = 1)
    private Character canEdit = 'N';

    @Builder.Default
    @Column(name = "can_delete", length = 1)
    private Character canDelete = 'N';

    @Builder.Default
    @Column(name = "can_approve", length = 1)
    private Character canApprove = 'N';

    @Builder.Default
    @Column(name = "can_print", length = 1)
    private Character canPrint = 'N';
}


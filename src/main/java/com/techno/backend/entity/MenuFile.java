package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Menu File Entity
 * Represents system menu items for role-based access
 * Based on MENU_FILES table from DOCUMNET.MD
 */
@Entity
@Table(name = "menu_files", uniqueConstraints = {
        @UniqueConstraint(columnNames = "menu_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    @NotBlank(message = "رمز القائمة مطلوب")
    @Size(max = 50)
    @Column(name = "menu_code", nullable = false, unique = true, length = 50)
    private String menuCode;

    @NotBlank(message = "اسم القائمة مطلوب")
    @Size(max = 250)
    @Column(name = "menu_name", nullable = false, length = 250)
    private String menuName;

    @Size(max = 500)
    @Column(name = "menu_url", length = 500)
    private String menuUrl;

    @Column(name = "parent_menu_id")
    private Long parentMenuId;

    @Column(name = "menu_order")
    private Integer menuOrder;

    @Builder.Default
    @Column(name = "is_active", length = 1)
    private Character isActive = 'Y';
}

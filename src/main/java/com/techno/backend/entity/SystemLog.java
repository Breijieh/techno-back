package com.techno.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * System Log Entity
 * Represents system audit logs and event tracking
 * Used for monitoring user actions, system events, errors, and debugging
 */
@Entity
@Table(name = "system_logs", indexes = {
    @Index(name = "idx_system_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_system_logs_level", columnList = "log_level"),
    @Index(name = "idx_system_logs_module", columnList = "module"),
    @Index(name = "idx_system_logs_created_date", columnList = "created_date"),
    @Index(name = "idx_system_logs_action_type", columnList = "action_type"),
    @Index(name = "idx_system_logs_date_range", columnList = "created_date, log_level")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /**
     * User ID who performed the action (nullable for system events)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Type of action performed
     * Examples: LOGIN, LOGOUT, CREATE, UPDATE, DELETE, VIEW, APPROVE, EXPORT, ERROR
     */
    @NotBlank(message = "نوع الإجراء مطلوب")
    @Size(max = 50, message = "نوع الإجراء لا يجب أن يتجاوز 50 حرفاً")
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /**
     * System module where the action occurred
     * Examples: Authentication, Employee Management, Payroll, Projects, Warehouse, System Settings, Reports, Approvals
     */
    @NotBlank(message = "الوحدة مطلوبة")
    @Size(max = 100, message = "الوحدة لا يجب أن تتجاوز 100 حرف")
    @Column(name = "module", nullable = false, length = 100)
    private String module;

    /**
     * Detailed description of the action/event
     */
    @NotBlank(message = "الوصف مطلوب")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Log level: INFO, WARNING, ERROR, DEBUG
     */
    @NotNull(message = "مستوى السجل مطلوب")
    @Pattern(regexp = "^(INFO|WARNING|ERROR|DEBUG)$", message = "مستوى السجل يجب أن يكون INFO أو WARNING أو ERROR أو DEBUG")
    @Column(name = "log_level", nullable = false, length = 10)
    private String logLevel;

    /**
     * IP address of the user/client (IPv4 or IPv6)
     */
    @Size(max = 45, message = "عنوان IP لا يجب أن يتجاوز 45 حرفاً")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}


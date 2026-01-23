-- ====================================================================
-- Roles & Permissions Tables Migration Script
-- Creates ROLES and ROLE_PERMISSIONS tables
-- ====================================================================

-- ====================================================================
-- PART 1: Create ROLES Table
-- ====================================================================

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active CHAR(1) DEFAULT 'Y',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_date TIMESTAMP,
    modified_by BIGINT
);

-- Create index on role_name for faster lookups
CREATE INDEX IF NOT EXISTS idx_roles_role_name ON roles(role_name);
CREATE INDEX IF NOT EXISTS idx_roles_is_active ON roles(is_active);

-- ====================================================================
-- PART 2: Create ROLE_PERMISSIONS Table
-- ====================================================================

CREATE TABLE IF NOT EXISTS role_permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL UNIQUE,
    can_manage_employees CHAR(1) DEFAULT 'N',
    can_manage_attendance CHAR(1) DEFAULT 'N',
    can_manage_leave CHAR(1) DEFAULT 'N',
    can_manage_loans CHAR(1) DEFAULT 'N',
    can_manage_payroll CHAR(1) DEFAULT 'N',
    can_manage_projects CHAR(1) DEFAULT 'N',
    can_manage_warehouse CHAR(1) DEFAULT 'N',
    can_view_reports CHAR(1) DEFAULT 'N',
    can_approve CHAR(1) DEFAULT 'N',
    can_manage_settings CHAR(1) DEFAULT 'N',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_date TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

-- Create index on role_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);

-- ====================================================================
-- PART 3: Insert Initial Roles from UserType Enum
-- ====================================================================

-- Insert roles based on existing UserType enum values
INSERT INTO roles (role_name, description, is_active, created_date, created_by)
VALUES 
    ('Super Admin', 'Full system access with all permissions and administrative controls', 'Y', CURRENT_TIMESTAMP, 1),
    ('Admin', 'Administrative access to manage employees, payroll, and projects. Cannot manage roles/permissions.', 'Y', CURRENT_TIMESTAMP, 1),
    ('General Manager', 'Strategic oversight, view most modules, approve payroll', 'Y', CURRENT_TIMESTAMP, 1),
    ('HR Manager', 'Manage employee records, attendance, and leave requests', 'Y', CURRENT_TIMESTAMP, 1),
    ('Finance Manager', 'Manage payroll calculations, approvals, and financial reports', 'Y', CURRENT_TIMESTAMP, 1),
    ('Project Manager', 'Manage projects, payment schedules, and labor assignments', 'Y', CURRENT_TIMESTAMP, 1),
    ('Warehouse Manager', 'Manage inventory, purchase orders, and warehouse operations', 'Y', CURRENT_TIMESTAMP, 1),
    ('Employee', 'Basic user access with limited read-only permissions', 'Y', CURRENT_TIMESTAMP, 1)
ON CONFLICT (role_name) DO NOTHING;

-- ====================================================================
-- PART 4: Insert Initial Permissions for Each Role
-- ====================================================================

-- Super Admin - All permissions
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Super Admin'
ON CONFLICT (role_id) DO NOTHING;

-- Admin - All permissions EXCEPT settings (cannot manage roles/permissions)
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Admin'
ON CONFLICT (role_id) DO NOTHING;

-- General Manager - View reports and approve payroll
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'General Manager'
ON CONFLICT (role_id) DO NOTHING;

-- HR Manager - Manage employees, attendance, leave, and approve
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'Y', 'Y', 'Y', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'HR Manager'
ON CONFLICT (role_id) DO NOTHING;

-- Finance Manager - Manage payroll, loans, and approve
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'Y', 'Y', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Finance Manager'
ON CONFLICT (role_id) DO NOTHING;

-- Project Manager - Manage projects, view reports, and approve team leave
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'Y', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Project Manager'
ON CONFLICT (role_id) DO NOTHING;

-- Warehouse Manager - Manage warehouse and view reports
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Warehouse Manager'
ON CONFLICT (role_id) DO NOTHING;

-- Employee - No special permissions (self-service only)
INSERT INTO role_permissions (role_id, can_manage_employees, can_manage_attendance, can_manage_leave, can_manage_loans, can_manage_payroll, can_manage_projects, can_manage_warehouse, can_view_reports, can_approve, can_manage_settings, created_date, created_by)
SELECT role_id, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Employee'
ON CONFLICT (role_id) DO NOTHING;

-- ====================================================================
-- Notes:
-- - This script uses IF NOT EXISTS to be idempotent
-- - ON CONFLICT DO NOTHING ensures safe re-execution
-- - Foreign key constraint ensures referential integrity
-- - CASCADE delete ensures permissions are deleted when role is deleted
-- ====================================================================


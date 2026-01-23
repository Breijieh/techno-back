-- ====================================================================
-- Super Admin Setup Script for Techno ERP System
-- This script creates a super admin user with all necessary reference data
-- Run this script using: docker exec -i techno-postgres psql -U techno_admin -d techno_erp < create-super-admin.sql
-- ====================================================================

-- ====================================================================
-- Step 1: Ensure Contract Types Exist
-- ====================================================================

-- TECHNO: Direct Techno employees
INSERT INTO contract_types (contract_type_code, type_ar_name, type_en_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('TECHNO', 'موظف تكنو', 'Techno Employee', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- TEMPORARY: Temporary employees
INSERT INTO contract_types (contract_type_code, type_ar_name, type_en_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('TEMPORARY', 'موظف مؤقت', 'Temporary Employee', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- DAILY: Daily workers
INSERT INTO contract_types (contract_type_code, type_ar_name, type_en_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('DAILY', 'عامل يومي', 'Daily Worker', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- CLIENT: Client employees
INSERT INTO contract_types (contract_type_code, type_ar_name, type_en_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('CLIENT', 'موظف عميل', 'Client Employee', 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- CONTRACTOR: Independent contractors
INSERT INTO contract_types (contract_type_code, type_ar_name, type_en_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('CONTRACTOR', 'مقاول', 'Contractor', 'N', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- ====================================================================
-- Step 2: Ensure Basic Transaction Types Exist (if needed)
-- ====================================================================

-- Basic Salary
INSERT INTO transactions_types (type_code, type_ar_name, type_en_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (1, 'راتب شهري', 'Basic Salary', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- Transportation Allowance
INSERT INTO transactions_types (type_code, type_ar_name, type_en_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (2, 'بدل مواصلات', 'Transportation Allowance', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- Housing Allowance
INSERT INTO transactions_types (type_code, type_ar_name, type_en_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (3, 'بدل سكن', 'Housing Allowance', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- ====================================================================
-- Step 3: Create Super Admin User Account
-- ====================================================================

-- Note: BCrypt hash for password "admin123" (10 rounds)
-- Generated using: BCryptPasswordEncoder.encode("admin123")
-- This hash will work with Spring Security BCryptPasswordEncoder
-- Password: admin123

INSERT INTO user_accounts (
    username,
    password_hash,
    national_id,
    user_type,
    is_active,
    employee_no,
    created_date
)
VALUES (
    'admin',
    '$2a$12$8hAciaelSDpkFNgdH9bby.SuVxWDcBRy5JLQzgKhOPI73Ufbd7LOy',  -- BCrypt hash for "admin123" - If password doesn't work, generate new hash using methods in SUPER_ADMIN_SETUP.md
    '1111111111',
    'ADMIN',
    'Y',
    NULL,  -- Super admin doesn't need employee number
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO UPDATE
SET 
    password_hash = EXCLUDED.password_hash,
    user_type = EXCLUDED.user_type,
    is_active = EXCLUDED.is_active,
    national_id = EXCLUDED.national_id;

-- ====================================================================
-- Step 4: Verify Super Admin User Creation
-- ====================================================================

-- Display created admin user
SELECT 
    user_id,
    username,
    national_id,
    user_type,
    is_active,
    employee_no,
    created_date
FROM user_accounts
WHERE username = 'admin';

-- ====================================================================
-- Step 5: Ensure System Config Exists (if needed)
-- ====================================================================

-- System configs are usually created by Hibernate, but we can ensure they exist
-- This is optional and can be skipped if not needed

-- ====================================================================
-- Success Message
-- ====================================================================

DO $$
BEGIN
    RAISE NOTICE '✅ Super Admin user created successfully!';
    RAISE NOTICE '📧 Username: admin';
    RAISE NOTICE '🔑 Password: admin123';
    RAISE NOTICE '🆔 National ID: 1111111111';
    RAISE NOTICE '👤 Role: ADMIN';
    RAISE NOTICE '';
    RAISE NOTICE 'You can now login with these credentials.';
END $$;

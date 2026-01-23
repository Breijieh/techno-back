-- Seed data for Contract Types
-- Standard contract types as per DOCUMNET.MD specification

-- TECHNO: Direct Techno employees - Full payroll calculated, self-service access
INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('TECHNO', 'موظف تكنو', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- TEMPORARY: Temporary employees - Payroll calculated, self-service access
INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('TEMPORARY', 'موظف مؤقت', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- DAILY: Daily workers - Payroll calculated, self-service access
INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('DAILY', 'عامل يومي', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- CLIENT: Client employees - No payroll calculation, self-service access
INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('CLIENT', 'موظف عميل', 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- CONTRACTOR: Independent contractors - No payroll, no self-service
INSERT INTO contract_types (contract_type_code, type_name, calculate_salary, allow_self_service, is_active, created_date)
VALUES ('CONTRACTOR', 'مقاول', 'N', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (contract_type_code) DO NOTHING;

-- ====================================================================
-- Transaction Types Seed Data
-- As per DOCUMNET.MD Section 2.4 - Standard Transaction Types
-- ====================================================================

-- ALLOWANCES (A = Allowance)
-- Basic salary components (1-9)
INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (1, 'راتب شهري', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (2, 'بدل مواصلات', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (3, 'بدل سكن', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (4, 'بدل إتصالات', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (5, 'بدل طبيعة عمل', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (6, 'بدل خطر', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (7, 'بدل مناوبة', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (8, 'بدل أخرى', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- Overtime and bonuses (9-19)
INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (9, 'عمل إضافي', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (10, 'زيادة راتب', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (11, 'مكافأة', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (12, 'علاوة سنوية', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (13, 'حافز أداء', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (14, 'عمولة', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (15, 'عمل عطلة', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- DEDUCTIONS (D = Deduction)
-- Attendance deductions (20-29)
INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (20, 'حسم تأخير', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (21, 'حسم غياب', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (22, 'حسم خروج مبكر', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (23, 'حسم نقص ساعات', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (24, 'حسم إجازة بدون راتب', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- Loan and other deductions (30-39)
INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (30, 'قسط سلفة', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (31, 'تأمينات اجتماعية', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (32, 'حسم أخرى', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (33, 'جزاءات', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

INSERT INTO transactions_types (type_code, type_name, allowance_deduction, is_system_generated, is_active, created_date)
VALUES (34, 'حسم أمانة', 'N', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (type_code) DO NOTHING;

-- ====================================================================
-- Salary Breakdown Percentages Seed Data
-- As per DOCUMNET.MD Section 2.4 - Salary Structure
-- ====================================================================

-- SAUDI EMPLOYEES (S) - Total must equal 100%
-- Saudi breakdown: 83.4% Basic + 16.6% Transportation = 100%
INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('S', 1, 0.8340, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('S', 2, 0.1660, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

-- FOREIGN EMPLOYEES (F) - Total must equal 100%
-- Foreign breakdown: 55% Basic + 13.75% Transport + 5.2% Communication + 25% Housing + 1.05% Other = 100%
INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('F', 1, 0.5500, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('F', 2, 0.1375, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('F', 4, 0.0520, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('F', 3, 0.2500, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

INSERT INTO salary_breakdown_percentages (employee_category, trans_type_code, salary_percentage, is_deleted, created_date)
VALUES ('F', 8, 0.0105, 'N', CURRENT_TIMESTAMP)
ON CONFLICT (employee_category, trans_type_code) DO NOTHING;

-- ====================================================================
-- Weekend Days Seed Data
-- Phase 4 - Attendance System
-- Saudi Arabia weekend: Friday (5) and Saturday (6) in ISO-8601 format
-- ====================================================================

INSERT INTO weekend_days (day_of_week, day_name, is_active, created_date)
VALUES (5, 'الجمعة', CURRENT_TIMESTAMP)
ON CONFLICT (day_of_week) DO NOTHING;

-- INSERT INTO weekend_days (day_of_week, day_name, is_active, created_date)
-- VALUES (6, 'السبت', CURRENT_TIMESTAMP)
-- ON CONFLICT (day_of_week) DO NOTHING;

-- ====================================================================
-- Holidays Seed Data
-- Phase 4 - Attendance System
-- Saudi Arabia public holidays for 2025
-- ====================================================================

-- National Day (September 23) - Recurring holiday
INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-09-23', 'اليوم الوطني السعودي', 2025, 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

-- Eid Al-Fitr 2025 (approximate dates - based on Hijri calendar 1446)
-- Note: Actual dates may vary by 1-2 days based on moon sighting
INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-03-30', 'عيد الفطر - اليوم الأول', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-03-31', 'عيد الفطر - اليوم الثاني', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-04-01', 'عيد الفطر - اليوم الثالث', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-04-02', 'عيد الفطر - اليوم الرابع', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

-- Eid Al-Adha 2025 (approximate dates - based on Hijri calendar 1446)
-- Note: Actual dates may vary by 1-2 days based on moon sighting
INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-06-06', 'عيد الأضحى - اليوم الأول', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-06-07', 'عيد الأضحى - اليوم الثاني', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-06-08', 'عيد الأضحى - اليوم الثالث', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-06-09', 'عيد الأضحى - اليوم الرابع', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

-- Arafat Day (Day before Eid Al-Adha)
INSERT INTO eids_holidays (holiday_date, holiday_name, holiday_year, is_recurring, is_active, is_paid, created_date)
VALUES ('2025-06-05', 'يوم عرفة', 2025, 'N', 'Y', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (holiday_date) DO NOTHING;

-- ====================================================================
-- Time Schedule Seed Data
-- Phase 4 - Attendance System
-- Default work schedule: 08:00-17:00 (8 hours), 15 minutes grace period
-- ====================================================================

-- Default general schedule (no department or project specified)
INSERT INTO time_schedule (schedule_name, department_code, project_code, scheduled_start_time, scheduled_end_time, required_hours, grace_period_minutes, is_active, created_date)
SELECT 'Default 8-Hour Schedule', NULL, NULL, '08:00:00', '17:00:00', 8.00, 15, 'Y', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM time_schedule WHERE schedule_name = 'Default 8-Hour Schedule'
);

-- ====================================================================
-- Approval Workflow Configuration
-- Phase 6 - Allowances & Deductions
-- Defines approval chains for manual allowances and deductions
-- ====================================================================

-- Allowance approval workflow (ALLOW) - Single level: HR Manager
INSERT INTO requests_approval_set (request_type, level_no, function_call)
VALUES ('ALLOW', 1, 'GetHRManager')
ON CONFLICT (request_type, level_no) DO NOTHING;

-- Deduction approval workflow (DEDUCT) - Single level: Finance Manager
INSERT INTO requests_approval_set (request_type, level_no, function_call)
VALUES ('DEDUCT', 1, 'GetFinManager')
ON CONFLICT (request_type, level_no) DO NOTHING;

-- ====================================================================
-- Payroll Approval Workflow Configuration
-- Phase 7 - Payroll Calculation
-- 3-level approval: HR Manager → Finance Manager → General Manager
-- ====================================================================

-- Level 1: HR Manager reviews payroll calculations and attendance
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, is_active)
VALUES ('PAYROLL', 1, 'GetHRManager', 'N', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, is_active = 'Y';

-- Level 2: Finance Manager verifies financial aspects and amounts
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, is_active)
VALUES ('PAYROLL', 2, 'GetFinManager', 'N', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, is_active = 'Y';

-- Level 3: General Manager final approval and authorization
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, is_active)
VALUES ('PAYROLL', 3, 'GetGeneralManager', 'Y', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, is_active = 'Y';

-- ====================================================================
-- Leave Request (VAC) Approval Workflow Configuration
-- 3-level approval: Direct Manager → Project Manager → HR Manager
-- ====================================================================

-- Level 1: Direct Manager (from employee's department)
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('VAC', 1, 'GetDirectManager', 'N', 'Direct manager approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 2: Project Manager (from employee's primary project)
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('VAC', 2, 'GetProjectManager', 'N', 'Project manager approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 3: HR Manager final approval
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('VAC', 3, 'GetHRManager', 'Y', 'HR final approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- Loan (LOAN) Approval Workflow Configuration
-- 2-level approval: HR Manager → Finance Manager
-- ====================================================================

-- Level 1: HR Manager reviews employee eligibility and loan terms
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('LOAN', 1, 'GetHRManager', 'N', 'HR Manager reviews loan eligibility', 'Y')
ON CONFLICT (request_type, level_no) 
DO UPDATE SET 
    function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y';

-- Level 2: Finance Manager final approval and fund allocation
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('LOAN', 2, 'GetFinManager', 'Y', 'Finance Manager final approval', 'Y')
ON CONFLICT (request_type, level_no) 
DO UPDATE SET 
    function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y';

-- ====================================================================
-- Salary Increase (INCR) Approval Workflow Configuration
-- 4-level approval: Direct Manager → HR Manager → Finance Manager → General Manager
-- ====================================================================

-- Level 1: Direct Manager recommends salary increase
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('INCR', 1, 'GetDirectManager', 'N', 'Direct manager recommendation', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 2: HR Manager reviews and validates
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('INCR', 2, 'GetHRManager', 'N', 'HR Manager validation', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 3: Finance Manager budget approval
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('INCR', 3, 'GetFinManager', 'N', 'Finance Manager budget approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 4: General Manager final authorization
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('INCR', 4, 'GetGeneralManager', 'Y', 'General Manager final authorization', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- Project Payment (PROJ_PAYMENT) Approval Workflow Configuration
-- 3-level approval: Project Manager → Finance Manager → General Manager
-- ====================================================================

-- Level 1: Project Manager verifies project expenses
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('PROJ_PAYMENT', 1, 'GetProjectManager', 'N', 'Project Manager verification', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 2: Finance Manager validates budget and amount
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('PROJ_PAYMENT', 2, 'GetFinManager', 'N', 'Finance Manager validation', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 3: General Manager final approval for payment
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('PROJ_PAYMENT', 3, 'GetGeneralManager', 'Y', 'General Manager final approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- Project Transfer (PROJ_TRANSFER) Approval Workflow Configuration
-- 3-level approval: Source Project Manager → Target Project Manager → HR Manager
-- ====================================================================

-- Level 1: Source Project Manager (from current project) approval
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('PROJ_TRANSFER', 1, 'GetProjectManager', 'N', 'Source project manager approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 2: Target Project Manager (to new project) approval
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('PROJ_TRANSFER', 2, 'GetProjectManager', 'N', 'Target project manager approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 3: HR Manager final approval and record update
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('PROJ_TRANSFER', 3, 'GetHRManager', 'Y', 'HR Manager final approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- Loan Postponement (POSTLOAN) Approval Workflow Configuration
-- 3-level approval: HR Manager → Finance Manager → General Manager
-- ====================================================================

-- Level 1: HR Manager reviews postponement reason
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('POSTLOAN', 1, 'GetHRManager', 'N', 'HR Manager review', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 2: Finance Manager checks financial impact
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('POSTLOAN', 2, 'GetFinManager', 'N', 'Finance Manager impact check', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 3: General Manager final authorization
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('POSTLOAN', 3, 'GetGeneralManager', 'Y', 'General Manager final authorization', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- Manual Attendance (MANUAL_ATTENDANCE) Approval Workflow Configuration
-- 2-level approval: Direct Manager → HR Manager
-- ====================================================================

-- Level 1: Direct Manager verifies attendance
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('MANUAL_ATTENDANCE', 1, 'GetDirectManager', 'N', 'Direct Manager verification', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- Level 2: HR Manager final approval
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('MANUAL_ATTENDANCE', 2, 'GetHRManager', 'Y', 'HR Manager final approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- Labor Request (LABOR_REQ) Approval Workflow Configuration
-- 1-level approval: Project Manager (Requester) -> HR Manager
-- ====================================================================

-- Level 1: HR Manager final approval
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active)
VALUES ('LABOR_REQ', 1, 'GetHRManager', 'Y', 'HR Manager final approval', 'Y')
ON CONFLICT (request_type, level_no) DO UPDATE SET function_call = EXCLUDED.function_call, close_level = EXCLUDED.close_level, remarks = EXCLUDED.remarks, is_active = 'Y';

-- ====================================================================
-- PHASE 9: Notifications & Email System
-- In-app notifications and email alerts for all business events
-- ====================================================================

-- ====================================================================
-- Email Configuration Table
-- Stores SMTP configuration for sending emails
-- ====================================================================
CREATE TABLE IF NOT EXISTS email_config (
    config_id BIGSERIAL PRIMARY KEY,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INTEGER NOT NULL,
    smtp_username VARCHAR(255),
    smtp_password VARCHAR(500),
    smtp_auth VARCHAR(1) DEFAULT 'Y',
    smtp_starttls_enable VARCHAR(1) DEFAULT 'Y',
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(255) DEFAULT 'Techno ERP',
    is_active VARCHAR(1) DEFAULT 'Y',
    send_emails_enabled VARCHAR(1) DEFAULT 'Y',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP
);

-- Default email configuration (update with actual SMTP details)
INSERT INTO email_config (smtp_host, smtp_port, smtp_username, from_email, from_name, smtp_auth, smtp_starttls_enable, is_active, send_emails_enabled)
VALUES ('smtp.gmail.com', 587, 'no-reply@technoerp.com', 'no-reply@technoerp.com', 'Techno ERP System', 'Y', 'Y', 'Y', 'N')
ON CONFLICT (config_id) DO NOTHING;

-- ====================================================================
-- Email Templates Table
-- Bilingual templates (Arabic + English) for all notification types
-- ====================================================================
CREATE TABLE IF NOT EXISTS email_templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    template_category VARCHAR(50) NOT NULL,
    subject_en VARCHAR(500) NOT NULL,
    body_en TEXT NOT NULL,
    subject_ar VARCHAR(500) NOT NULL,
    body_ar TEXT NOT NULL,
    available_variables TEXT,
    is_active VARCHAR(1) DEFAULT 'Y',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_template_code ON email_templates(template_code);
CREATE INDEX IF NOT EXISTS idx_email_template_category ON email_templates(template_category);

-- ====================================================================
-- Notifications Table
-- Note: Table creation moved to schema.sql to avoid initialization order issues
-- Foreign key to employees_details will be added by Hibernate or SchemaMigration
-- ====================================================================

-- ====================================================================
-- Email Templates - Sample Templates for Leave Notifications
-- Additional templates should be added for all 42 notification types
-- ====================================================================

-- LEAVE_SUBMITTED: Notify approver when leave is submitted
INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LEAVE_SUBMITTED',
    'LEAVE',
    'New Leave Request from {{employeeName}}',
    '<h2>New Leave Request</h2><p>Dear Manager,</p><p><strong>{{employeeName}}</strong> has submitted a new leave request:</p><ul><li><strong>From:</strong> {{leaveFromDate}}</li><li><strong>To:</strong> {{leaveToDate}}</li><li><strong>Days:</strong> {{leaveDays}}</li><li><strong>Reason:</strong> {{leaveReason}}</li></ul><p>Please review and approve or reject this request.</p><p><a href="{{linkUrl}}">View Leave Request</a></p>',
    'طلب إجازة جديد من {{employeeName}}',
    '<h2 dir="rtl">طلب إجازة جديد</h2><p dir="rtl">عزيزي المدير،</p><p dir="rtl">قدم <strong>{{employeeName}}</strong> طلب إجازة جديد:</p><ul dir="rtl"><li><strong>من:</strong> {{leaveFromDate}}</li><li><strong>إلى:</strong> {{leaveToDate}}</li><li><strong>عدد الأيام:</strong> {{leaveDays}}</li><li><strong>السبب:</strong> {{leaveReason}}</li></ul><p dir="rtl">يرجى مراجعة الطلب والموافقة عليه أو رفضه.</p><p dir="rtl"><a href="{{linkUrl}}">عرض طلب الإجازة</a></p>',
    'employeeName,leaveFromDate,leaveToDate,leaveDays,leaveReason,linkUrl',
    'Y'
)
ON CONFLICT (template_code) DO NOTHING;

-- LEAVE_APPROVED_INTERMEDIATE: Notify next approver
INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LEAVE_APPROVED_INTERMEDIATE',
    'LEAVE',
    'Leave Request for {{employeeName}} Needs Your Approval',
    '<h2>Leave Approval Required</h2><p>Dear Manager,</p><p>A leave request from <strong>{{employeeName}}</strong> has been approved at the previous level and now requires your approval:</p><ul><li><strong>From:</strong> {{leaveFromDate}}</li><li><strong>To:</strong> {{leaveToDate}}</li><li><strong>Days:</strong> {{leaveDays}}</li></ul><p><a href="{{linkUrl}}">View and Approve</a></p>',
    'طلب إجازة {{employeeName}} يحتاج موافقتك',
    '<h2 dir="rtl">مطلوب الموافقة على الإجازة</h2><p dir="rtl">عزيزي المدير،</p><p dir="rtl">تمت الموافقة على طلب إجازة من <strong>{{employeeName}}</strong> في المستوى السابق ويحتاج الآن إلى موافقتك:</p><ul dir="rtl"><li><strong>من:</strong> {{leaveFromDate}}</li><li><strong>إلى:</strong> {{leaveToDate}}</li><li><strong>عدد الأيام:</strong> {{leaveDays}}</li></ul><p dir="rtl"><a href="{{linkUrl}}">عرض والموافقة</a></p>',
    'employeeName,leaveFromDate,leaveToDate,leaveDays,linkUrl',
    'Y'
)
ON CONFLICT (template_code) DO NOTHING;

-- LEAVE_APPROVED_FINAL: Notify employee of approval
INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LEAVE_APPROVED_FINAL',
    'LEAVE',
    'Your Leave Request Has Been Approved',
    '<h2>Leave Request Approved</h2><p>Dear {{employeeName}},</p><p>Your leave request has been <strong>approved</strong>!</p><ul><li><strong>From:</strong> {{leaveFromDate}}</li><li><strong>To:</strong> {{leaveToDate}}</li><li><strong>Days:</strong> {{leaveDays}}</li></ul><p>Your leave balance has been updated accordingly.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تمت الموافقة على طلب إجازتك',
    '<h2 dir="rtl">تمت الموافقة على طلب الإجازة</h2><p dir="rtl">عزيزي {{employeeName}}،</p><p dir="rtl">تمت <strong>الموافقة</strong> على طلب إجازتك!</p><ul dir="rtl"><li><strong>من:</strong> {{leaveFromDate}}</li><li><strong>إلى:</strong> {{leaveToDate}}</li><li><strong>عدد الأيام:</strong> {{leaveDays}}</li></ul><p dir="rtl">تم تحديث رصيد إجازتك وفقًا لذلك.</p><p dir="rtl"><a href="{{linkUrl}}">عرض التفاصيل</a></p>',
    'employeeName,leaveFromDate,leaveToDate,leaveDays,linkUrl',
    'Y'
)
ON CONFLICT (template_code) DO NOTHING;

-- LEAVE_REJECTED: Notify employee of rejection
INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LEAVE_REJECTED',
    'LEAVE',
    'Your Leave Request Has Been Rejected',
    '<h2>Leave Request Rejected</h2><p>Dear {{employeeName}},</p><p>Unfortunately, your leave request has been <strong>rejected</strong>.</p><ul><li><strong>From:</strong> {{leaveFromDate}}</li><li><strong>To:</strong> {{leaveToDate}}</li><li><strong>Days:</strong> {{leaveDays}}</li><li><strong>Rejection Reason:</strong> {{rejectionReason}}</li></ul><p>Please contact your manager for more details.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب إجازتك',
    '<h2 dir="rtl">تم رفض طلب الإجازة</h2><p dir="rtl">عزيزي {{employeeName}}،</p><p dir="rtl">للأسف، تم <strong>رفض</strong> طلب إجازتك.</p><ul dir="rtl"><li><strong>من:</strong> {{leaveFromDate}}</li><li><strong>إلى:</strong> {{leaveToDate}}</li><li><strong>عدد الأيام:</strong> {{leaveDays}}</li><li><strong>سبب الرفض:</strong> {{rejectionReason}}</li></ul><p dir="rtl">يرجى الاتصال بمديرك لمزيد من التفاصيل.</p><p dir="rtl"><a href="{{linkUrl}}">عرض التفاصيل</a></p>',
    'employeeName,leaveFromDate,leaveToDate,leaveDays,rejectionReason,linkUrl',
    'Y'
)
ON CONFLICT (template_code) DO NOTHING;

-- LEAVE_CANCELLED: Notify approver that employee cancelled
INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LEAVE_CANCELLED',
    'LEAVE',
    'Leave Request Cancelled by {{employeeName}}',
    '<h2>Leave Request Cancelled</h2><p>Dear Manager,</p><p><strong>{{employeeName}}</strong> has cancelled their leave request:</p><ul><li><strong>From:</strong> {{leaveFromDate}}</li><li><strong>To:</strong> {{leaveToDate}}</li><li><strong>Days:</strong> {{leaveDays}}</li></ul><p>No action is required from you.</p>',
    'تم إلغاء طلب الإجازة من قبل {{employeeName}}',
    '<h2 dir="rtl">تم إلغاء طلب الإجازة</h2><p dir="rtl">عزيزي المدير،</p><p dir="rtl">قام <strong>{{employeeName}}</strong> بإلغاء طلب إجازته:</p><ul dir="rtl"><li><strong>من:</strong> {{leaveFromDate}}</li><li><strong>إلى:</strong> {{leaveToDate}}</li><li><strong>عدد الأيام:</strong> {{leaveDays}}</li></ul><p dir="rtl">لا يلزم اتخاذ أي إجراء من جانبك.</p>',
    'employeeName,leaveFromDate,leaveToDate,leaveDays,linkUrl',
    'Y'
)
ON CONFLICT (template_code) DO NOTHING;

-- ====================================================================
-- LOAN Email Templates (9 templates)
-- ====================================================================

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_SUBMITTED',
    'LOAN',
    'New Loan Request from {{employeeName}}',
    '<h2>New Loan Request</h2><p>Dear Manager,</p><p><strong>{{employeeName}}</strong> has submitted a new loan request:</p><ul><li><strong>Amount:</strong> SAR {{loanAmount}}</li><li><strong>Installments:</strong> {{installments}}</li><li><strong>Reason:</strong> {{loanReason}}</li></ul><p>Please review and approve or reject this request.</p><p><a href="{{linkUrl}}">View Loan Request</a></p>',
    'طلب قرض جديد من {{employeeName}}',
    '<h2 dir="rtl">طلب قرض جديد</h2><p dir="rtl">عزيزي المدير، قدم <strong>{{employeeName}}</strong> طلب قرض جديد بمبلغ {{loanAmount}} ريال على {{installments}} قسط. يرجى المراجعة.</p><p dir="rtl"><a href="{{linkUrl}}">عرض الطلب</a></p>',
    'employeeName,loanAmount,installments,loanReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_APPROVED_INTERMEDIATE',
    'LOAN',
    'Loan Request for {{employeeName}} Needs Your Approval',
    '<h2>Loan Approval Required</h2><p>Dear Manager,</p><p>A loan request from <strong>{{employeeName}}</strong> has been approved at the previous level:</p><ul><li><strong>Amount:</strong> SAR {{loanAmount}}</li><li><strong>Installments:</strong> {{installments}}</li></ul><p><a href="{{linkUrl}}">Review and Approve</a></p>',
    'طلب قرض {{employeeName}} يحتاج موافقتك',
    '<h2 dir="rtl">مطلوب الموافقة على القرض</h2><p dir="rtl">قرض بمبلغ {{loanAmount}} ريال يحتاج موافقتك.</p><p dir="rtl"><a href="{{linkUrl}}">عرض والموافقة</a></p>',
    'employeeName,loanAmount,installments,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_APPROVED_FINAL',
    'LOAN',
    'Your Loan Request Has Been Approved',
    '<h2>Loan Approved</h2><p>Dear {{employeeName}},</p><p>Your loan request has been <strong>approved</strong>!</p><ul><li><strong>Amount:</strong> SAR {{loanAmount}}</li><li><strong>Monthly Installment:</strong> SAR {{monthlyInstallment}}</li><li><strong>Number of Installments:</strong> {{installments}}</li><li><strong>First Deduction Date:</strong> {{firstDeductionDate}}</li></ul><p>Installments will be deducted from your monthly salary.</p><p><a href="{{linkUrl}}">View Loan Details</a></p>',
    'تمت الموافقة على طلب القرض',
    '<h2 dir="rtl">تمت الموافقة على القرض</h2><p dir="rtl">عزيزي {{employeeName}}، تمت الموافقة على قرضك بمبلغ {{loanAmount}} ريال. القسط الشهري {{monthlyInstallment}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,loanAmount,monthlyInstallment,installments,firstDeductionDate,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_REJECTED',
    'LOAN',
    'Your Loan Request Has Been Rejected',
    '<h2>Loan Request Rejected</h2><p>Dear {{employeeName}},</p><p>Unfortunately, your loan request has been <strong>rejected</strong>.</p><ul><li><strong>Amount:</strong> SAR {{loanAmount}}</li><li><strong>Rejection Reason:</strong> {{rejectionReason}}</li></ul><p>Please contact HR or Finance for more details.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب القرض',
    '<h2 dir="rtl">تم رفض طلب القرض</h2><p dir="rtl">للأسف تم رفض طلب القرض. السبب: {{rejectionReason}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,loanAmount,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_POSTPONEMENT_SUBMITTED',
    'LOAN',
    'Loan Postponement Request from {{employeeName}}',
    '<h2>Loan Postponement Request</h2><p>Dear Manager,</p><p><strong>{{employeeName}}</strong> has requested to postpone their loan installment:</p><ul><li><strong>Loan Amount:</strong> SAR {{loanAmount}}</li><li><strong>Remaining Balance:</strong> SAR {{remainingBalance}}</li><li><strong>Postponement Reason:</strong> {{postponementReason}}</li></ul><p><a href="{{linkUrl}}">Review Request</a></p>',
    'طلب تأجيل قسط القرض من {{employeeName}}',
    '<h2 dir="rtl">طلب تأجيل قسط</h2><p dir="rtl">طلب تأجيل من {{employeeName}} للمبلغ المتبقي {{remainingBalance}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">المراجعة</a></p>',
    'employeeName,loanAmount,remainingBalance,postponementReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_POSTPONEMENT_APPROVED',
    'LOAN',
    'Your Loan Postponement Has Been Approved',
    '<h2>Postponement Approved</h2><p>Dear {{employeeName}},</p><p>Your loan postponement request has been approved. Your next installment will be deferred to {{nextDueDate}}.</p><p><a href="{{linkUrl}}">View Loan Schedule</a></p>',
    'تمت الموافقة على تأجيل القسط',
    '<h2 dir="rtl">تمت الموافقة على التأجيل</h2><p dir="rtl">تم تأجيل القسط القادم إلى {{nextDueDate}}.</p><p dir="rtl"><a href="{{linkUrl}}">الجدول</a></p>',
    'employeeName,nextDueDate,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_POSTPONEMENT_REJECTED',
    'LOAN',
    'Your Loan Postponement Request Has Been Rejected',
    '<h2>Postponement Rejected</h2><p>Dear {{employeeName}},</p><p>Your loan postponement request has been rejected. Reason: {{rejectionReason}}</p><p>Your installment schedule remains unchanged.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب تأجيل القسط',
    '<h2 dir="rtl">تم رفض التأجيل</h2><p dir="rtl">السبب: {{rejectionReason}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_FULLY_PAID',
    'LOAN',
    'Congratulations! Your Loan is Fully Paid',
    '<h2>Loan Completed</h2><p>Dear {{employeeName}},</p><p>Congratulations! You have successfully completed all installments for your loan of SAR {{loanAmount}}.</p><p>Your loan account is now closed. Thank you for your timely payments.</p><p><a href="{{linkUrl}}">View Loan History</a></p>',
    'تهانينا! تم سداد القرض بالكامل',
    '<h2 dir="rtl">تم السداد</h2><p dir="rtl">تهانينا {{employeeName}}! تم سداد قرضك بمبلغ {{loanAmount}} ريال بالكامل.</p><p dir="rtl"><a href="{{linkUrl}}">السجل</a></p>',
    'employeeName,loanAmount,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'LOAN_INSTALLMENT_PAID',
    'LOAN',
    'Loan Installment Deducted - {{month}}',
    '<h2>Installment Deducted</h2><p>Dear {{employeeName}},</p><p>Your loan installment for {{month}} has been deducted:</p><ul><li><strong>Installment Amount:</strong> SAR {{installmentAmount}}</li><li><strong>Remaining Balance:</strong> SAR {{remainingBalance}}</li><li><strong>Remaining Installments:</strong> {{remainingInstallments}}</li></ul><p><a href="{{linkUrl}}">View Loan Details</a></p>',
    'تم خصم قسط القرض - {{month}}',
    '<h2 dir="rtl">تم الخصم</h2><p dir="rtl">تم خصم {{installmentAmount}} ريال. المتبقي {{remainingBalance}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,month,installmentAmount,remainingBalance,remainingInstallments,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

-- ====================================================================
-- PAYROLL Email Templates (6 templates)
-- ====================================================================

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYROLL_CALCULATED',
    'PAYROLL',
    'Payroll for {{month}} Has Been Calculated',
    '<h2>Payroll Ready for Review</h2><p>Dear {{employeeName}},</p><p>The payroll for <strong>{{month}}</strong> has been calculated and is now pending approval.</p><ul><li><strong>Gross Salary:</strong> SAR {{grossSalary}}</li><li><strong>Net Salary:</strong> SAR {{netSalary}}</li></ul><p>Your payslip will be available once approved.</p><p><a href="{{linkUrl}}">View Preliminary Payslip</a></p>',
    'تم احتساب الراتب لشهر {{month}}',
    '<h2 dir="rtl">تم احتساب الراتب</h2><p dir="rtl">الراتب الإجمالي {{grossSalary}} ريال، الصافي {{netSalary}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">عرض</a></p>',
    'employeeName,month,grossSalary,netSalary,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYROLL_APPROVED_L1',
    'PAYROLL',
    'Payroll {{month}} - HR Approval Complete',
    '<h2>Payroll Approved - Level 1</h2><p>Payroll for {{month}} has been approved by HR Manager and is now pending Finance Manager approval.</p><p>Total employees: {{employeeCount}}</p><p>Total amount: SAR {{totalAmount}}</p><p><a href="{{linkUrl}}">View Payroll Summary</a></p>',
    'الراتب {{month}} - تمت موافقة الموارد البشرية',
    '<h2 dir="rtl">موافقة المستوى الأول</h2><p dir="rtl">إجمالي {{totalAmount}} ريال لـ {{employeeCount}} موظف.</p><p dir="rtl"><a href="{{linkUrl}}">عرض</a></p>',
    'month,employeeCount,totalAmount,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYROLL_APPROVED_L2',
    'PAYROLL',
    'Payroll {{month}} - Finance Approval Complete',
    '<h2>Payroll Approved - Level 2</h2><p>Payroll for {{month}} has been approved by Finance Manager and is now pending General Manager final approval.</p><p>Total amount: SAR {{totalAmount}}</p><p><a href="{{linkUrl}}">Review for Final Approval</a></p>',
    'الراتب {{month}} - تمت موافقة المالية',
    '<h2 dir="rtl">موافقة المستوى الثاني</h2><p dir="rtl">بانتظار الموافقة النهائية.</p><p dir="rtl"><a href="{{linkUrl}}">عرض</a></p>',
    'month,totalAmount,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYROLL_APPROVED_FINAL',
    'PAYROLL',
    'Your Salary for {{month}} is Ready',
    '<h2>Payslip Available</h2><p>Dear {{employeeName}},</p><p>Your salary for <strong>{{month}}</strong> has been finalized:</p><ul><li><strong>Gross Salary:</strong> SAR {{grossSalary}}</li><li><strong>Total Allowances:</strong> SAR {{totalAllowances}}</li><li><strong>Total Deductions:</strong> SAR {{totalDeductions}}</li><li><strong>Net Salary:</strong> SAR {{netSalary}}</li></ul><p><a href="{{linkUrl}}">Download Payslip</a></p>',
    'راتبك لشهر {{month}} جاهز',
    '<h2 dir="rtl">كشف الراتب متاح</h2><p dir="rtl">الراتب الصافي {{netSalary}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">تحميل كشف الراتب</a></p>',
    'employeeName,month,grossSalary,totalAllowances,totalDeductions,netSalary,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYROLL_REJECTED',
    'PAYROLL',
    'Payroll {{month}} Rejected - Recalculation Required',
    '<h2>Payroll Rejected</h2><p>The payroll for {{month}} has been rejected and requires recalculation.</p><p>Rejection reason: {{rejectionReason}}</p><p>Please review and recalculate.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض الراتب {{month}}',
    '<h2 dir="rtl">تم الرفض</h2><p dir="rtl">السبب: {{rejectionReason}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'month,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYROLL_RECALCULATED',
    'PAYROLL',
    'Your Salary for {{month}} Has Been Updated',
    '<h2>Salary Recalculated</h2><p>Dear {{employeeName}},</p><p>Your salary for {{month}} has been recalculated:</p><ul><li><strong>Previous Net:</strong> SAR {{previousNet}}</li><li><strong>New Net:</strong> SAR {{newNet}}</li><li><strong>Difference:</strong> SAR {{difference}}</li><li><strong>Reason:</strong> {{recalculationReason}}</li></ul><p><a href="{{linkUrl}}">View Updated Payslip</a></p>',
    'تم تحديث راتبك لشهر {{month}}',
    '<h2 dir="rtl">إعادة احتساب</h2><p dir="rtl">الصافي الجديد {{newNet}} ريال. الفرق {{difference}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">عرض</a></p>',
    'employeeName,month,previousNet,newNet,difference,recalculationReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

-- ====================================================================
-- PAYMENT REQUEST Email Templates (4 templates)
-- ====================================================================

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYMENT_REQUEST_SUBMITTED',
    'PAYMENT_REQUEST',
    'New Payment Request - {{projectName}}',
    '<h2>Payment Request Submitted</h2><p>Dear Manager,</p><p>A new payment request has been submitted for project <strong>{{projectName}}</strong>:</p><ul><li><strong>Amount:</strong> SAR {{paymentAmount}}</li><li><strong>Purpose:</strong> {{paymentPurpose}}</li><li><strong>Requested by:</strong> {{requesterName}}</li></ul><p><a href="{{linkUrl}}">Review and Approve</a></p>',
    'طلب دفع جديد - {{projectName}}',
    '<h2 dir="rtl">طلب دفع</h2><p dir="rtl">مبلغ {{paymentAmount}} ريال للمشروع {{projectName}}.</p><p dir="rtl"><a href="{{linkUrl}}">المراجعة</a></p>',
    'projectName,paymentAmount,paymentPurpose,requesterName,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYMENT_REQUEST_APPROVED_INTERMEDIATE',
    'PAYMENT_REQUEST',
    'Payment Request Needs Your Approval - {{projectName}}',
    '<h2>Payment Approval Required</h2><p>A payment request for project {{projectName}} has been approved at the previous level:</p><ul><li><strong>Amount:</strong> SAR {{paymentAmount}}</li><li><strong>Purpose:</strong> {{paymentPurpose}}</li></ul><p><a href="{{linkUrl}}">Review and Approve</a></p>',
    'طلب دفع يحتاج موافقتك - {{projectName}}',
    '<h2 dir="rtl">مطلوب موافقة</h2><p dir="rtl">مبلغ {{paymentAmount}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">الموافقة</a></p>',
    'projectName,paymentAmount,paymentPurpose,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYMENT_REQUEST_APPROVED_FINAL',
    'PAYMENT_REQUEST',
    'Payment Request Approved - {{projectName}}',
    '<h2>Payment Approved</h2><p>The payment request for project <strong>{{projectName}}</strong> has been fully approved:</p><ul><li><strong>Amount:</strong> SAR {{paymentAmount}}</li><li><strong>Purpose:</strong> {{paymentPurpose}}</li></ul><p>The payment can now be processed.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تمت الموافقة على طلب الدفع - {{projectName}}',
    '<h2 dir="rtl">تمت الموافقة</h2><p dir="rtl">يمكن معالجة الدفع بمبلغ {{paymentAmount}} ريال.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'projectName,paymentAmount,paymentPurpose,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYMENT_REQUEST_REJECTED',
    'PAYMENT_REQUEST',
    'Payment Request Rejected - {{projectName}}',
    '<h2>Payment Request Rejected</h2><p>The payment request for project {{projectName}} has been rejected:</p><ul><li><strong>Amount:</strong> SAR {{paymentAmount}}</li><li><strong>Rejection Reason:</strong> {{rejectionReason}}</li></ul><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب الدفع - {{projectName}}',
    '<h2 dir="rtl">تم الرفض</h2><p dir="rtl">السبب: {{rejectionReason}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'projectName,paymentAmount,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

-- ====================================================================
-- TRANSFER Email Templates (4 templates)
-- ====================================================================

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'TRANSFER_SUBMITTED',
    'TRANSFER',
    'Transfer Request - {{employeeName}}',
    '<h2>Employee Transfer Request</h2><p>Dear Manager,</p><p>A transfer request has been submitted for <strong>{{employeeName}}</strong>:</p><ul><li><strong>From:</strong> {{fromProject}}</li><li><strong>To:</strong> {{toProject}}</li><li><strong>Transfer Date:</strong> {{transferDate}}</li><li><strong>Reason:</strong> {{transferReason}}</li></ul><p><a href="{{linkUrl}}">Review Request</a></p>',
    'طلب نقل - {{employeeName}}',
    '<h2 dir="rtl">طلب نقل موظف</h2><p dir="rtl">من {{fromProject}} إلى {{toProject}} بتاريخ {{transferDate}}.</p><p dir="rtl"><a href="{{linkUrl}}">المراجعة</a></p>',
    'employeeName,fromProject,toProject,transferDate,transferReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'TRANSFER_APPROVED_INTERMEDIATE',
    'TRANSFER',
    'Transfer Request Needs Your Approval - {{employeeName}}',
    '<h2>Transfer Approval Required</h2><p>A transfer request for {{employeeName}} has been approved at the previous level:</p><ul><li><strong>From:</strong> {{fromProject}}</li><li><strong>To:</strong> {{toProject}}</li></ul><p><a href="{{linkUrl}}">Review and Approve</a></p>',
    'طلب نقل يحتاج موافقتك - {{employeeName}}',
    '<h2 dir="rtl">مطلوب موافقة</h2><p dir="rtl">نقل من {{fromProject}} إلى {{toProject}}.</p><p dir="rtl"><a href="{{linkUrl}}">الموافقة</a></p>',
    'employeeName,fromProject,toProject,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'TRANSFER_APPROVED_FINAL',
    'TRANSFER',
    'Your Transfer Request Has Been Approved',
    '<h2>Transfer Approved</h2><p>Dear {{employeeName}},</p><p>Your transfer request has been approved:</p><ul><li><strong>From:</strong> {{fromProject}}</li><li><strong>To:</strong> {{toProject}}</li><li><strong>Effective Date:</strong> {{transferDate}}</li></ul><p>Please coordinate with both project managers for the handover.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تمت الموافقة على طلب النقل',
    '<h2 dir="rtl">تمت الموافقة</h2><p dir="rtl">سيتم نقلك من {{fromProject}} إلى {{toProject}} بتاريخ {{transferDate}}.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,fromProject,toProject,transferDate,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'TRANSFER_REJECTED',
    'TRANSFER',
    'Your Transfer Request Has Been Rejected',
    '<h2>Transfer Request Rejected</h2><p>Dear {{employeeName}},</p><p>Your transfer request has been rejected:</p><ul><li><strong>Rejection Reason:</strong> {{rejectionReason}}</li></ul><p>You will remain assigned to {{fromProject}}.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب النقل',
    '<h2 dir="rtl">تم الرفض</h2><p dir="rtl">السبب: {{rejectionReason}}. ستبقى في {{fromProject}}.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,fromProject,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

-- ====================================================================
-- ALLOWANCE & DEDUCTION Email Templates (6 templates)
-- ====================================================================

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'ALLOWANCE_SUBMITTED',
    'ALLOWANCE',
    'New Allowance Request - {{employeeName}}',
    '<h2>Allowance Request</h2><p>A new allowance has been submitted for <strong>{{employeeName}}</strong>:</p><ul><li><strong>Type:</strong> {{allowanceType}}</li><li><strong>Amount:</strong> SAR {{amount}}</li><li><strong>Month:</strong> {{month}}</li><li><strong>Reason:</strong> {{reason}}</li></ul><p><a href="{{linkUrl}}">Review and Approve</a></p>',
    'طلب بدل جديد - {{employeeName}}',
    '<h2 dir="rtl">طلب بدل</h2><p dir="rtl">نوع {{allowanceType}} بمبلغ {{amount}} ريال لشهر {{month}}.</p><p dir="rtl"><a href="{{linkUrl}}">المراجعة</a></p>',
    'employeeName,allowanceType,amount,month,reason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'ALLOWANCE_APPROVED',
    'ALLOWANCE',
    'Allowance Approved - {{month}}',
    '<h2>Allowance Approved</h2><p>Dear {{employeeName}},</p><p>Your allowance request has been approved:</p><ul><li><strong>Type:</strong> {{allowanceType}}</li><li><strong>Amount:</strong> SAR {{amount}}</li><li><strong>Month:</strong> {{month}}</li></ul><p>This will be included in your {{month}} payroll.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تمت الموافقة على البدل - {{month}}',
    '<h2 dir="rtl">تمت الموافقة</h2><p dir="rtl">سيتم إضافة {{amount}} ريال إلى راتب {{month}}.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,allowanceType,amount,month,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'ALLOWANCE_REJECTED',
    'ALLOWANCE',
    'Allowance Request Rejected',
    '<h2>Allowance Rejected</h2><p>Dear {{employeeName}},</p><p>Your allowance request has been rejected:</p><ul><li><strong>Type:</strong> {{allowanceType}}</li><li><strong>Amount:</strong> SAR {{amount}}</li><li><strong>Rejection Reason:</strong> {{rejectionReason}}</li></ul><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب البدل',
    '<h2 dir="rtl">تم الرفض</h2><p dir="rtl">السبب: {{rejectionReason}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,allowanceType,amount,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'DEDUCTION_SUBMITTED',
    'DEDUCTION',
    'Deduction Submitted - {{employeeName}}',
    '<h2>Deduction Request</h2><p>A deduction has been submitted for <strong>{{employeeName}}</strong>:</p><ul><li><strong>Type:</strong> {{deductionType}}</li><li><strong>Amount:</strong> SAR {{amount}}</li><li><strong>Month:</strong> {{month}}</li><li><strong>Reason:</strong> {{reason}}</li></ul><p><a href="{{linkUrl}}">Review and Approve</a></p>',
    'تم تقديم خصم - {{employeeName}}',
    '<h2 dir="rtl">طلب خصم</h2><p dir="rtl">نوع {{deductionType}} بمبلغ {{amount}} ريال لشهر {{month}}.</p><p dir="rtl"><a href="{{linkUrl}}">المراجعة</a></p>',
    'employeeName,deductionType,amount,month,reason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'DEDUCTION_APPROVED',
    'DEDUCTION',
    'Deduction Approved - {{month}}',
    '<h2>Deduction Approved</h2><p>Dear {{employeeName}},</p><p>A deduction has been approved:</p><ul><li><strong>Type:</strong> {{deductionType}}</li><li><strong>Amount:</strong> SAR {{amount}}</li><li><strong>Month:</strong> {{month}}</li></ul><p>This will be deducted from your {{month}} payroll.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تمت الموافقة على الخصم - {{month}}',
    '<h2 dir="rtl">تمت الموافقة</h2><p dir="rtl">سيتم خصم {{amount}} ريال من راتب {{month}}.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,deductionType,amount,month,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'DEDUCTION_REJECTED',
    'DEDUCTION',
    'Deduction Request Rejected',
    '<h2>Deduction Rejected</h2><p>The deduction request has been rejected:</p><ul><li><strong>Employee:</strong> {{employeeName}}</li><li><strong>Type:</strong> {{deductionType}}</li><li><strong>Amount:</strong> SAR {{amount}}</li><li><strong>Rejection Reason:</strong> {{rejectionReason}}</li></ul><p><a href="{{linkUrl}}">View Details</a></p>',
    'تم رفض طلب الخصم',
    '<h2 dir="rtl">تم الرفض</h2><p dir="rtl">السبب: {{rejectionReason}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,deductionType,amount,rejectionReason,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

-- ====================================================================
-- SALARY RAISE & ALERT Email Templates (10 templates)
-- ====================================================================

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'SALARY_RAISE_PROCESSED',
    'SALARY_RAISE',
    'Your Salary Increase Has Been Processed',
    '<h2>Salary Increase</h2><p>Dear {{employeeName}},</p><p>Congratulations! Your salary increase has been processed:</p><ul><li><strong>Previous Salary:</strong> SAR {{previousSalary}}</li><li><strong>New Salary:</strong> SAR {{newSalary}}</li><li><strong>Increase:</strong> SAR {{increase}} ({{percentageIncrease}}%)</li><li><strong>Effective From:</strong> {{effectiveDate}}</li></ul><p>Your new salary will be reflected in your next payroll.</p><p><a href="{{linkUrl}}">View Details</a></p>',
    'تمت معالجة زيادة راتبك',
    '<h2 dir="rtl">زيادة الراتب</h2><p dir="rtl">تهانينا! راتبك الجديد {{newSalary}} ريال (زيادة {{increase}} ريال). ساري من {{effectiveDate}}.</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'employeeName,previousSalary,newSalary,increase,percentageIncrease,effectiveDate,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

INSERT INTO email_templates (template_code, template_category, subject_en, body_en, subject_ar, body_ar, available_variables, is_active)
VALUES (
    'PAYMENT_DUE_ALERT',
    'ALERT',
    'Payment Due Alert - {{projectName}}',
    '<h2>Payment Due Alert</h2><p>Dear Manager,</p><p>A project payment is due:</p><ul><li><strong>Project:</strong> {{projectName}}</li><li><strong>Payment #:</strong> {{paymentSequence}}</li><li><strong>Amount:</strong> SAR {{dueAmount}}</li><li><strong>Due Date:</strong> {{dueDate}}</li><li><strong>Alert Type:</strong> {{alertType}}</li><li><strong>Status:</strong> {{message}}</li></ul><p><a href="{{linkUrl}}">View Payment Details</a></p>',
    'تنبيه دفعة مستحقة - {{projectName}}',
    '<h2 dir="rtl">تنبيه دفعة</h2><p dir="rtl">مشروع {{projectName}}: دفعة {{dueAmount}} ريال مستحقة {{dueDate}}. {{message}}</p><p dir="rtl"><a href="{{linkUrl}}">التفاصيل</a></p>',
    'projectName,projectCode,paymentSequence,dueAmount,dueDate,alertType,message,linkUrl',
    'Y'
) ON CONFLICT (template_code) DO NOTHING;

-- ============================================================
-- PHASE 13: System Configuration
-- Application-wide configuration settings with caching support
-- ============================================================

CREATE TABLE IF NOT EXISTS system_config (
    config_id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(50) UNIQUE NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    config_type VARCHAR(20) NOT NULL CHECK (config_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON')),
    config_category VARCHAR(50),
    config_description VARCHAR(1000),
    is_active VARCHAR(1) DEFAULT 'Y' NOT NULL CHECK (is_active IN ('Y', 'N')),
    is_editable VARCHAR(1) DEFAULT 'Y' NOT NULL CHECK (is_editable IN ('Y', 'N')),
    default_value VARCHAR(500),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    modified_date TIMESTAMP,
    modified_by BIGINT,
    is_deleted VARCHAR(1) DEFAULT 'N' NOT NULL CHECK (is_deleted IN ('Y', 'N'))
);

CREATE INDEX IF NOT EXISTS idx_system_config_key ON system_config(config_key);
CREATE INDEX IF NOT EXISTS idx_system_config_category ON system_config(config_category);
CREATE INDEX IF NOT EXISTS idx_system_config_active ON system_config(is_active, is_deleted);

-- ====================================================================
-- System Configuration Seed Data (from DOCUMNET.MD + additional)
-- ====================================================================

-- Default Configurations from DOCUMNET.MD
INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('DEFAULT_GPS_RADIUS_METERS', '500', 'Default GPS radius for check-in validation (meters)', 'Y', 'Y', '500', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('OVERTIME_MULTIPLIER', '1.5', 'Overtime pay multiplier rate', 'Y', 'Y', '1.5', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('AUTO_CHECKOUT_GRACE_HOURS', '2', 'Hours after shift end for automatic checkout', 'Y', 'Y', '2', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('WORKING_DAYS_PER_MONTH', '30', 'Days used in monthly salary calculations', 'Y', 'Y', '30', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('MAX_FILE_SIZE_MB', '10', 'Maximum file upload size in megabytes', 'Y', 'Y', '10', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

-- Additional useful configurations
INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('LATE_THRESHOLD_MINUTES', '15', 'Minutes late before marking as late', 'Y', 'Y', '15', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('EARLY_DEPARTURE_THRESHOLD_MINUTES', '15', 'Minutes early before marking as early departure', 'Y', 'Y', '15', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('REQUIRE_GPS_CHECKIN', 'Y', 'Require GPS validation for check-in', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('ALLOW_REMOTE_CHECKIN', 'N', 'Allow check-in from non-project locations', 'Y', 'Y', 'N', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('MIN_WAREHOUSE_STOCK_ALERT', '10', 'Minimum stock quantity before alert', 'Y', 'Y', '10', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

-- System admin configs (not editable via UI)
INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('SYSTEM_VERSION', '1.0.0', 'Current system version', 'Y', 'N', '1.0.0', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('DATABASE_MIGRATION_VERSION', '13', 'Latest database migration version', 'Y', 'N', '13', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

-- Approval Manager Configurations (migrated from hardcoded values)
INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('HR_MANAGER_EMPLOYEE_NO', '2', 'Employee number of HR Manager for approvals', 'Y', 'N', '2', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('FINANCE_MANAGER_EMPLOYEE_NO', '3', 'Employee number of Finance Manager for approvals', 'Y', 'N', '3', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, config_type, config_category, config_description, is_active, is_editable, default_value, created_date, is_deleted)
VALUES ('GENERAL_MANAGER_EMPLOYEE_NO', '1', 'Employee number of General Manager for approvals', 'Y', 'N', '1', CURRENT_TIMESTAMP, 'N')
ON CONFLICT (config_key) DO NOTHING;

-- ====================================================================
-- MISSING ROLES SEED DATA
-- Added to ensure Project Secretary, Advisor, and Regional Manager exist
-- ====================================================================

INSERT INTO roles (role_name, description, is_active, created_date, created_by)
VALUES 
    ('Project Secretary', 'Administrative support for projects, attendance management', 'Y', CURRENT_TIMESTAMP, 1),
    ('Project Advisor', 'Consultant role with read-only access to project data', 'Y', CURRENT_TIMESTAMP, 1),
    ('Regional Project Manager', 'Oversight role for multiple projects within a region', 'Y', CURRENT_TIMESTAMP, 1)
ON CONFLICT (role_name) DO NOTHING;

-- Grant initial permissions for Project Secretary
INSERT INTO role_permissions (role_id, can_manage_attendance, can_manage_projects, can_manage_employees, can_view_reports, created_date, created_by)
SELECT role_id, 'Y', 'N', 'N', 'Y', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Project Secretary'
ON CONFLICT (role_id) DO NOTHING;

-- Grant initial permissions for Project Advisor
INSERT INTO role_permissions (role_id, can_manage_attendance, can_manage_projects, can_manage_employees, can_view_reports, created_date, created_by)
SELECT role_id, 'N', 'N', 'N', 'Y', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Project Advisor'
ON CONFLICT (role_id) DO NOTHING;

-- Grant initial permissions for Regional Project Manager
INSERT INTO role_permissions (role_id, can_manage_attendance, can_manage_projects, can_manage_employees, can_view_reports, created_date, created_by)
SELECT role_id, 'N', 'Y', 'N', 'Y', CURRENT_TIMESTAMP, 1
FROM roles WHERE role_name = 'Regional Project Manager'
ON CONFLICT (role_id) DO NOTHING;

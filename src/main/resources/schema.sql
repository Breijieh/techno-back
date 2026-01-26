-- Pre-initialization schema for Techno ERP
-- This ensures tables and constraints required by data.sql exist before Hibernate starts

-- 1. Sequences
CREATE SEQUENCE IF NOT EXISTS system_config_seq START WITH 1 INCREMENT BY 1;

-- 2. Core Tables and Unique Indexes

-- Contract Types
CREATE TABLE IF NOT EXISTS contract_types (
    contract_type_code VARCHAR(20) PRIMARY KEY,
    type_name VARCHAR(100),
    calculate_salary VARCHAR(1),
    allow_self_service VARCHAR(1),
    is_active VARCHAR(1),
    created_date TIMESTAMP
);

-- Transaction Types
CREATE TABLE IF NOT EXISTS transactions_types (
    type_code BIGINT PRIMARY KEY,
    type_name VARCHAR(250) NOT NULL,
    allowance_deduction VARCHAR(1),
    is_system_generated VARCHAR(1),
    is_active VARCHAR(1),
    created_date TIMESTAMP
);

-- Salary Breakdown Percentages
CREATE TABLE IF NOT EXISTS salary_breakdown_percentages (
    ser_no BIGSERIAL PRIMARY KEY,
    employee_category VARCHAR(1),
    trans_type_code BIGINT,
    salary_percentage DECIMAL(5,4),
    is_deleted VARCHAR(1),
    created_date TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_salary_breakdown ON salary_breakdown_percentages (employee_category, trans_type_code);

-- Weekend Days
CREATE TABLE IF NOT EXISTS weekend_days (
    weekend_id BIGSERIAL PRIMARY KEY,
    day_of_week INTEGER,
    day_name VARCHAR(50) NOT NULL,
    is_active VARCHAR(1),
    created_date TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_weekend_day ON weekend_days (day_of_week);

-- Holidays
CREATE TABLE IF NOT EXISTS eids_holidays (
    holiday_id BIGSERIAL PRIMARY KEY,
    holiday_date DATE,
    holiday_name VARCHAR(200) NOT NULL,
    holiday_year INTEGER,
    is_recurring VARCHAR(1),
    is_active VARCHAR(1),
    is_paid VARCHAR(1),
    created_date TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_holiday_date ON eids_holidays (holiday_date);

-- Ensure is_paid column exists (for PostgreSQL 9.6+ which supports IF NOT EXISTS)
-- This handles cases where table was created by Hibernate without this column
ALTER TABLE eids_holidays ADD COLUMN IF NOT EXISTS is_paid VARCHAR(1) DEFAULT 'Y';

-- Approval Workflow
CREATE TABLE IF NOT EXISTS requests_approval_set (
    approval_id BIGSERIAL PRIMARY KEY,
    request_type VARCHAR(20),
    level_no INTEGER,
    function_call VARCHAR(50),
    close_level VARCHAR(1),
    remarks VARCHAR(500),
    is_active VARCHAR(1) DEFAULT 'Y'
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_rule ON requests_approval_set (request_type, level_no);

-- Email Templates
CREATE TABLE IF NOT EXISTS email_templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100),
    template_category VARCHAR(50),
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    available_variables TEXT,
    is_active VARCHAR(1)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_template ON email_templates (template_code);

-- Time Schedule
CREATE TABLE IF NOT EXISTS time_schedule (
    schedule_id BIGSERIAL PRIMARY KEY,
    schedule_name VARCHAR(100) NOT NULL,
    department_code BIGINT,
    project_code BIGINT,
    scheduled_start_time TIME NOT NULL,
    scheduled_end_time TIME NOT NULL,
    required_hours DECIMAL(4,2) NOT NULL,
    grace_period_minutes INTEGER NOT NULL DEFAULT 15,
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y',
    created_date TIMESTAMP,
    modified_date TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT
);
CREATE INDEX IF NOT EXISTS idx_schedule_dept ON time_schedule (department_code);
CREATE INDEX IF NOT EXISTS idx_schedule_project ON time_schedule (project_code);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    employee_no BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    title_ar VARCHAR(255) NOT NULL,
    message_en TEXT NOT NULL,
    message_ar TEXT NOT NULL,
    is_read VARCHAR(1) DEFAULT 'N',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    link_url VARCHAR(500),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    sent_via_email VARCHAR(1) DEFAULT 'N',
    email_sent_date TIMESTAMP,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_date TIMESTAMP
);
-- Note: Foreign key to employees_details is added after Hibernate creates the table
-- to avoid dependency issues during initialization
CREATE INDEX IF NOT EXISTS idx_notification_employee ON notifications(employee_no);
CREATE INDEX IF NOT EXISTS idx_notification_read ON notifications(employee_no, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications(created_date);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(notification_type);

-- System Configuration
CREATE TABLE IF NOT EXISTS system_config (
    config_id BIGINT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    config_value VARCHAR(500),
    config_type VARCHAR(20),
    config_category VARCHAR(50),
    config_description VARCHAR(1000),
    is_active VARCHAR(1) DEFAULT 'Y',
    is_editable VARCHAR(1) DEFAULT 'Y',
    default_value VARCHAR(500),
    is_deleted VARCHAR(1) DEFAULT 'N',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Force default value for config_id
ALTER TABLE system_config ALTER COLUMN config_id SET DEFAULT nextval('system_config_seq');

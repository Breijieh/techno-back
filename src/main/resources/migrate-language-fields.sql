-- Migration: Simplify dual-language fields to single name fields
-- Author: Techno ERP Team
-- Date: 2026-01-14
-- Description: Remove *_ar_name and *_en_name columns, replace with single *_name column
--              Uses Arabic names as the source of truth

-- 1. PROJECTS table
ALTER TABLE projects ADD COLUMN project_name VARCHAR(250);
UPDATE projects SET project_name = project_ar_name;
ALTER TABLE projects ALTER COLUMN project_name SET NOT NULL;
ALTER TABLE projects DROP COLUMN project_ar_name;
ALTER TABLE projects DROP COLUMN project_en_name;

-- 2. EMPLOYEES_DETAILS table
ALTER TABLE employees_details ADD COLUMN employee_name VARCHAR(250);
UPDATE employees_details SET employee_name = employee_ar_name;
ALTER TABLE employees_details ALTER COLUMN employee_name SET NOT NULL;
ALTER TABLE employees_details DROP COLUMN employee_ar_name;
ALTER TABLE employees_details DROP COLUMN employee_en_name;

-- 3. DEPARTMENTS table
ALTER TABLE departments ADD COLUMN dept_name VARCHAR(250);
UPDATE departments SET dept_name = dept_ar_name;
ALTER TABLE departments ALTER COLUMN dept_name SET NOT NULL;
ALTER TABLE departments DROP COLUMN dept_ar_name;
ALTER TABLE departments DROP COLUMN dept_en_name;

-- 4. STORE_ITEMS table
ALTER TABLE store_items ADD COLUMN item_name VARCHAR(250);
UPDATE store_items SET item_name = item_ar_name;
ALTER TABLE store_items ALTER COLUMN item_name SET NOT NULL;
ALTER TABLE store_items DROP COLUMN item_ar_name;
ALTER TABLE store_items DROP COLUMN item_en_name;

-- 5. PROJECT_STORES table
ALTER TABLE project_stores ADD COLUMN store_name VARCHAR(200);
UPDATE project_stores SET store_name = store_ar_name;
ALTER TABLE project_stores ALTER COLUMN store_name SET NOT NULL;
ALTER TABLE project_stores DROP COLUMN store_ar_name;
ALTER TABLE project_stores DROP COLUMN store_en_name;

-- 6. ITEM_CATEGORIES table
ALTER TABLE item_categories ADD COLUMN category_name VARCHAR(200);
UPDATE item_categories SET category_name = category_ar_name;
ALTER TABLE item_categories ALTER COLUMN category_name SET NOT NULL;
ALTER TABLE item_categories DROP COLUMN category_ar_name;
ALTER TABLE item_categories DROP COLUMN category_en_name;

-- 7. TRANSACTION_TYPES table
ALTER TABLE transactions_types ADD COLUMN type_name VARCHAR(250);
UPDATE transactions_types SET type_name = COALESCE(type_ar_name, type_en_name, 'Unknown Type');
ALTER TABLE transactions_types ALTER COLUMN type_name SET NOT NULL;
ALTER TABLE transactions_types DROP COLUMN type_ar_name;
ALTER TABLE transactions_types DROP COLUMN type_en_name;

-- 8. CONTRACT_TYPES table
ALTER TABLE contract_types ADD COLUMN type_name VARCHAR(100);
UPDATE contract_types SET type_name = type_ar_name;
ALTER TABLE contract_types ALTER COLUMN type_name SET NOT NULL;
ALTER TABLE contract_types DROP COLUMN type_ar_name;
ALTER TABLE contract_types DROP COLUMN type_en_name;

-- 9. MENU_FILES table
ALTER TABLE menu_files ADD COLUMN menu_name VARCHAR(250);
UPDATE menu_files SET menu_name = menu_ar_name;
ALTER TABLE menu_files ALTER COLUMN menu_name SET NOT NULL;
ALTER TABLE menu_files DROP COLUMN menu_ar_name;
ALTER TABLE menu_files DROP COLUMN menu_en_name;

-- 10. SUPPLIERS table
ALTER TABLE suppliers ADD COLUMN supplier_name VARCHAR(250);
UPDATE suppliers SET supplier_name = supplier_ar_name;
ALTER TABLE suppliers DROP COLUMN supplier_ar_name;

-- 11. HOLIDAYS table
ALTER TABLE eids_holidays ADD COLUMN holiday_name VARCHAR(200);
UPDATE eids_holidays SET holiday_name = COALESCE(holiday_name_ar, 'Unknown Holiday');
ALTER TABLE eids_holidays ALTER COLUMN holiday_name SET NOT NULL;
ALTER TABLE eids_holidays DROP COLUMN holiday_name_ar;

-- 12. WEEKEND_DAYS table
ALTER TABLE weekend_days ADD COLUMN day_name VARCHAR(50);
UPDATE weekend_days SET day_name = day_name_ar;
ALTER TABLE weekend_days ALTER COLUMN day_name SET NOT NULL;
ALTER TABLE weekend_days DROP COLUMN day_name_ar;

-- 13. EMAIL_TEMPLATES table
ALTER TABLE email_templates ADD COLUMN subject VARCHAR(500);
ALTER TABLE email_templates ADD COLUMN body TEXT;
UPDATE email_templates SET subject = subject_ar, body = body_ar;
ALTER TABLE email_templates ALTER COLUMN subject SET NOT NULL;
ALTER TABLE email_templates ALTER COLUMN body SET NOT NULL;
ALTER TABLE email_templates DROP COLUMN subject_ar;
ALTER TABLE email_templates DROP COLUMN subject_en;
ALTER TABLE email_templates DROP COLUMN body_ar;
ALTER TABLE email_templates DROP COLUMN body_en;

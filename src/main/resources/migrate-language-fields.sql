-- Migration: Simplify dual-language fields to single name fields
-- Author: Techno ERP Team
-- Date: 2026-01-14
-- Description: Remove *_ar_name and *_en_name columns, replace with single *_name column
--              Uses Arabic names as the source of truth
--              This script is idempotent and safe to run multiple times

-- Helper function to check if column exists (PostgreSQL 9.1+)
DO $$
BEGIN
    -- 1. PROJECTS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'projects' AND column_name = 'project_ar_name') THEN
        ALTER TABLE projects ADD COLUMN IF NOT EXISTS project_name VARCHAR(250);
        UPDATE projects SET project_name = project_ar_name WHERE project_name IS NULL;
        ALTER TABLE projects ALTER COLUMN project_name SET NOT NULL;
        ALTER TABLE projects DROP COLUMN IF EXISTS project_ar_name;
        ALTER TABLE projects DROP COLUMN IF EXISTS project_en_name;
    END IF;

    -- 2. EMPLOYEES_DETAILS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'employees_details' AND column_name = 'employee_ar_name') THEN
        ALTER TABLE employees_details ADD COLUMN IF NOT EXISTS employee_name VARCHAR(250);
        UPDATE employees_details SET employee_name = employee_ar_name WHERE employee_name IS NULL;
        ALTER TABLE employees_details ALTER COLUMN employee_name SET NOT NULL;
        ALTER TABLE employees_details DROP COLUMN IF EXISTS employee_ar_name;
        ALTER TABLE employees_details DROP COLUMN IF EXISTS employee_en_name;
    END IF;

    -- 3. DEPARTMENTS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'departments' AND column_name = 'dept_ar_name') THEN
        ALTER TABLE departments ADD COLUMN IF NOT EXISTS dept_name VARCHAR(250);
        UPDATE departments SET dept_name = dept_ar_name WHERE dept_name IS NULL;
        ALTER TABLE departments ALTER COLUMN dept_name SET NOT NULL;
        ALTER TABLE departments DROP COLUMN IF EXISTS dept_ar_name;
        ALTER TABLE departments DROP COLUMN IF EXISTS dept_en_name;
    END IF;

    -- 4. STORE_ITEMS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'store_items' AND column_name = 'item_ar_name') THEN
        ALTER TABLE store_items ADD COLUMN IF NOT EXISTS item_name VARCHAR(250);
        UPDATE store_items SET item_name = item_ar_name WHERE item_name IS NULL;
        ALTER TABLE store_items ALTER COLUMN item_name SET NOT NULL;
        ALTER TABLE store_items DROP COLUMN IF EXISTS item_ar_name;
        ALTER TABLE store_items DROP COLUMN IF EXISTS item_en_name;
    END IF;

    -- 5. PROJECT_STORES table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'project_stores' AND column_name = 'store_ar_name') THEN
        ALTER TABLE project_stores ADD COLUMN IF NOT EXISTS store_name VARCHAR(200);
        UPDATE project_stores SET store_name = store_ar_name WHERE store_name IS NULL;
        ALTER TABLE project_stores ALTER COLUMN store_name SET NOT NULL;
        ALTER TABLE project_stores DROP COLUMN IF EXISTS store_ar_name;
        ALTER TABLE project_stores DROP COLUMN IF EXISTS store_en_name;
    END IF;

    -- 6. ITEM_CATEGORIES table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'item_categories' AND column_name = 'category_ar_name') THEN
        ALTER TABLE item_categories ADD COLUMN IF NOT EXISTS category_name VARCHAR(200);
        UPDATE item_categories SET category_name = category_ar_name WHERE category_name IS NULL;
        ALTER TABLE item_categories ALTER COLUMN category_name SET NOT NULL;
        ALTER TABLE item_categories DROP COLUMN IF EXISTS category_ar_name;
        ALTER TABLE item_categories DROP COLUMN IF EXISTS category_en_name;
    END IF;

    -- 7. TRANSACTION_TYPES table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'transactions_types' AND column_name = 'type_ar_name') THEN
        ALTER TABLE transactions_types ADD COLUMN IF NOT EXISTS type_name VARCHAR(250);
        UPDATE transactions_types SET type_name = COALESCE(type_ar_name, type_en_name, 'Unknown Type') 
            WHERE type_name IS NULL;
        ALTER TABLE transactions_types ALTER COLUMN type_name SET NOT NULL;
        ALTER TABLE transactions_types DROP COLUMN IF EXISTS type_ar_name;
        ALTER TABLE transactions_types DROP COLUMN IF EXISTS type_en_name;
    END IF;

    -- 8. CONTRACT_TYPES table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'contract_types' AND column_name = 'type_ar_name') THEN
        ALTER TABLE contract_types ADD COLUMN IF NOT EXISTS type_name VARCHAR(100);
        UPDATE contract_types SET type_name = type_ar_name WHERE type_name IS NULL;
        ALTER TABLE contract_types ALTER COLUMN type_name SET NOT NULL;
        ALTER TABLE contract_types DROP COLUMN IF EXISTS type_ar_name;
        ALTER TABLE contract_types DROP COLUMN IF EXISTS type_en_name;
    END IF;

    -- 9. MENU_FILES table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'menu_files' AND column_name = 'menu_ar_name') THEN
        ALTER TABLE menu_files ADD COLUMN IF NOT EXISTS menu_name VARCHAR(250);
        UPDATE menu_files SET menu_name = menu_ar_name WHERE menu_name IS NULL;
        ALTER TABLE menu_files ALTER COLUMN menu_name SET NOT NULL;
        ALTER TABLE menu_files DROP COLUMN IF EXISTS menu_ar_name;
        ALTER TABLE menu_files DROP COLUMN IF EXISTS menu_en_name;
    END IF;

    -- 10. SUPPLIERS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'suppliers' AND column_name = 'supplier_ar_name') THEN
        ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS supplier_name VARCHAR(250);
        UPDATE suppliers SET supplier_name = supplier_ar_name WHERE supplier_name IS NULL;
        ALTER TABLE suppliers DROP COLUMN IF EXISTS supplier_ar_name;
    END IF;

    -- 11. HOLIDAYS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'eids_holidays' AND column_name = 'holiday_name_ar') THEN
        ALTER TABLE eids_holidays ADD COLUMN IF NOT EXISTS holiday_name VARCHAR(200);
        UPDATE eids_holidays SET holiday_name = COALESCE(holiday_name_ar, 'Unknown Holiday') 
            WHERE holiday_name IS NULL;
        ALTER TABLE eids_holidays ALTER COLUMN holiday_name SET NOT NULL;
        ALTER TABLE eids_holidays DROP COLUMN IF EXISTS holiday_name_ar;
    END IF;

    -- 12. WEEKEND_DAYS table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'weekend_days' AND column_name = 'day_name_ar') THEN
        ALTER TABLE weekend_days ADD COLUMN IF NOT EXISTS day_name VARCHAR(50);
        UPDATE weekend_days SET day_name = day_name_ar WHERE day_name IS NULL;
        ALTER TABLE weekend_days ALTER COLUMN day_name SET NOT NULL;
        ALTER TABLE weekend_days DROP COLUMN IF EXISTS day_name_ar;
    END IF;

    -- 13. EMAIL_TEMPLATES table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'email_templates' AND column_name = 'subject_ar') THEN
        ALTER TABLE email_templates ADD COLUMN IF NOT EXISTS subject VARCHAR(500);
        ALTER TABLE email_templates ADD COLUMN IF NOT EXISTS body TEXT;
        UPDATE email_templates SET subject = subject_ar, body = body_ar 
            WHERE subject IS NULL OR body IS NULL;
        ALTER TABLE email_templates ALTER COLUMN subject SET NOT NULL;
        ALTER TABLE email_templates ALTER COLUMN body SET NOT NULL;
        ALTER TABLE email_templates DROP COLUMN IF EXISTS subject_ar;
        ALTER TABLE email_templates DROP COLUMN IF EXISTS subject_en;
        ALTER TABLE email_templates DROP COLUMN IF EXISTS body_ar;
        ALTER TABLE email_templates DROP COLUMN IF EXISTS body_en;
    END IF;
END $$;

-- ====================================================================
-- Employee Contract Allowances Table Migration Script
-- Creates EMPLOYEE_CONTRACT_ALLOWANCES table for employee-specific salary breakdown percentages
-- ====================================================================

CREATE TABLE IF NOT EXISTS employee_contract_allowances (
    record_id BIGSERIAL PRIMARY KEY,
    employee_no BIGINT NOT NULL,
    trans_type_code BIGINT NOT NULL,
    salary_percentage NUMERIC(5,2) NOT NULL CHECK (salary_percentage >= 0 AND salary_percentage <= 100),
    is_active CHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_date TIMESTAMP,
    modified_by BIGINT,
    CONSTRAINT fk_emp_contract_allowance_employee FOREIGN KEY (employee_no)
        REFERENCES employees_details(employee_no) ON DELETE CASCADE,
    CONSTRAINT fk_emp_contract_allowance_trans_type FOREIGN KEY (trans_type_code)
        REFERENCES transactions_types(type_code) ON DELETE RESTRICT,
    CONSTRAINT uk_emp_contract_allowance_emp_type UNIQUE (employee_no, trans_type_code)
);

-- Create indexes for commonly queried columns
CREATE INDEX IF NOT EXISTS idx_emp_contract_allowance_employee ON employee_contract_allowances(employee_no);
CREATE INDEX IF NOT EXISTS idx_emp_contract_allowance_type ON employee_contract_allowances(trans_type_code);
CREATE INDEX IF NOT EXISTS idx_emp_contract_allowance_active ON employee_contract_allowances(is_active);
CREATE INDEX IF NOT EXISTS idx_emp_contract_allowance_created_date ON employee_contract_allowances(created_date DESC);


-- ====================================================================
-- Project Payment (PROJ_PAYMENT) Approval Workflow Configuration
-- 3-level approval: Project Manager → Finance Manager → General Manager
-- ====================================================================
-- This script creates the approval chain for project payment requests
-- Required for PaymentRequestService to work correctly
-- ====================================================================

-- Level 1: Project Manager verifies project expenses
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active, created_date)
VALUES ('PROJ_PAYMENT', 1, 'GetProjectManager', 'N', 'Project Manager verification', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (request_type, level_no) DO UPDATE
SET function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y',
    modified_date = CURRENT_TIMESTAMP;

-- Level 2: Finance Manager validates budget and amount
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active, created_date)
VALUES ('PROJ_PAYMENT', 2, 'GetFinManager', 'N', 'Finance Manager validation', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (request_type, level_no) DO UPDATE
SET function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y',
    modified_date = CURRENT_TIMESTAMP;

-- Level 3: General Manager final approval for payment
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active, created_date)
VALUES ('PROJ_PAYMENT', 3, 'GetGeneralManager', 'Y', 'General Manager final approval', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (request_type, level_no) DO UPDATE
SET function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y',
    modified_date = CURRENT_TIMESTAMP;

-- ====================================================================
-- Verification Query (uncomment to run)
-- ====================================================================
-- SELECT request_type, level_no, function_call, close_level, remarks, is_active
-- FROM requests_approval_set
-- WHERE request_type = 'PROJ_PAYMENT'
-- ORDER BY level_no;

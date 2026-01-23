-- ====================================================================
-- Project Transfer (PROJ_TRANSFER) Approval Workflow Configuration
-- 2-level approval: Current Project Manager → Target Project Manager
-- ====================================================================
-- This script creates the approval chain for project employee transfer requests
-- Required for TransferService to work correctly
-- ====================================================================
-- Approval Flow:
-- Level 1: Current Project Manager (fromProject) - Verifies employee release
-- Level 2: Target Project Manager (toProject) - Final approval for transfer
-- ====================================================================

-- Level 1: Current Project Manager verifies employee can be released
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active, created_date)
VALUES ('PROJ_TRANSFER', 1, 'GetProjectManager', 'N', 'Current Project Manager approval to release employee', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (request_type, level_no) DO UPDATE
SET function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y',
    modified_date = CURRENT_TIMESTAMP;

-- Level 2: Target Project Manager final approval to accept employee
INSERT INTO requests_approval_set (request_type, level_no, function_call, close_level, remarks, is_active, created_date)
VALUES ('PROJ_TRANSFER', 2, 'GetProjectManager', 'Y', 'Target Project Manager final approval to accept employee', 'Y', CURRENT_TIMESTAMP)
ON CONFLICT (request_type, level_no) DO UPDATE
SET function_call = EXCLUDED.function_call,
    close_level = EXCLUDED.close_level,
    remarks = EXCLUDED.remarks,
    is_active = 'Y',
    modified_date = CURRENT_TIMESTAMP;

-- ====================================================================
-- Verification Query
-- Run this to verify the approval chain was created:
-- ====================================================================
-- SELECT request_type, level_no, function_call, close_level, remarks, is_active
-- FROM requests_approval_set
-- WHERE request_type = 'PROJ_TRANSFER'
-- ORDER BY level_no;

-- Reset Attendance Module Data
-- This script truncates only attendance-related tables
-- WARNING: This will delete all attendance data!

BEGIN;

-- 1. Delete allowances created from attendance (Overtime - Type 9)
DELETE FROM emp_monthly_allowances WHERE transaction_type = 9;

-- 2. Delete deductions created from attendance (Late/Early/Shortage - Type 20)
DELETE FROM emp_monthly_deductions WHERE transaction_type = 20;

-- 3. Delete attendance day closures
TRUNCATE TABLE attendance_day_closure CASCADE;

-- 4. Delete manual attendance requests
TRUNCATE TABLE manual_attendance_requests CASCADE;

-- 5. Delete attendance transactions
TRUNCATE TABLE emp_attendance_transactions CASCADE;

-- 6. Reset sequences (adjust sequence names based on your actual sequences)
-- Note: PostgreSQL uses different naming, check with: \ds
DO $$
DECLARE
    seq_name TEXT;
BEGIN
    -- Reset attendance transaction sequence
    SELECT setval(pg_get_serial_sequence('emp_attendance_transactions', 'transaction_id'), 1, false)
    WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'emp_attendance_transactions');
    
    -- Reset manual request sequence
    SELECT setval(pg_get_serial_sequence('manual_attendance_requests', 'request_id'), 1, false)
    WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'manual_attendance_requests');
    
    -- Reset closure sequence
    SELECT setval(pg_get_serial_sequence('attendance_day_closure', 'closure_id'), 1, false)
    WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'attendance_day_closure');
END $$;

COMMIT;

-- Verify reset
SELECT 
    'Attendance Transactions' AS table_name,
    COUNT(*) AS record_count
FROM emp_attendance_transactions
UNION ALL
SELECT 
    'Manual Requests',
    COUNT(*)
FROM manual_attendance_requests
UNION ALL
SELECT 
    'Day Closures',
    COUNT(*)
FROM attendance_day_closure;

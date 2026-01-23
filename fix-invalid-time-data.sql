-- Fix Invalid Time Data in user_accounts table
-- This script sets all last_login_time values to NULL to fix the DateTimeException
-- The issue occurs when PostgreSQL TIME values have invalid nanoseconds that cannot be converted to Java LocalTime

-- Quick fix: Set all last_login_time to NULL
-- This is safe because last_login_time is optional and will be set on next login
UPDATE user_accounts 
SET last_login_time = NULL;

-- Verify the fix
SELECT 
    user_id,
    username,
    last_login_date,
    last_login_time
FROM user_accounts
ORDER BY user_id;

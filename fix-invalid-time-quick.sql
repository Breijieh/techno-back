-- Quick Fix: Set all last_login_time to NULL
-- This fixes the DateTimeException: Invalid value for NanoOfSecond
-- Run this script using Docker:
-- type fix-invalid-time-quick.sql | docker exec -i techno-postgres psql -U techno_admin -d techno_erp

UPDATE user_accounts SET last_login_time = NULL;

-- Verify
SELECT user_id, username, last_login_date, last_login_time FROM user_accounts LIMIT 10;

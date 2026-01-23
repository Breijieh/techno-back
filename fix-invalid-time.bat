@echo off
REM ====================================================================
REM Fix Invalid Time Data in user_accounts table
REM This fixes the DateTimeException: Invalid value for NanoOfSecond
REM ====================================================================

echo.
echo ========================================
echo   Fix Invalid Time Data
echo ========================================
echo.

REM Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Desktop is not running!
    pause
    exit /b 1
)

REM Check if PostgreSQL container is running
docker ps | findstr "techno-postgres" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] techno-postgres container is not running.
    pause
    exit /b 1
)

echo [INFO] Fixing invalid time data...
echo.

REM Execute SQL script
type fix-invalid-time-quick.sql | docker exec -i techno-postgres psql -U techno_admin -d techno_erp

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Invalid time data fixed!
    echo All last_login_time values have been set to NULL.
    echo They will be set correctly on next login.
) else (
    echo.
    echo [ERROR] Failed to fix time data
)

echo.
pause

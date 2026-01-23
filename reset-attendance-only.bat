@echo off
REM ====================================================================
REM Reset Attendance Data Only
REM This script clears only attendance-related tables
REM ====================================================================

echo.
echo ========================================
echo   Reset Attendance Data Only
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

echo [WARNING] This will delete all attendance data!
set /p confirm="Type 'YES' to continue: "

if /i not "%confirm%"=="YES" (
    echo [CANCELLED] Operation cancelled.
    pause
    exit /b 0
)

echo.
echo [INFO] Resetting attendance data...
echo.

REM Execute SQL script
if exist "reset-attendance-data.sql" (
    type reset-attendance-data.sql | docker exec -i techno-postgres psql -U techno_admin -d techno_erp
    if %errorlevel% equ 0 (
        echo.
        echo [SUCCESS] Attendance data reset complete!
    ) else (
        echo.
        echo [ERROR] Failed to reset attendance data
    )
) else (
    echo [ERROR] reset-attendance-data.sql not found!
)

echo.
pause

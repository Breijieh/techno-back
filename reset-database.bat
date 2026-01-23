@echo off
REM ====================================================================
REM Reset Database Script for Techno ERP
REM WARNING: This will delete all data in techno_erp database!
REM ====================================================================

echo.
echo ========================================
echo   Techno ERP Database Reset Script
echo ========================================
echo.

REM Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Desktop is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Check if PostgreSQL container is running
docker ps | findstr "techno-postgres" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] techno-postgres container is not running.
    echo Please start the database using: docker-compose up -d
    pause
    exit /b 1
)

echo [OK] Docker and PostgreSQL container are running
echo.

REM Confirmation
echo [WARNING] This will delete all data in techno_erp database!
set /p confirm="Type 'YES' to continue: "

if /i not "%confirm%"=="YES" (
    echo.
    echo [CANCELLED] Operation cancelled.
    pause
    exit /b 0
)

echo.
echo [INFO] Resetting database...
echo.

REM Drop database
echo Dropping database...
docker exec techno-postgres psql -U techno_admin -d postgres -c "DROP DATABASE IF EXISTS techno_erp;" >nul 2>&1

REM Create fresh database
echo Creating fresh database...
docker exec techno-postgres psql -U techno_admin -d postgres -c "CREATE DATABASE techno_erp WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';" >nul 2>&1

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Database reset complete!
    echo.
    echo Next steps:
    echo   1. Start backend: mvn spring-boot:run
    echo   2. Hibernate will create all tables automatically
    echo   3. (Optional) Run seed data if available
) else (
    echo.
    echo [ERROR] Failed to reset database
    echo Please check the error messages above
)

echo.
pause

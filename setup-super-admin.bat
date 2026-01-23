@echo off
REM ====================================================================
REM Super Admin Setup Script for Techno ERP System (Windows)
REM This script creates a super admin user with all necessary reference data
REM ====================================================================

echo.
echo 🚀 Starting Super Admin Setup...
echo.

REM Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Docker is not running. Please start Docker and try again.
    exit /b 1
)

REM Check if PostgreSQL container is running
docker ps | findstr "techno-postgres" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: techno-postgres container is not running.
    echo    Please start the database using: docker-compose up -d postgres
    exit /b 1
)

echo ✅ Docker and PostgreSQL container are running
echo.

REM Wait for PostgreSQL to be ready
echo ⏳ Waiting for PostgreSQL to be ready...
timeout /t 3 /nobreak >nul

REM Check database connection
docker exec techno-postgres pg_isready -U techno_admin -d techno_erp >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Cannot connect to PostgreSQL database.
    echo    Please ensure the database is running and accessible.
    exit /b 1
)

echo ✅ Database connection successful
echo.

REM Execute SQL script
echo 📝 Executing SQL script to create super admin...
docker exec -i techno-postgres psql -U techno_admin -d techno_erp < create-super-admin.sql

if %errorlevel% equ 0 (
    echo.
    echo ✅ Super Admin setup completed successfully!
    echo.
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo   Login Credentials:
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo   Username: admin
    echo   Password: admin123
    echo   National ID: 1111111111
    echo   Role: ADMIN (Super Admin)
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo.
    echo You can now login at: http://localhost:3000/login
    echo.
) else (
    echo.
    echo ❌ Error: Failed to execute SQL script
    exit /b 1
)

pause

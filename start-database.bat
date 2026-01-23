@echo off
echo ========================================
echo Starting Techno ERP PostgreSQL Database
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Desktop is not running!
    echo.
    echo Please start Docker Desktop and wait for it to fully start,
    echo then run this script again.
    echo.
    pause
    exit /b 1
)

echo [OK] Docker is running
echo.
echo Starting PostgreSQL container...
docker-compose up -d

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo [SUCCESS] PostgreSQL is starting!
    echo ========================================
    echo.
    echo Waiting for database to be ready...
    timeout /t 10 /nobreak >nul
    
    echo.
    echo Database Connection Details:
    echo ---------------------------
    echo Host:     localhost
    echo Port:     5432
    echo Database: techno_erp
    echo Username: techno_admin
    echo Password: techno_2025
    echo.
    echo JDBC URL: jdbc:postgresql://localhost:5432/techno_erp
    echo.
    echo ========================================
    echo You can now start the Spring Boot application!
    echo ========================================
) else (
    echo.
    echo [ERROR] Failed to start PostgreSQL container
    echo Please check the error messages above
)

echo.
pause


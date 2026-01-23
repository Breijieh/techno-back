@echo off
REM ====================================================================
REM Super Admin Setup Script using Backend API (Windows)
REM This script creates a super admin user via the backend API
REM ====================================================================

echo.
echo 🚀 Starting Super Admin Setup via API...
echo.

REM Configuration
set BACKEND_URL=http://localhost:8080
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD=admin123
set ADMIN_NATIONAL_ID=1111111111

REM Check if backend is running
echo ⏳ Checking if backend is running...
curl -s -f "%BACKEND_URL%/api/public/health" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Backend is not running at %BACKEND_URL%
    echo    Please start the backend using: mvnw spring-boot:run
    exit /b 1
)

echo ✅ Backend is running
echo.

REM Create super admin user via API
echo 📝 Creating super admin user...

curl -s -w "\n%%{http_code}" -X POST "%BACKEND_URL%/api/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"%ADMIN_USERNAME%\", \"password\": \"%ADMIN_PASSWORD%\", \"nationalId\": \"%ADMIN_NATIONAL_ID%\", \"userType\": \"ADMIN\"}" > temp_response.txt

REM Extract HTTP code (last line)
for /f "tokens=*" %%i in ('powershell -Command "(Get-Content temp_response.txt | Select-Object -Last 1)"') do set HTTP_CODE=%%i

if "%HTTP_CODE%"=="201" (
    echo ✅ Super Admin user created successfully!
) else if "%HTTP_CODE%"=="409" (
    echo ⚠️  User already exists. If you need to reset the password, use the reset password feature in the UI.
) else if "%HTTP_CODE%"=="403" (
    echo ❌ Error: Access denied. Registration endpoint might require authentication.
    del temp_response.txt
    exit /b 1
) else (
    echo ❌ Error: Failed to create user (HTTP %HTTP_CODE%^)
    type temp_response.txt
    del temp_response.txt
    exit /b 1
)

del temp_response.txt >nul 2>&1

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   Login Credentials:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   Username: %ADMIN_USERNAME%
echo   Password: %ADMIN_PASSWORD%
echo   National ID: %ADMIN_NATIONAL_ID%
echo   Role: ADMIN (Super Admin^)
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo You can now login at: http://localhost:3000/login
echo.

pause

@echo off
echo ===================================================
echo Starting Techno Backend (Skipping Tests)
echo ===================================================
echo.
call mvn spring-boot:run "-Dmaven.test.skip=true"
if errorlevel 1 (
    echo.
    echo [ERROR] Application failed to start.
    pause
)

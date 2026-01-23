@echo off
echo ========================================
echo Assign Schedule to Project 3
echo ========================================
echo.
echo This script will assign a schedule to project 3.
echo.
echo First, let's see available schedules:
echo.
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, required_hours FROM time_schedule WHERE is_active = 'Y' ORDER BY schedule_id;"

echo.
echo Enter the schedule_id you want to assign to project 3:
set /p SCHEDULE_ID=

if "%SCHEDULE_ID%"=="" (
    echo No schedule ID provided. Exiting.
    pause
    exit /b
)

echo.
echo Assigning schedule %SCHEDULE_ID% to project 3...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "UPDATE time_schedule SET project_code = 3 WHERE schedule_id = %SCHEDULE_ID%;"

echo.
echo Verifying assignment...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, required_hours FROM time_schedule WHERE schedule_id = %SCHEDULE_ID%;"

echo.
echo Done! Schedule %SCHEDULE_ID% has been assigned to project 3.
pause

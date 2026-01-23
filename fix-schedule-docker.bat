@echo off
echo ========================================
echo Checking Schedule for Project 3
echo ========================================
echo.

echo [1] Checking if schedule exists for project 3...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, department_code, required_hours, is_active FROM time_schedule WHERE project_code = 3;"

echo.
echo [2] Listing all available schedules...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, department_code, required_hours, is_active FROM time_schedule ORDER BY schedule_id;"

echo.
echo [3] Checking project 3 details...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT project_code, project_name FROM projects WHERE project_code = 3;"

echo.
echo [4] Checking employee 11's assignments...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT employee_no, employee_name, primary_project_code, primary_dept_code FROM employees_details WHERE employee_no = 11;"

echo.
echo [5] Checking active labor assignments for employee 11...
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT assignment_no, employee_no, project_code, start_date, end_date, assignment_status FROM project_labor_assignments WHERE employee_no = 11 AND assignment_status = 'ACTIVE';"

echo.
echo ========================================
echo To assign a schedule to project 3, run:
echo docker exec techno-postgres psql -U techno_admin -d techno_erp -c "UPDATE time_schedule SET project_code = 3 WHERE schedule_id = 2;"
echo ========================================
pause

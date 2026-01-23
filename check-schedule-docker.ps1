# PowerShell script to check schedule for project 3
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Checking Schedule for Project 3" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1] Checking if schedule exists for project 3..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, department_code, required_hours, is_active FROM time_schedule WHERE project_code = 3;"

Write-Host ""
Write-Host "[2] Listing all available schedules..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, department_code, required_hours, is_active FROM time_schedule ORDER BY schedule_id;"

Write-Host ""
Write-Host "[3] Checking project 3 details..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT project_code, project_name FROM projects WHERE project_code = 3;"

Write-Host ""
Write-Host "[4] Checking employee 11's assignments..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT employee_no, employee_name, primary_project_code, primary_dept_code FROM employees_details WHERE employee_no = 11;"

Write-Host ""
Write-Host "[5] Checking active labor assignments for employee 11..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT assignment_no, employee_no, project_code, start_date, end_date, assignment_status FROM project_labor_assignments WHERE employee_no = 11 AND assignment_status = 'ACTIVE';"

Write-Host ""
Write-Host "[6] Checking attendance record (transaction 12)..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT transaction_id, employee_no, attendance_date, project_code, scheduled_hours, working_hours FROM emp_attendance_transactions WHERE transaction_id = 12;"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "To assign a schedule to project 3, run:" -ForegroundColor Green
Write-Host 'docker exec techno-postgres psql -U techno_admin -d techno_erp -c "UPDATE time_schedule SET project_code = 3 WHERE schedule_id = 2;"' -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan

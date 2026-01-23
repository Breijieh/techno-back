# Docker SQL Queries for Schedule Debugging

## Quick Check Commands

### 1. Check if schedule exists for project 3
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, department_code, required_hours, is_active FROM time_schedule WHERE project_code = 3;"
```

### 2. List all schedules
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, department_code, required_hours, is_active FROM time_schedule ORDER BY schedule_id;"
```

### 3. Check project 3 details
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT project_code, project_name FROM projects WHERE project_code = 3;"
```

### 4. Check employee 11's primary project
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT employee_no, employee_name, primary_project_code, primary_dept_code FROM employees_details WHERE employee_no = 11;"
```

### 5. Check active labor assignments for employee 11
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT assignment_no, employee_no, project_code, start_date, end_date, assignment_status FROM project_labor_assignments WHERE employee_no = 11 AND assignment_status = 'ACTIVE';"
```

### 6. Check attendance record (transaction 12)
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT transaction_id, employee_no, attendance_date, project_code, scheduled_hours, working_hours FROM emp_attendance_transactions WHERE transaction_id = 12;"
```

## Fix Commands

### Assign a schedule to project 3
```bash
# First, see available schedules
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, required_hours FROM time_schedule WHERE is_active = 'Y' ORDER BY schedule_id;"

# Then assign (replace 2 with the schedule_id you want)
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "UPDATE time_schedule SET project_code = 3 WHERE schedule_id = 2;"

# Verify the assignment
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, required_hours FROM time_schedule WHERE schedule_id = 2;"
```

### Remove schedule assignment from a project
```bash
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "UPDATE time_schedule SET project_code = NULL WHERE schedule_id = 2;"
```

## Batch Files

You can also use the provided batch files:
- `check-schedule-docker.bat` - Runs all check queries
- `assign-schedule-to-project3.bat` - Interactive script to assign a schedule

## Notes

- Database: `techno_erp`
- Username: `techno_admin`
- Container: `techno-postgres`
- If container name is different, replace `techno-postgres` with your actual container name

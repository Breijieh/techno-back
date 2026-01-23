-- Check if a schedule exists for project 3
SELECT 
    schedule_id, 
    schedule_name, 
    project_code, 
    department_code, 
    required_hours, 
    scheduled_start_time, 
    scheduled_end_time, 
    is_active 
FROM time_schedule 
WHERE project_code = 3;

-- Check all schedules to see what's available
SELECT 
    schedule_id, 
    schedule_name, 
    project_code, 
    department_code, 
    required_hours, 
    is_active 
FROM time_schedule 
ORDER BY schedule_id;

-- Check if project 3 exists
SELECT project_code, project_name FROM projects WHERE project_code = 3;

-- Check employee 11's primary project
SELECT employee_no, employee_name, primary_project_code, primary_dept_code 
FROM employees_details 
WHERE employee_no = 11;

-- Check labor assignments for employee 11
SELECT assignment_no, employee_no, project_code, start_date, end_date, assignment_status
FROM project_labor_assignments
WHERE employee_no = 11 AND assignment_status = 'ACTIVE';

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

-- To assign a schedule to project 3, first check available schedules:
-- Example: If schedule_id = 2 should be assigned to project 3:
-- UPDATE time_schedule SET project_code = 3 WHERE schedule_id = 2;

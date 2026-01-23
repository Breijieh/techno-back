-- Update roles table with missing roles
INSERT INTO roles (role_name, description, is_active, created_date, created_by)
VALUES 
    ('Project Secretary', 'Administrative support for projects, attendance management', 'Y', CURRENT_TIMESTAMP, 1),
    ('Project Advisor', 'Consultant role with read-only access to project data', 'Y', CURRENT_TIMESTAMP, 1),
    ('Regional Project Manager', 'Oversight role for multiple projects within a region', 'Y', CURRENT_TIMESTAMP, 1)
ON CONFLICT (role_name) DO NOTHING;

-- Grant initial permissions (example - adjust as needed)
-- Project Secretary: Manage Attendance, View Projects, View Employees
INSERT INTO role_permissions (role_id, can_manage_attendance, can_manage_projects, can_manage_employees, can_view_reports, created_by)
SELECT role_id, 'Y', 'N', 'N', 'Y', 1
FROM roles WHERE role_name = 'Project Secretary'
ON CONFLICT (role_id) DO NOTHING;

-- Project Advisor: View Projects, View Reports
INSERT INTO role_permissions (role_id, can_manage_attendance, can_manage_projects, can_manage_employees, can_view_reports, created_by)
SELECT role_id, 'N', 'N', 'N', 'Y', 1
FROM roles WHERE role_name = 'Project Advisor'
ON CONFLICT (role_id) DO NOTHING;

-- Regional Project Manager: Manage Projects, View Reports
INSERT INTO role_permissions (role_id, can_manage_attendance, can_manage_projects, can_manage_employees, can_view_reports, created_by)
SELECT role_id, 'N', 'Y', 'N', 'Y', 1
FROM roles WHERE role_name = 'Regional Project Manager'
ON CONFLICT (role_id) DO NOTHING;

# PowerShell script to assign a schedule to project 3
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Assign Schedule to Project 3" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Available active schedules:" -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, required_hours FROM time_schedule WHERE is_active = 'Y' ORDER BY schedule_id;"

Write-Host ""
$scheduleId = Read-Host "Enter the schedule_id you want to assign to project 3"

if ([string]::IsNullOrWhiteSpace($scheduleId)) {
    Write-Host "No schedule ID provided. Exiting." -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "Assigning schedule $scheduleId to project 3..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "UPDATE time_schedule SET project_code = 3 WHERE schedule_id = $scheduleId;"

Write-Host ""
Write-Host "Verifying assignment..." -ForegroundColor Yellow
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT schedule_id, schedule_name, project_code, required_hours FROM time_schedule WHERE schedule_id = $scheduleId;"

Write-Host ""
Write-Host "Done! Schedule $scheduleId has been assigned to project 3." -ForegroundColor Green

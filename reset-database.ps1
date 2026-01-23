# Reset Database Script for Techno ERP
# WARNING: This will delete all data in techno_erp database!

param(
    [switch]$FullReset,
    [switch]$AttendanceOnly,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Techno ERP Database Reset Script" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check if Docker is running
try {
    docker ps | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not running"
    }
} catch {
    Write-Host "[ERROR] Docker is not running" -ForegroundColor Red
    Write-Host "   Please start Docker Desktop and try again." -ForegroundColor Yellow
    exit 1
}

# Check if PostgreSQL container is running
$containerRunning = docker ps --filter "name=techno-postgres" --format "{{.Names}}" | Select-String "techno-postgres"
if (-not $containerRunning) {
    Write-Host "[ERROR] techno-postgres container is not running" -ForegroundColor Red
    Write-Host "   Please start the database using: docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

# Check if PostgreSQL is accessible
try {
    $testConnection = docker exec techno-postgres pg_isready -U techno_admin -d techno_erp 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Cannot connect to PostgreSQL"
    }
} catch {
    Write-Host "[ERROR] Cannot connect to PostgreSQL database" -ForegroundColor Red
    Write-Host "   Make sure PostgreSQL container is running and healthy." -ForegroundColor Yellow
    exit 1
}

# Confirmation
if (-not $Force) {
    Write-Host "[WARNING] This will delete data!" -ForegroundColor Red
    if ($FullReset) {
        Write-Host "   Mode: FULL RESET (Drop and recreate entire database)" -ForegroundColor Yellow
    } elseif ($AttendanceOnly) {
        Write-Host "   Mode: ATTENDANCE ONLY (Clear only attendance-related data)" -ForegroundColor Yellow
    } else {
        Write-Host "   Mode: FULL RESET (Drop and recreate entire database)" -ForegroundColor Yellow
    }
    $confirm = Read-Host "`nType 'YES' to continue"
    
    if ($confirm -ne "YES") {
        Write-Host "`n[CANCELLED] Operation cancelled." -ForegroundColor Yellow
        exit 0
    }
}

# Stop backend if running
Write-Host "`nStopping backend processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.Path -like "*java*" -and (Get-WmiObject Win32_Process -Filter "ProcessId = $($_.Id)").CommandLine -like "*spring-boot*"
}
if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force
    Start-Sleep -Seconds 2
    Write-Host "   [OK] Backend stopped" -ForegroundColor Green
} else {
    Write-Host "   [INFO] No backend process found" -ForegroundColor Gray
}

if ($AttendanceOnly) {
    # Reset only attendance data
    Write-Host "`nResetting attendance data only..." -ForegroundColor Yellow
    
    $scriptPath = Join-Path $PSScriptRoot "reset-attendance-data.sql"
    if (Test-Path $scriptPath) {
        Get-Content $scriptPath | docker exec -i techno-postgres psql -U techno_admin -d techno_erp
        if ($LASTEXITCODE -eq 0) {
            Write-Host "   [OK] Attendance data reset complete!" -ForegroundColor Green
        } else {
            Write-Host "   [ERROR] Error resetting attendance data" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "   [ERROR] reset-attendance-data.sql not found!" -ForegroundColor Red
        exit 1
    }
} else {
    # Full database reset
    Write-Host "`nResetting database..." -ForegroundColor Yellow
    
    # Drop database
    Write-Host "   Dropping database..." -ForegroundColor Gray
    docker exec techno-postgres psql -U techno_admin -d postgres -c "DROP DATABASE IF EXISTS techno_erp;" 2>&1 | Out-Null
    
    # Create fresh database
    Write-Host "   Creating fresh database..." -ForegroundColor Gray
    docker exec techno-postgres psql -U techno_admin -d postgres -c "CREATE DATABASE techno_erp WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';" 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] Database reset complete!" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] Error resetting database" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Reset completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "   1. Start backend: mvn spring-boot:run" -ForegroundColor White
Write-Host "   2. Hibernate will create all tables automatically" -ForegroundColor White
if (-not $AttendanceOnly) {
    Write-Host "   3. (Optional) Run seed data using:" -ForegroundColor White
    $seedCommand = "Get-Content src/main/resources/data-seed.sql | docker exec -i techno-postgres psql -U techno_admin -d techno_erp"
    Write-Host "      $seedCommand" -ForegroundColor Gray
}
Write-Host ""

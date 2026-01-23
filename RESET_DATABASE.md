# Database & Backend Reset Guide

## ⚠️ WARNING
**This will delete all data in the database. Make sure you have backups if needed!**

---

## Option 1: Full Database Reset (Drop & Recreate)

### Step 1: Stop Backend Server
- Press `Ctrl+C` in the terminal running the backend
- Or close the terminal/IDE

### Step 2: Drop and Recreate Database

**Using PostgreSQL Command Line:**
```bash
# Connect to PostgreSQL
psql -U postgres

# Drop database (if exists)
DROP DATABASE IF EXISTS techno_erp;

# Create fresh database
CREATE DATABASE techno_erp 
    WITH ENCODING 'UTF8' 
    LC_COLLATE='en_US.UTF-8' 
    LC_CTYPE='en_US.UTF-8';

# Exit psql
\q
```

**Using pgAdmin:**
1. Right-click on `techno_erp` database
2. Select "Delete/Drop"
3. Confirm deletion
4. Right-click on "Databases"
5. Create new database: `techno_erp`
6. Set encoding to `UTF8`

### Step 3: Run Schema Script
```bash
cd techno-backend
# The schema will be created automatically by Hibernate (ddl-auto=update)
# OR run schema.sql manually if needed
```

### Step 4: Run Seed Data (Optional)
```bash
# If you have seed data, run it
psql -U techno_admin -d techno_erp -f src/main/resources/data-seed.sql
```

### Step 5: Restart Backend
```bash
mvn spring-boot:run
```

---

## Option 2: Selective Reset (Keep Structure, Clear Data)

### Reset Only Attendance-Related Tables

**SQL Script:**
```sql
-- Connect to database
\c techno_erp

-- Disable foreign key checks temporarily (PostgreSQL doesn't have this, but we'll use CASCADE)
-- Delete in correct order to respect foreign keys

-- 1. Delete attendance transactions (this will cascade to related records)
TRUNCATE TABLE emp_attendance_transactions CASCADE;

-- 2. Delete manual attendance requests
TRUNCATE TABLE manual_attendance_requests CASCADE;

-- 3. Delete attendance day closures
TRUNCATE TABLE attendance_day_closure CASCADE;

-- 4. Delete allowances created from attendance
DELETE FROM emp_monthly_allowances WHERE transaction_type = 9; -- Overtime

-- 5. Delete deductions created from attendance
DELETE FROM emp_monthly_deductions WHERE transaction_type = 20; -- Late/Early/Shortage

-- 6. Reset sequences (if using auto-increment)
-- Note: Adjust sequence names based on your actual sequences
ALTER SEQUENCE IF EXISTS emp_attendance_transactions_transaction_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS manual_attendance_requests_request_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS attendance_day_closure_closure_id_seq RESTART WITH 1;
```

**Save as:** `reset-attendance-data.sql`

**Run it:**
```bash
psql -U techno_admin -d techno_erp -f reset-attendance-data.sql
```

---

## Option 3: Complete Database Reset (All Tables)

### Reset All Tables (Nuclear Option)

**SQL Script:**
```sql
-- Connect to database
\c techno_erp

-- Drop all tables (CASCADE will handle foreign keys)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO techno_admin;
GRANT ALL ON SCHEMA public TO public;

-- Now run schema.sql to recreate all tables
```

**Then run:**
```bash
psql -U techno_admin -d techno_erp -f src/main/resources/schema.sql
```

---

## Option 4: Reset via Spring Boot (Hibernate)

### Let Hibernate Recreate Tables

1. **Update application.properties:**
```properties
# Change from 'update' to 'create-drop' (WARNING: This drops all tables on shutdown!)
spring.jpa.hibernate.ddl-auto=create-drop
```

2. **Start Backend:**
```bash
mvn spring-boot:run
```
- Tables will be created on startup

3. **Stop Backend:**
- Press Ctrl+C
- Tables will be dropped on shutdown

4. **Change back to 'update':**
```properties
spring.jpa.hibernate.ddl-auto=update
```

5. **Start Backend again:**
- Tables will be recreated

---

## Quick Reset Script (PowerShell)

Save this as `reset-database.ps1`:

```powershell
# Reset Database Script
# WARNING: This will delete all data!

Write-Host "⚠️  WARNING: This will delete all data in techno_erp database!" -ForegroundColor Red
$confirm = Read-Host "Type 'YES' to continue"

if ($confirm -ne "YES") {
    Write-Host "Cancelled." -ForegroundColor Yellow
    exit
}

# Stop backend if running (optional)
Write-Host "`nStopping backend processes..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*spring-boot*" } | Stop-Process -Force

# Drop and recreate database
Write-Host "`nResetting database..." -ForegroundColor Yellow
$env:PGPASSWORD = "techno_2025"
psql -U techno_admin -d postgres -c "DROP DATABASE IF EXISTS techno_erp;"
psql -U techno_admin -d postgres -c "CREATE DATABASE techno_erp WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';"

Write-Host "`n✅ Database reset complete!" -ForegroundColor Green
Write-Host "Now start the backend: mvn spring-boot:run" -ForegroundColor Cyan
```

**Run it:**
```powershell
cd techno-backend
.\reset-database.ps1
```

---

## Reset Backend (Clear Cache, Restart)

### Step 1: Stop Backend
```bash
# In the terminal running backend, press Ctrl+C
```

### Step 2: Clear Maven Cache (Optional)
```bash
cd techno-backend
mvn clean
```

### Step 3: Clear Target Directory
```bash
# Remove compiled classes
Remove-Item -Recurse -Force target
```

### Step 4: Restart Backend
```bash
mvn spring-boot:run
```

---

## Recommended Reset Procedure

### For Development/Testing:

1. **Stop Backend** (Ctrl+C)

2. **Reset Database:**
   ```bash
   psql -U techno_admin -d postgres -c "DROP DATABASE IF EXISTS techno_erp;"
   psql -U techno_admin -d postgres -c "CREATE DATABASE techno_erp WITH ENCODING 'UTF8';"
   ```

3. **Start Backend:**
   ```bash
   cd techno-backend
   mvn spring-boot:run
   ```
   - Hibernate will create all tables automatically (`ddl-auto=update`)

4. **Run Seed Data (if available):**
   ```bash
   psql -U techno_admin -d techno_erp -f src/main/resources/data-seed.sql
   ```

---

## Verify Reset

### Check Database is Empty:
```sql
-- Connect to database
\c techno_erp

-- List all tables
\dt

-- Check if attendance tables exist but are empty
SELECT COUNT(*) FROM emp_attendance_transactions; -- Should be 0
SELECT COUNT(*) FROM manual_attendance_requests; -- Should be 0
```

### Check Backend Started Successfully:
- Look for: `Started BackendApplication in X.XXX seconds`
- No errors in console
- Server running on `http://localhost:8080/api`

---

## Quick Commands Reference

### Using Docker (Recommended)

```bash
# Full reset (using Docker)
docker exec techno-postgres psql -U techno_admin -d postgres -c "DROP DATABASE IF EXISTS techno_erp;"
docker exec techno-postgres psql -U techno_admin -d postgres -c "CREATE DATABASE techno_erp WITH ENCODING 'UTF8';"

# Reset only attendance data
type reset-attendance-data.sql | docker exec -i techno-postgres psql -U techno_admin -d techno_erp

# Check database size
docker exec techno-postgres psql -U techno_admin -d techno_erp -c "SELECT pg_size_pretty(pg_database_size('techno_erp'));"

# Connect to database
docker exec -it techno-postgres psql -U techno_admin -d techno_erp
```

### Using Batch Files (Windows)

```bash
# Full database reset
reset-database.bat

# Reset attendance data only
reset-attendance-only.bat
```

### Using PowerShell Script

```powershell
# Full database reset
.\reset-database.ps1

# Reset attendance data only
.\reset-database.ps1 -AttendanceOnly
```

---

**Last Updated**: January 18, 2025

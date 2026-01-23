# Repair data.sql values

$dataFile = "c:\Projects\Techno-Fullstack\techno-backend\src\main\resources\data.sql"
$content = [System.IO.File]::ReadAllText($dataFile, [System.Text.UTF8Encoding]::new($false))

# 1. Fix Contract Types
# Missing is_active ('Y')
# Pattern: VALUES ('CODE', 'Name', 'Y', 'Y', CURRENT_TIMESTAMP)
# Target:  VALUES ('CODE', 'Name', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)
$content = $content -replace "VALUES \('([^']+)', '([^']+)', 'Y', 'Y', CURRENT_TIMESTAMP\)", "VALUES ('`$1', '`$2', 'Y', 'Y', 'Y', CURRENT_TIMESTAMP)"

# 2. Fix Transaction Types
# Missing allowance_deduction ('A' or 'D')
# Pattern: VALUES (ID, 'Name', 'Y', 'Y', CURRENT_TIMESTAMP)

# Allowances (IDs 1-19): Add 'A'
# Using regex that matches IDs 1-9 or 10-19
$content = $content -replace "VALUES \((\d{1}|1\d), '([^']+)', 'Y', 'Y', CURRENT_TIMESTAMP\)", "VALUES (`$1, '`$2', 'A', 'Y', 'Y', CURRENT_TIMESTAMP)"

# Deductions (IDs 20-39): Add 'D'
$content = $content -replace "VALUES \((2\d|3\d), '([^']+)', 'Y', 'Y', CURRENT_TIMESTAMP\)", "VALUES (`$1, '`$2', 'D', 'Y', 'Y', CURRENT_TIMESTAMP)"

[System.IO.File]::WriteAllText($dataFile, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "Repaired data.sql" -ForegroundColor Green

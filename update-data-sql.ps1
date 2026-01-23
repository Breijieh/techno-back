# Update data.sql to use single language fields

$dataFile = "c:\Projects\Techno-Fullstack\techno-backend\src\main\resources\data.sql"
$content = [System.IO.File]::ReadAllText($dataFile, [System.Text.UTF8Encoding]::new($false))

# 1. Contract Types
# Pattern: type_ar_name, type_en_name -> type_name
$content = $content -replace 'type_ar_name, type_en_name', 'type_name'
# Values: 'Arabic', 'English' -> 'Arabic'
# Regex: '([^']+)',\s*'([^']+)' -> '$1' (applied to specific blocks)
# We can't apply globally because email_templates uses similar structure.
# We'll target specific INSERT statements using table names.

# Helper to fix values for a simplified table schema
function Fix-Values {
    param($text, $tableName, $targetColumn)
    
    # Replace column definition
    # e.g. type_ar_name, type_en_name -> type_name
    $text = $text -replace "${targetColumn}_ar_name, ${targetColumn}_en_name", "${targetColumn}_name"
    $text = $text -replace "${targetColumn}_ar, ${targetColumn}_en", "${targetColumn}"
    
    # Replace VALUES
    # We look for INSERT INTO tableName ... VALUES ...
    # This is tricky with regex across lines.
    # Instead, let's look for specific patterns we know exist in data.sql
    
    return $text
}

# Apply specific known replacements for this file
# Contract Types & Transaction Types
# VALUES ('CODE', 'Arabic', 'English', ...
$content = $content -replace "VALUES \('([^']+)', '([^']+)', '([^']+)',", "VALUES ('`$1', '`$2',"

# Transaction Types (Int ID)
# VALUES (1, 'Arabic', 'English', ...
$content = $content -replace "VALUES \((\d+), '([^']+)', '([^']+)',", "VALUES (`$1, '`$2',"

# Weekend Days
# VALUES (5, 'الجمعة', 'Friday',
$content = $content -replace "day_name_ar, day_name_en", "day_name"
$content = $content -replace "VALUES \((\d+), '([^']+)', '([^']+)',", "VALUES (`$1, '`$2',"

# Holidays - eids_holidays
# holiday_name_ar, holiday_name_en -> holiday_name
$content = $content -replace "holiday_name_ar, holiday_name_en", "holiday_name"
# VALUES ('2025-09-23', 'Arabic', 'English',
$content = $content -replace "VALUES \('([^']+)', '([^']+)', '([^']+)',", "VALUES ('`$1', '`$2',"

[System.IO.File]::WriteAllText($dataFile, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "Updated data.sql" -ForegroundColor Green

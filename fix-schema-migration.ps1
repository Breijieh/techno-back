# Final fix for SchemaMigration.java SQL statements

$file = "c:\Projects\Techno-Fullstack\techno-backend\src\main\java\com\techno\backend\config\SchemaMigration.java"

Write-Host "Fixing SchemaMigration.java SQL statements..." -ForegroundColor Green

$content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))

# Fix employee INSERT statements
$content = $content -replace 'employee_ar_name, employee_en_name, national_id', 'employee_name, national_id'
$content = $content -replace 'VALUES \(\?, \?, \?, \?, \?, \?, \?, \?, \?, \?, \?, CURRENT_TIMESTAMP', 'VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP'

# Remove the extra parameter (English name) from the VALUES
$content = $content -replace '"\u0645\u062f\u064a\u0631 \u0627\u0644\u0646\u0638\u0627\u0645", "System Administrator", "1000000000"', '"\u0645\u062f\u064a\u0631 \u0627\u0644\u0646\u0638\u0627\u0645", "1000000000"'
$content = $content -replace '"\u0645\u062f\u064a\u0631 \u0627\u0644\u0646\u0638\u0627\u0645 \u0627\u0644\u0641\u0627\u0626\u0642", "Super System Administrator", "1000000001"', '"\u0645\u062f\u064a\u0631 \u0627\u0644\u0646\u0638\u0627\u0645 \u0627\u0644\u0641\u0627\u0626\u0642", "1000000001"'

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($file, $content, $utf8NoBom)

Write-Host "Fixed SchemaMigration.java" -ForegroundColor Green

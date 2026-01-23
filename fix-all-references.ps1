# Enhanced PowerShell script to fix ALL remaining language field references
# This includes DTOs, repositories, queries, and any missed references

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src"

Write-Host "Fixing ALL remaining language field references..." -ForegroundColor Green

# Define comprehensive replacements
$replacements = @{
    # Field declarations in DTOs and classes
    'private String employeeEnName;' = 'private String employeeName;';
    'private String employeeArName;' = 'private String employeeName;';
    'private String projectEnName;' = 'private String projectName;';
    'private String projectArName;' = 'private String projectName;';
    'private String deptEnName;' = 'private String deptName;';
    'private String deptArName;' = 'private String deptName;';
    'private String itemEnName;' = 'private String itemName;';
    'private String itemArName;' = 'private String itemName;';
    'private String storeEnName;' = 'private String storeName;';
    'private String storeArName;' = 'private String storeName;';
    'private String categoryEnName;' = 'private String categoryName;';
    'private String categoryArName;' = 'private String categoryName;';
    'private String typeEnName;' = 'private String typeName;';
    'private String typeArName;' = 'private String typeName;';
    'private String menuEnName;' = 'private String menuName;';
    'private String menuArName;' = 'private String menuName;';
    'private String supplierArName;' = 'private String supplierName;';
    'private String holidayNameAr;' = 'private String holidayName;';
    'private String holidayNameEn;' = 'private String holidayName;';
    'private String dayNameAr;' = 'private String dayName;';
    'private String dayNameEn;' = 'private String dayName;';
    
    # JPQL/HQL query strings in repositories
    '"employeeArName"' = '"employeeName"';
    '"employeeEnName"' = '"employeeName"';
    'e\.employeeArName' = 'e.employeeName';
    'e\.employeeEnName' = 'e.employeeName';
    '"projectArName"' = '"projectName"';
    '"projectEnName"' = '"projectName"';
    'p\.projectArName' = 'p.projectName';
    'p\.projectEnName' = 'p.projectName';
    '"deptArName"' = '"deptName"';
    '"deptEnName"' = '"deptName"';
    'd\.deptArName' = 'd.deptName';
    'd\.deptEnName' = 'd.deptName';
    '"itemArName"' = '"itemName"';
    '"itemEnName"' = '"itemName"';
    'i\.itemArName' = 'i.itemName';
    'i\.itemEnName' = 'i.itemName';
    '"storeArName"' = '"storeName"';
    '"storeEnName"' = '"storeName"';
    's\.storeArName' = 's.storeName';
    's\.storeEnName' = 's.storeName';
    
    # Criteria API field references
    'root\.get\("employeeArName"\)' = 'root.get("employeeName")';
    'root\.get\("employeeEnName"\)' = 'root.get("employeeName")';
    'root\.get\("projectArName"\)' = 'root.get("projectName")';
    'root\.get\("projectEnName"\)' = 'root.get("projectName")';
    'root\.get\("deptArName"\)' = 'root.get("deptName")';
    'root\.get\("deptEnName"\)' = 'root.get("deptName")';
    'root\.get\("itemArName"\)' = 'root.get("itemName")';
    'root\.get\("itemEnName"\)' = 'root.get("itemName")';
    
    # Method references that might have been missed
    '\.employeeArName' = '.employeeName';
    '\.employeeEnName' = '.employeeName';
    '\.projectArName' = '.projectName';
    '\.projectEnName' = '.projectName';
    '\.deptArName' = '.deptName';
    '\.deptEnName' = '.deptName';
    '\.itemArName' = '.itemName';
    '\.itemEnName' = '.itemName';
    '\.storeArName' = '.storeName';
    '\.storeEnName' = '.storeName';
    '\.categoryArName' = '.categoryName';
    '\.categoryEnName' = '.categoryName';
    '\.typeArName' = '.typeName';
    '\.typeEnName' = '.typeName';
    '\.menuArName' = '.menuName';
    '\.menuEnName' = '.menuName';
    '\.supplierArName' = '.supplierName';
    '\.holidayNameAr' = '.holidayName';
    '\.holidayNameEn' = '.holidayName';
    '\.dayNameAr' = '.dayName';
    '\.dayNameEn' = '.dayName';
    
    # Conditional expressions
    'projectArName != null \|\| projectEnName != null' = 'projectName != null';
}

# Get all Java files
$javaFiles = Get-ChildItem -Path $backendPath -Filter "*.java" -Recurse

$totalFiles = $javaFiles.Count
$processedFiles = 0
$modifiedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    Write-Progress -Activity "Processing Java files" -Status "$processedFiles of $totalFiles" -PercentComplete (($processedFiles / $totalFiles) * 100)
    
    # Read as UTF8 without BOM
    $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.UTF8Encoding]::new($false))
    $originalContent = $content
    
    # Apply all replacements
    foreach ($pattern in $replacements.Keys) {
        $replacement = $replacements[$pattern]
        $content = $content -replace $pattern, $replacement
    }
    
    # Only write if content changed
    if ($content -ne $originalContent) {
        # Write as UTF8 without BOM
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        $modifiedFiles++
        Write-Host "Modified: $($file.FullName)" -ForegroundColor Yellow
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green
Write-Host "Total files processed: $totalFiles" -ForegroundColor Cyan
Write-Host "Files modified: $modifiedFiles" -ForegroundColor Cyan

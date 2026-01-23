# PowerShell script to fix all dual-language getter/setter references
# This script replaces old method calls with new single-name equivalents

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src"

Write-Host "Fixing language field references in Java files..." -ForegroundColor Green

# Define replacements: old pattern -> new pattern
$replacements = @{
    # Employee
    'getEmployeeEnName\(\)' = 'getEmployeeName()';
    'getEmployeeArName\(\)' = 'getEmployeeName()';
    'setEmployeeEnName\(' = 'setEmployeeName(';
    'setEmployeeArName\(' = 'setEmployeeName(';
    'employeeEnName\(' = 'employeeName(';
    'employeeArName\(' = 'employeeName(';
    'Employee::getEmployeeEnName' = 'Employee::getEmployeeName';
    'Employee::getEmployeeArName' = 'Employee::getEmployeeName';
    
    # Project
    'getProjectEnName\(\)' = 'getProjectName()';
    'getProjectArName\(\)' = 'getProjectName()';
    'setProjectEnName\(' = 'setProjectName(';
    'setProjectArName\(' = 'setProjectName(';
    'projectEnName\(' = 'projectName(';
    'projectArName\(' = 'projectName(';
    
    # Department
    'getDeptEnName\(\)' = 'getDeptName()';
    'getDeptArName\(\)' = 'getDeptName()';
    'setDeptEnName\(' = 'setDeptName(';
    'setDeptArName\(' = 'setDeptName(';
    'deptEnName\(' = 'deptName(';
    'deptArName\(' = 'deptName(';
    
    # StoreItem
    'getItemEnName\(\)' = 'getItemName()';
    'getItemArName\(\)' = 'getItemName()';
    'setItemEnName\(' = 'setItemName(';
    'setItemArName\(' = 'setItemName(';
    'itemEnName\(' = 'itemName(';
    'itemArName\(' = 'itemName(';
    
    # ProjectStore
    'getStoreEnName\(\)' = 'getStoreName()';
    'getStoreArName\(\)' = 'getStoreName()';
    'setStoreEnName\(' = 'setStoreName(';
    'setStoreArName\(' = 'setStoreName(';
    'storeEnName\(' = 'storeName(';
    'storeArName\(' = 'storeName(';
    
    # ItemCategory
    'getCategoryEnName\(\)' = 'getCategoryName()';
    'getCategoryArName\(\)' = 'getCategoryName()';
    'setCategoryEnName\(' = 'setCategoryName(';
    'setCategoryArName\(' = 'setCategoryName(';
    'categoryEnName\(' = 'categoryName(';
    'categoryArName\(' = 'categoryName(';
    
    # TransactionType
    'getTypeEnName\(\)' = 'getTypeName()';
    'getTypeArName\(\)' = 'getTypeName()';
    'setTypeEnName\(' = 'setTypeName(';
    'setTypeArName\(' = 'setTypeName(';
    'typeEnName\(' = 'typeName(';
    'typeArName\(' = 'typeName(';
    
    # ContractType (same as TransactionType)
    
    # MenuFile
    'getMenuEnName\(\)' = 'getMenuName()';
    'getMenuArName\(\)' = 'getMenuName()';
    'setMenuEnName\(' = 'setMenuName(';
    'setMenuArName\(' = 'setMenuName(';
    'menuEnName\(' = 'menuName(';
    'menuArName\(' = 'menuName(';
    
    # Supplier
    'getSupplierArName\(\)' = 'getSupplierName()';
    'setSupplierArName\(' = 'setSupplierName(';
    'supplierArName\(' = 'supplierName(';
    
    # Holiday
    'getHolidayNameAr\(\)' = 'getHolidayName()';
    'getHolidayNameEn\(\)' = 'getHolidayName()';
    'setHolidayNameAr\(' = 'setHolidayName(';
    'setHolidayNameEn\(' = 'setHolidayName(';
    'holidayNameAr\(' = 'holidayName(';
    'holidayNameEn\(' = 'holidayName(';
    
    # WeekendDay
    'getDayNameAr\(\)' = 'getDayName()';
    'getDayNameEn\(\)' = 'getDayName()';
    'setDayNameAr\(' = 'setDayName(';
    'setDayNameEn\(' = 'setDayName(';
    'dayNameAr\(' = 'dayName(';
    'dayNameEn\(' = 'dayName(';
}

# Get all Java files
$javaFiles = Get-ChildItem -Path $backendPath -Filter "*.java" -Recurse

$totalFiles = $javaFiles.Count
$processedFiles = 0
$modifiedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    Write-Progress -Activity "Processing Java files" -Status "$processedFiles of $totalFiles" -PercentComplete (($processedFiles / $totalFiles) * 100)
    
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Apply all replacements
    foreach ($pattern in $replacements.Keys) {
        $replacement = $replacements[$pattern]
        $content = $content -replace $pattern, $replacement
    }
    
    # Only write if content changed
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $modifiedFiles++
        Write-Host "Modified: $($file.FullName)" -ForegroundColor Yellow
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green
Write-Host "Total files processed: $totalFiles" -ForegroundColor Cyan
Write-Host "Files modified: $modifiedFiles" -ForegroundColor Cyan

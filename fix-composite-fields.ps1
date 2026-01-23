# Final comprehensive fix for composite field names and helper methods

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src"

Write-Host "Fixing composite field names and helper methods..." -ForegroundColor Green

$replacements = @{
    # Composite field names - from/to prefixes
    'private String fromStoreArName;' = 'private String fromStoreName;';
    'private String fromStoreEnName;' = 'private String fromStoreName;';
    'private String toStoreArName;' = 'private String toStoreName;';
    'private String toStoreEnName;' = 'private String toStoreName;';
    'private String fromProjectArName;' = 'private String fromProjectName;';
    'private String fromProjectEnName;' = 'private String fromProjectName;';
    'private String toProjectArName;' = 'private String toProjectName;';
    'private String toProjectEnName;' = 'private String toProjectName;';
    
    # Manager/Department/Project composite names
    'private String projectManagerArName;' = 'private String projectManagerName;';
    'private String projectManagerEnName;' = 'private String projectManagerName;';
    'private String primaryDeptArName;' = 'private String primaryDeptName;';
    'private String primaryDeptEnName;' = 'private String primaryDeptName;';
    'private String primaryProjectArName;' = 'private String primaryProjectName;';
    'private String primaryProjectEnName;' = 'private String primaryProjectName;';
    
    # Transaction type composite names
    'private String transTypeArName;' = 'private String transTypeName;';
    'private String transTypeEnName;' = 'private String transTypeName;';
    
    # Helper method references in conditional expressions
    'storeArName : storeEnName' = 'storeName';
    'projectArName : projectEnName' = 'projectName';
    'itemArName : itemEnName' = 'itemName';
    'categoryArName : categoryEnName' = 'categoryName';
    
    # Getter/setter references
    '\.fromStoreArName' = '.fromStoreName';
    '\.fromStoreEnName' = '.fromStoreName';
    '\.toStoreArName' = '.toStoreName';
    '\.toStoreEnName' = '.toStoreName';
    '\.fromProjectArName' = '.fromProjectName';
    '\.fromProjectEnName' = '.fromProjectName';
    '\.toProjectArName' = '.toProjectName';
    '\.toProjectEnName' = '.toProjectName';
    '\.projectManagerArName' = '.projectManagerName';
    '\.projectManagerEnName' = '.projectManagerName';
    '\.primaryDeptArName' = '.primaryDeptName';
    '\.primaryDeptEnName' = '.primaryDeptName';
    '\.primaryProjectArName' = '.primaryProjectName';
    '\.primaryProjectEnName' = '.primaryProjectName';
    '\.transTypeArName' = '.transTypeName';
    '\.transTypeEnName' = '.transTypeName';
    
    # Method calls
    'getFromStoreArName\(\)' = 'getFromStoreName()';
    'getFromStoreEnName\(\)' = 'getFromStoreName()';
    'getToStoreArName\(\)' = 'getToStoreName()';
    'getToStoreEnName\(\)' = 'getToStoreName()';
    'getFromProjectArName\(\)' = 'getFromProjectName()';
    'getFromProjectEnName\(\)' = 'getFromProjectName()';
    'getToProjectArName\(\)' = 'getToProjectName()';
    'getToProjectEnName\(\)' = 'getToProjectName()';
    'getProjectManagerArName\(\)' = 'getProjectManagerName()';
    'getProjectManagerEnName\(\)' = 'getProjectManagerName()';
    'getPrimaryDeptArName\(\)' = 'getPrimaryDeptName()';
    'getPrimaryDeptEnName\(\)' = 'getPrimaryDeptName()';
    'getPrimaryProjectArName\(\)' = 'getPrimaryProjectName()';
    'getPrimaryProjectEnName\(\)' = 'getPrimaryProjectName()';
    'getTransTypeArName\(\)' = 'getTransTypeName()';
    'getTransTypeEnName\(\)' = 'getTransTypeName()';
    
    # Setters
    'setFromStoreArName\(' = 'setFromStoreName(';
    'setFromStoreEnName\(' = 'setFromStoreName(';
    'setToStoreArName\(' = 'setToStoreName(';
    'setToStoreEnName\(' = 'setToStoreName(';
    'setFromProjectArName\(' = 'setFromProjectName(';
    'setFromProjectEnName\(' = 'setFromProjectName(';
    'setToProjectArName\(' = 'setToProjectName(';
    'setToProjectEnName\(' = 'setToProjectName(';
    'setProjectManagerArName\(' = 'setProjectManagerName(';
    'setProjectManagerEnName\(' = 'setProjectManagerName(';
    'setPrimaryDeptArName\(' = 'setPrimaryDeptName(';
    'setPrimaryDeptEnName\(' = 'setPrimaryDeptName(';
    'setPrimaryProjectArName\(' = 'setPrimaryProjectName(';
    'setPrimaryProjectEnName\(' = 'setPrimaryProjectName(';
    'setTransTypeArName\(' = 'setTransTypeName(';
    'setTransTypeEnName\(' = 'setTransTypeName(';
}

$javaFiles = Get-ChildItem -Path $backendPath -Filter "*.java" -Recurse
$totalFiles = $javaFiles.Count
$processedFiles = 0
$modifiedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    Write-Progress -Activity "Processing Java files" -Status "$processedFiles of $totalFiles" -PercentComplete (($processedFiles / $totalFiles) * 100)
    
    $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.UTF8Encoding]::new($false))
    $originalContent = $content
    
    foreach ($pattern in $replacements.Keys) {
        $replacement = $replacements[$pattern]
        $content = $content -replace $pattern, $replacement
    }
    
    if ($content -ne $originalContent) {
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        $modifiedFiles++
        Write-Host "Modified: $($file.FullName)" -ForegroundColor Yellow
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green
Write-Host "Total files processed: $totalFiles" -ForegroundColor Cyan
Write-Host "Files modified: $modifiedFiles" -ForegroundColor Cyan

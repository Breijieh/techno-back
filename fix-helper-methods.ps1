# Script to fix broken helper methods with incomplete ternary operators

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src"

Write-Host "Fixing broken helper methods..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path $backendPath -Filter "*.java" -Recurse
$totalFiles = $javaFiles.Count
$processedFiles = 0
$modifiedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    Write-Progress -Activity "Processing Java files" -Status "$processedFiles of $totalFiles" -PercentComplete (($processedFiles / $totalFiles) * 100)
    
    $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.UTF8Encoding]::new($false))
    $originalContent = $content
    
    # Fix broken ternary operators - these were created when we replaced the field names
    # Pattern: return "ar".equalsIgnoreCase(language) ? fieldName;
    # Should be: return fieldName;
    
    $content = $content -replace 'return\s+"ar"\.equalsIgnoreCase\(language\)\s+\?\s+(\w+);', 'return $1;'
    
    # Also remove the language parameter from these methods since we don't need it anymore
    $content = $content -replace 'public\s+String\s+get(\w+)\(String\s+language\)\s+\{', 'public String get$1() {'
    
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

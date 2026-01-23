# Script to remove duplicate field declarations caused by the replacement

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src"

Write-Host "Removing duplicate field declarations..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path $backendPath -Filter "*.java" -Recurse
$totalFiles = $javaFiles.Count
$processedFiles = 0
$modifiedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    Write-Progress -Activity "Processing Java files" -Status "$processedFiles of $totalFiles" -PercentComplete (($processedFiles / $totalFiles) * 100)
    
    $lines = [System.IO.File]::ReadAllLines($file.FullName, [System.Text.UTF8Encoding]::new($false))
    $newLines = New-Object System.Collections.ArrayList
    $seenFields = @{}
    $modified = $false
    
    foreach ($line in $lines) {
        # Check if this is a private field declaration
        if ($line -match '^\s*private\s+\w+\s+(\w+);') {
            $fieldName = $matches[1]
            
            # If we've already seen this field in this file, skip it (it's a duplicate)
            if ($seenFields.ContainsKey($fieldName)) {
                Write-Host "Removing duplicate field '$fieldName' in: $($file.FullName)" -ForegroundColor Yellow
                $modified = $true
                continue
            }
            
            $seenFields[$fieldName] = $true
        }
        
        [void]$newLines.Add($line)
    }
    
    if ($modified) {
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllLines($file.FullName, $newLines, $utf8NoBom)
        $modifiedFiles++
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green
Write-Host "Total files processed: $totalFiles" -ForegroundColor Cyan
Write-Host "Files with duplicates removed: $modifiedFiles" -ForegroundColor Cyan

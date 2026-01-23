# PowerShell script to fix BOM (Byte Order Mark) issues in Java files
# The previous script used UTF8 encoding which added BOM characters

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src"

Write-Host "Fixing BOM encoding issues in Java files..." -ForegroundColor Green

# Get all Java files
$javaFiles = Get-ChildItem -Path $backendPath -Filter "*.java" -Recurse

$totalFiles = $javaFiles.Count
$processedFiles = 0
$fixedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    Write-Progress -Activity "Processing Java files" -Status "$processedFiles of $totalFiles" -PercentComplete (($processedFiles / $totalFiles) * 100)
    
    # Read file as bytes
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    
    # Check if file starts with UTF-8 BOM (EF BB BF)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Host "Removing BOM from: $($file.FullName)" -ForegroundColor Yellow
        
        # Remove BOM (skip first 3 bytes)
        $newBytes = $bytes[3..($bytes.Length - 1)]
        
        # Write back without BOM
        [System.IO.File]::WriteAllBytes($file.FullName, $newBytes)
        $fixedFiles++
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green
Write-Host "Total files processed: $totalFiles" -ForegroundColor Cyan
Write-Host "Files fixed: $fixedFiles" -ForegroundColor Cyan

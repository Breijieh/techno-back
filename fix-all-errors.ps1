# Comprehensive fix for all remaining compilation errors

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src\main\java"

Write-Host "Fixing all remaining compilation errors..." -ForegroundColor Green

# 1. Fix duplicate repository methods
Write-Host "`n1. Fixing duplicate repository methods..." -ForegroundColor Cyan

$storeItemRepo = "$backendPath\com\techno\backend\repository\StoreItemRepository.java"
$content = [System.IO.File]::ReadAllText($storeItemRepo, [System.Text.UTF8Encoding]::new($false))
# Remove duplicate existsByitemName method
$content = $content -replace '(?m)^\s*boolean existsByitemName\(String itemName\);\s*$\r?\n', ''
[System.IO.File]::WriteAllText($storeItemRepo, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "Fixed StoreItemRepository" -ForegroundColor Yellow

$itemCategoryRepo = "$backendPath\com\techno\backend\repository\ItemCategoryRepository.java"
$content = [System.IO.File]::ReadAllText($itemCategoryRepo, [System.Text.UTF8Encoding]::new($false))
# Remove duplicate existsBycategoryName method
$content = $content -replace '(?m)^\s*boolean existsBycategoryName\(String categoryName\);\s*$\r?\n', ''
[System.IO.File]::WriteAllText($itemCategoryRepo, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "Fixed ItemCategoryRepository" -ForegroundColor Yellow

# 2. Add explicit logger fields to all classes that use log but don't have @Slf4j working
Write-Host "`n2. Adding explicit logger fields..." -ForegroundColor Cyan

$classesNeedingLogger = @(
    "$backendPath\com\techno\backend\config\AsyncConfig.java",
    "$backendPath\com\techno\backend\config\DatabaseInitializer.java",
    "$backendPath\com\techno\backend\config\SchemaMigration.java",
    "$backendPath\com\techno\backend\security\JwtAuthenticationFilter.java",
    "$backendPath\com\techno\backend\security\JwtAuthenticationEntryPoint.java",
    "$backendPath\com\techno\backend\security\JwtTokenProvider.java",
    "$backendPath\com\techno\backend\controller\AllowanceController.java",
    "$backendPath\com\techno\backend\service\AllowanceService.java"
)

foreach ($classFile in $classesNeedingLogger) {
    if (Test-Path $classFile) {
        $content = [System.IO.File]::ReadAllText($classFile, [System.Text.UTF8Encoding]::new($false))
        
        # Check if already has logger field
        if ($content -notmatch 'private static final.*Logger.*log\s*=') {
            # Add logger import if not present
            if ($content -notmatch 'import org\.slf4j\.Logger;') {
                $content = $content -replace '(package [^;]+;)', "`$1`r`nimport org.slf4j.Logger;`r`nimport org.slf4j.LoggerFactory;"
            }
            
            # Add logger field after class declaration
            $className = [System.IO.Path]::GetFileNameWithoutExtension($classFile)
            $content = $content -replace "(@\w+\s*)*\s*public\s+class\s+$className", "`$0`r`n`r`n    private static final Logger log = LoggerFactory.getLogger($className.class);"
            
            [System.IO.File]::WriteAllText($classFile, $content, [System.Text.UTF8Encoding]::new($false))
            Write-Host "Added logger to $className" -ForegroundColor Yellow
        }
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green

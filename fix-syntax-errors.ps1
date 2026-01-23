# Fix syntax errors in all files with logger declarations

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src\main\java"

Write-Host "Fixing syntax errors in logger declarations..." -ForegroundColor Green

$filesToFix = @(
    "$backendPath\com\techno\backend\config\AsyncConfig.java",
    "$backendPath\com\techno\backend\config\DatabaseInitializer.java",
    "$backendPath\com\techno\backend\config\SchemaMigration.java",
    "$backendPath\com\techno\backend\security\JwtAuthenticationFilter.java",
    "$backendPath\com\techno\backend\security\JwtAuthenticationEntryPoint.java",
    "$backendPath\com\techno\backend\security\JwtTokenProvider.java",
    "$backendPath\com\techno\backend\service\AllowanceService.java"
)

foreach ($file in $filesToFix) {
    if (Test-Path $file) {
        $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
        
        # Fix pattern: "public class ClassName\r\n\r\n    private static final Logger log = LoggerFactory.getLogger(ClassName.class); {"
        # Should be: "public class ClassName {\r\n\r\n    private static final Logger log = LoggerFactory.getLogger(ClassName.class);"
        
        $content = $content -replace '(public\s+class\s+\w+)\s*\r?\n\s*private\s+static\s+final\s+Logger\s+log\s*=\s*LoggerFactory\.getLogger\((\w+)\.class\);\s*\{', '$1 {' + "`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(`$2.class);"
        
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
        Write-Host "Fixed: $file" -ForegroundColor Yellow
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green

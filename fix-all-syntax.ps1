# Final comprehensive fix for all syntax errors

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src\main\java"

Write-Host "Fixing all remaining syntax errors..." -ForegroundColor Green

# Files to fix
$fixes = @{
    "$backendPath\com\techno\backend\config\DatabaseInitializer.java" = @{
        Pattern = 'public class DatabaseInitializer\s*\r?\n\s*private static final Logger log = LoggerFactory\.getLogger\(DatabaseInitializer\.class\);\s*\{'
        Replacement = "public class DatabaseInitializer {`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);"
    }
    "$backendPath\com\techno\backend\config\SchemaMigration.java" = @{
        Pattern = 'public class SchemaMigration\s*\r?\n\s*private static final Logger log = LoggerFactory\.getLogger\(SchemaMigration\.class\);\s*(implements CommandLineRunner)?\s*\{'
        Replacement = "public class SchemaMigration implements CommandLineRunner {`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);"
    }
    "$backendPath\com\techno\backend\security\JwtAuthenticationFilter.java" = @{
        Pattern = 'public class JwtAuthenticationFilter\s*\r?\n\s*private static final Logger log = LoggerFactory\.getLogger\(JwtAuthenticationFilter\.class\);\s*(extends OncePerRequestFilter)?\s*\{'
        Replacement = "public class JwtAuthenticationFilter extends OncePerRequestFilter {`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);"
    }
    "$backendPath\com\techno\backend\security\JwtAuthenticationEntryPoint.java" = @{
        Pattern = 'public class JwtAuthenticationEntryPoint\s*\r?\n\s*private static final Logger log = LoggerFactory\.getLogger\(JwtAuthenticationEntryPoint\.class\);\s*(implements AuthenticationEntryPoint)?\s*\{'
        Replacement = "public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);"
    }
    "$backendPath\com\techno\backend\security\JwtTokenProvider.java" = @{
        Pattern = 'public class JwtTokenProvider\s*\r?\n\s*private static final Logger log = LoggerFactory\.getLogger\(JwtTokenProvider\.class\);\s*\{'
        Replacement = "public class JwtTokenProvider {`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);"
    }
    "$backendPath\com\techno\backend\service\AllowanceService.java" = @{
        Pattern = 'public class AllowanceService\s*\r?\n\s*private static final Logger log = LoggerFactory\.getLogger\(AllowanceService\.class\);\s*\{'
        Replacement = "public class AllowanceService {`r`n`r`n    private static final Logger log = LoggerFactory.getLogger(AllowanceService.class);"
    }
}

foreach ($file in $fixes.Keys) {
    if (Test-Path $file) {
        $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
        $fix = $fixes[$file]
        
        $content = $content -replace $fix.Pattern, $fix.Replacement
        
        # Also remove @Slf4j annotation if present (causes duplicate log field)
        $content = $content -replace '@Slf4j\s*\r?\n', ''
        
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
        Write-Host "Fixed: $file" -ForegroundColor Yellow
    }
}

# Also remove @Slf4j from AsyncConfig and AllowanceController
$additionalFiles = @(
    "$backendPath\com\techno\backend\config\AsyncConfig.java",
    "$backendPath\com\techno\backend\controller\AllowanceController.java"
)

foreach ($file in $additionalFiles) {
    if (Test-Path $file) {
        $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
        $content = $content -replace '@Slf4j\s*\r?\n', ''
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
        Write-Host "Removed @Slf4j from: $file" -ForegroundColor Yellow
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green

# Ultimate fix - Generate all missing Lombok methods manually

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src\main\java"

Write-Host "Generating all missing Lombok methods..." -ForegroundColor Green

# Helper function to add methods before the last closing brace
function Add-MethodsToClass {
    param(
        [string]$FilePath,
        [string[]]$Methods
    )
    
    $content = [System.IO.File]::ReadAllText($FilePath, [System.Text.UTF8Encoding]::new($false))
    
    # Find the last closing brace
    $lastBraceIndex = $content.LastIndexOf('}')
    
    if ($lastBraceIndex -gt 0) {
        $methodsText = "`r`n    // Auto-generated Lombok methods`r`n" + ($Methods -join "`r`n") + "`r`n"
        $content = $content.Substring(0, $lastBraceIndex) + $methodsText + $content.Substring($lastBraceIndex)
        
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($FilePath, $content, $utf8NoBom)
        return $true
    }
    return $false
}

# 1. Fix UserAccount entity
Write-Host "`n1. Adding methods to UserAccount..." -ForegroundColor Cyan
$userAccountMethods = @(
    "    public Long getUserId() { return userId; }",
    "    public String getUsername() { return username; }",
    "    public UserType getUserType() { return userType; }",
    "    public Long getEmployeeNo() { return employeeNo; }"
)
Add-MethodsToClass "$backendPath\com\techno\backend\entity\UserAccount.java" $userAccountMethods
Write-Host "Added methods to UserAccount" -ForegroundColor Yellow

# 2. Fix EmpMonthlyAllowance entity
Write-Host "`n2. Adding methods to EmpMonthlyAllowance..." -ForegroundColor Cyan
$allowanceMethods = @(
    "    public Long getTransactionNo() { return transactionNo; }",
    "    public Long getEmployeeNo() { return employeeNo; }",
    "    public Employee getEmployee() { return employee; }",
    "    public Long getTypeCode() { return typeCode; }",
    "    public TransactionType getTransactionType() { return transactionType; }",
    "    public java.time.LocalDate getTransactionDate() { return transactionDate; }",
    "    public java.math.BigDecimal getAllowanceAmount() { return allowanceAmount; }",
    "    public String getEntryReason() { return entryReason; }",
    "    public String getTransStatus() { return transStatus; }",
    "    public String getIsManualEntry() { return isManualEntry; }",
    "    public java.time.LocalDateTime getApprovedDate() { return approvedDate; }",
    "    public Long getApprovedBy() { return approvedBy; }",
    "    public String getRejectionReason() { return rejectionReason; }",
    "    public Long getNextApproval() { return nextApproval; }",
    "    public Integer getNextAppLevel() { return nextAppLevel; }",
    "    public String getIsDeleted() { return isDeleted; }"
)
Add-MethodsToClass "$backendPath\com\techno\backend\entity\EmpMonthlyAllowance.java" $allowanceMethods
Write-Host "Added methods to EmpMonthlyAllowance" -ForegroundColor Yellow

# 3. Fix Employee entity
Write-Host "`n3. Adding methods to Employee..." -ForegroundColor Cyan
$employeeMethods = @(
    "    public Long getPrimaryDeptCode() { return primaryDeptCode; }",
    "    public Long getPrimaryProjectCode() { return primaryProjectCode; }"
)
Add-MethodsToClass "$backendPath\com\techno\backend\entity\Employee.java" $employeeMethods
Write-Host "Added methods to Employee" -ForegroundColor Yellow

# 4. Fix TransactionType entity
Write-Host "`n4. Adding methods to TransactionType..." -ForegroundColor Cyan
$transactionTypeMethods = @(
    "    public String getAllowanceDeduction() { return allowanceDeduction; }"
)
Add-MethodsToClass "$backendPath\com\techno\backend\entity\TransactionType.java" $transactionTypeMethods
Write-Host "Added methods to TransactionType" -ForegroundColor Yellow

# 5. Fix ApprovalInfo class
Write-Host "`n5. Adding methods to ApprovalInfo..." -ForegroundColor Cyan
$approvalInfoFile = Get-ChildItem -Path "$backendPath\com\techno\backend\service" -Filter "ApprovalWorkflowService.java" -Recurse | Select-Object -First 1

if ($approvalInfoFile) {
    $content = [System.IO.File]::ReadAllText($approvalInfoFile.FullName, [System.Text.UTF8Encoding]::new($false))
    
    # Check if ApprovalInfo already has getters
    if ($content -notmatch 'public String getTransStatus\(\)') {
        # Find ApprovalInfo class and add getters
        $approvalInfoMethods = @(
            "        public String getTransStatus() { return transStatus; }",
            "        public Long getNextApproval() { return nextApproval; }",
            "        public Integer getNextAppLevel() { return nextAppLevel; }"
        )
        
        # This is a nested class, so we need special handling
        $pattern = '(public\s+static\s+class\s+ApprovalInfo\s*\{[^\}]*)'
        if ($content -match $pattern) {
            $methodsText = "`r`n" + ($approvalInfoMethods -join "`r`n") + "`r`n    "
            $content = $content -replace '(\}\s*//\s*End\s+of\s+ApprovalInfo)', "$methodsText`$1"
            
            $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
            [System.IO.File]::WriteAllText($approvalInfoFile.FullName, $content, $utf8NoBom)
            Write-Host "Added methods to ApprovalInfo" -ForegroundColor Yellow
        }
    }
}

Write-Host "`nCompleted!" -ForegroundColor Green

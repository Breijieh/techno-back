# Add missing setters to entities

$backendPath = "c:\Projects\Techno-Fullstack\techno-backend\src\main\java"

Write-Host "Adding missing setters..." -ForegroundColor Green

# Helper function to append content before the last brace
function Append-Methods {
    param(
        [string]$FilePath,
        [string]$Content
    )
    
    $fileContent = [System.IO.File]::ReadAllText($FilePath, [System.Text.UTF8Encoding]::new($false))
    $lastBrace = $fileContent.LastIndexOf('}')
    
    if ($lastBrace -gt 0) {
        $newContent = $fileContent.Substring(0, $lastBrace) + "`r`n" + $Content + "`r`n" + $fileContent.Substring($lastBrace)
        [System.IO.File]::WriteAllText($FilePath, $newContent, [System.Text.UTF8Encoding]::new($false))
        return $true
    }
    return $false
}

# 1. Add setters to EmpMonthlyAllowance
$allowanceSetters = @"
    // Auto-generated Setters
    public void setTransactionNo(Long transactionNo) { this.transactionNo = transactionNo; }
    public void setEmployeeNo(Long employeeNo) { this.employeeNo = employeeNo; }
    public void setTypeCode(Long typeCode) { this.typeCode = typeCode; }
    public void setTransactionDate(java.time.LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setAllowanceAmount(java.math.BigDecimal allowanceAmount) { this.allowanceAmount = allowanceAmount; }
    public void setEntryReason(String entryReason) { this.entryReason = entryReason; }
    public void setTransStatus(String transStatus) { this.transStatus = transStatus; }
    public void setIsManualEntry(String isManualEntry) { this.isManualEntry = isManualEntry; }
    public void setApprovedDate(java.time.LocalDateTime approvedDate) { this.approvedDate = approvedDate; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setNextApproval(Long nextApproval) { this.nextApproval = nextApproval; }
    public void setNextAppLevel(Integer nextAppLevel) { this.nextAppLevel = nextAppLevel; }
    public void setIsDeleted(String isDeleted) { this.isDeleted = isDeleted; }
    public void setOvertimeHours(java.math.BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public void setPeriodicalAllowance(String periodicalAllowance) { this.periodicalAllowance = periodicalAllowance; }
    public void setAllowanceStartDate(java.time.LocalDate allowanceStartDate) { this.allowanceStartDate = allowanceStartDate; }
    public void setAllowanceEndDate(java.time.LocalDate allowanceEndDate) { this.allowanceEndDate = allowanceEndDate; }
"@

Append-Methods "$backendPath\com\techno\backend\entity\EmpMonthlyAllowance.java" $allowanceSetters
Write-Host "Added setters to EmpMonthlyAllowance" -ForegroundColor Yellow

# 2. Add setters to UserAccount (just in case)
$userAccountSetters = @"
    // Auto-generated Setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public void setIsActive(Character isActive) { this.isActive = isActive; }
    public void setEmployeeNo(Long employeeNo) { this.employeeNo = employeeNo; }
    public void setLastLoginDate(java.time.LocalDate lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    public void setLastLoginTime(java.time.LocalTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
"@

Append-Methods "$backendPath\com\techno\backend\entity\UserAccount.java" $userAccountSetters
Write-Host "Added setters to UserAccount" -ForegroundColor Yellow

# 3. Add setters to Employee (just in case)
$employeeSetters = @"
    // Auto-generated Setters
    public void setPrimaryDeptCode(Long primaryDeptCode) { this.primaryDeptCode = primaryDeptCode; }
    public void setPrimaryProjectCode(Long primaryProjectCode) { this.primaryProjectCode = primaryProjectCode; }
"@

Append-Methods "$backendPath\com\techno\backend\entity\Employee.java" $employeeSetters
Write-Host "Added setters to Employee" -ForegroundColor Yellow

Write-Host "Completed!" -ForegroundColor Green

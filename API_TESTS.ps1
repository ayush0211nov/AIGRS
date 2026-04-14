# AIGRS Backend - Automated API Testing Script
# This script performs comprehensive REST API testing with curl
# Tests all major workflows: Auth, Grievances, Files, Search, Admin

param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [int]$TimeoutSeconds = 300
)

# Color coding for output
$Green = "`e[32m"
$Red = "`e[31m"
$Yellow = "`e[33m"
$Blue = "`e[34m"
$Reset = "`e[0m"

# Counters
$TotalTests = 0
$PassedTests = 0
$FailedTests = 0
$Results = @()

function Log-Test {
    param([string]$Name, [bool]$Passed, [string]$Response)
    $TotalTests++
    if ($Passed) {
        $PassedTests++
        Write-Host "$Green✓ PASS$Reset $Name" -ForegroundColor Green
        $Results += [PSCustomObject]@{
            Test = $Name
            Status = "PASS"
            Response = $Response.Substring(0, [Math]::Min(100, $Response.Length))
        }
    } else {
        $FailedTests++
        Write-Host "$Red✗ FAIL$Reset $Name" -ForegroundColor Red
        $Results += [PSCustomObject]@{
            Test = $Name
            Status = "FAIL"
            Response = $Response.Substring(0, [Math]::Min(100, $Response.Length))
        }
    }
}

function Invoke-API {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Body = $null,
        [string]$Token = $null,
        [string]$ContentType = "application/json"
    )
    
    $Uri = "$BaseUrl$Endpoint"
    $Headers = @{
        "Content-Type" = $ContentType
    }
    
    if ($Token) {
        $Headers["Authorization"] = "Bearer $Token"
    }
    
    try {
        if ($Body) {
            $Response = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -Body $Body -TimeoutSec $TimeoutSeconds -ErrorAction Stop
        } else {
            $Response = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -TimeoutSec $TimeoutSeconds -ErrorAction Stop
        }
        return @{
            Success = $true
            StatusCode = $Response.StatusCode
            Content = $Response.Content
        }
    } catch {
        return @{
            Success = $false
            StatusCode = $_.Exception.Response.StatusCode
            Content = $_.Exception.Message
        }
    }
}

function Extract-Json-Value {
    param([string]$Json, [string]$Path)
    
    try {
        $Obj = $Json | ConvertFrom-Json
        $Keys = $Path.Split(".")
        foreach ($Key in $Keys) {
            $Obj = $Obj.$Key
        }
        return $Obj
    } catch {
        return $null
    }
}

Write-Host "$Blue════════════════════════════════════════════════════════$Reset" -ForegroundColor Cyan
Write-Host "$Blue  AIGRS Backend - Automated API Testing Suite$Reset" -ForegroundColor Cyan
Write-Host "$Blue════════════════════════════════════════════════════════$Reset" -ForegroundColor Cyan
Write-Host "BaseUrl: $BaseUrl`n"

# ============== WORKFLOW 1: AUTHENTICATION ==============
Write-Host "$Yellow[WORKFLOW 1] Authentication Tests$Reset" -ForegroundColor Yellow

# Test 1: Register User
$RegisterBody = @{
    name = "Test User $(Get-Random)"
    phone = "98765432$(Get-Random -Minimum 10 -Maximum 99)"
    password = "TestPass123!"
    orgId = "550e8400-e29b-41d4-a716-446655440000"
} | ConvertTo-Json

$RegResponse = Invoke-API -Method POST -Endpoint "/auth/register" -Body $RegisterBody
$RegSuccess = $RegResponse.Success -and $RegResponse.StatusCode -eq 201
Log-Test "Auth: Register User" $RegSuccess $RegResponse.Content

$AccessToken = $null
if ($RegSuccess) {
    $AccessToken = Extract-Json-Value -Json $RegResponse.Content -Path "data.accessToken"
    Write-Host "  Token: $($AccessToken.Substring(0, 20))..." -ForegroundColor Cyan
}

# Test 2: Health Check
$HealthResponse = Invoke-API -Method GET -Endpoint "/../actuator/health"
$HealthSuccess = $HealthResponse.Success -and $HealthResponse.StatusCode -eq 200
Log-Test "System: Health Check" $HealthSuccess $HealthResponse.Content

# ============== WORKFLOW 2: GRIEVANCE SUBMISSION ==============
Write-Host "`n$Yellow[WORKFLOW 2] Grievance Management Tests$Reset" -ForegroundColor Yellow

if ($AccessToken) {
    # Test 3: Submit Grievance
    $GrievanceBody = @{
        title = "Test Grievance $(Get-Random)"
        description = "This is a test grievance submission"
        priority = "HIGH"
        categoryId = "550e8400-e29b-41d4-a716-446655440001"
        departmentId = "550e8400-e29b-41d4-a716-446655440002"
        location = "Test Location"
        latitude = 28.7041
        longitude = 77.1025
    } | ConvertTo-Json

    $GrievanceResponse = Invoke-API -Method POST -Endpoint "/grievances" -Body $GrievanceBody -Token $AccessToken
    $GrievanceSuccess = $GrievanceResponse.Success -and $GrievanceResponse.StatusCode -eq 201
    Log-Test "Grievance: Submit Grievance" $GrievanceSuccess $GrievanceResponse.Content

    $GrievanceId = $null
    if ($GrievanceSuccess) {
        $GrievanceId = Extract-Json-Value -Json $GrievanceResponse.Content -Path "data.id"
        Write-Host "  Grievance ID: $GrievanceId" -ForegroundColor Cyan
    }

    # Test 4: List Grievances
    $ListResponse = Invoke-API -Method GET -Endpoint "/grievances?page=0&size=10" -Token $AccessToken
    $ListSuccess = $ListResponse.Success -and $ListResponse.StatusCode -eq 200
    Log-Test "Grievance: List Grievances" $ListSuccess $ListResponse.Content

    # Test 5: Get Single Grievance
    if ($GrievanceId) {
        $GetResponse = Invoke-API -Method GET -Endpoint "/grievances/$GrievanceId" -Token $AccessToken
        $GetSuccess = $GetResponse.Success -and $GetResponse.StatusCode -eq 200
        Log-Test "Grievance: Get Single Grievance" $GetSuccess $GetResponse.Content
    }

    # Test 6: Add Comment
    if ($GrievanceId) {
        $CommentBody = @{
            content = "Test comment for grievance"
            isInternal = $false
        } | ConvertTo-Json

        $CommentResponse = Invoke-API -Method POST -Endpoint "/grievances/$GrievanceId/comments" -Body $CommentBody -Token $AccessToken
        $CommentSuccess = $CommentResponse.Success -and $CommentResponse.StatusCode -eq 201
        Log-Test "Grievance: Add Comment" $CommentSuccess $CommentResponse.Content
    }

    # Test 7: Update Grievance Status
    if ($GrievanceId) {
        $StatusBody = @{
            status = "IN_PROGRESS"
            remarks = "Grievance is being processed"
        } | ConvertTo-Json

        $StatusResponse = Invoke-API -Method PATCH -Endpoint "/grievances/$GrievanceId/status" -Body $StatusBody -Token $AccessToken
        $StatusSuccess = $StatusResponse.Success -and ($StatusResponse.StatusCode -eq 200 -or $StatusResponse.StatusCode -eq 202)
        Log-Test "Grievance: Update Status" $StatusSuccess $StatusResponse.Content
    }
}

# ============== WORKFLOW 3: SEARCH ==============
Write-Host "`n$Yellow[WORKFLOW 3] Search & Filter Tests$Reset" -ForegroundColor Yellow

if ($AccessToken) {
    # Test 8: Full-text Search
    $SearchResponse = Invoke-API -Method GET -Endpoint "/search?query=test&page=0&size=10" -Token $AccessToken
    $SearchSuccess = $SearchResponse.Success -and $SearchResponse.StatusCode -eq 200
    Log-Test "Search: Full-text Search" $SearchSuccess $SearchResponse.Content

    # Test 9: Search with Filters
    $FilteredResponse = Invoke-API -Method GET -Endpoint "/search?query=test&status=IN_PROGRESS&priority=HIGH&page=0&size=10" -Token $AccessToken
    $FilteredSuccess = $FilteredResponse.Success -and $FilteredResponse.StatusCode -eq 200
    Log-Test "Search: Filtered Search" $FilteredSuccess $FilteredResponse.Content
}

# ============== WORKFLOW 4: ADMIN ENDPOINTS ==============
Write-Host "`n$Yellow[WORKFLOW 4] Admin Panel Tests$Reset" -ForegroundColor Yellow

if ($AccessToken) {
    # Test 10: Dashboard Stats
    $DashResponse = Invoke-API -Method GET -Endpoint "/admin/dashboard" -Token $AccessToken
    $DashSuccess = $DashResponse.Success -and $DashResponse.StatusCode -eq 200
    Log-Test "Admin: Dashboard Stats" $DashSuccess $DashResponse.Content

    # Test 11: List Categories
    $CatResponse = Invoke-API -Method GET -Endpoint "/admin/categories?page=0&size=10" -Token $AccessToken
    $CatSuccess = $CatResponse.Success -and $CatResponse.StatusCode -eq 200
    Log-Test "Admin: List Categories" $CatSuccess $CatResponse.Content

    # Test 12: List Departments
    $DeptResponse = Invoke-API -Method GET -Endpoint "/admin/departments?page=0&size=10" -Token $AccessToken
    $DeptSuccess = $DeptResponse.Success -and $DeptResponse.StatusCode -eq 200
    Log-Test "Admin: List Departments" $DeptSuccess $DeptResponse.Content

    # Test 13: Export CSV
    $ExportResponse = Invoke-API -Method GET -Endpoint "/admin/export/csv" -Token $AccessToken
    $ExportSuccess = $ExportResponse.Success -and $ExportResponse.StatusCode -eq 200
    Log-Test "Admin: Export CSV" $ExportSuccess $ExportResponse.Content
}

# ============== WORKFLOW 5: NOTIFICATIONS ==============
Write-Host "`n$Yellow[WORKFLOW 5] Notification Tests$Reset" -ForegroundColor Yellow

if ($AccessToken) {
    # Test 14: List Notifications
    $NotifResponse = Invoke-API -Method GET -Endpoint "/notifications?page=0&size=10" -Token $AccessToken
    $NotifSuccess = $NotifResponse.Success -and $NotifResponse.StatusCode -eq 200
    Log-Test "Notification: List Notifications" $NotifSuccess $NotifResponse.Content
}

# ============== TEST SUMMARY ==============
Write-Host "`n$Blue════════════════════════════════════════════════════════$Reset" -ForegroundColor Cyan
Write-Host "$Blue  TEST SUMMARY REPORT$Reset" -ForegroundColor Cyan
Write-Host "$Blue════════════════════════════════════════════════════════$Reset" -ForegroundColor Cyan

Write-Host "Total Tests:   $TotalTests"
Write-Host "$Green Passed:     $PassedTests$Reset" -ForegroundColor Green
Write-Host "$Red Failed:     $FailedTests$Reset" -ForegroundColor Red

$PassRate = if ($TotalTests -gt 0) { [math]::Round(($PassedTests / $TotalTests) * 100, 2) } else { 0 }
Write-Host "Pass Rate:     $PassRate%"

Write-Host "`n$Blue[Test Results Table]$Reset" -ForegroundColor Cyan
$Results | Format-Table -AutoSize

# Export report
$ReportFile = "d:\stonepro\API_TEST_REPORT.txt"
"AIGRS Backend API Testing Report - $(Get-Date)" | Out-File $ReportFile
"================================================" | Out-File $ReportFile -Append
"Total Tests: $TotalTests" | Out-File $ReportFile -Append
"Passed: $PassedTests" | Out-File $ReportFile -Append
"Failed: $FailedTests" | Out-File $ReportFile -Append
"Pass Rate: $PassRate%" | Out-File $ReportFile -Append
`n | Out-File $ReportFile -Append
"Test Details:" | Out-File $ReportFile -Append
$Results | Format-Table -AutoSize | Out-File $ReportFile -Append

Write-Host "`nReport saved to: $ReportFile" -ForegroundColor Cyan

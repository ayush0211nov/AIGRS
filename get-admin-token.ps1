#!/usr/bin/env pwsh
<#
.SYNOPSIS
Quick script to get an ADMIN token for testing AIGRS Admin APIs

.DESCRIPTION
Registers an admin user (or logs in if already exists) and saves the token for later use

.PARAMETER Name
Admin user full name (default: "Admin Test User")

.PARAMETER Email
Admin user email (default: "admin@demo.gov")

.PARAMETER Phone
Admin user phone (default: "+1-555-9999")

.PARAMETER Password
Admin user password (default: "AdminPass123!")

.PARAMETER OrgId
Organization UUID (default: "f47ac10b-58cc-4372-a567-0e02b2c3d479")

.PARAMETER BaseUrl
Backend API URL (default: "http://localhost:8080")

.PARAMETER TokenFile
File to save the token (default: "admin_token.txt")

.EXAMPLE
.\get-admin-token.ps1

.EXAMPLE
.\get-admin-token.ps1 -Name "John Admin" -Phone "+1-555-8888" -TokenFile "my_token.txt"
#>

param(
    [string]$Name = "Admin Test User",
    [string]$Email = "admin@demo.gov",
    [string]$Phone = "+1-555-9999",
    [string]$Password = "AdminPass123!",
    [string]$OrgId = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    [string]$BaseUrl = "http://localhost:8080",
    [string]$TokenFile = "admin_token.txt"
)

Write-Host "🔐 AIGRS Admin Token Generator" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Check if backend is running
Write-Host "✓ Checking backend availability..." -ForegroundColor Gray
try {
    $healthCheck = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -ErrorAction Stop
    Write-Host "✓ Backend is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Backend is not running at $BaseUrl" -ForegroundColor Red
    Write-Host "  Start it with: docker compose up --build" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "📝 Registering admin user..." -ForegroundColor Gray
Write-Host "  Name: $Name"
Write-Host "  Email: $Email"
Write-Host "  Phone: $Phone"
Write-Host "  Role: ADMIN"
Write-Host "  Org: $OrgId"
Write-Host ""

# Prepare registration payload
$payload = @{
    name = $Name
    email = $Email
    phone = $Phone
    password = $Password
    orgId = $OrgId
    role = "ADMIN"
} | ConvertTo-Json

# Register user
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/v1/auth/register" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $payload `
        -ErrorAction Stop | ConvertFrom-Json

    if ($response.data.accessToken) {
        $accessToken = $response.data.accessToken
        $refreshToken = $response.data.refreshToken
        $userId = $response.data.userId

        # Save token to file
        $accessToken | Out-File $TokenFile -Force
        Write-Host "✓ Admin user registered successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "📋 Details:" -ForegroundColor Cyan
        Write-Host "  User ID: $userId"
        Write-Host "  Token saved to: $TokenFile"
        Write-Host ""
        Write-Host "🔑 Access Token:" -ForegroundColor Yellow
        Write-Host "  $($accessToken.Substring(0, 50))..." -ForegroundColor Gray
        Write-Host ""
        Write-Host "📌 Use this token for all admin API calls:" -ForegroundColor Cyan
        Write-Host '  $ADMIN_TOKEN = Get-Content ' + $TokenFile
        Write-Host '  curl.exe -X GET http://localhost:8080/api/v1/admin/dashboard/stats `'
        Write-Host '    -H "Authorization: Bearer $ADMIN_TOKEN"'
        Write-Host ""
        Write-Host "✅ Ready to test admin APIs!" -ForegroundColor Green
    }
} catch {
    $errorResponse = $_ | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($errorResponse.message) {
        Write-Host "⚠️  $($errorResponse.message)" -ForegroundColor Yellow
        if ($errorResponse.message -like "*already exists*") {
            Write-Host ""
            Write-Host "→ User already exists, trying login..." -ForegroundColor Yellow
            
            $loginPayload = @{
                phone = $Phone
                password = $Password
            } | ConvertTo-Json
            
            try {
                $loginResponse = Invoke-WebRequest -Uri "$BaseUrl/api/v1/auth/login" `
                    -Method Post `
                    -Headers @{"Content-Type" = "application/json"} `
                    -Body $loginPayload `
                    -ErrorAction Stop | ConvertFrom-Json

                if ($loginResponse.data.accessToken) {
                    $accessToken = $loginResponse.data.accessToken
                    $accessToken | Out-File $TokenFile -Force
                    Write-Host "✓ Logged in successfully!" -ForegroundColor Green
                    Write-Host ""
                    Write-Host "🔑 Access Token:" -ForegroundColor Yellow
                    Write-Host "  $($accessToken.Substring(0, 50))..." -ForegroundColor Gray
                    Write-Host ""
                    Write-Host "✅ Ready to test admin APIs!" -ForegroundColor Green
                }
            } catch {
                Write-Host "✗ Login failed: $_" -ForegroundColor Red
                exit 1
            }
        }
    } else {
        Write-Host "✗ Registration failed: $_" -ForegroundColor Red
        exit 1
    }
}

# 🔐 Getting Admin Access & Testing Admin APIs

## Overview
The AIGRS backend includes admin features that require `ADMIN`, `SUPERVISOR`, or `SUPER_ADMIN` role. This guide shows how to create an admin user account and access protected admin endpoints.

---

## Step 1: Register as ADMIN User

### Using PowerShell with Curl

```powershell
$adminPayload = @{
    name = "Admin Test User"
    email = "admin@demo.gov"
    phone = "+1-555-9999"
    password = "AdminPass123!"
    orgId = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
    role = "ADMIN"
} | ConvertTo-Json

curl.exe -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" `
  -d $adminPayload | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### Expected Response
```json
{
  "status": "success",
  "message": "Registration successful",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "role": "ADMIN",
    "orgId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  },
  "timestamp": "2026-04-13T12:34:56.123456"
}
```

---

## Step 2: Login (If You Already Have an Admin Account)

```powershell
$loginPayload = @{
    phone = "+1-555-9999"
    password = "AdminPass123!"
} | ConvertTo-Json

curl.exe -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d $loginPayload | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Save the `accessToken` from the response** - you'll use it for all admin API calls.

---

## Step 3: Use the Admin Token to Access Protected Endpoints

### ✅ Get Dashboard Statistics
```powershell
$ADMIN_TOKEN = "YOUR_ACCESS_TOKEN_HERE"

curl.exe -X GET http://localhost:8080/api/v1/admin/dashboard/stats `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ Get Dashboard Charts
```powershell
curl.exe -X GET http://localhost:8080/api/v1/admin/dashboard/charts `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ List All Grievances (Admin View)
```powershell
curl.exe -X GET "http://localhost:8080/api/v1/admin/grievances?page=0&size=10" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ List Staff Members
```powershell
curl.exe -X GET http://localhost:8080/api/v1/admin/staff `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ Get Staff Performance Metrics
```powershell
$STAFF_ID = "550e8400-e29b-41d4-a716-446655440000"  # Replace with actual staff ID

curl.exe -X GET http://localhost:8080/api/v1/admin/staff/$STAFF_ID/performance `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ List Departments
```powershell
curl.exe -X GET http://localhost:8080/api/v1/admin/departments `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ List Categories
```powershell
curl.exe -X GET http://localhost:8080/api/v1/admin/categories `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ Create New Category
```powershell
$categoryPayload = @{
    name = "Infrastructure"
    description = "Roads, water, electricity issues"
    slaHours = 48
    departmentId = "550e8400-e29b-41d4-a716-446655440000"  # Replace with actual dept ID
} | ConvertTo-Json

curl.exe -X POST http://localhost:8080/api/v1/admin/categories `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" `
  -d $categoryPayload | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ Export Grievances Report
```powershell
# Export as CSV
curl.exe -X GET "http://localhost:8080/api/v1/admin/reports/export?format=csv" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -OutFile "grievances.csv"

# Export as Excel
curl.exe -X GET "http://localhost:8080/api/v1/admin/reports/export?format=excel" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -OutFile "grievances.xlsx"
```

### ✅ Get Heatmap Data
```powershell
curl.exe -X GET http://localhost:8080/api/v1/admin/analytics/heatmap `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ Get SLA Configuration
```powershell
curl.exe -X GET http://localhost:8080/api/v1/admin/sla/config `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### ✅ Update SLA Configuration
```powershell
$slaPayload = @{
    LOW = 72
    MEDIUM = 48
    HIGH = 24
    CRITICAL = 4
} | ConvertTo-Json

curl.exe -X PUT http://localhost:8080/api/v1/admin/sla/config `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" `
  -d $slaPayload
```

---

## 🛡️ Available Admin Roles

| Role | Access | Use Case |
|------|--------|----------|
| **ADMIN** | Full admin features | Organization administrators |
| **SUPERVISOR** | All admin endpoints | Department supervisors |
| **SUPER_ADMIN** | All features including system config | System administrators |

All three roles can access the `/api/v1/admin/*` endpoints.

---

## 📋 Complete Admin Endpoint Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/dashboard/stats` | Dashboard statistics (cached 5min) |
| GET | `/api/v1/admin/dashboard/charts` | Chart data and trends |
| GET | `/api/v1/admin/grievances` | All grievances with all filters |
| GET | `/api/v1/admin/staff` | Staff list with performance metrics |
| GET | `/api/v1/admin/staff/{id}/performance` | Individual staff performance |
| GET | `/api/v1/admin/departments` | Organization departments |
| GET | `/api/v1/admin/categories` | Grievance categories |
| POST | `/api/v1/admin/categories` | Create new category |
| PUT | `/api/v1/admin/categories/{id}` | Update category |
| GET | `/api/v1/admin/reports/export` | Export grievances (CSV/Excel) |
| GET | `/api/v1/admin/analytics/heatmap` | Location heatmap data |
| GET | `/api/v1/admin/sla/config` | Get SLA configuration |
| PUT | `/api/v1/admin/sla/config` | Update SLA configuration |

---

## 🔑 Quick Reference Token Storage

Save your token for repeated use:

```powershell
# Store in variable for session
$ADMIN_TOKEN = "eyJhbGciOiJIUzUxMiJ9..."

# Use in API calls
curl.exe -X GET http://localhost:8080/api/v1/admin/dashboard/stats `
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Or save to a file:

```powershell
# Save token to file
"eyJhbGciOiJIUzUxMiJ9..." | Out-File admin_token.txt

# Read and use
$ADMIN_TOKEN = Get-Content admin_token.txt
```

---

## ⚠️ Common Issues

### "Access Denied" / 403 Error
- ❌ Token is a CITIZEN token (needs ADMIN/SUPERVISOR/SUPER_ADMIN)
- ❌ Token has expired (refresh it)
- ❌ Wrong orgId in token

### "Authorization header missing" / 401 Error
- ❌ Forgot to include `Authorization: Bearer <token>` header
- ❌ Token format is incorrect

### Solution: Re-register or login with correct role
```powershell
# Register new ADMIN user
$adminPayload = @{
    name = "Admin User"
    email = "admin@demo.gov"
    phone = "+1-555-9999"
    password = "AdminPass123!"
    orgId = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
    role = "ADMIN"
} | ConvertTo-Json

curl.exe -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" `
  -d $adminPayload
```

---

## 🔄 Refreshing Token (When Expired)

JWT tokens expire after a certain period. Use the refresh token to get a new access token:

```powershell
$refreshPayload = @{
    refreshToken = "YOUR_REFRESH_TOKEN_HERE"
} | ConvertTo-Json

curl.exe -X POST http://localhost:8080/api/v1/auth/refresh-token `
  -H "Content-Type: application/json" `
  -d $refreshPayload | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

---

## Testing Tips

1. **Use Postman** for easier token management and request history
2. **Save tokens** to avoid repeated registration
3. **Check bearer token format** in Authorization header
4. **Verify orgId** matches your organization
5. **Test with different roles** to understand permission levels

---

## ✅ Now You're Ready!

You can now:
- ✅ Register admin users
- ✅ Obtain admin JWT tokens
- ✅ Access all admin-protected endpoints
- ✅ Manage organization settings, staff, and reports
- ✅ Monitor dashboard metrics and analytics


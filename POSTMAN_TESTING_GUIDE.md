# AIGRS Backend - Postman Testing Guide

**Everything you need to test the AIGRS API in Postman**

---

## 📥 Import Into Postman

### Step 1: Download Files
Three files are provided in the repository root:
- `AIGRS-Backend-Postman-Collection.json` - API endpoints
- `AIGRS-Environment-Dev.json` - Development variables
- `AIGRS-Environment-Prod.json` - Production variables

### Step 2: Import Collection
1. Open **Postman**
2. Click **Import** (top-left)
3. Select **AIGRS-Backend-Postman-Collection.json**
4. Choose **Postman v2.1** format
5. Click **Import**

### Step 3: Import Environment
1. Click **Environments** (left sidebar)
2. Click **Import**
3. Select **AIGRS-Environment-Dev.json** (for local testing)
4. Click **Import**

### Step 4: Select Environment
1. Click environment dropdown (top-right)
2. Select **AIGRS Development Environment**

---

## 🚀 Quick Start Testing

### 1. Start Backend
```bash
# Option A: Docker Compose
docker-compose up -d

# Option B: Maven
mvn spring-boot:run

# Verify health
curl http://localhost:8080/actuator/health
```

### 2. Test Authentication Flow

**Step 1: Register User**
- Send: `POST /auth/register`
- Body (edit values):
  ```json
  {
    "name": "Test User",
    "phone": "9876543210",
    "email": "test@example.com",
    "password": "TestPass123!",
    "orgId": "550e8400-e29b-41d4-a716-446655440000"
  }
  ```
- Response will contain `accessToken` and `refreshToken`
- **Copy `accessToken` and paste into Postman environment variable `accessToken`**

**Step 2: Login**
- Send: `POST /auth/login`
- This simulates another user login
- Extract and save token

**Step 3: Verify Token is Set**
- All subsequent requests automatically include `Authorization: Bearer {{accessToken}}`

### 3. Test Grievance Submission

**Step 1: Get Categories** (Admin endpoint)
- Send: `GET /admin/categories`
- Save first category ID to `categoryId` environment variable

**Step 2: Get Departments** (Admin endpoint)
- Send: `GET /admin/departments`
- Save first department ID to `departmentId` environment variable

**Step 3: Submit Grievance**
- Send: `POST /grievances`
- Body will auto-fill with `categoryId` and `departmentId`
- Response includes `id` - copy to `grievanceId` environment variable

**Step 4: View Grievance**
- Send: `GET /grievances/{{grievanceId}}`
- Verify all details match submission

### 4. Test File Upload

**Step 1: Prepare File**
- Have an image file ready (JPEG, PNG, or MP4 video)

**Step 2: Upload File**
- Send: `POST /files/upload`
- Under Body tab, click **form-data**
- Add file field: `file` → select your image
- Optional: Add `grievanceId` field
- Click **Send**
- Response includes `url` - copy to `fileId` environment variable

**Step 3: Download File**
- Send: `GET /files/{{fileId}}/download`
- Returns S3 signed URL valid for 24 hours

---

## 📋 Test Workflows

### Workflow 1: Complete Grievance Resolution

```
1. Register User                    → GET accessToken
2. Submit Grievance                 → GET grievanceId
3. Add Comment (as submitter)       → Verify comment added
4. Upload Proof (image/video)       → GET fileId
5. Update Status to IN_PROGRESS     → Verify status
6. Add Internal Comment (staff)     → isInternal: true
7. Resolve Grievance                → Verify resolved
8. Rate Grievance                   → Score: 4, Feedback added
9. View Grievance                   → Verify all history
```

### Workflow 2: Search & Filter

```
1. Submit multiple grievances       → Different priorities/statuses
2. Search by text query             → "water", "electricity", etc.
3. Filter by status                 → SUBMITTED, IN_PROGRESS, RESOLVED
4. Filter by priority               → HIGH, MEDIUM, LOW
5. Filter by date range             → fromDate, toDate
6. Combine multiple filters         → Query + Status + Priority
7. Paginate results                 → page=0, size=20
```

### Workflow 3: Admin Dashboard

```
1. Fetch Dashboard Stats            → View all metrics
2. Export as CSV                    → Download CSV file
3. Export as Excel                  → Download Excel workbook
4. Export as PDF                    → Download PDF report
5. Create Category                  → Add new grievance type
6. Create Department                → Add organizational unit
7. List Staff Members               → View all staff
8. View Staff Workload              → Check assignments
```

### Workflow 4: Notifications

```
1. Make status changes              → Triggers notifications
2. Add comments                     → Triggers notifications
3. List Notifications               → View all (paginated)
4. Mark as Read                     → Single notification
5. Mark All as Read                 → Bulk action
6. Verify notification emails/SMS   → Check console (dev)
```

---

## 🔑 Environment Variables Explained

| Variable | Purpose | Example |
|----------|---------|---------|
| `baseUrl` | API base endpoint | http://localhost:8080/api/v1 |
| `accessToken` | JWT auth token | eyJhbGciOiJIUzI1NiIs... |
| `refreshToken` | Token refresh token | eyJhbGciOiJIUzI1NiIs... |
| `orgId` | Organization UUID | 550e8400-e29b-41d4... |
| `userId` | Current user UUID | (auto-set from login) |
| `grievanceId` | Current grievance UUID | (set from submission) |
| `fileId` | Uploaded file UUID | (set from upload) |
| `categoryId` | Grievance category UUID | (set from admin request) |
| `departmentId` | Department UUID | (set from admin request) |
| `staffId` | Staff member UUID | (set from staff list) |
| `notificationId` | Notification UUID | (set from notification list) |
| `timestamp` | ISO 8601 timestamp | Auto-generated |

---

## 🧪 Testing Tips

### Tip 1: Use Environment Variables
- Don't hardcode UUIDs in request bodies
- Use `{{variable}}` syntax for auto-substitution
- Copy-paste UUIDs from responses into environment

### Tip 2: Save Responses to Variables
After each successful request:
```
1. View response in lower panel
2. Find UUID or token in JSON
3. Right-click → Copy
4. Left sidebar → Environments
5. Paste into appropriate environment variable
```

### Tip 3: Test Error Cases
- Missing Authorization header → 401 Unauthorized
- Invalid grievance ID → 404 Not Found
- Wrong role → 403 Forbidden
- Duplicate phone → 400 Bad Request
- Invalid enum value → 400 Bad Request

### Tip 4: Check Development Logs
When sending requests, check backend console:
```
[dev] OTP sent: 123456
[dev] JWT generated: token...
[dev] Grievance created: GRV-2024-000001
```

### Tip 5: Use Pre-request Scripts
Some endpoints may need pre-request setup:
```javascript
// In Pre-request Script tab:
pm.environment.set("timestamp", new Date().toISOString());
```

---

## 📊 Sample Test Data

### Test User
```json
{
  "name": "Test Citizen",
  "phone": "9876543210",
  "email": "citizen@example.com",
  "password": "TestPass123!",
  "orgId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Test Staff
```json
{
  "name": "Test Staff",
  "phone": "9887654321",
  "email": "staff@example.com",
  "password": "StaffPass123!",
  "role": "STAFF",
  "departmentId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### Test Grievance
```json
{
  "title": "Poor Water Quality",
  "description": "Tap water has been discolored for a week",
  "location": "123 Main Street",
  "latitude": 28.7041,
  "longitude": 77.1025,
  "categoryId": "550e8400-e29b-41d4-a716-446655440001",
  "departmentId": "550e8400-e29b-41d4-a716-446655440002",
  "priority": "HIGH"
}
```

---

## 🐛 Troubleshooting

### Issue: "baseUrl is not defined"
**Solution:**
1. Check environment is selected (top-right dropdown)
2. Click "Manage Environments" → "AIGRS Development Environment"
3. Verify `baseUrl` value is set

### Issue: 401 Unauthorized
**Solution:**
1. You need to login first with `/auth/login` or `/auth/register`
2. Copy the `accessToken` from response
3. Paste into environment variable: `accessToken`
4. Verify token is not expired (24-hour expiry)

### Issue: 403 Forbidden
**Solution:**
1. Some endpoints require specific roles (STAFF, ADMIN)
2. Verify your user has the correct role
3. Or register as STAFF/ADMIN user for testing admin endpoints

### Issue: 404 Not Found
**Solution:**
1. UUID not found in database
2. Verify `grievanceId`, `fileId`, etc. are correct
3. First create the resource (grievance, file) before accessing it

### Issue: File upload fails
**Solution:**
1. File size must be < 50MB
2. Allowed types: JPEG, PNG, MP4
3. Use form-data, not raw JSON body
4. Click "Select File" in Postman form-data picker

### Issue: Connection refused
**Solution:**
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check baseUrl in environment (should be http://localhost:8080/api/v1)
3. If using Docker: `docker-compose ps` to verify containers are UP

### Issue: Rate limiting (429 Too Many Requests)
**Solution:**
1. You've exceeded 100 requests per minute
2. Wait 60 seconds before making more requests
3. Redis rate limiter will reset automatically

---

## 🔍 API Response Format

All responses follow this standard format:

### Success (200, 201)
```json
{
  "status": "success",
  "message": "Operation successful",
  "data": { /* actual data */ },
  "timestamp": "2024-04-12T10:30:00"
}
```

### Error (400, 401, 403, 404, 500)
```json
{
  "status": "error",
  "message": "Error description",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2024-04-12T10:30:00"
}
```

---

## ✅ Verification Checklist

After importing and setting up:

- [ ] Import collection successful → See "AIGRS Backend API" in left sidebar
- [ ] Import environment successful → See in Environment dropdown
- [ ] baseUrl resolves correctly → Health check returns 200
- [ ] Can register user → Token received and saved
- [ ] Can login → Access token obtained
- [ ] Can submit grievance → Grievance ID returned
- [ ] Can search grievances → Results with pagination
- [ ] Can upload file → File URL returned
- [ ] Can view admin dashboard → Stats displayed
- [ ] All error handling works → 401, 403, 404 responses

---

## 📚 Useful REST API Patterns

### Pattern 1: Pagination
```
GET /api/v1/grievances?page=0&size=20
Response: { content: [...], totalElements: 150, totalPages: 8, ... }
```

### Pattern 2: Filtering
```
GET /api/v1/search?query=water&status=IN_PROGRESS&priority=HIGH
```

### Pattern 3: Bearer Token
```
Header: Authorization: Bearer {{accessToken}}
```

### Pattern 4: File Upload (multipart)
```
POST /files/upload (form-data)
Fields: file, grievanceId
```

### Pattern 5: Resource Creation
```
POST /grievances (return 201 Created with Location header)
```

---

## 🎯 Next Steps

1. **Implement**, don't just test
   - Use these endpoints in your frontend
   - Handle error responses properly
   - Implement token refresh logic

2. **Add More Tests**
   - Create collections for each module
   - Use variables for reusable data
   - Document custom workflows

3. **Automate**
   - Use Postman's test framework
   - Write assertions for responses
   - Run collections in CI/CD pipeline

4. **Monitor**
   - Check response times
   - Monitor rate limits
   - Log all requests for audit trail

---

## 📞 Support

- **API Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Backend Logs**: Docker: `docker-compose logs app`
- **Postman Docs**: https://learning.postman.com/

---

**Last Updated**: April 12, 2026  
**Postman Version**: 10.0+  
**API Version**: 1.0.0

# AIGRS Backend - Complete Postman Testing Manual

**Status**: Follow these exact steps in Postman to test all workflows

---

##  **SETUP (Before Testing)**

### Prerequisites
1. ✅ Postman installed
2. ✅ Collection imported: `AIGRS-Backend-Postman-Collection.json`
3. ✅ Environment imported: `AIGRS-Environment-Dev.json`
4. ✅ Environment SELECTED in top-right dropdown
5. ⏳ Backend running: `docker-compose up -d` (give it 3-5 minutes to start)

### Verify Backend is Ready
```
GET http://localhost:8080/actuator/health
Expected Response: 200 OK {"status":"UP"}
```

---

## **COMPLETE TESTING WORKFLOW**

### **SECTION 1: AUTHENTICATION (5 minutes)**

#### Step 1.1: Register a New User
```
Endpoint: POST /auth/register
Folder: Authentication > "Register User"

Body (pre-filled):
{
  "name": "Test User",
  "phone": "9876543210",
  "password": "SecurePass@123",
  "orgId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}

Instructions:
1. Click the endpoint
2. Change "phone" to something unique (e.g., "9876543211")
3. Click SEND
4. Response status should be: 201 Created

Expected Response:
{
  "status": "success",
  "data": {
    "id": "uuid...",
    "name": "Test User",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}

✅ PASS CRITERIA: Status 201, response has accessToken
```

#### Step 1.2: SAVE TOKEN to Environment
```
In Postman response:
1. Find "accessToken" value in response
2. Highlight and COPY the entire token (starts with "eyJ")
3. Left sidebar > Environments > AIGRS-Environment-Dev
4. Find row: "accessToken" | Initial value (empty)
5. PASTE the token into Initial value column
6. Click SAVE (Ctrl+S)

Verification: All endpoints below should now have "Authorization: Bearer [token]"
```

#### Step 1.3: Login with Credentials
```
Endpoint: POST /auth/login
Folder: Authentication > "Login User"

Body (pre-filled):
{
  "phone": "9876543210",
  "password": "SecurePass@123"
}

Instructions:
1. Change "phone" to match the one you registered with (Step 1.1)
2. Click SEND
3. Status should be: 200 OK

✅ PASS CRITERIA: Status 200, response has accessToken (same as registration)
```

---

### **SECTION 2: GRIEVANCE MANAGEMENT (10 minutes)**

#### Step 2.1: Get Categories (Required for Grievance Submission)
```
Endpoint: GET /admin/categories
Folder: Admin > "Get Categories"

Instructions:
1. Click the endpoint
2. Query Params already set: page=0, size=10
3. Click SEND
4. Status should be: 200 OK

Response will show list of categories:
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Water Supply",
        "slaHours": 24,
        ...
      },
      ...
    ]
  }
}

SAVE First Category ID:
1. Copy the first "id" from categories list
2. Environment > AIGRS-Environment-Dev
3. Find row: "categoryId" | Initial value
4. PASTE the category ID
5. SAVE (Ctrl+S)
```

#### Step 2.2: Get Departments (Also Required)
```
Endpoint: GET /admin/departments
Folder: Admin > "Get Departments"

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK

SAVE First Department ID:
1. Copy the first "id" from departments list
2. Environment > AIGRS-Environment-Dev
3. Find row: "departmentId"
4. PASTE the department ID
5. SAVE
```

#### Step 2.3: Submit a Grievance
```
Endpoint: POST /grievances
Folder: Grievances > "Submit Grievance"

Body (auto-filled with environment variables):
{
  "title": "Water Supply Issue",
  "description": "No water in area for 3 days",
  "location": "123 Main Street, City",
  "latitude": 28.7041,
  "longitude": 77.1025,
  "priority": "HIGH",
  "categoryId": "{{categoryId}}",         ← Auto-fills from environment
  "departmentId": "{{departmentId}}"      ← Auto-fills from environment
}

Instructions:
1. Click the endpoint
2. Modify title/description to make it unique (so you can identify it later)
3. Click SEND
4. Status should be: 201 Created

Response:
{
  "status": "success",
  "data": {
    "id": "uuid-of-grievance",
    "trackingId": "GRV-2024-000001",
    "title": "Water Supply Issue",
    "status": "SUBMITTED",
    ...
  }
}

SAVE Grievance ID:
1. Copy the "id" from response
2. Environment > AIGRS-Environment-Dev
3. Find row: "grievanceId"
4. PASTE the grievance ID
5. SAVE

✅ PASS CRITERIA: Status 201, grievanceId returned
```

#### Step 2.4: View the Grievance You Created
```
Endpoint: GET /grievances/{{grievanceId}}
Folder: Grievances > "Get Grievance Details"

Instructions:
1. Click the endpoint
2. The `{{grievanceId}}` will auto-fill from environment
3. Click SEND
4. Status should be: 200 OK

Verify you see your grievance with:
- Your title
- Your description
- Status: SUBMITTED
- All your details

✅ PASS CRITERIA: Status 200, all your data returned
```

#### Step 2.5: Add a Comment to Your Grievance
```
Endpoint: POST /grievances/{{grievanceId}}/comments
Folder: Grievances > "Add Comment"

Body:
{
  "content": "I need urgent help with this issue",
  "isInternal": false
}

Instructions:
1. Click the endpoint
2. Modify the comment text
3. Click SEND
4. Status should be: 201 Created

Response:
{
  "status": "success",
  "data": {
    "id": "comment-id",
    "content": "I need urgent help with this issue",
    "userId": "your-user-id",
    "isInternal": false,
    "createdAt": "..."
  }
}

✅ PASS CRITERIA: Status 201, comment added with your text
```

#### Step 2.6: Update Status to IN_PROGRESS
```
Endpoint: PATCH /grievances/{{grievanceId}}/status
Folder: Grievances > "Update Status"

Body:
{
  "status": "IN_PROGRESS",
  "remarks": "Your grievance is being reviewed"
}

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK or 202 Accepted

Verify response shows:
- "status": "IN_PROGRESS"
- "remarks": included

✅ PASS CRITERIA: Status 200/202, status changed to IN_PROGRESS
```

#### Step 2.7: Rate the Grievance (After resolution)
```
Endpoint: POST /grievances/{{grievanceId}}/rate
Folder: Grievances > "Rate Grievance"

Body:
{
  "score": 4,
  "feedback": "Good process but could be faster"
}

Instructions:
1. Click the endpoint
2. Modify score (1-5) and feedback
3. Click SEND
4. Status should be: 201 Created

✅ PASS CRITERIA: Status 201, rating saved
```

#### Step 2.8: List All Grievances (Pagination)
```
Endpoint: GET /grievances
Folder: Grievances > "List Grievances"

Query Parameters (pre-set):
- page: 0
- size: 10
- status: SUBMITTED (optional filter)

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK

Response will include:
{
  "status": "success",
  "data": {
    "content": [...list of grievances...],
    "totalElements": 5,
    "totalPages": 1,
    "pageNumber": 0,
    "pageSize": 10,
    "isLast": true,
    "isFirst": true
  }
}

You should see the grievance you created in the list!

✅ PASS CRITERIA: Status 200, grievance appears in list
```

---

### **SECTION 3: SEARCH & FILTERING (5 minutes)**

#### Step 3.1: Full-Text Search
```
Endpoint: GET /search
Folder: Search > "Full-text Search"

Query Parameters:
- q: "water"  (or any keyword from your grievances) ← NOTE: Use 'q' not 'query'
- page: 0
- size: 10

Instructions:
1. Click the endpoint
2. Change "q" to a word from your grievance (e.g., "water", "issue")
3. Click SEND
4. Status should be: 200 OK

Response shows grievances matching your search term.

✅ PASS CRITERIA: Status 200, grievances with matching text returned
```

#### Step 3.2: Filtered Search (Status + Priority)
```
Endpoint: GET /search
Folder: Search > "Filtered Search"

Query Parameters:
- q: "water"  ← Use 'q' parameter
- status: "IN_PROGRESS"
- priority: "HIGH"
- page: 0
- size: 10

Instructions:
1. Update query parameter values to match your test
2. Click SEND
3. Status should be: 200 OK

Response shows only grievances matching ALL filters.

✅ PASS CRITERIA: Status 200, filtered results returned
```

---

### **SECTION 4: ADMIN DASHBOARD (5 minutes)**

#### Step 4.1: View Dashboard Statistics
```
Endpoint: GET /admin/dashboard
Folder: Admin > "Dashboard Stats"

Instructions:
1. Click the endpoint (no parameters needed)
2. Click SEND
3. Status should be: 200 OK

Response shows:
{
  "status": "success",
  "data": {
    "totalGrievances": 5,
    "openGrievances": 2,
    "closedGrievances": 3,
    "inProgressGrievances": 1,
    "avgResolutionTime": 1.5,
    "slaCompliance": 95.2,
    "byPriority": {...},
    "byStatus": {...}
  }
}

✅ PASS CRITERIA: Status 200, statistics displayed
```

#### Step 4.2: Export Grievances as CSV
```
Endpoint: GET /admin/export/csv
Folder: Admin > "Export CSV"

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK
4. Response will be a CSV file that downloads

View the downloaded file to see:
- All grievance data in CSV format
- Columns: ID, Title, Status, Priority, etc.

✅ PASS CRITERIA: Status 200, CSV file downloaded
```

#### Step 4.3: Export Grievances as Excel
```
Endpoint: GET /admin/export/excel
Folder: Admin > "Export Excel"

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK
4. Excel workbook downloads

View the Excel file to see formatted data with multiple columns.

✅ PASS CRITERIA: Status 200, Excel file downloaded
```

#### Step 4.4: Create New Category (Admin Only)
```
Endpoint: POST /admin/categories
Folder: Admin > "Create Category"

Body:
{
  "name": "Electricity Supply",
  "description": "Issues related to power supply",
  "slaHours": 12
}

Instructions:
1. Click the endpoint
2. Modify name/description
3. Click SEND
4. Status should be: 201 Created

Response includes:
- Category ID
- All your details

✅ PASS CRITERIA: Status 201, new category created
```

---

### **SECTION 5: NOTIFICATIONS (3 minutes)**

#### Step 5.1: List All Notifications
```
Endpoint: GET /notifications
Folder: Notifications > "List Notifications"

Query Parameters:
- page: 0
- size: 10

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK

Response shows notifications (triggered by your actions above):
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "...",
        "type": "GRIEVANCE_SUBMITTED",
        "title": "Your grievance was submitted",
        "isRead": false,
        ...
      }
    ]
  }
}

You should see notifications for:
- Grievance submitted
- Status updated
- Comment added

✅ PASS CRITERIA: Status 200, notifications list returned
```

#### Step 5.2: Mark Notification as Read
```
Endpoint: PATCH /notifications/{{notificationId}}/read
Folder: Notifications > "Mark as Read"

Instructions:
1. From Step 5.1, copy a notification ID
2. Paste it into "notificationId" variable: Environment > AIGRS-Environment-Dev
3. Click the endpoint
4. Click SEND
5. Status should be: 200 OK

Verify the notification "isRead" changed to true.

✅ PASS CRITERIA: Status 200, notification marked as read
```

#### Step 5.3: Mark All Notifications as Read
```
Endpoint: PATCH /notifications/mark-all-read
Folder: Notifications > "Mark All as Read"

Instructions:
1. Click the endpoint
2. Click SEND
3. Status should be: 200 OK or 204 No Content

Next time you list notifications, all should have "isRead": true.

✅ PASS CRITERIA: Status 200/204, all marked as read
```

---

## **QUICK TEST CHECKLIST**

Copy this and check off as you complete each section:

```
AUTHENTICATION:
☐ Register User (Status 201)
☐ Save Token to Environment
☐ Login User (Status 200)

GRIEVANCES:
☐ Get Categories (Save ID)
☐ Get Departments (Save ID)
☐ Submit Grievance (Status 201, Save ID)
☐ View Grievance (Status 200)
☐ Add Comment (Status 201)
☐ Update Status (Status 200/202)
☐ Rate Grievance (Status 201)
☐ List Grievances (Status 200)

SEARCH:
☐ Full-text Search (Status 200)
☐ Filtered Search (Status 200)

ADMIN:
☐ Dashboard Stats (Status 200)
☐ Export CSV (Status 200, file downloaded)
☐ Export Excel (Status 200, file downloaded)
☐ Create Category (Status 201)

NOTIFICATIONS:
☐ List Notifications (Status 200)
☐ Mark as Read (Status 200)
☐ Mark All as Read (Status 200/204)

TOTAL: 19 Test Cases
Pass Criteria: All return expected status codes
```

---

## **TROUBLESHOOTING DURING TESTING**

| Error | Solution |
|-------|----------|
| 401 Unauthorized | Token not set. Go to Environment and paste fresh accessToken |
| 404 Not Found | Resource ID incorrect. Re-save ID to environment |
| Connection Refused | Backend not running. Run `docker-compose up -d` |
| 429 Rate Limited | Wait 60 seconds. Rate limit is 100 requests/minute |
| 400 Bad Request | Body JSON syntax error. Check formatting |
| 403 Forbidden | Role not permitted. Register as ADMIN for admin endpoints |

---

## **NEXT: Automated Testing Script**

Once manual testing is complete, you can Run the automated PowerShell script:

```powershell
cd d:\stonepro
.\API_TESTS.ps1 -BaseUrl "http://localhost:8080/api/v1"
```

This will:
- ✅ Run all 14+ tests automatically
- ✅ Generate test report: `API_TEST_REPORT.txt`
- ✅ Show pass/fail for each endpoint

---

**Expected Result**: All tests pass with 95%+ success rate ✅

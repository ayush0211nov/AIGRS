# AIGRS - AI Grievance Redressal System Backend

Production-grade Spring Boot 3.x multi-tenant backend.

## Technology Versions

| Dependency | Version |
|---|---|
| Spring Boot | 3.5.13 |
| Java | 17 |
| JJWT | 0.13.0 (modular: api + impl + jackson) |
| springdoc-openapi | 2.8.16 (stable for Boot 3.x) |
| Firebase Admin SDK | 9.8.0 |
| Lombok | managed by Boot parent |
| PostgreSQL driver | managed by Boot parent |
| Thumbnailator | 0.4.20 |
| Apache POI | 5.2.5 |
| AWS SDK S3 | 2.25.0 |

> [!IMPORTANT]
> Using springdoc 2.8.16 instead of 3.0.2 — the 3.x line has breaking API changes and limited community adoption. 2.8.x is the proven stable choice for Spring Boot 3.5.x.

---

## User Review Required

> [!IMPORTANT]
> **File Storage: AWS S3 vs Cloudinary** — The spec mentions both. I'll implement with **AWS S3** as the primary provider with a `FileStorageService` interface so Cloudinary can be swapped in later. Confirm this is acceptable.

> [!IMPORTANT]
> **SMS Provider** — The spec mentions Twilio or MSG91. I'll implement `SmsService` as an interface with a **Twilio** implementation. The actual sending will be behind a feature flag (`app.sms.enabled=false` by default in dev) so the app runs without credentials.

> [!WARNING]
> **AI Service Dependency** — `POST http://aigrs-ai-service:8000/api/v1/analyze` is an external microservice. The `GrievanceService` will call it via `RestTemplate` with a **circuit breaker pattern** (fallback: skip AI enrichment, set defaults). This prevents the entire grievance submission from failing if the AI service is down.

> [!NOTE]
> **OTP via SMS** — In dev profile, OTPs will be logged to console instead of actually sent. Phone-based login assumes phone number is a unique identifier per org.

---

## Proposed Changes

The entire project will be created from scratch in `d:\stonepro` with the following package structure:

```
d:\stonepro\
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
└── src/main/
    ├── java/com/aigrs/backend/
    │   ├── AigrsApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── RedisConfig.java
    │   │   ├── FirebaseConfig.java
    │   │   ├── S3Config.java
    │   │   ├── OpenApiConfig.java
    │   │   ├── WebConfig.java
    │   │   └── RestTemplateConfig.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── GrievanceController.java
    │   │   ├── AdminController.java
    │   │   ├── StaffController.java
    │   │   ├── FileController.java
    │   │   ├── NotificationController.java
    │   │   ├── SearchController.java
    │   │   └── OrganizationController.java
    │   ├── service/
    │   │   ├── AuthService.java
    │   │   ├── GrievanceService.java
    │   │   ├── NotificationService.java
    │   │   ├── FileStorageService.java
    │   │   ├── S3FileStorageService.java
    │   │   ├── SmsService.java
    │   │   ├── TwilioSmsService.java
    │   │   ├── EmailService.java
    │   │   ├── DashboardService.java
    │   │   ├── StaffService.java
    │   │   ├── OrganizationService.java
    │   │   ├── SearchService.java
    │   │   ├── AiAnalysisService.java
    │   │   ├── SlaService.java
    │   │   ├── ReportExportService.java
    │   │   └── CustomUserDetailsService.java
    │   ├── repository/ (14 JPA repos)
    │   ├── entity/ (12 entities + BaseEntity)
    │   ├── dto/
    │   │   ├── request/ (RegisterRequest, LoginRequest, GrievanceRequest, etc.)
    │   │   └── response/ (ApiResponse, AuthResponse, GrievanceResponse, DashboardStats, etc.)
    │   ├── enums/ (UserRole, GrievanceStatus, Priority, NotificationType)
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   ├── UnauthorizedException.java
    │   │   ├── ForbiddenException.java
    │   │   ├── ResourceNotFoundException.java
    │   │   ├── DuplicateResourceException.java
    │   │   └── BadRequestException.java
    │   ├── security/
    │   │   ├── JwtUtil.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── CustomUserDetailsService.java
    │   ├── scheduler/
    │   │   ├── SlaScheduler.java
    │   │   └── DigestScheduler.java
    │   └── util/
    │       ├── GrievanceIdGenerator.java
    │       └── TenantContext.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── templates/mail/ (Thymeleaf email templates)
```

---

### Phase 1: Project Setup + Entities + Enums

#### [NEW] pom.xml
- Spring Boot 3.5.13 parent, Java 17
- All required dependencies (web, JPA, security, validation, PostgreSQL, Redis, JJWT 0.13.0, springdoc 2.8.16, mail, Firebase 9.8.0, Thumbnailator, Lombok, POI, AWS S3)

#### [NEW] Entity classes (13 files)
- `BaseEntity` with `id` (UUID), `createdAt`, `updatedAt`, `orgId` — all entities extend this
- `Organization`: name, code, logoUrl, address, contactEmail, contactPhone, isActive, slaConfig (JSON), escalationConfig (JSON)
- `User`: name, email, phone, passwordHash, role (UserRole enum), orgId, fcmToken, isActive, avatarUrl, departmentId
- `Grievance`: title, description, trackingId, status, priority, categoryId, departmentId, submitterId, assignedStaffId, location, latitude, longitude, slaDeadline, resolvedAt, deletedAt, isDuplicate, duplicateOfId, aiSentiment, estimatedHours, attachmentIds
- `Category`: name, description, slaHours, departmentId, isActive
- `Department`: name, description, headUserId, isActive
- `StaffAssignment`: grievanceId, staffId, assignedBy, assignedAt, unassignedAt, isActive
- `StatusHistory`: grievanceId, fromStatus, toStatus, changedBy, remarks
- `Comment`: grievanceId, userId, content, isInternal, attachmentIds
- `Rating`: grievanceId, userId, score (1-5), feedback
- `Notification`: userId, type (NotificationType), title, body, data (JSON), isRead, grievanceId
- `SlaEvent`: grievanceId, eventType (STARTED, WARNING, BREACHED, PAUSED, RESUMED), pausedAt, resumedAt, pausedDurationMinutes
- `Escalation`: grievanceId, escalatedFrom, escalatedTo, reason, level
- `FileEntity`: originalName, storedName, url, thumbnailUrl, contentType, sizeBytes, uploadedBy, grievanceId

#### [NEW] Enum classes (4 files)
- `UserRole`, `GrievanceStatus`, `Priority`, `NotificationType`

---

### Phase 2: Configuration + Security Skeleton

#### [NEW] application.yml / application-dev.yml / application-prod.yml
- PostgreSQL datasource, Redis, JWT secrets, Firebase path, S3/Cloudinary, CORS origins, AI service URL
- Dev profile: `ddl-auto: update`, relaxed CORS, SMS disabled

#### [NEW] SecurityConfig.java
- Whitelist auth endpoints, swagger, actuator
- JWT filter registration
- Role-based access rules
- CORS configuration from properties
- Stateless session management

#### [NEW] JwtUtil.java
- HS256 signing with configurable secret
- Payload: sub (userId), role, org_id, iat, exp
- Access token 24h, refresh token 30d
- Token validation + claims extraction

#### [NEW] JwtAuthenticationFilter.java
- Extract Bearer token from Authorization header
- Validate token, check Redis blacklist
- Set SecurityContext with userId, role, orgId
- Populate TenantContext ThreadLocal

#### [NEW] RateLimitFilter.java
- Redis INCR + EXPIRE pattern: `rate:{ip}` key, 100 req/min
- Return 429 Too Many Requests on breach

#### [NEW] TenantContext.java
- ThreadLocal<Long> storing current org_id from JWT
- Static get/set/clear methods

#### [NEW] RedisConfig.java, FirebaseConfig.java, S3Config.java, OpenApiConfig.java, WebConfig.java, RestTemplateConfig.java

---

### Phase 3: Authentication APIs

#### [NEW] AuthController.java
- `POST /api/v1/auth/register` — register with role (citizen default)
- `POST /api/v1/auth/send-otp` — generate 6-digit OTP, store in Redis (10min TTL)
- `POST /api/v1/auth/verify-otp` — verify and return JWT pair
- `POST /api/v1/auth/login` — phone + password login
- `POST /api/v1/auth/refresh-token` — rotate access token
- `POST /api/v1/auth/logout` — blacklist token in Redis
- `POST /api/v1/auth/forgot-password` — send reset OTP
- `PUT /api/v1/auth/reset-password` — validate OTP + set new password

#### [NEW] AuthService.java
- BCrypt password hashing
- OTP generation + Redis storage
- JWT minting via JwtUtil

#### [NEW] ApiResponse.java (generic envelope)
```java
{ "status": "success"|"error", "message": "...", "data": T, 
  "error_code": "...", "timestamp": "ISO8601" }
```

#### [NEW] GlobalExceptionHandler.java
- Catches all custom exceptions + Spring validation errors
- Maps to proper HTTP status codes with standard envelope

---

### Phase 4: Grievance CRUD + AI Integration

#### [NEW] GrievanceController.java — all CRUD + workflow endpoints
#### [NEW] GrievanceService.java
- Full 11-step submission workflow (AI call with fallback)
- GrievanceIdGenerator: `{ORG_CODE}-{YEAR}-{6-digit-seq}`
- Edit guard (citizen only, within 1 hour)
- Reopen guard (within 48 hours of resolution)
- Soft delete (admin only)

#### [NEW] AiAnalysisService.java
- RestTemplate POST to AI service
- Fallback on timeout/error: default category, MEDIUM priority, no duplicate

---

### Phase 5: File Upload

#### [NEW] FileController.java — upload, get, delete
#### [NEW] FileStorageService.java (interface)
#### [NEW] S3FileStorageService.java
- Multipart upload to S3
- Image compression via Thumbnailator (>2MB → compress)
- Signed URL generation (24h expiry)
- Content-type validation (jpeg, png, mp4)
- 50MB max size

---

### Phase 6: Admin Dashboard

#### [NEW] AdminController.java — all admin endpoints
#### [NEW] DashboardService.java
- Stats query with Redis caching (5min TTL, key: `stats:{orgId}`)
- Chart data queries (daily trends, category distribution, priority breakdown, dept performance)
- Heatmap aggregation from lat/lng

#### [NEW] ReportExportService.java
- CSV export via standard Java IO
- Excel export via Apache POI
- PDF export via iText or simple HTML-to-PDF

---

### Phase 7: Staff Endpoints

#### [NEW] StaffController.java — assigned grievances, status updates, proof upload, internal comments, extension requests
#### [NEW] StaffService.java

---

### Phase 8: Notification System

#### [NEW] NotificationController.java — list, mark read, preferences
#### [NEW] NotificationService.java
- Firebase FCM push notifications
- Email via Spring Mail + Thymeleaf
- SMS via Twilio REST
- Event-driven triggers on status change, assignment, SLA events, comments, ratings

---

### Phase 9: SLA Engine

#### [NEW] SlaScheduler.java
- `@Scheduled(fixedRate=60000)` — check breaches
- `@Scheduled(fixedRate=300000)` — check warnings (75% elapsed)
- `@Scheduled(cron="0 0 9 * * *")` — daily digest

#### [NEW] SlaService.java
- Pause/resume logic with ON_HOLD tracking
- Business hours calculation (Mon-Sat 9AM-6PM)
- Holiday exclusion from SLA config

---

### Phase 10: Organization Management

#### [NEW] OrganizationController.java — SUPER_ADMIN CRUD
#### [NEW] OrganizationService.java — create org + admin user, suspend, usage stats

---

### Phase 11: Search

#### [NEW] SearchController.java
- Full-text search with PostgreSQL tsvector
- Combined filters (status, category, priority, date range)
- Pagination

---

### Phase 12: Docker + Swagger

#### [NEW] Dockerfile — multi-stage build (maven:3.9 → openjdk:17-slim)
#### [NEW] docker-compose.yml — app + postgres + redis services
#### [NEW] .dockerignore

Swagger annotations (`@Tag`, `@Operation`, `@ApiResponse`) on all controllers.

---

## Open Questions

> [!IMPORTANT]
> 1. **S3 vs Cloudinary**: I'll default to S3 with an abstraction layer. OK?
> 2. **SMS Provider**: Twilio as default. OK?
> 3. **PDF Export Library**: Apache POI handles Excel. For PDF, should I use **iText** (AGPL license) or **OpenPDF** (LGPL, iText fork)? I'll default to **OpenPDF** for permissive licensing.
> 4. **Database migrations**: Should I include **Flyway** or **Liquibase** for schema migrations, or rely on JPA `ddl-auto` for now?

## Verification Plan

### Automated Tests
1. `mvn compile` — verify all code compiles cleanly
2. `mvn test` — run unit tests (service layer mocks)
3. Docker build: `docker build -t aigrs-backend .`
4. Docker compose: `docker-compose up` — verify app starts with Postgres + Redis

### Manual Verification
- Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- Auth flow testable via Swagger or curl
- All endpoints documented with request/response examples

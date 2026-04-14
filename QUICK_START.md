# AIGRS Backend - Quick Start Guide

## 📋 Project Overview

AIGRS (AI Grievance Redressal System) is a production-grade, multi-tenant backend for managing citizen grievances with AI-powered sentiment analysis, SLA tracking, and advanced workflow management.

**Tech Stack**: Spring Boot 3.5.13 | Java 17 | PostgreSQL 16 | Redis 7 | JWT Auth

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)
```bash
# Start all services (app + postgres + redis)
docker-compose up -d

# App available at: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health: curl http://localhost:8080/actuator/health
```

### Option 2: Local Development
```bash
# Prerequisites
# - Java 17
# - PostgreSQL 16
# - Redis 7
# - Maven 3.9+

# Step 1: Create database
createdb aigrs
createuser aigrs_user -P  # password: aigrs_pass

# Step 2: Set environment variables (optional for dev)
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost
export REDIS_HOST=localhost

# Step 3: Run the application
mvn spring-boot:run

# Step 4: Access
# API: http://localhost:8080/api/v1/...
# Swagger: http://localhost:8080/swagger-ui.html
```

---

## 🔑 Key Endpoints

### Authentication
```bash
# Register
POST /api/v1/auth/register
{
  "name": "John Doe",
  "phone": "9876543210",
  "email": "john@example.com",
  "password": "securepass123",
  "orgId": "uuid-here"
}

# Login
POST /api/v1/auth/login
{
  "phone": "9876543210",
  "password": "securepass123",
  "orgId": "uuid-here"
}

# Send OTP
POST /api/v1/auth/send-otp
{
  "phone": "9876543210"
}

# Verify OTP
POST /api/v1/auth/verify-otp
{
  "phone": "9876543210",
  "otp": "123456"
}
```

### Grievances
```bash
# Submit grievance
POST /api/v1/grievances
{
  "title": "Complaint Title",
  "description": "Details here",
  "location": "Address",
  "categoryId": "uuid",
  "departmentId": "uuid"
}

# List grievances
GET /api/v1/grievances?page=0&size=20

# Get single grievance
GET /api/v1/grievances/{id}

# Update status
PUT /api/v1/grievances/{id}/status
{
  "status": "IN_PROGRESS"
}
```

### Files
```bash
# Upload file
POST /api/v1/files/upload
- multipart form-data
- field: "file"
- field: "grievanceId" (optional)

# Download file
GET /api/v1/files/{fileId}

# Delete file
DELETE /api/v1/files/{fileId}
```

### Search
```bash
# Search grievances
GET /api/v1/search?query=...&status=...&priority=...&page=0&size=20
```

### Admin
```bash
# Dashboard stats
GET /api/v1/admin/dashboard

# Export report
GET /api/v1/admin/export?format=csv|excel|pdf

# List all staff
GET /api/v1/admin/staff
```

---

## 🔒 Authentication

### JWT Flow
1. **Register** → receives access + refresh token
2. **Include token** in every request: `Authorization: Bearer {token}`
3. **Token expires** in 24 hours
4. **Refresh token** → get new access token (valid 30 days)
5. **Logout** → blacklist token in Redis

### Roles & Permissions
```
CITIZEN      - Can submit grievances, rate, comment
STAFF        - Can handle grievances, add proofs
MANAGER      - Can supervise staff, escalate
ADMIN        - Can manage org, view dashboards
SUPER_ADMIN  - Can create/manage organizations
```

---

## 📁 Project Structure

```
d:\stonepro/
├── src/main/java/com/aigrs/backend/
│   ├── AigrsApplication.java                 # Entry point
│   ├── config/                                # 8 config classes
│   ├── controller/                            # 8 REST controllers
│   ├── service/                               # 16 business logic services
│   ├── repository/                            # 13 JPA repositories
│   ├── entity/                                # 14 JPA entities
│   ├── dto/request/                           # 11 request DTOs
│   ├── dto/response/                          # 13 response DTOs
│   ├── enums/                                 # 5 enums
│   ├── exception/                             # 6 exception classes
│   ├── security/                              # JWT + auth logic
│   ├── scheduler/                             # Background jobs
│   └── util/                                  # Utilities (TenantContext, etc)
├── src/main/resources/
│   ├── application.yml                        # Base config
│   ├── application-dev.yml                    # Dev profile
│   ├── application-prod.yml                   # Prod profile
│   └── templates/mail/                        # 5 email templates
├── src/test/java/com/aigrs/backend/
│   └── service/                               # 3 test suites
├── pom.xml                                    # Maven dependencies
├── Dockerfile                                 # Multi-stage build
├── docker-compose.yml                         # Orchestration
├── .dockerignore                              # Docker context filter
├── implementation_plan.md                     # Original spec
├── COMPLETION_SUMMARY.md                      # Full documentation
└── QUICK_START.md                             # This file
```

---

## ⚙️ Configuration

### Environment Variables (can be set instead of properties files)

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=aigrs
DB_USERNAME=aigrs_user
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=use-256-bit-key-min-43-chars-long
JWT_ACCESS_EXPIRY=86400  # seconds (24 hours)
JWT_REFRESH_EXPIRY=2592000  # seconds (30 days)

# AWS S3
AWS_S3_BUCKET=aigrs-files
AWS_S3_REGION=ap-south-1
AWS_ACCESS_KEY=your-key
AWS_SECRET_KEY=your-secret

# Firebase
FIREBASE_CREDENTIALS_PATH=/path/to/firebase-key.json

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=app-password

# SMS (Optional)
SMS_ENABLED=false
TWILIO_ACCOUNT_SID=your-sid
TWILIO_AUTH_TOKEN=your-token
TWILIO_PHONE_NUMBER=+1234567890

# AI Service
AI_SERVICE_URL=http://localhost:8000

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:5173
```

### Profiles

```bash
# Development (default)
export SPRING_PROFILES_ACTIVE=dev
# Features: DDL auto update, debug logging, SMS disabled

# Production
export SPRING_PROFILES_ACTIVE=prod
# Features: DDL auto validate, security hardening, SMS enabled
```

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=AuthServiceTest
```

### Test Coverage
```bash
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

### Test Files Location
- `src/test/java/com/aigrs/backend/service/`
  - AuthServiceTest
  - StaffServiceTest
  - EmailServiceTest

---

## 📊 Database Schema

### Main Tables
- `organizations` - Tenant data
- `users` - All users (citizen, staff, admin)
- `grievances` - Grievance records with AI sentiment
- `categories` - Grievance types
- `departments` - Org departments
- `staff_assignments` - Grievance→Staff mapping
- `status_history` - Audit trail
- `comments` - Public/internal notes
- `ratings` - Citizen feedback (1-5 stars)
- `notifications` - Push/email/SMS queue
- `sla_events` - SLA state tracking
- `escalations` - Escalation records
- `files` - Uploaded attachments

### Key Features
- ✅ All tables include `created_at`, `updated_at`, `org_id`
- ✅ Soft delete support (grievances have `deleted_at`)
- ✅ UUID primary keys (globally unique)
- ✅ Proper indexes on foreign keys

---

## 🔔 Event-Driven Features

### Automatic Notifications Triggered By:
1. Grievance submitted → Citizen + (assigned) Staff
2. Grievance assigned → Staff member
3. Status changed → Citizen
4. Comment added → Relevant parties
5. SLA warning (75% time) → Staff
6. SLA breached → Manager + Admin
7. Rating received → Staff

### Notification Channels
- **Email**: Thymeleaf templates
- **SMS**: Twilio integration (feature-flagged)
- **Push**: Firebase Cloud Messaging

---

## 🚨 Common Issues & Solutions

### Issue: "Database connection refused"
```
Solution:
1. Check PostgreSQL is running: psql -l
2. Verify DB credentials in application.yml
3. Create database: createdb aigrs
```

### Issue: "Redis connection timeout"
```
Solution:
1. Check Redis is running: redis-cli ping
2. Verify host/port in application.yml
3. Fallback: Rate limiting will be disabled
```

### Issue: "JWT secret too short"
```
Solution:
1. Generate 256-bit key: openssl rand -base64 32
2. Set JWT_SECRET environment variable
3. Must be at least 43 characters
```

### Issue: "File upload fails"
```
Solution:
1. Check S3 credentials and bucket access
2. File size limit: 50MB
3. Allowed types: JPEG, PNG, MP4
```

---

## 📈 Performance Tuning

### Redis Caching
- Dashboard stats: 5-minute TTL
- Rate limit keys: 1-minute TTL
- OTP: 10-minute TTL

### Database Optimization
- Connection pool: 20 max (tunable)
- Batch operations: 20-size batches
- Lazy loading: @ManyToOne FetchType.LAZY

### Image Compression
- Automatic for images > 2MB
- Compression ratio: 60%-80%
- Uses Thumbnailator library

---

## 🔐 Security Best Practices

1. **JWT Secrets**: Use strong 256-bit keys
2. **Database Passwords**: Change defaults in production
3. **Email Credentials**: Use app-specific passwords (Gmail)
4. **AWS Keys**: Use IAM roles (not root credentials)
5. **HTTPS**: Always use in production
6. **CORS**: Whitelist only known origins
7. **Rate Limiting**: Monitor and adjust per environment

---

## 📚 Additional Resources

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **JJWT Docs**: https://github.com/jwtk/jjwt
- **PostgreSQL Docs**: https://www.postgresql.org/docs/
- **Redis Docs**: https://redis.io/documentation

---

## 💡 Development Workflow

### Adding a New Feature

1. **Create Entity** (if needed)
   ```java
   @Entity
   public class MyEntity extends BaseEntity {
       // fields
   }
   ```

2. **Create Repository**
   ```java
   public interface MyEntityRepository extends JpaRepository<MyEntity, UUID> {
       // custom queries
   }
   ```

3. **Create Service**
   ```java
   @Service
   @RequiredArgsConstructor
   public class MyService {
       private final MyEntityRepository repository;
       // business logic
   }
   ```

4. **Create Controller**
   ```java
   @RestController
   @RequestMapping("/api/v1/my-entities")
   @Tag(name = "Feature", description = "...")
   public class MyController {
       // endpoints
   }
   ```

5. **Create DTOs** (Request/Response)
6. **Add Tests**: AuthServiceTest, StaffServiceTest pattern
7. **Update application.yml** (if new configs needed)
8. **Document in Swagger**: @Operation, @ApiResponse

---

## 🎯 Next Steps

- [ ] Deploy to staging environment
- [ ] Run integration tests with TestContainers
- [ ] Load testing (JMeter/Gatling)
- [ ] Security audit (OWASP Top 10)
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure log aggregation (ELK)
- [ ] Automated backup strategy

---

**Last Updated**: April 12, 2026  
**Version**: 1.0.0 (Release Candidate)  
**Status**: ✅ Ready for Deployment

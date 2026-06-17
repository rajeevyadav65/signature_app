# 📄 Document Signature App

> Enterprise-grade DocuSign-like platform | Java 17 · Spring Boot 3.2 · MySQL · PDFBox · JWT

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5 |
| Security | Spring Security 6, JWT (jjwt 0.12.5) |
| Database | MySQL 8+ (user: root / pass: 12345) |
| ORM | Spring Data JPA, Hibernate 6 |
| PDF Engine | Apache PDFBox 3.0.2 |
| Email | JavaMailSender (SMTP) |
| Build | Maven 3.8+ |

---

## 📁 Project Structure

```
signature-app/
├── pom.xml
├── demo-server.js                        ← Node.js API demo (no Java needed)
├── src/
│   ├── main/
│   │   ├── java/com/signatureapp/
│   │   │   ├── SignatureAppApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java       ← JWT + CORS + route rules
│   │   │   │   └── FileStorageConfig.java    ← upload dirs + static serving
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java       ← /api/auth/**
│   │   │   │   ├── DocumentController.java   ← /api/docs/**
│   │   │   │   ├── SignatureController.java  ← /api/signatures/**
│   │   │   │   ├── AuditController.java      ← /api/audit/**
│   │   │   │   └── PublicController.java     ← /api/public/** (no auth)
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java          ← register, login, profile
│   │   │   │   ├── DocumentService.java      ← upload, list, signing links
│   │   │   │   ├── SignatureService.java     ← place, sign, reject, finalize
│   │   │   │   ├── PdfService.java           ← PDFBox embed + watermark
│   │   │   │   ├── FileStorageService.java   ← save/delete/URL generation
│   │   │   │   ├── AuditLogService.java      ← async audit trail
│   │   │   │   └── EmailService.java         ← SMTP notifications
│   │   │   ├── model/
│   │   │   │   ├── User.java
│   │   │   │   ├── Document.java
│   │   │   │   ├── Signature.java
│   │   │   │   └── AuditLog.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── DocumentRepository.java
│   │   │   │   ├── SignatureRepository.java
│   │   │   │   └── AuditLogRepository.java
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java          ← generic wrapper
│   │   │   │   ├── AuthDto.java
│   │   │   │   ├── DocumentDto.java
│   │   │   │   ├── SignatureDto.java
│   │   │   │   └── AuditLogDto.java
│   │   │   ├── security/
│   │   │   │   ├── JwtUtils.java             ← generate + validate tokens
│   │   │   │   ├── JwtAuthFilter.java        ← OncePerRequestFilter
│   │   │   │   ├── UserDetailsImpl.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── BadRequestException.java
│   │   │       └── UnauthorizedException.java
│   │   └── resources/
│   │       └── application.properties        ← DB, JWT, mail, file config
│   └── test/
│       └── java/com/signatureapp/
│           └── SignatureAppApplicationTests.java
└── uploads/                                  ← auto-created on startup
    └── signed/                               ← signed PDFs go here
```

---

## ⚡ Quick Start

### Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |

### 1 — MySQL Setup

```sql
-- Run in MySQL as root:
CREATE DATABASE IF NOT EXISTS signature_db;
-- application.properties already uses root/12345
```

### 2 — Clone & Build

```bash
cd signature-app
mvn clean install -DskipTests
```

### 3 — Run

```bash
mvn spring-boot:run
# OR run the fat JAR:
java -jar target/signature-app-1.0.0.jar
```

Server starts at: **http://localhost:8080**

---

## 🔌 API Reference

Base URL: `http://localhost:8080`

All protected routes require:
```
Authorization: Bearer <JWT_TOKEN>
```

---

### 🔐 Auth APIs

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "Rajeev Kumar",
  "email": "rajeev@example.com",
  "password": "secret123"
}
```
**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "userId": 1,
    "name": "Rajeev Kumar",
    "email": "rajeev@example.com",
    "role": "USER",
    "expiresIn": 86400000
  }
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "rajeev@example.com",
  "password": "secret123"
}
```

#### Profile
```http
GET /api/auth/me
Authorization: Bearer <token>
```

---

### 📄 Document APIs

#### Upload PDF
```http
POST /api/docs/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=@contract.pdf
title=Vendor Agreement Q3 2025
```

#### List My Documents
```http
GET /api/docs
GET /api/docs?status=PENDING
GET /api/docs?status=SIGNED
Authorization: Bearer <token>
```

#### Get Document Detail
```http
GET /api/docs/{id}
Authorization: Bearer <token>
```

#### Dashboard Stats
```http
GET /api/docs/dashboard
Authorization: Bearer <token>
```
**Response:**
```json
{
  "data": {
    "totalDocuments": 5,
    "pendingDocuments": 2,
    "signedDocuments": 3,
    "rejectedDocuments": 0,
    "recentDocuments": [...]
  }
}
```

#### Generate Signing Link
```http
POST /api/docs/{id}/signing-link
Authorization: Bearer <token>
Content-Type: application/json

{
  "expiryHours": 72,
  "signerEmail": "vendor@company.com",
  "signerName": "John Vendor"
}
```
**Response:**
```json
{
  "data": {
    "signingUrl": "http://localhost:8080/api/public/sign/abc123token",
    "token": "abc123token",
    "expiresAt": "2025-09-15T10:00:00"
  }
}
```

#### Delete Document
```http
DELETE /api/docs/{id}
Authorization: Bearer <token>
```

---

### ✍️ Signature APIs

#### Place Signature Field (Owner defines WHERE signers sign)
```http
POST /api/signatures
Authorization: Bearer <token>
Content-Type: application/json

{
  "documentId": 1,
  "signerName": "John Vendor",
  "signerEmail": "vendor@company.com",
  "xCoordinate": 100.0,
  "yCoordinate": 650.0,
  "pageNumber": 1,
  "width": 200.0,
  "height": 60.0
}
```

#### List Signatures for a Document
```http
GET /api/signatures/{docId}
Authorization: Bearer <token>
```

#### Sign Document (Signer performs the action)
```http
POST /api/signatures/{signatureId}/sign
Authorization: Bearer <token>
Content-Type: application/json

{
  "signatureData": "data:image/png;base64,iVBORw0KGgo...",
  "signatureType": "DRAWN"
}
```

#### Reject Signature
```http
POST /api/signatures/{signatureId}/reject
Authorization: Bearer <token>
Content-Type: application/json

{
  "rejectionReason": "Terms need revision before I can sign"
}
```

#### Generate Signed PDF (Manual trigger)
```http
POST /api/signatures/finalize
Authorization: Bearer <token>
Content-Type: application/json

{
  "documentId": 1
}
```

#### Sign via Public Link (No auth required)
```http
POST /api/signatures/public/{token}/{signatureId}/sign
Content-Type: application/json

{
  "signatureData": "data:image/png;base64,iVBORw0KGgo...",
  "signatureType": "DRAWN",
  "signerName": "John Vendor",
  "signerEmail": "vendor@company.com"
}
```

---

### 🕵️ Audit APIs

#### Get Audit Trail
```http
GET /api/audit/{docId}
Authorization: Bearer <token>
```
**Response:**
```json
{
  "data": {
    "documentId": 1,
    "documentTitle": "Vendor Agreement",
    "totalLogs": 5,
    "logs": [
      {
        "id": 1,
        "actorName": "Rajeev Kumar",
        "actorEmail": "rajeev@example.com",
        "action": "DOCUMENT_UPLOADED",
        "details": "Document uploaded: Vendor Agreement",
        "ipAddress": "192.168.1.1",
        "timestamp": "2025-09-12T09:15:00"
      },
      {
        "id": 2,
        "action": "SIGNING_LINK_GENERATED",
        "timestamp": "2025-09-12T09:20:00"
      },
      {
        "id": 3,
        "actorName": "John Vendor",
        "action": "SIGNATURE_SIGNED",
        "timestamp": "2025-09-12T10:05:00"
      }
    ]
  }
}
```

---

### 🌐 Public APIs (No Auth)

```http
GET  /api/public/sign/{token}    ← Access document via signing link
GET  /api/public/health          ← Health check
```

---

## 📬 Postman Collection (Quick Test Flow)

Import this sequence in Postman:

```
1. POST /api/auth/register      → save {{token}}
2. POST /api/docs/upload        → save {{docId}}   (multipart, attach PDF)
3. POST /api/signatures         → save {{sigId}}   (place signature field)
4. POST /api/docs/{{docId}}/signing-link
5. POST /api/signatures/{{sigId}}/sign              (sign with base64 image)
6. POST /api/signatures/finalize
7. GET  /api/audit/{{docId}}
```

---

## 🔐 Security Design

```
Client → [JwtAuthFilter] → Spring Security → Controller → Service → DB
                ↓
         Extracts Bearer token
         Validates JWT signature
         Loads UserDetails
         Sets SecurityContext
```

- **BCrypt** (strength 12) for password hashing
- **HS256 JWT** with 24h expiry
- **Stateless** sessions (no server-side sessions)
- **CORS** configured for all origins (tighten in production)
- Public routes: `/api/auth/**`, `/api/public/**`, `/uploads/**`

---

## 📋 Database Schema (Auto-created by Hibernate)

```
users          → id, name, email, password, role, is_active, created_at
documents      → id, title, original_file_name, stored_file_name, file_path,
                 signed_file_path, file_size, status, owner_id,
                 signing_token, signing_token_expiry
signatures     → id, document_id, signer_id, signer_name, signer_email,
                 x_coordinate, y_coordinate, page_number, width, height,
                 signature_data, signature_type, signer_ip_address,
                 status, rejection_reason, signed_at
audit_logs     → id, document_id, user_id, actor_name, actor_email,
                 action, details, ip_address, user_agent, timestamp
```

---

## 📧 Email Configuration

Update `application.properties`:
```properties
spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-app-password   # Gmail App Password (not account password)
```

Emails sent for:
- ✉️ Signing request (with link)
- ✅ Document fully signed
- ❌ Signature rejected

---

## 🔄 Document Lifecycle

```
UPLOAD → PENDING → [all signatures placed]
                        ↓
              Signer opens link → SIGNS → SIGNED (if all done → PDF generated)
                        ↓
              Signer REJECTS → REJECTED (owner notified)
```

---

## 🛠️ Configuration Reference

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/signature_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=12345

# JWT (change secret in production!)
app.jwt.secret=4a8b3c2d1e5f6a7b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b
app.jwt.expiration-ms=86400000        # 24 hours

# File Upload
app.file.upload-dir=./uploads
spring.servlet.multipart.max-file-size=20MB

# Base URL (for signing links and file URLs)
app.base-url=http://localhost:8080
```

---

## 🚀 Deployment

### Docker (optional)
```bash
# Build fat JAR first
mvn clean package -DskipTests

# Dockerfile (create in root):
FROM eclipse-temurin:17-jre-alpine
COPY target/signature-app-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Railway / Render
Set environment variables:
```
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/signature_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=12345
APP_JWT_SECRET=<strong-random-secret>
APP_BASE_URL=https://your-domain.com
```

---

## 👤 Author

**Rajeev Kumar** — B.Tech CSE, GL Bajaj Group of Institutions (2023–2027)  
Backend Developer | Java · Spring Boot · MySQL · Docker

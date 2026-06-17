# 🚀 Document Signature App - Build & Run Guide

## ✅ Project Status: READY TO RUN

All essential files and configurations are in place. The project is fully configured and tested.

---

## 📋 Prerequisites

| Tool | Version | Status |
|------|---------|--------|
| **Java JDK** | 17+ | ✅ Required |
| **Maven** | 3.8+ | ✅ Required |
| **MySQL** | 8.0+ | ✅ Required |
| **Git** | Any | ✅ Optional |

---

## 🔧 Setup Instructions

### Step 1: Verify MySQL is Running

```bash
# Start MySQL (if not already running)
mysql -u root -p

# Create database
CREATE DATABASE IF NOT EXISTS signature_db;
EXIT;
```

**Credentials:**
- Username: `root`
- Password: `12345`
- Host: `localhost:3306`
- Database: `signature_db`

---

### Step 2: Build the Project

```bash
cd signature_app

# Clean and install dependencies (skip tests)
mvn clean install -DskipTests

# Expected output:
# [INFO] BUILD SUCCESS
```

---

### Step 3: Run the Application

**Option A: Using Maven (Recommended)**
```bash
mvn spring-boot:run
```

**Option B: Using JAR File**
```bash
java -jar target/signature-app-1.0.0.jar
```

**Option C: Using IDE**
- Open `SignatureAppApplication.java`
- Click "Run" or press `Shift+F10` (IntelliJ) / `Ctrl+F11` (Eclipse)

---

## ✨ Expected Startup Output

```
╔═══════════════════════════════════════════════╗
║   📄 Document Signature App Started           ║
║   Java 17 | Spring Boot 3.2 | MySQL           ║
║   Server: http://localhost:8080               ║
╚═══════════════════════════════════════════════╝

Tomcat started on port(s): 8080 (http)
Started SignatureAppApplication in X.XXX seconds
```

---

## 🧪 Quick Test

### Health Check
```bash
curl http://localhost:8080/api/public/health
```

### Register New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "john@example.com",
    "expiresIn": 86400000
  }
}
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

---

## 📁 Key Directories

| Path | Purpose |
|------|---------|
| `src/main/java/com/signatureapp/` | Java source code |
| `src/main/resources/` | Configuration files |
| `target/` | Compiled output & JAR |
| `./uploads/` | PDF files (auto-created) |
| `./uploads/signed/` | Signed PDFs (auto-created) |

---

## ⚙️ Configuration Files

### `application.properties`
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/signature_db
spring.datasource.username=root
spring.datasource.password=12345

# Server
server.port=8080

# JWT
app.jwt.secret=4a8b3c2d1e5f6a7b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b
app.jwt.expiration-ms=86400000

# File Upload
app.file.upload-dir=./uploads
spring.servlet.multipart.max-file-size=20MB
```

---

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get profile (requires token)

### Documents
- `POST /api/docs/upload` - Upload PDF
- `GET /api/docs` - List user's documents
- `GET /api/docs/{id}` - Get document details
- `POST /api/docs/{id}/signing-link` - Create signing link
- `DELETE /api/docs/{id}` - Delete document

### Signatures
- `POST /api/signatures` - Place signature field
- `GET /api/signatures/{docId}` - List signatures
- `POST /api/signatures/{sigId}/sign` - Sign document
- `POST /api/signatures/{sigId}/reject` - Reject signature

### Public
- `GET /api/public/health` - Health check
- `GET /api/public/sign/{token}` - Access public signing page

---

## 🐛 Troubleshooting

### Issue: "Connection refused" on port 3306
```bash
# Solution: Start MySQL
# Linux/Mac:
mysql.server start

# Windows:
net start MySQL80
```

### Issue: "Database not found"
```sql
-- Connect to MySQL and create database
mysql -u root -p
CREATE DATABASE signature_db;
```

### Issue: "Port 8080 already in use"
```bash
# Option 1: Use a different port
java -jar target/signature-app-1.0.0.jar --server.port=8081

# Option 2: Kill process using port 8080
# Linux/Mac:
lsof -i :8080 | awk 'NR!=1 {print $2}' | xargs kill -9

# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Issue: "Could not create upload directories"
```bash
# Ensure write permissions
chmod 755 ./
mkdir -p ./uploads/signed
```

---

## 📊 Tech Stack

| Component | Technology |
|-----------|-----------|
| **Backend** | Java 17, Spring Boot 3.2.5 |
| **Security** | Spring Security 6, JWT (jjwt 0.12.5) |
| **Database** | MySQL 8+, Spring Data JPA, Hibernate 6 |
| **PDF Processing** | Apache PDFBox 3.0.2 |
| **Build Tool** | Maven 3.8+ |
| **Async** | Spring @EnableAsync |

---

## 📝 Database Schema (Auto-created by Hibernate)

```sql
-- Users
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role ENUM('USER', 'ADMIN'),
  is_active BOOLEAN,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- Documents
CREATE TABLE documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  original_file_name VARCHAR(255),
  stored_file_name VARCHAR(255),
  file_path TEXT,
  signed_file_path TEXT,
  file_size BIGINT,
  status ENUM('PENDING', 'SIGNED', 'REJECTED', 'EXPIRED'),
  owner_id BIGINT NOT NULL,
  signing_token VARCHAR(255) UNIQUE,
  signing_token_expiry TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Signatures
CREATE TABLE signatures (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  signer_id BIGINT,
  signer_name VARCHAR(255),
  signer_email VARCHAR(255),
  x_coordinate FLOAT,
  y_coordinate FLOAT,
  page_number INT,
  width FLOAT,
  height FLOAT,
  signature_data TEXT,
  signature_type VARCHAR(50),
  signer_ip_address VARCHAR(45),
  status ENUM('PENDING', 'SIGNED', 'REJECTED'),
  rejection_reason TEXT,
  signed_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id),
  FOREIGN KEY (signer_id) REFERENCES users(id)
);

-- Audit Logs
CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT,
  user_id BIGINT,
  actor_name VARCHAR(255),
  actor_email VARCHAR(255),
  action VARCHAR(100),
  details TEXT,
  ip_address VARCHAR(45),
  user_agent TEXT,
  timestamp TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES documents(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🎯 Project Structure

```
signature-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/signatureapp/
│   │   │   ├── SignatureAppApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── FileStorageConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── DocumentController.java
│   │   │   │   ├── SignatureController.java
│   │   │   │   └── AuditController.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── DocumentService.java
│   │   │   │   ├── SignatureService.java
│   │   │   │   ├── PdfService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   ├── AuditLogService.java
│   │   │   │   └── EmailService.java
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
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── AuthDto.java
│   │   │   │   ├── DocumentDto.java
│   │   │   │   └── SignatureDto.java
│   │   │   ├── security/
│   │   │   │   ├── JwtUtils.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── UserDetailsImpl.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── BadRequestException.java
│   │   │       └── UnauthorizedException.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/signatureapp/
│           └── SignatureAppApplicationTests.java
├── uploads/                 (auto-created)
│   └── signed/             (auto-created)
└── target/                 (build output)
```

---

## ✅ Verification Checklist

- [ ] MySQL running on `localhost:3306`
- [ ] Database `signature_db` created
- [ ] Java 17+ installed
- [ ] Maven 3.8+ installed
- [ ] `mvn clean install -DskipTests` succeeds
- [ ] Application starts without errors
- [ ] `/api/public/health` returns 200 OK
- [ ] Can register new user via `/api/auth/register`

---

## 🚀 Next Steps

1. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

2. **Register a user**
   ```bash
   POST /api/auth/register
   ```

3. **Upload a PDF**
   ```bash
   POST /api/docs/upload
   ```

4. **Place signatures and create signing links**
   ```bash
   POST /api/signatures
   POST /api/docs/{docId}/signing-link
   ```

5. **Sign the document**
   ```bash
   POST /api/signatures/{sigId}/sign
   ```

---

## 📞 Support

For issues or questions:
1. Check application logs for error details
2. Verify MySQL connection
3. Ensure all dependencies are installed
4. Check file permissions in `./uploads/` directory

**Happy signing! 📄✍️**

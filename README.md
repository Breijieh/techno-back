# Techno ERP System - Backend

Complete ERP system for managing 500+ employees across multiple construction projects.

## 🚀 Quick Start

### 1. Start Docker Desktop
Ensure Docker Desktop is running.

### 2. Start Database
```bash
start-database.bat
```

### 3. Start Application
```bash
mvnw spring-boot:run
```

### 4. Verify
Open: http://localhost:8080/api/public/health

---

## 📊 System Status

| Component | Endpoint | Status |
|-----------|----------|--------|
| Health Check | `/api/public/health` | ✅ Working |
| Database Check | `/api/public/health/db` | ✅ Working |
| System Info | `/api/public/health/info` | ✅ Working |

---

## 🗄️ Database

**Connection Details:**
- Host: `localhost:5432`
- Database: `techno_erp`
- Username: `techno_admin`
- Password: `techno_2025`

**Tables Created:**
- `user_accounts` - User authentication (ADMIN, MANAGER, HR, FINANCE, EMPLOYEE)
- `menu_files` - Menu structure
- `user_permissions` - Access control

**Management:**
```bash
# Stop database
stop-database.bat

# View tables
docker exec -it techno-postgres psql -U techno_admin -d techno_erp -c "\dt"

# Reset database
docker-compose down -v
docker-compose up -d
```

See [DATABASE_SETUP.md](DATABASE_SETUP.md) for detailed commands.

---

## 📁 Project Structure

```
backend/
├── config/          ✅ Security, Web, JPA
├── entity/          ✅ UserAccount, MenuFile, UserPermission
├── repository/      ✅ Data access layer
├── controller/      ✅ HealthController
├── service/         ⬜ Create next
├── dto/             ⬜ Create next
└── exception/       ⬜ Create next
```

---

## 🔧 Configuration

**Application:** `src/main/resources/application.properties`
- Server: Port 8080, context path `/api`
- Database: PostgreSQL connection configured
- JPA: Auto-create/update tables
- Security: CORS enabled for React (localhost:3000, localhost:5173)

---

## 🛠️ Technology Stack

- **Framework:** Spring Boot 4.0.0
- **Java:** 17+
- **Database:** PostgreSQL 15
- **ORM:** Spring Data JPA (Hibernate)
- **Security:** Spring Security + BCrypt
- **Build:** Maven
- **Container:** Docker

---

## 🏗️ Development

**Daily Workflow:**
1. Start Docker Desktop
2. `start-database.bat`
3. `mvnw spring-boot:run`
4. Code (hot reload enabled)
5. Test & commit

**Commands:**
```bash
# Build
mvnw clean install

# Test
mvnw test

# Package
mvnw clean package
```

---

## 📚 Key Features (From DOCUMNET.MD)

- User Management (Role-based access)
- Employee Management (500+ employees)
- GPS Attendance Tracking
- Leave Management (Multi-level approval)
- Loan Management
- Automated Payroll
- Project Management
- Warehouse Inventory
- Comprehensive Reports

---

## 🎯 Next Development Steps

**Priority 1: Authentication**
- JWT token service
- Login/logout endpoints
- Password encryption

**Priority 2: Employee Management**
- Employee entity (from DOCUMNET.MD)
- CRUD operations
- Department & Position management

**Priority 3: Attendance System**
- GPS check-in/check-out
- Overtime calculation
- Attendance reports

**Priority 4: Leave Management**
- Leave types & balances
- Request workflow
- Approval system

**Priority 5: Payroll**
- Salary calculation engine
- Deductions & allowances
- Payslip generation

---

## 📖 Documentation

- **[DOCUMNET.MD](DOCUMNET.MD)** - Complete technical specification (2463 lines)
- **[DATABASE_SETUP.md](DATABASE_SETUP.md)** - Database commands & troubleshooting

---

**Version:** 1.0.0  
**Last Updated:** November 2025

![Deploy Backend](https://github.com/Breijieh/techno-back/actions/workflows/deploy.yml/badge.svg)

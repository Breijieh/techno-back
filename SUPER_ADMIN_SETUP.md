# Super Admin Setup Guide

This guide provides multiple methods to create a super admin user for the Techno ERP system.

## 🚀 Quick Start - Recommended Method (API)

The easiest and most reliable method is to use the backend API, which handles password hashing automatically.

### Windows:
```cmd
setup-super-admin-api.bat
```

### Linux/Mac:
```bash
chmod +x setup-super-admin-api.sh
./setup-super-admin-api.sh
```

**Prerequisites:**
- Backend must be running on `http://localhost:8080`
- Database must be running

**Default Credentials:**
- Username: `admin`
- Password: `admin123`
- National ID: `1111111111`
- Role: `ADMIN`

---

## 🗄️ Method 2: Direct SQL Script (Docker)

If the API method doesn't work or backend is not running, you can create the admin directly in the database.

### Windows:
```cmd
setup-super-admin.bat
```

### Linux/Mac:
```bash
chmod +x setup-super-admin.sh
./setup-super-admin.sh
```

**Or manually:**
```bash
docker exec -i techno-postgres psql -U techno_admin -d techno_erp < create-super-admin.sql
```

**Prerequisites:**
- Docker must be running
- PostgreSQL container (`techno-postgres`) must be running
- Database must be initialized

**Note:** This method uses a pre-generated BCrypt hash. If the password doesn't work, see "Generating New Password Hash" section below.

---

## 🔧 Method 3: Manual Docker Command

If the scripts don't work, you can run the SQL directly:

```bash
docker exec -i techno-postgres psql -U techno_admin -d techno_erp <<EOF
INSERT INTO user_accounts (
    username, password_hash, national_id, user_type, is_active, created_date
) VALUES (
    'admin',
    '$2a$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '1111111111',
    'ADMIN',
    'Y',
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    user_type = EXCLUDED.user_type,
    is_active = EXCLUDED.is_active;
SELECT * FROM user_accounts WHERE username = 'admin';
EOF
```

---

## 🔑 Generating New Password Hash

If the default password hash doesn't work, you can generate a new one:

### Method 1: Using Java (Recommended)

Create a file `GenerateHash.java`:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("BCrypt Hash: " + hash);
    }
}
```

Compile and run:
```bash
javac -cp "path/to/spring-security-crypto.jar" GenerateHash.java
java -cp ".:path/to/spring-security-crypto.jar" GenerateHash
```

### Method 2: Online Tool

1. Go to: https://bcrypt-generator.com/
2. Enter password: `admin123`
3. Set rounds: 10
4. Copy the generated hash
5. Replace the hash in `create-super-admin.sql`

### Method 3: Using Spring Boot

If you have the backend running, you can create a test endpoint or use the Spring Boot console:

```java
@Autowired
private BCryptPasswordEncoder passwordEncoder;

String hash = passwordEncoder.encode("admin123");
System.out.println(hash);
```

---

## ✅ Verification

After running any method, verify the admin user was created:

```sql
SELECT 
    user_id,
    username,
    national_id,
    user_type,
    is_active,
    created_date
FROM user_accounts
WHERE username = 'admin';
```

Expected output:
- `username`: admin
- `user_type`: ADMIN
- `is_active`: Y
- `national_id`: 1111111111

---

## 🔐 Login

1. Open: http://localhost:3000/login
2. Enter credentials:
   - Username: `admin` (or National ID: `1111111111`)
   - Password: `admin123`

---

## 📋 What Gets Created

The setup scripts ensure the following reference data exists:

### Contract Types:
- TECHNO - Techno Employee
- TEMPORARY - Temporary Employee
- DAILY - Daily Worker
- CLIENT - Client Employee
- CONTRACTOR - Contractor

### Transaction Types:
- Basic Salary (Type 1)
- Transportation Allowance (Type 2)
- Housing Allowance (Type 3)

### User Account:
- Username: admin
- Password: admin123 (BCrypt hashed)
- Role: ADMIN (Super Admin with full access)
- National ID: 1111111111

---

## 🐛 Troubleshooting

### Issue: "Docker is not running"
- Start Docker Desktop
- Wait for it to fully start
- Verify: `docker ps`

### Issue: "Container not found"
- Start the database: `docker-compose up -d postgres`
- Wait for container to be healthy: `docker ps` (should show "healthy")

### Issue: "Cannot connect to database"
- Check if container is running: `docker ps | grep techno-postgres`
- Check database logs: `docker logs techno-postgres`
- Verify connection string matches docker-compose.yml

### Issue: "User already exists"
- This is okay - the script uses `ON CONFLICT DO UPDATE`
- If you need to reset the password, use the API method or update manually

### Issue: "Password doesn't work"
- The BCrypt hash might be incorrect
- Generate a new hash using one of the methods above
- Update the hash in the database:
  ```sql
  UPDATE user_accounts 
  SET password_hash = 'YOUR_NEW_HASH_HERE' 
  WHERE username = 'admin';
  ```

### Issue: "Backend API returns 403"
- The registration endpoint might require authentication
- Use the SQL method instead
- Or check backend security configuration

---

## 🔒 Security Notes

⚠️ **IMPORTANT**: These are default credentials for setup only!

- **DO NOT** use `admin123` in production
- Change the password immediately after first login
- Use strong, unique passwords
- Consider implementing 2FA for admin accounts
- Regularly review and audit admin access

---

## 📁 Files

- `create-super-admin.sql` - SQL script with all reference data
- `setup-super-admin.bat` - Windows batch script (SQL method)
- `setup-super-admin.sh` - Linux/Mac shell script (SQL method)
- `setup-super-admin-api.bat` - Windows batch script (API method)
- `setup-super-admin-api.sh` - Linux/Mac shell script (API method)
- `SUPER_ADMIN_SETUP.md` - This guide

---

## 🆘 Need Help?

If you encounter any issues:
1. Check the troubleshooting section above
2. Review backend logs: `docker logs techno-postgres`
3. Check backend application logs
4. Verify database connection settings
5. Ensure all services are running (Docker, Database, Backend)

---

**Last Updated:** 2024

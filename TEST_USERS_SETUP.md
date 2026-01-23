# Test Users Setup Guide

This guide explains how to create test users for all roles in the Techno ERP system.

## Quick Start

### Option 1: Use Backend API (Recommended)

Test users are automatically created when you run `data.sql` during initial database setup. If you need to create additional test users, use the API method below.
   ```sql
   SELECT username, user_type, national_id, is_active 
   FROM user_accounts 
   WHERE username IN ('admin', 'general.manager', 'hr.manager', 'finance.manager', 
                      'project.manager', 'warehouse.manager', 'employee')
   ORDER BY user_type;
   ```

### Option 2: Create Users via Backend API

If the SQL migration doesn't work, you can create users via the API:

1. **Start the backend**:
   ```bash
   cd techno-backend
   .\mvnw.cmd spring-boot:run
   ```

2. **Register users via API** (using Postman, curl, or frontend):
   ```bash
   POST http://localhost:8080/api/auth/register
   Content-Type: application/json
   
   {
     "username": "admin",
     "password": "password123",
     "nationalId": "1111111111",
     "userType": "ADMIN",
     "employeeNo": 1
   }
   ```

   Repeat for each user type (see the table below for all users).

## Test Users Created

All users have the password: **password123**

| Role | Username | National ID | Employee No |
|------|----------|--------------|-------------|
| ADMIN | admin | 1111111111 | 1 |
| GENERAL_MANAGER | general.manager | 2222222222 | 2 |
| HR_MANAGER | hr.manager | 3333333333 | 3 |
| FINANCE_MANAGER | finance.manager | 4444444444 | 4 |
| PROJECT_MANAGER | project.manager | 5555555555 | 5 |
| WAREHOUSE_MANAGER | warehouse.manager | 6666666666 | 6 |
| EMPLOYEE | employee | 7777777777 | 7 |

## Login Instructions

1. **Open the frontend**: http://localhost:3000
2. **Enter credentials**:
   - Username: Use any username from the table above (e.g., `admin`)
   - Password: `password123`
   - OR use National ID instead of username

3. **Test different roles**:
   - Each role has different permissions
   - Try logging in with different users to see role-based features

## If Password Doesn't Work

If the BCrypt hash in the migration doesn't work, you can generate a new one:

### Method 1: Using Java Code

Create a simple Java class:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("password123");
        System.out.println("BCrypt Hash: " + hash);
    }
}
```

Run it and use the generated hash when creating users via API.

### Method 2: Using Online Tool

1. Go to: https://bcrypt-generator.com/
2. Enter password: `password123`
3. Set rounds: 10
4. Copy the generated hash
5. Use the hash when creating users via API

### Method 3: Use API to Change Password

After logging in with a working account, use the change password API (if available) or update directly in the database.

## Verifying Users

Run this SQL query to verify all users:

```sql
SELECT 
    username,
    user_type,
    national_id,
    is_active,
    employee_no,
    created_date
FROM user_accounts
WHERE username IN (
    'admin', 
    'general.manager', 
    'hr.manager', 
    'finance.manager',
    'project.manager', 
    'warehouse.manager', 
    'employee'
)
ORDER BY user_type;
```

## Removing Test Users

To remove all test users:

```sql
DELETE FROM user_accounts 
WHERE username IN (
    'admin', 
    'general.manager', 
    'hr.manager', 
    'finance.manager',
    'project.manager', 
    'warehouse.manager', 
    'employee'
);
```

Or use the SQL DELETE statement above.

## Troubleshooting

### Issue: Users not created

- Check database connection
- Verify PostgreSQL is running
- Check for constraint violations (unique username/national_id)
- Review database logs

### Issue: Cannot login

- Verify password hash is correct
- Check user is active (`is_active = 'Y'`)
- Verify username/national_id is correct
- Check backend logs for authentication errors

### Issue: Wrong permissions

- Verify `user_type` is set correctly
- Check Spring Security configuration
- Review role-based access control settings

## Security Notes

⚠️ **IMPORTANT**: These are test credentials only!

- **DO NOT** use these passwords in production
- Change all passwords before deploying
- Use strong, unique passwords
- Consider implementing password policies
- Enable 2FA for production systems

## Files

- **This Guide**: `TEST_USERS_SETUP.md`
- **Database Setup**: See [DATABASE_SETUP.md](DATABASE_SETUP.md)
- **Backend Setup**: See [README.md](README.md)

---

For more information, see the main [README.md](README.md) or [DATABASE_SETUP.md](DATABASE_SETUP.md).


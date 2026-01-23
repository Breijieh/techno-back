# Database Commands Reference

## Quick Commands

### Start/Stop Database

```bash
# Start (Windows)
start-database.bat

# Stop (Windows)
stop-database.bat

# Or manually
docker-compose up -d      # Start
docker-compose down       # Stop
docker-compose down -v    # Stop & delete data
```

### Check Status

```bash
# View running containers
docker ps

# View logs
docker-compose logs -f postgres

# Check database health
docker exec techno-postgres pg_isready -U techno_admin
```

---

## Database Access

### Connect via psql

```bash
docker exec -it techno-postgres psql -U techno_admin -d techno_erp
```

### Useful SQL Commands

```sql
\l                          -- List databases
\dt                         -- List tables
\d table_name              -- Describe table
\d+ table_name             -- Detailed table info

-- View data
SELECT * FROM user_accounts;
SELECT * FROM menu_files;
SELECT * FROM user_permissions;

-- Count records
SELECT COUNT(*) FROM user_accounts;

\q                         -- Exit psql
```

---

## Connection Details

| Parameter | Value                                       |
| --------- | ------------------------------------------- |
| Host      | localhost                                   |
| Port      | 5432                                        |
| Database  | techno_erp                                  |
| Username  | techno_admin                                |
| Password  | techno_2025                                 |
| JDBC URL  | jdbc:postgresql://localhost:5432/techno_erp |

---

## Troubleshooting

### Port 5432 already in use

**Windows:**

```bash
netstat -ano | findstr :5432
taskkill /PID <PID> /F
```

**Or change port in docker-compose.yml:**

```yaml
ports:
  - "5433:5432" # Use 5433 instead
```

Then update `application.properties` accordingly.

### Container won't start

```bash
docker-compose down -v
docker-compose up -d
```

### Reset database (delete all data)

```bash
docker-compose down -v
docker volume rm backend_postgres_data
docker-compose up -d
```

### View container logs

```bash
docker-compose logs postgres
docker-compose logs -f postgres  # Follow logs
```

---

## Database Backup & Restore

### Backup

```bash
docker exec techno-postgres pg_dump -U techno_admin techno_erp > backup.sql
```

### Restore

```bash
docker exec -i techno-postgres psql -U techno_admin -d techno_erp < backup.sql
```

---

## Spring Boot Configuration

Database connection is configured in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/techno_erp
spring.datasource.username=techno_admin
spring.datasource.password=techno_2025
spring.jpa.hibernate.ddl-auto=update  # Auto-create/update tables
```

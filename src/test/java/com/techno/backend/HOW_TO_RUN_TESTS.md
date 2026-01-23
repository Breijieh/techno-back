# How to Run Attendance Module Tests

## ⚠️ Important Note

The project has `maven.test.skip=true` in `pom.xml` (line 31), which skips tests by default. You need to override this to run tests.

---

## 🚀 Quick Start

### Run All Attendance Tests

**For PowerShell (Windows):**
```powershell
cd techno-backend
mvn test "-Dmaven.test.skip=false" "-Dtest=*Attendance*Test"
```

**For Bash/Linux/Mac:**
```bash
cd techno-backend
mvn test -Dmaven.test.skip=false -Dtest=*Attendance*Test
```

### Run All Tests (Including Non-Attendance)

**For PowerShell (Windows):**
```powershell
cd techno-backend
mvn test "-Dmaven.test.skip=false"
```

**For Bash/Linux/Mac:**
```bash
cd techno-backend
mvn test -Dmaven.test.skip=false
```

---

## 📋 Running Specific Test Classes

### Run Single Test Class

**For PowerShell (Windows):**
```powershell
# Example: Run only AttendanceServiceTest
mvn test "-Dmaven.test.skip=false" "-Dtest=AttendanceServiceTest"

# Example: Run only GPSCalculatorTest
mvn test "-Dmaven.test.skip=false" "-Dtest=GPSCalculatorTest"
```

**For Bash/Linux/Mac:**
```bash
# Example: Run only AttendanceServiceTest
mvn test -Dmaven.test.skip=false -Dtest=AttendanceServiceTest

# Example: Run only GPSCalculatorTest
mvn test -Dmaven.test.skip=false -Dtest=GPSCalculatorTest
```

### Run Multiple Specific Test Classes

**For PowerShell (Windows):**
```powershell
# Run specific test classes
mvn test "-Dmaven.test.skip=false" "-Dtest=AttendanceServiceTest,AttendanceCalculationServiceTest"
```

**For Bash/Linux/Mac:**
```bash
# Run specific test classes
mvn test -Dmaven.test.skip=false -Dtest=AttendanceServiceTest,AttendanceCalculationServiceTest
```

### Run Tests by Pattern

**For PowerShell (Windows):**
```powershell
# Run all utility tests
mvn test "-Dmaven.test.skip=false" "-Dtest=*Calculator*Test"

# Run all service tests
mvn test "-Dmaven.test.skip=false" "-Dtest=*Service*Test"

# Run all attendance-related tests
mvn test "-Dmaven.test.skip=false" "-Dtest=*Attendance*Test"
```

**For Bash/Linux/Mac:**
```bash
# Run all utility tests
mvn test -Dmaven.test.skip=false -Dtest=*Calculator*Test

# Run all service tests
mvn test -Dmaven.test.skip=false -Dtest=*Service*Test

# Run all attendance-related tests
mvn test -Dmaven.test.skip=false -Dtest=*Attendance*Test
```

---

## 🎯 Running Specific Test Methods

### Run Single Test Method
```bash
# Run specific test method in a class
mvn test -Dmaven.test.skip=false -Dtest=AttendanceServiceTest#checkIn_ValidGPSWithinRadius_Success
```

### Run Multiple Test Methods
```bash
# Run multiple test methods
mvn test -Dmaven.test.skip=false -Dtest=AttendanceServiceTest#checkIn_ValidGPSWithinRadius_Success+checkOut_ValidGPS_Success
```

---

## 📊 Running Tests with Coverage Report

### Generate Coverage Report (JaCoCo)
```bash
# Run tests and generate coverage report
mvn clean test -Dmaven.test.skip=false jacoco:report

# View report: Open target/site/jacoco/index.html in browser
```

### Coverage Report Location
```
techno-backend/target/site/jacoco/index.html
```

---

## 🛠️ IDE-Specific Instructions

### IntelliJ IDEA

1. **Run All Tests in a Class:**
   - Right-click on test class → `Run 'ClassName'`
   - Or click green arrow next to class name

2. **Run Single Test Method:**
   - Right-click on test method → `Run 'methodName()'`
   - Or click green arrow next to method name

3. **Run All Tests:**
   - Right-click on `src/test/java` → `Run 'All Tests'`
   - Or use shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

4. **Run Tests with Coverage:**
   - Right-click on test class → `Run 'ClassName' with Coverage`
   - Or use menu: `Run` → `Run with Coverage`

5. **Configure Test Runner:**
   - Go to `Run` → `Edit Configurations`
   - Add new JUnit configuration
   - Set VM options if needed: `-Dmaven.test.skip=false`

### Eclipse

1. **Run All Tests in a Class:**
   - Right-click on test class → `Run As` → `JUnit Test`

2. **Run Single Test Method:**
   - Right-click on test method → `Run As` → `JUnit Test`

3. **Run All Tests:**
   - Right-click on project → `Run As` → `JUnit Test`

4. **Run Tests with Coverage:**
   - Install EclEmma plugin
   - Right-click on test → `Coverage As` → `JUnit Test`

### VS Code

1. **Install Java Test Runner Extension:**
   - Install "Extension Pack for Java" or "Test Runner for Java"

2. **Run Tests:**
   - Click test icon in sidebar
   - Click play button next to test class/method
   - Or use command: `Java: Run Tests`

---

## 🔧 Maven Commands Reference

### Basic Commands
```bash
# Clean and compile
mvn clean compile

# Compile test classes
mvn test-compile

# Run tests (skips by default due to pom.xml setting)
mvn test

# Run tests (override skip)
mvn test -Dmaven.test.skip=false

# Skip compilation, only run tests
mvn surefire:test -Dmaven.test.skip=false
```

### Advanced Commands
```bash
# Run tests in parallel (faster)
mvn test -Dmaven.test.skip=false -Dparallel=classes

# Run tests with specific JVM options
mvn test -Dmaven.test.skip=false -DargLine="-Xmx2g"

# Run tests and show output
mvn test -Dmaven.test.skip=false -X

# Run tests and stop on first failure
mvn test -Dmaven.test.skip=false -Dmaven.test.failure.ignore=false
```

---

## 📁 Test File Locations

All test files are located in:
```
techno-backend/src/test/java/com/techno/backend/
├── util/
│   ├── GPSCalculatorTest.java
│   └── AttendanceCalculatorTest.java
├── service/
│   ├── AttendanceServiceTest.java
│   ├── AttendanceCalculationServiceTest.java
│   ├── ManualAttendanceRequestServiceTest.java
│   ├── AttendanceScheduledServiceTest.java
│   ├── AttendanceAllowanceDeductionServiceTest.java
│   ├── OvertimeAlertServiceTest.java
│   ├── AttendanceIntegrationTest.java
│   └── AttendanceEdgeCasesTest.java
```

---

## ✅ Verify Tests Are Running

### Check Test Output
When tests run, you should see output like:
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.techno.backend.service.AttendanceServiceTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
```

### Check Test Results
Test results are saved in:
```
techno-backend/target/surefire-reports/
├── AttendanceServiceTest.txt
├── AttendanceServiceTest.xml
└── ...
```

---

## 🐛 Troubleshooting

### Tests Not Running
**Problem**: Tests are skipped even with `-Dmaven.test.skip=false`

**Solution**: Check `pom.xml` line 31. You may need to temporarily change:
```xml
<maven.test.skip>true</maven.test.skip>
```
to:
```xml
<maven.test.skip>false</maven.test.skip>
```

### Compilation Errors
**Problem**: Tests fail to compile

**Solution**: 
```bash
# Clean and rebuild
mvn clean compile test-compile

# Check for missing dependencies
mvn dependency:tree
```

### Mockito Errors
**Problem**: `MockitoException` or `NullPointerException` in tests

**Solution**: Ensure all `@Mock` dependencies are properly initialized. Check that:
- `@ExtendWith(MockitoExtension.class)` is present
- All required mocks are declared with `@Mock`
- Service under test uses `@InjectMocks`

### Test Timeout
**Problem**: Tests take too long or timeout

**Solution**: 
```bash
# Increase timeout
mvn test -Dmaven.test.skip=false -Dsurefire.timeout=600
```

---

## 📈 Test Execution Order

Tests are executed in this order by default:
1. **Utility Tests**: GPSCalculatorTest, AttendanceCalculatorTest
2. **Service Tests**: All service test classes (alphabetical)
3. **Integration Tests**: AttendanceIntegrationTest
4. **Edge Case Tests**: AttendanceEdgeCasesTest

To control execution order, use `@TestMethodOrder` annotation.

---

## 🎓 Best Practices

1. **Run Tests Before Committing:**
   ```bash
   mvn clean test -Dmaven.test.skip=false
   ```

2. **Run Tests with Coverage:**
   ```bash
   mvn clean test -Dmaven.test.skip=false jacoco:report
   ```

3. **Run Only Fast Tests:**
   ```bash
   mvn test -Dmaven.test.skip=false -Dtest=*Test -DexcludedGroups=slow
   ```

4. **Run Tests in CI/CD:**
   ```yaml
   # Example GitHub Actions
   - name: Run Tests
     run: mvn test -Dmaven.test.skip=false
   ```

---

## 📚 Additional Resources

- **Maven Surefire Plugin**: https://maven.apache.org/surefire/maven-surefire-plugin/
- **JUnit 5 User Guide**: https://junit.org/junit5/docs/current/user-guide/
- **Mockito Documentation**: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **JaCoCo Coverage**: https://www.jacoco.org/jacoco/trunk/doc/

---

## 🚀 Quick Reference Card

**For PowerShell (Windows):**
```powershell
# Most Common Commands
mvn test "-Dmaven.test.skip=false"                    # Run all tests
mvn test "-Dmaven.test.skip=false" "-Dtest=*Attendance*Test"  # Run attendance tests
mvn test "-Dmaven.test.skip=false" "-Dtest=AttendanceServiceTest"  # Run one class
mvn clean test "-Dmaven.test.skip=false" jacoco:report  # With coverage
```

**For Bash/Linux/Mac:**
```bash
# Most Common Commands
mvn test -Dmaven.test.skip=false                    # Run all tests
mvn test -Dmaven.test.skip=false -Dtest=*Attendance*Test  # Run attendance tests
mvn test -Dmaven.test.skip=false -Dtest=AttendanceServiceTest  # Run one class
mvn clean test -Dmaven.test.skip=false jacoco:report  # With coverage
```

---

**Last Updated**: January 18, 2025  
**Test Framework**: JUnit 5 + Mockito  
**Build Tool**: Maven

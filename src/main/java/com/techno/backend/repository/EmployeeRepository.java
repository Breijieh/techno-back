package com.techno.backend.repository;

import com.techno.backend.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Employee entity.
 * Provides database access methods for employee management.
 *
 * Uses JpaSpecificationExecutor for dynamic filtering and searching.
 *
 * @author Techno ERP Team
 * @version 2.0
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

       /**
        * Find employee by national ID
        */
       Optional<Employee> findByNationalId(String nationalId);

       /**
        * Check if national ID already exists (for duplicate checking)
        */
       boolean existsByNationalId(String nationalId);

       /**
        * Check if national ID exists excluding specific employee (for update
        * validation)
        */
       @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e " +
                     "WHERE e.nationalId = :nationalId AND e.employeeNo <> :employeeNo")
       boolean existsByNationalIdAndNotEmployeeNo(@Param("nationalId") String nationalId,
                     @Param("employeeNo") Long employeeNo);

       /**
        * Find all employees by department
        */
       Page<Employee> findByPrimaryDeptCode(Long deptCode, Pageable pageable);

       /**
        * Find all employees by project
        */
       Page<Employee> findByPrimaryProjectCode(Long projectCode, Pageable pageable);

       /**
        * Find all employees by contract type
        */
       Page<Employee> findByEmpContractType(String contractType, Pageable pageable);

       /**
        * Find all employees by employment status
        */
       Page<Employee> findByEmploymentStatus(String employmentStatus, Pageable pageable);

       /**
        * Find all employees by category (Saudi/Foreign)
        */
       Page<Employee> findByEmployeeCategory(String employeeCategory, Pageable pageable);

       /**
        * Search employees by name (Arabic or English) or national ID
        * Uses LIKE for partial matching
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "LOWER(e.employeeName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
                     "LOWER(e.employeeName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
                     "LOWER(e.nationalId) LIKE LOWER(CONCAT('%', :searchText, '%'))")
       Page<Employee> searchByText(@Param("searchText") String searchText, Pageable pageable);

       /**
        * Find employees with expiring passports (within specified days)
        * Only returns foreign employees with passport expiry dates set
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.employeeCategory = 'F' AND " +
                     "e.passportExpiryDate IS NOT NULL AND " +
                     "e.passportExpiryDate BETWEEN :today AND :expiryDate AND " +
                     "e.employmentStatus = 'ACTIVE' " +
                     "ORDER BY e.passportExpiryDate ASC")
       List<Employee> findEmployeesWithExpiringPassports(@Param("today") LocalDate today,
                     @Param("expiryDate") LocalDate expiryDate);

       /**
        * Find employees with expired passports
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.employeeCategory = 'F' AND " +
                     "e.passportExpiryDate IS NOT NULL AND " +
                     "e.passportExpiryDate < :today AND " +
                     "e.employmentStatus = 'ACTIVE' " +
                     "ORDER BY e.passportExpiryDate ASC")
       List<Employee> findEmployeesWithExpiredPassports(@Param("today") LocalDate today);

       /**
        * Find employees with expiring residencies (within specified days)
        * Only returns foreign employees with residency expiry dates set
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.employeeCategory = 'F' AND " +
                     "e.residencyExpiryDate IS NOT NULL AND " +
                     "e.residencyExpiryDate BETWEEN :today AND :expiryDate AND " +
                     "e.employmentStatus = 'ACTIVE' " +
                     "ORDER BY e.residencyExpiryDate ASC")
       List<Employee> findEmployeesWithExpiringResidencies(@Param("today") LocalDate today,
                     @Param("expiryDate") LocalDate expiryDate);

       /**
        * Find employees with expired residencies
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.employeeCategory = 'F' AND " +
                     "e.residencyExpiryDate IS NOT NULL AND " +
                     "e.residencyExpiryDate < :today AND " +
                     "e.employmentStatus = 'ACTIVE' " +
                     "ORDER BY e.residencyExpiryDate ASC")
       List<Employee> findEmployeesWithExpiredResidencies(@Param("today") LocalDate today);

       /**
        * Find employees with any expiring documents (passport OR residency)
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.employeeCategory = 'F' AND " +
                     "e.employmentStatus = 'ACTIVE' AND " +
                     "((e.passportExpiryDate IS NOT NULL AND e.passportExpiryDate BETWEEN :today AND :expiryDate) OR " +
                     " (e.residencyExpiryDate IS NOT NULL AND e.residencyExpiryDate BETWEEN :today AND :expiryDate)) " +
                     "ORDER BY LEAST(COALESCE(e.passportExpiryDate, :expiryDate), COALESCE(e.residencyExpiryDate, :expiryDate)) ASC")
       List<Employee> findEmployeesWithExpiringDocuments(@Param("today") LocalDate today,
                     @Param("expiryDate") LocalDate expiryDate);

       /**
        * Find a specific employee with expiring documents (passport OR residency)
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.employeeNo = :employeeNo AND " +
                     "e.employeeCategory = 'F' AND " +
                     "e.employmentStatus = 'ACTIVE' AND " +
                     "((e.passportExpiryDate IS NOT NULL AND e.passportExpiryDate BETWEEN :today AND :expiryDate) OR " +
                     " (e.residencyExpiryDate IS NOT NULL AND e.residencyExpiryDate BETWEEN :today AND :expiryDate)) " +
                     "ORDER BY LEAST(COALESCE(e.passportExpiryDate, :expiryDate), COALESCE(e.residencyExpiryDate, :expiryDate)) ASC")
       List<Employee> findEmployeeWithExpiringDocuments(@Param("employeeNo") Long employeeNo,
                     @Param("today") LocalDate today,
                     @Param("expiryDate") LocalDate expiryDate);

       /**
        * Count active employees
        */
       @Query("SELECT COUNT(e) FROM Employee e WHERE e.employmentStatus = 'ACTIVE'")
       Long countActiveEmployees();

       /**
        * Count employees by contract type
        */
       Long countByEmpContractType(String contractType);

       /**
        * Count employees by category
        */
       Long countByEmployeeCategory(String employeeCategory);

       /**
        * Count employees by department
        */
       Long countByPrimaryDeptCode(Long deptCode);

       /**
        * Count employees by project
        */
       Long countByPrimaryProjectCode(Long projectCode);

       /**
        * Find all active employees (for reports and batch processing)
        */
       @Query("SELECT e FROM Employee e WHERE e.employmentStatus = 'ACTIVE' ORDER BY e.employeeNo ASC")
       List<Employee> findAllActiveEmployees();

       /**
        * Find employees hired in a specific month/year (for anniversary reports)
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "YEAR(e.hireDate) = :year AND MONTH(e.hireDate) = :month " +
                     "ORDER BY e.hireDate ASC")
       List<Employee> findEmployeesHiredInMonth(@Param("year") int year, @Param("month") int month);

       /**
        * Find employees by nationality
        */
       Page<Employee> findByNationality(String nationality, Pageable pageable);

       /**
        * Get all distinct nationalities in the system
        */
       @Query("SELECT DISTINCT e.nationality FROM Employee e ORDER BY e.nationality ASC")
       List<String> findAllDistinctNationalities();

       /**
        * Find all TECHNO contract employees with leave balance, with optional filters.
        * Used for listing all employees' leave balances in the leave balance page.
        * Only returns employees with empContractType = 'TECHNO'.
        */
       @Query("SELECT e FROM Employee e WHERE " +
                     "e.empContractType = 'TECHNO' AND " +
                     "(:employeeNo IS NULL OR e.employeeNo = :employeeNo) AND " +
                     "(:departmentCode IS NULL OR e.primaryDeptCode = :departmentCode) AND " +
                     "(:employmentStatus IS NULL OR e.employmentStatus = :employmentStatus) " +
                     "ORDER BY e.employeeNo ASC")
       Page<Employee> findAllWithLeaveBalance(
                     @Param("employeeNo") Long employeeNo,
                     @Param("departmentCode") Long departmentCode,
                     @Param("employmentStatus") String employmentStatus,
                     Pageable pageable);
}

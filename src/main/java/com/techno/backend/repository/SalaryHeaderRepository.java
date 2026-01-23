package com.techno.backend.repository;

import com.techno.backend.entity.SalaryHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryHeaderRepository extends JpaRepository<SalaryHeader, Long> {

    @Query("SELECT s FROM SalaryHeader s WHERE " +
           "s.employeeNo = :employeeNo AND " +
           "s.salaryMonth = :salaryMonth AND " +
           "s.isLatest = 'Y'")
    Optional<SalaryHeader> findLatestByEmployeeAndMonth(
            @Param("employeeNo") Long employeeNo,
            @Param("salaryMonth") String salaryMonth);

    @Query("SELECT s FROM SalaryHeader s WHERE " +
           "s.salaryMonth = :salaryMonth AND " +
           "s.isLatest = 'Y' " +
           "ORDER BY s.employeeNo ASC")
    List<SalaryHeader> findAllLatestBySalaryMonth(@Param("salaryMonth") String salaryMonth);

    @Query("SELECT s FROM SalaryHeader s WHERE " +
           "s.employeeNo = :employeeNo " +
           "ORDER BY s.salaryMonth DESC, s.salaryVersion DESC")
    List<SalaryHeader> findByEmployeeNoOrderByMonthDesc(@Param("employeeNo") Long employeeNo);

    @Query("SELECT s FROM SalaryHeader s WHERE " +
           "s.salaryMonth = :salaryMonth AND " +
           "s.transStatus = :status AND " +
           "s.isLatest = 'Y'")
    List<SalaryHeader> findBySalaryMonthAndStatus(
            @Param("salaryMonth") String salaryMonth,
            @Param("status") String status);
}

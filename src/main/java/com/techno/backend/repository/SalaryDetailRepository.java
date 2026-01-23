package com.techno.backend.repository;

import com.techno.backend.entity.SalaryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryDetailRepository extends JpaRepository<SalaryDetail, Long> {

    List<SalaryDetail> findBySalaryIdOrderByLineNoAsc(Long salaryId);

    @Query("SELECT d FROM SalaryDetail d WHERE " +
           "d.salaryId = :salaryId AND " +
           "d.transCategory = :category " +
           "ORDER BY d.lineNo ASC")
    List<SalaryDetail> findBySalaryIdAndCategory(
            @Param("salaryId") Long salaryId,
            @Param("category") String category);

    @Query("SELECT d FROM SalaryDetail d WHERE " +
           "d.salaryId = :salaryId AND " +
           "d.transTypeCode = :typeCode")
    List<SalaryDetail> findBySalaryIdAndTypeCode(
            @Param("salaryId") Long salaryId,
            @Param("typeCode") Long typeCode);
}

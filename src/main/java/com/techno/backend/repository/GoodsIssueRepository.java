package com.techno.backend.repository;

import com.techno.backend.entity.GoodsIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsIssueRepository extends JpaRepository<GoodsIssue, Long> {

    @Query("SELECT g FROM GoodsIssue g WHERE g.issueId = :issueId AND g.isDeleted = false")
    Optional<GoodsIssue> findById(Long issueId);

    @Query("SELECT g FROM GoodsIssue g WHERE g.store.storeCode = :storeCode AND g.isDeleted = false ORDER BY g.issueDate DESC")
    List<GoodsIssue> findByStoreCode(Long storeCode);

    @Query("SELECT g FROM GoodsIssue g WHERE g.project.projectCode = :projectCode AND g.isDeleted = false ORDER BY g.issueDate DESC")
    List<GoodsIssue> findByProjectCode(Long projectCode);

    @Query("SELECT COUNT(g) > 0 FROM GoodsIssue g WHERE g.issueNumber = :issueNumber AND g.isDeleted = false")
    boolean existsByIssueNumber(String issueNumber);
}

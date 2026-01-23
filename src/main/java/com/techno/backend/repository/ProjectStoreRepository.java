package com.techno.backend.repository;

import com.techno.backend.entity.ProjectStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectStoreRepository extends JpaRepository<ProjectStore, Long> {

    @Query("SELECT s FROM ProjectStore s WHERE s.storeCode = :storeCode AND s.isDeleted = false")
    Optional<ProjectStore> findById(Long storeCode);

    @Query("SELECT s FROM ProjectStore s WHERE s.isDeleted = false ORDER BY s.storeName")
    List<ProjectStore> findAll();

    @Query("SELECT s FROM ProjectStore s WHERE s.project.projectCode = :projectCode AND s.isDeleted = false ORDER BY s.storeName")
    List<ProjectStore> findByProjectCode(Long projectCode);

    @Query("SELECT s FROM ProjectStore s WHERE s.isActive = true AND s.isDeleted = false ORDER BY s.storeName")
    List<ProjectStore> findAllActive();

    @Query("SELECT COUNT(s) > 0 FROM ProjectStore s WHERE (s.storeName = :arName OR s.storeName = :enName) AND s.isDeleted = false")
    boolean existsByName(String arName, String enName);

    @Query("SELECT COUNT(s) > 0 FROM ProjectStore s WHERE (s.storeName = :arName OR s.storeName = :enName) AND s.storeCode != :storeCode AND s.isDeleted = false")
    boolean existsByNameExcludingId(String arName, String enName, Long storeCode);
}

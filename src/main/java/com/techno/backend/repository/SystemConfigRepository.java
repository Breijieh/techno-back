package com.techno.backend.repository;

import com.techno.backend.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.isDeleted = 'N'")
    Optional<SystemConfig> findByConfigKey(@Param("configKey") String configKey);

    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isDeleted = 'N' ORDER BY sc.configCategory, sc.configKey")
    List<SystemConfig> findAllActive();

    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configCategory = :category AND sc.isDeleted = 'N' ORDER BY sc.configKey")
    List<SystemConfig> findByCategory(@Param("category") String category);

    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isActive = 'Y' AND sc.isDeleted = 'N' ORDER BY sc.configKey")
    List<SystemConfig> findAllActiveConfigs();

    @Query("SELECT DISTINCT sc.configCategory FROM SystemConfig sc WHERE sc.isDeleted = 'N' AND sc.configCategory IS NOT NULL ORDER BY sc.configCategory")
    List<String> findAllCategories();

    boolean existsByConfigKey(String configKey);
}

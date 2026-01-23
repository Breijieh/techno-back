package com.techno.backend.repository;

import com.techno.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Department entity
 * Provides CRUD operations and custom queries for department hierarchy
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    /**
     * Find root departments (no parent)
     * @return List of root departments
     */
    List<Department> findByParentDeptCodeIsNull();
    
    /**
     * Find child departments by parent code
     * @param parentCode the parent department code
     * @return List of child departments
     */
    List<Department> findByParentDeptCode(Long parentCode);
    
    /**
     * Find departments by active status
     * @param isActive the active status ('Y' or 'N')
     * @return List of departments
     */
    List<Department> findByIsActive(Character isActive);
    
    /**
     * Find active departments by parent code
     * @param parentCode the parent department code
     * @return List of active child departments
     */
    List<Department> findByParentDeptCodeAndIsActive(Long parentCode, Character isActive);
    
    /**
     * Check if department code exists
     * @param deptCode the department code to check
     * @return true if department exists
     */
    boolean existsByDeptCode(Long deptCode);
    
    /**
     * Find active root departments
     * @return List of active root departments
     */
    List<Department> findByParentDeptCodeIsNullAndIsActive(Character isActive);
}


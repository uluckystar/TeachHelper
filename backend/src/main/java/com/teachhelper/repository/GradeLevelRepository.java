package com.teachhelper.repository;

import com.teachhelper.entity.GradeLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年级Repository
 */
@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {
    
    /**
     * 查找所有激活的年级，按排序顺序和名称排序
     */
    List<GradeLevel> findByIsActiveTrueOrderBySortOrderAscNameAsc();
    
    /**
     * 分页查找激活的年级
     */
    Page<GradeLevel> findByIsActiveTrueOrderBySortOrderAscNameAsc(Pageable pageable);
    
    /**
     * 根据类别查找年级
     */
    List<GradeLevel> findByCategoryAndIsActiveTrueOrderBySortOrderAscNameAsc(String category);
    
    /**
     * 根据名称查找年级
     */
    Optional<GradeLevel> findByNameAndIsActiveTrue(String name);
    
    /**
     * 检查年级名称是否存在
     */
    boolean existsByNameAndIsActiveTrue(String name);
    
    /**
     * 根据名称模糊搜索年级
     */
    @Query("SELECT g FROM GradeLevel g WHERE g.isActive = true AND g.name LIKE %:name% ORDER BY g.sortOrder ASC, g.name ASC")
    List<GradeLevel> findByNameContainingAndIsActiveTrue(@Param("name") String name);
    
    /**
     * 获取所有激活年级的名称列表
     */
    @Query("SELECT g.name FROM GradeLevel g WHERE g.isActive = true ORDER BY g.sortOrder ASC, g.name ASC")
    List<String> findAllActiveGradeLevelNames();
    
    /**
     * 获取所有激活的年级类别
     */
    @Query("SELECT DISTINCT g.category FROM GradeLevel g WHERE g.isActive = true AND g.category IS NOT NULL ORDER BY g.category")
    List<String> findDistinctActiveCategories();
    
    /**
     * 统计激活的年级数量
     */
    long countByIsActiveTrue();
}

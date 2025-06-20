package com.teachhelper.repository;

import com.teachhelper.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学科Repository
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    /**
     * 查找所有激活的学科，按排序顺序和名称排序
     */
    List<Subject> findByIsActiveTrueOrderBySortOrderAscNameAsc();
    
    /**
     * 分页查找激活的学科
     */
    Page<Subject> findByIsActiveTrueOrderBySortOrderAscNameAsc(Pageable pageable);
    
    /**
     * 根据名称查找学科
     */
    Optional<Subject> findByNameAndIsActiveTrue(String name);
    
    /**
     * 检查学科名称是否存在
     */
    boolean existsByNameAndIsActiveTrue(String name);
    
    /**
     * 根据名称模糊搜索学科
     */
    @Query("SELECT s FROM Subject s WHERE s.isActive = true AND s.name LIKE %:name% ORDER BY s.sortOrder ASC, s.name ASC")
    List<Subject> findByNameContainingAndIsActiveTrue(@Param("name") String name);
    
    /**
     * 获取所有激活学科的名称列表
     */
    @Query("SELECT s.name FROM Subject s WHERE s.isActive = true ORDER BY s.sortOrder ASC, s.name ASC")
    List<String> findAllActiveSubjectNames();
    
    /**
     * 统计激活的学科数量
     */
    long countByIsActiveTrue();
}

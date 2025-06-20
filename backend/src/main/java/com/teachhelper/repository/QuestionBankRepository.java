package com.teachhelper.repository;

import com.teachhelper.entity.QuestionBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 题目库Repository
 */
@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    
    /**
     * 查找用户的所有题目库
     */
    Page<QuestionBank> findByCreatedByAndIsActiveTrueOrderByUpdatedAtDesc(Long createdBy, Pageable pageable);
    
    /**
     * 查找公开的题目库
     */
    Page<QuestionBank> findByIsPublicTrueAndIsActiveTrueOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * 查找用户可访问的题目库（自己的+公开的）
     */
    @Query("SELECT qb FROM QuestionBank qb WHERE qb.isActive = true AND (qb.createdBy = :userId OR qb.isPublic = true) ORDER BY qb.updatedAt DESC")
    Page<QuestionBank> findAccessibleByUser(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 按科目查找题目库
     */
    Page<QuestionBank> findBySubjectAndIsActiveTrueOrderByUpdatedAtDesc(String subject, Pageable pageable);
    
    /**
     * 按年级查找题目库
     */
    Page<QuestionBank> findByGradeLevelAndIsActiveTrueOrderByUpdatedAtDesc(String gradeLevel, Pageable pageable);
    
    /**
     * 检查名称是否存在
     */
    boolean existsByNameAndCreatedBy(String name, Long createdBy);
    
    /**
     * 获取所有不重复的科目
     */
    @Query("SELECT DISTINCT qb.subject FROM QuestionBank qb WHERE qb.isActive = true AND qb.subject IS NOT NULL")
    List<String> findDistinctSubjects();
    
    /**
     * 获取所有不重复的年级
     */
    @Query("SELECT DISTINCT qb.gradeLevel FROM QuestionBank qb WHERE qb.isActive = true AND qb.gradeLevel IS NOT NULL")
    List<String> findDistinctGradeLevels();
    
    /**
     * 统计指定学科的题目库数量
     */
    long countBySubjectAndIsActiveTrue(String subject);
    
    /**
     * 统计指定年级的题目库数量
     */
    long countByGradeLevelAndIsActiveTrue(String gradeLevel);
    
    /**
     * 查找指定用户的题目库
     */
    Optional<QuestionBank> findByIdAndCreatedByAndIsActiveTrue(Long id, Long createdBy);
}

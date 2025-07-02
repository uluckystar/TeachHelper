package com.teachhelper.repository;

import com.teachhelper.entity.ExamPaperTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 试卷模板Repository
 */
@Repository
public interface ExamPaperTemplateRepository extends JpaRepository<ExamPaperTemplate, Long> {
    
    /**
     * 根据创建者查找模板
     */
    List<ExamPaperTemplate> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    /**
     * 根据创建者分页查找模板
     */
    Page<ExamPaperTemplate> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);
    
    /**
     * 根据科目查找模板
     */
    List<ExamPaperTemplate> findBySubjectOrderByCreatedAtDesc(String subject);
    
    /**
     * 根据科目和年级查找模板
     */
    List<ExamPaperTemplate> findBySubjectAndGradeLevelOrderByCreatedAtDesc(String subject, String gradeLevel);
    
    /**
     * 根据状态查找模板
     */
    List<ExamPaperTemplate> findByStatusOrderByCreatedAtDesc(ExamPaperTemplate.TemplateStatus status);
    
    /**
     * 根据模板类型查找模板
     */
    List<ExamPaperTemplate> findByTemplateTypeOrderByCreatedAtDesc(ExamPaperTemplate.TemplateType templateType);
    
    /**
     * 查找公开模板
     */
    List<ExamPaperTemplate> findByIsPublicTrueOrderByUsageCountDescCreatedAtDesc();
    
    /**
     * 根据名称模糊查找模板
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.name LIKE %:name% ORDER BY t.createdAt DESC")
    List<ExamPaperTemplate> findByNameContainingOrderByCreatedAtDesc(@Param("name") String name);
    
    /**
     * 根据创建者和科目查找模板
     */
    List<ExamPaperTemplate> findByCreatedByAndSubjectOrderByCreatedAtDesc(Long createdBy, String subject);
    
    /**
     * 查找可用的模板（就绪或已发布状态）
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.status IN ('READY', 'PUBLISHED') ORDER BY t.usageCount DESC, t.createdAt DESC")
    List<ExamPaperTemplate> findUsableTemplatesOrderByUsageCountDesc();
    
    /**
     * 根据创建者查找可用的模板
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.createdBy = :createdBy AND t.status IN ('READY', 'PUBLISHED') ORDER BY t.usageCount DESC, t.createdAt DESC")
    List<ExamPaperTemplate> findUsableTemplatesByCreatorOrderByUsageCountDesc(@Param("createdBy") Long createdBy);
    
    /**
     * 查找热门模板（使用次数最多的）
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.status IN ('READY', 'PUBLISHED') ORDER BY t.usageCount DESC, t.lastUsedAt DESC")
    List<ExamPaperTemplate> findPopularTemplatesOrderByUsageCountDesc(Pageable pageable);
    
    /**
     * 根据标签查找模板
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.tags LIKE %:tag% AND t.status IN ('READY', 'PUBLISHED') ORDER BY t.usageCount DESC, t.createdAt DESC")
    List<ExamPaperTemplate> findByTagOrderByUsageCountDesc(@Param("tag") String tag);
    
    /**
     * 查找最近使用的模板
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.lastUsedAt IS NOT NULL ORDER BY t.lastUsedAt DESC")
    List<ExamPaperTemplate> findRecentlyUsedTemplatesOrderByLastUsedAtDesc(Pageable pageable);
    
    /**
     * 根据创建者查找最近使用的模板
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.createdBy = :createdBy AND t.lastUsedAt IS NOT NULL ORDER BY t.lastUsedAt DESC")
    List<ExamPaperTemplate> findRecentlyUsedTemplatesByCreatorOrderByLastUsedAtDesc(@Param("createdBy") Long createdBy);
    
    /**
     * 统计模板使用情况
     */
    @Query("SELECT COUNT(t) FROM ExamPaperTemplate t WHERE t.createdBy = :createdBy")
    long countByCreatedBy(@Param("createdBy") Long createdBy);
    
    /**
     * 统计公开模板数量
     */
    long countByIsPublicTrue();
    
    /**
     * 查找需要审核的模板（草稿状态）
     */
    @Query("SELECT t FROM ExamPaperTemplate t WHERE t.status = 'DRAFT' ORDER BY t.createdAt DESC")
    List<ExamPaperTemplate> findDraftTemplatesOrderByCreatedAtDesc();
} 
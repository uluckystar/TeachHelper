package com.teachhelper.repository;

import com.teachhelper.entity.ExamTemplate;
import com.teachhelper.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTemplateRepository extends JpaRepository<ExamTemplate, Long> {
    
    /**
     * 根据模板名称查找模板
     */
    Optional<ExamTemplate> findByTemplateNameAndCreatedBy(String templateName, User createdBy);
    
    /**
     * 根据科目查找模板
     */
    List<ExamTemplate> findBySubjectOrderByCreatedAtDesc(String subject);
    
    /**
     * 根据状态查找模板
     */
    List<ExamTemplate> findByStatusOrderByCreatedAtDesc(ExamTemplate.TemplateStatus status);
    
    /**
     * 根据创建者查找模板
     */
    Page<ExamTemplate> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);
    
    /**
     * 根据科目和状态查找模板
     */
    List<ExamTemplate> findBySubjectAndStatusOrderByCreatedAtDesc(String subject, ExamTemplate.TemplateStatus status);
    
    /**
     * 查找就绪状态的模板
     */
    @Query("SELECT t FROM ExamTemplate t WHERE t.status = 'READY' AND t.totalQuestions = t.matchedQuestions ORDER BY t.createdAt DESC")
    List<ExamTemplate> findReadyTemplates();
    
    /**
     * 查找需要人工审核的模板
     */
    @Query("SELECT t FROM ExamTemplate t WHERE t.status = 'DRAFT' AND (t.totalQuestions IS NULL OR t.matchedQuestions < t.totalQuestions) ORDER BY t.createdAt DESC")
    List<ExamTemplate> findTemplatesNeedingReview();
    
    /**
     * 根据考试标题模糊查询
     */
    List<ExamTemplate> findByExamTitleContainingIgnoreCaseOrderByCreatedAtDesc(String examTitle);
    
    /**
     * 统计用户的模板数量
     */
    @Query("SELECT COUNT(t) FROM ExamTemplate t WHERE t.createdBy = :user")
    long countByCreatedBy(@Param("user") User user);
    
    /**
     * 统计各状态的模板数量
     */
    @Query("SELECT t.status, COUNT(t) FROM ExamTemplate t GROUP BY t.status")
    List<Object[]> countByStatus();
} 
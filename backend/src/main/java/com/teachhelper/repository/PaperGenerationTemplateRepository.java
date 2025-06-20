package com.teachhelper.repository;

import com.teachhelper.entity.PaperGenerationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 试卷生成模板Repository
 */
@Repository
public interface PaperGenerationTemplateRepository extends JpaRepository<PaperGenerationTemplate, Long> {
    
    /**
     * 根据创建者查找模板
     */
    List<PaperGenerationTemplate> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    /**
     * 根据科目查找模板
     */
    List<PaperGenerationTemplate> findBySubjectOrderByCreatedAtDesc(String subject);
    
    /**
     * 根据科目和年级查找模板
     */
    List<PaperGenerationTemplate> findBySubjectAndGradeLevelOrderByCreatedAtDesc(String subject, String gradeLevel);
    
    /**
     * 根据名称模糊查找模板
     */
    @Query("SELECT t FROM PaperGenerationTemplate t WHERE t.name LIKE %:name% ORDER BY t.createdAt DESC")
    List<PaperGenerationTemplate> findByNameContainingOrderByCreatedAtDesc(@Param("name") String name);
    
    /**
     * 查找公共模板（系统预设）
     */
    @Query("SELECT t FROM PaperGenerationTemplate t WHERE t.createdBy IS NULL OR t.createdBy = 0 ORDER BY t.createdAt DESC")
    List<PaperGenerationTemplate> findPublicTemplatesOrderByCreatedAtDesc();
    
    /**
     * 根据创建者和科目查找模板
     */
    List<PaperGenerationTemplate> findByCreatedByAndSubjectOrderByCreatedAtDesc(Long createdBy, String subject);
}

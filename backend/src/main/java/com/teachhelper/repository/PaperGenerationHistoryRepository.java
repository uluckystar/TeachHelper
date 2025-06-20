package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.PaperGenerationHistory;

/**
 * 试卷生成历史记录Repository
 */
@Repository
public interface PaperGenerationHistoryRepository extends JpaRepository<PaperGenerationHistory, Long> {
    
    /**
     * 根据创建者ID查找生成历史
     */
    List<PaperGenerationHistory> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    /**
     * 根据模板ID查找生成历史
     */
    List<PaperGenerationHistory> findByTemplateIdOrderByCreatedAtDesc(Long templateId);
    
    /**
     * 根据考试ID查找生成历史
     */
    List<PaperGenerationHistory> findByExamIdOrderByCreatedAtDesc(Long examId);
    
    /**
     * 根据生成状态查找历史记录
     */
    List<PaperGenerationHistory> findByStatusOrderByCreatedAtDesc(PaperGenerationHistory.GenerationStatus status);
    
    /**
     * 根据学科查找生成历史
     */
    List<PaperGenerationHistory> findBySubjectOrderByCreatedAtDesc(String subject);
    
    /**
     * 分页查询生成历史
     */
    Page<PaperGenerationHistory> findByCreatedBy(Long createdBy, Pageable pageable);
    
    /**
     * 统计用户的生成历史数量
     */
    long countByCreatedBy(Long createdBy);
    
    /**
     * 统计成功生成的试卷数量
     */
    long countByStatus(PaperGenerationHistory.GenerationStatus status);
    
    /**
     * 查询最近的生成历史
     */
    @Query("SELECT h FROM PaperGenerationHistory h ORDER BY h.createdAt DESC")
    List<PaperGenerationHistory> findRecentHistory(Pageable pageable);
    
    /**
     * 根据创建者和状态查询
     */
    List<PaperGenerationHistory> findByCreatedByAndStatus(Long createdBy, PaperGenerationHistory.GenerationStatus status);
}

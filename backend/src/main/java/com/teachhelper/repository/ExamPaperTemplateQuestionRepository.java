package com.teachhelper.repository;

import com.teachhelper.entity.ExamPaperTemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 试卷模板题目Repository
 */
@Repository
public interface ExamPaperTemplateQuestionRepository extends JpaRepository<ExamPaperTemplateQuestion, Long> {
    
    /**
     * 根据模板ID查找题目
     */
    List<ExamPaperTemplateQuestion> findByTemplateIdOrderByQuestionOrderAsc(Long templateId);
    
    /**
     * 根据模板ID和题目类型查找题目
     */
    List<ExamPaperTemplateQuestion> findByTemplateIdAndQuestionTypeOrderByQuestionOrderAsc(Long templateId, String questionType);
    
    /**
     * 根据模板ID和状态查找题目
     */
    List<ExamPaperTemplateQuestion> findByTemplateIdAndStatusOrderByQuestionOrderAsc(Long templateId, ExamPaperTemplateQuestion.QuestionStatus status);
    
    /**
     * 根据题目ID查找模板题目
     */
    List<ExamPaperTemplateQuestion> findByQuestionId(Long questionId);
    
    /**
     * 统计模板中的题目数量
     */
    long countByTemplateId(Long templateId);
    
    /**
     * 统计模板中已配置的题目数量
     */
    @Query("SELECT COUNT(q) FROM ExamPaperTemplateQuestion q WHERE q.template.id = :templateId AND (q.questionId IS NOT NULL OR q.questionContent IS NOT NULL)")
    long countConfiguredQuestionsByTemplateId(@Param("templateId") Long templateId);
    
    /**
     * 统计模板中就绪的题目数量
     */
    long countByTemplateIdAndStatus(Long templateId, ExamPaperTemplateQuestion.QuestionStatus status);
    
    /**
     * 查找模板中最大的题目序号
     */
    @Query("SELECT MAX(q.questionOrder) FROM ExamPaperTemplateQuestion q WHERE q.template.id = :templateId")
    Integer findMaxQuestionOrderByTemplateId(@Param("templateId") Long templateId);
    
    /**
     * 根据模板ID删除所有题目
     */
    void deleteByTemplateId(Long templateId);
    
    /**
     * 查找引用指定题目的所有模板题目
     */
    @Query("SELECT q FROM ExamPaperTemplateQuestion q WHERE q.questionId = :questionId")
    List<ExamPaperTemplateQuestion> findByReferencedQuestionId(@Param("questionId") Long questionId);
} 
package com.teachhelper.repository;

import com.teachhelper.entity.ExamTemplate;
import com.teachhelper.entity.ExamTemplateQuestion;
import com.teachhelper.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTemplateQuestionRepository extends JpaRepository<ExamTemplateQuestion, Long> {
    
    /**
     * 根据模板查找所有题目
     */
    List<ExamTemplateQuestion> findByExamTemplateOrderByQuestionNumber(ExamTemplate examTemplate);
    
    /**
     * 根据模板和题目序号查找
     */
    Optional<ExamTemplateQuestion> findByExamTemplateAndQuestionNumber(ExamTemplate examTemplate, Integer questionNumber);
    
    /**
     * 查找模板中未匹配的题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndIsMatchedFalseOrderByQuestionNumber(ExamTemplate examTemplate);
    
    /**
     * 查找模板中已匹配的题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndIsMatchedTrueOrderByQuestionNumber(ExamTemplate examTemplate);
    
    /**
     * 查找需要人工审核的题目
     */
    @Query("SELECT q FROM ExamTemplateQuestion q WHERE q.examTemplate = :template AND (q.hasIssues = true OR q.matchingConfidence < 0.6 OR q.isMatched = false) ORDER BY q.questionNumber")
    List<ExamTemplateQuestion> findQuestionsNeedingReview(@Param("template") ExamTemplate template);
    
    /**
     * 统计模板的匹配情况
     */
    @Query("SELECT COUNT(q) FROM ExamTemplateQuestion q WHERE q.examTemplate = :template AND q.isMatched = true")
    long countMatchedQuestions(@Param("template") ExamTemplate template);
    
    /**
     * 根据段落标题查找题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndSectionHeaderOrderByQuestionNumber(ExamTemplate examTemplate, String sectionHeader);
    
    /**
     * 根据题目类型查找题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndQuestionTypeOrderByQuestionNumber(ExamTemplate examTemplate, String questionType);
    
    /**
     * 查找匹配到特定题目的模板题目
     */
    List<ExamTemplateQuestion> findByMatchedQuestion(Question question);
    
    /**
     * 根据匹配置信度查询
     */
    @Query("SELECT q FROM ExamTemplateQuestion q WHERE q.examTemplate = :template AND q.matchingConfidence >= :minConfidence ORDER BY q.matchingConfidence DESC")
    List<ExamTemplateQuestion> findByConfidenceAbove(@Param("template") ExamTemplate template, @Param("minConfidence") Double minConfidence);
    
    /**
     * 查找指定模板的所有题目
     */
    List<ExamTemplateQuestion> findByExamTemplate(ExamTemplate examTemplate);
    
    /**
     * 根据匹配状态查找题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndIsMatched(ExamTemplate examTemplate, Boolean isMatched);
    
    /**
     * 删除模板的所有题目
     */
    void deleteByExamTemplate(ExamTemplate examTemplate);
    
    /**
     * 根据ID和模板查找题目
     */
    Optional<ExamTemplateQuestion> findByIdAndExamTemplate(Long id, ExamTemplate examTemplate);
    
    /**
     * 查找已确认的题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndIsConfirmedTrueOrderByQuestionNumber(ExamTemplate examTemplate);
    
    /**
     * 查找未确认的题目
     */
    List<ExamTemplateQuestion> findByExamTemplateAndIsConfirmedFalseOrderByQuestionNumber(ExamTemplate examTemplate);
    
    /**
     * 统计已确认题目数量
     */
    @Query("SELECT COUNT(q) FROM ExamTemplateQuestion q WHERE q.examTemplate = :template AND q.isConfirmed = true")
    long countConfirmedQuestions(@Param("template") ExamTemplate template);
} 
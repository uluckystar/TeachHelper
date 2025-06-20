package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByExamIdOrderByCreatedAt(Long examId);
    
    List<Question> findByExamId(Long examId);
    
    Page<Question> findByExamId(Long examId, Pageable pageable);
    
    @Query("SELECT q FROM Question q WHERE q.exam.id = :examId AND q.title LIKE %:keyword%")
    List<Question> findByExamIdAndTitleContaining(@Param("examId") Long examId, @Param("keyword") String keyword);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.exam.id = :examId")
    long countByExamId(@Param("examId") Long examId);
    
    // 基于知识库和知识点的查询方法
    List<Question> findBySourceKnowledgeBaseId(Long sourceKnowledgeBaseId);
    
    Page<Question> findBySourceKnowledgeBaseIdOrderByCreatedAtDesc(Long sourceKnowledgeBaseId, Pageable pageable);
    
    List<Question> findBySourceKnowledgePointId(Long sourceKnowledgePointId);
    
    List<Question> findBySourceKnowledgeBaseIdAndSourceKnowledgePointId(Long sourceKnowledgeBaseId, Long sourceKnowledgePointId);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.sourceKnowledgeBaseId = :knowledgeBaseId")
    long countBySourceKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.sourceKnowledgePointId = :knowledgePointId")
    long countBySourceKnowledgePointId(@Param("knowledgePointId") Long knowledgePointId);

    void deleteByExamId(Long examId);
    
    // 题目库相关查询方法
    /**
     * 根据题目库ID查询题目
     */
    Page<Question> findByQuestionBankIdAndIsActiveTrueOrderByCreatedAtDesc(Long questionBankId, Pageable pageable);
    
    /**
     * 根据创建者查询题目
     */
    Page<Question> findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(Long createdBy, Pageable pageable);
    
    /**
     * 查询题目库中的题目数量
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.questionBank.id = :questionBankId AND q.isActive = true")
    long countByQuestionBankId(@Param("questionBankId") Long questionBankId);
    
    /**
     * 按题目类型查询
     */
    @Query("SELECT q FROM Question q WHERE q.questionBank.id = :questionBankId AND q.questionType = :questionType AND q.isActive = true ORDER BY q.createdAt DESC")
    List<Question> findByQuestionBankIdAndQuestionType(@Param("questionBankId") Long questionBankId, @Param("questionType") com.teachhelper.entity.QuestionType questionType);
    
    /**
     * 按难度查询
     */
    @Query("SELECT q FROM Question q WHERE q.questionBank.id = :questionBankId AND q.difficulty = :difficulty AND q.isActive = true ORDER BY q.createdAt DESC")
    List<Question> findByQuestionBankIdAndDifficulty(@Param("questionBankId") Long questionBankId, @Param("difficulty") com.teachhelper.entity.DifficultyLevel difficulty);
    
    /**
     * 按标签搜索题目
     */
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND (q.tags LIKE %:tag% OR q.keywords LIKE %:tag%) ORDER BY q.createdAt DESC")
    List<Question> findByTagsContaining(@Param("tag") String tag);
    
    /**
     * 全文搜索题目
     */
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND (q.title LIKE %:keyword% OR q.content LIKE %:keyword% OR q.keywords LIKE %:keyword%) ORDER BY q.createdAt DESC")
    Page<Question> searchQuestions(@Param("keyword") String keyword, Pageable pageable);
}

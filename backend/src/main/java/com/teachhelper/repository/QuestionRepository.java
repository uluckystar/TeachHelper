package com.teachhelper.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    // 预加载options、exam和questionBank的分页查询
    @EntityGraph(attributePaths = {"options", "exam", "questionBank"})
    Page<Question> findAll(Pageable pageable);
    
    // 预加载options、exam和questionBank的按考试ID查询
    @EntityGraph(attributePaths = {"options", "exam", "questionBank"})
    List<Question> findByExamId(Long examId);
    
    List<Question> findByExamIdOrderByCreatedAt(Long examId);
    
    /**
     * 根据考试ID查询题目，预加载选项
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "WHERE q.exam.id = :examId " +
           "ORDER BY q.createdAt")
    List<Question> findByExamIdWithOptions(@Param("examId") Long examId);
    
    /**
     * 根据考试ID查询题目，预加载评分标准
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.rubricCriteria " +
           "WHERE q.exam.id = :examId " +
           "ORDER BY q.createdAt")
    List<Question> findByExamIdWithCriteria(@Param("examId") Long examId);
    
    /**
     * 根据ID查询题目，预加载选项
     */
    @Query("SELECT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "WHERE q.id = :id")
    java.util.Optional<Question> findByIdWithOptions(@Param("id") Long id);
    
    /**
     * 根据ID查询题目，预加载评分标准
     */
    @Query("SELECT q FROM Question q " +
           "LEFT JOIN FETCH q.rubricCriteria " +
           "WHERE q.id = :id")
    java.util.Optional<Question> findByIdWithCriteria(@Param("id") Long id);
    
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
     * 多条件搜索题目 - 第一步：获取基本信息和选项
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "LEFT JOIN q.exam e " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "       q.title LIKE %:keyword% OR q.content LIKE %:keyword% OR q.keywords LIKE %:keyword%) " +
           "AND (:questionType IS NULL OR :questionType = '' OR q.questionType = :questionType) " +
           "AND (:sourceType IS NULL OR :sourceType = '' OR q.sourceType = :sourceType) " +
           "AND (:examId IS NULL OR e.id = :examId) " +
           "AND q.isActive = true " +
           "ORDER BY q.createdAt DESC")
    Page<Question> searchQuestionsWithFilters(
        @Param("keyword") String keyword,
        @Param("questionType") String questionType,
        @Param("sourceType") String sourceType,
        @Param("examId") Long examId,
        Pageable pageable
    );
    
    /**
     * 为指定题目加载评分标准
     */
    @Query("SELECT q FROM Question q " +
           "LEFT JOIN FETCH q.rubricCriteria " +
           "WHERE q.id IN :questionIds")
    List<Question> loadRubricCriteriaForQuestions(@Param("questionIds") List<Long> questionIds);
    
    /**
     * 分页查询题目，预加载选项和评分标准
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "LEFT JOIN FETCH q.rubricCriteria " +
           "ORDER BY q.createdAt DESC")
    Page<Question> findAllWithOptionsAndCriteria(Pageable pageable);
    
    /**
     * 分页查询题目，预加载选项
     */
    @Query("SELECT q FROM Question q " +
           "LEFT JOIN FETCH q.options " +
           "ORDER BY q.createdAt DESC")
    Page<Question> findAllWithOptions(Pageable pageable);
}

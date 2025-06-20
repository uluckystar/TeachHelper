package com.teachhelper.repository;

import com.teachhelper.entity.QuestionReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 题目引用Repository
 */
@Repository
public interface QuestionReferenceRepository extends JpaRepository<QuestionReference, Long> {
    
    /**
     * 查找考试的所有题目引用
     */
    List<QuestionReference> findByExamIdAndIsActiveTrueOrderByQuestionOrderAsc(Long examId);
    
    /**
     * 查找题目的所有引用
     */
    List<QuestionReference> findByQuestionIdAndIsActiveTrueOrderByIdDesc(Long questionId);
    
    /**
     * 查找特定考试中的题目引用
     */
    Optional<QuestionReference> findByExamIdAndQuestionIdAndIsActiveTrue(Long examId, Long questionId);
    
    /**
     * 检查题目是否已被考试引用
     */
    boolean existsByExamIdAndQuestionIdAndIsActiveTrue(Long examId, Long questionId);
    
    /**
     * 获取考试的题目数量
     */
    @Query("SELECT COUNT(qr) FROM QuestionReference qr WHERE qr.exam.id = :examId AND qr.isActive = true")
    long countByExamId(@Param("examId") Long examId);
    
    /**
     * 获取题目被引用的次数
     */
    @Query("SELECT COUNT(qr) FROM QuestionReference qr WHERE qr.question.id = :questionId AND qr.isActive = true")
    long countByQuestionId(@Param("questionId") Long questionId);
    
    /**
     * 批量删除考试的所有题目引用
     */
    void deleteByExamIdAndIsActiveTrue(Long examId);
    
    /**
     * 批量删除题目的所有引用
     */
    void deleteByQuestionIdAndIsActiveTrue(Long questionId);
}

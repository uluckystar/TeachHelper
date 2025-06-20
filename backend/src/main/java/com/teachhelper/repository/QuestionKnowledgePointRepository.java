package com.teachhelper.repository;

import com.teachhelper.entity.QuestionKnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 题目知识点关联Repository
 */
@Repository
public interface QuestionKnowledgePointRepository extends JpaRepository<QuestionKnowledgePoint, Long> {
    
    /**
     * 查找题目的所有关联知识点
     */
    List<QuestionKnowledgePoint> findByQuestionIdOrderByRelevanceScoreDesc(Long questionId);
    
    /**
     * 查找知识点的所有关联题目
     */
    List<QuestionKnowledgePoint> findByKnowledgePointIdOrderByRelevanceScoreDesc(Long knowledgePointId);
    
    /**
     * 查找题目的主要知识点
     */
    List<QuestionKnowledgePoint> findByQuestionIdAndIsPrimaryTrueOrderByRelevanceScoreDesc(Long questionId);
    
    /**
     * 查找知识点的主要关联题目
     */
    List<QuestionKnowledgePoint> findByKnowledgePointIdAndIsPrimaryTrueOrderByRelevanceScoreDesc(Long knowledgePointId);
    
    /**
     * 查找特定的题目-知识点关联
     */
    Optional<QuestionKnowledgePoint> findByQuestionIdAndKnowledgePointId(Long questionId, Long knowledgePointId);
    
    /**
     * 检查题目是否关联了指定知识点
     */
    boolean existsByQuestionIdAndKnowledgePointId(Long questionId, Long knowledgePointId);
    
    /**
     * 获取题目关联的知识点数量
     */
    @Query("SELECT COUNT(qkp) FROM QuestionKnowledgePoint qkp WHERE qkp.question.id = :questionId")
    long countByQuestionId(@Param("questionId") Long questionId);
    
    /**
     * 获取知识点关联的题目数量
     */
    @Query("SELECT COUNT(qkp) FROM QuestionKnowledgePoint qkp WHERE qkp.knowledgePoint.id = :knowledgePointId")
    long countByKnowledgePointId(@Param("knowledgePointId") Long knowledgePointId);
    
    /**
     * 批量删除题目的所有知识点关联
     */
    void deleteByQuestionId(Long questionId);
    
    /**
     * 批量删除知识点的所有题目关联
     */
    void deleteByKnowledgePointId(Long knowledgePointId);
    
    /**
     * 查找知识库下的所有题目知识点关联
     */
    @Query("SELECT qkp FROM QuestionKnowledgePoint qkp WHERE qkp.knowledgePoint.knowledgeBaseId = :knowledgeBaseId")
    List<QuestionKnowledgePoint> findByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}

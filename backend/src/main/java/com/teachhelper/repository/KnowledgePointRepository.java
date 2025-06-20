package com.teachhelper.repository;

import com.teachhelper.entity.KnowledgePoint;
import com.teachhelper.entity.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Long> {
    
    /**
     * 根据知识库ID查询知识点
     */
    List<KnowledgePoint> findByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 根据知识库ID查询知识点，按创建时间倒序
     */
    List<KnowledgePoint> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);

    /**
     * 根据知识库ID分页查询知识点，按创建时间倒序
     */
    Page<KnowledgePoint> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId, Pageable pageable);

    /**
     * 根据知识库ID和难度级别分页查询知识点
     */
    Page<KnowledgePoint> findByKnowledgeBaseIdAndDifficultyLevel(Long knowledgeBaseId, DifficultyLevel difficultyLevel, Pageable pageable);

    /**
     * 根据知识库ID和标题关键词分页查询知识点
     */
    Page<KnowledgePoint> findByKnowledgeBaseIdAndTitleContainingIgnoreCase(Long knowledgeBaseId, String keyword, Pageable pageable);

    /**
     * 根据知识库ID、难度级别和标题关键词分页查询知识点
     */
    Page<KnowledgePoint> findByKnowledgeBaseIdAndDifficultyLevelAndTitleContainingIgnoreCase(Long knowledgeBaseId, DifficultyLevel difficultyLevel, String keyword, Pageable pageable);
    
    /**
     * 根据源文档ID查询知识点
     */
    List<KnowledgePoint> findBySourceDocumentId(Long sourceDocumentId);
    
    /**
     * 删除指定文档的所有知识点
     */
    void deleteBySourceDocumentId(Long sourceDocumentId);
    
    /**
     * 根据难度级别查询知识点
     */
    @Query("SELECT kp FROM KnowledgePoint kp WHERE kp.knowledgeBaseId = :knowledgeBaseId AND kp.difficultyLevel = :difficultyLevel")
    List<KnowledgePoint> findByKnowledgeBaseIdAndDifficultyLevel(@Param("knowledgeBaseId") Long knowledgeBaseId, 
                                                                  @Param("difficultyLevel") DifficultyLevel difficultyLevel);
    
    /**
     * 搜索知识点标题和内容
     */
    @Query("SELECT kp FROM KnowledgePoint kp WHERE kp.knowledgeBaseId = :knowledgeBaseId AND " +
           "(kp.title LIKE %:keyword% OR kp.content LIKE %:keyword% OR kp.keywords LIKE %:keyword%)")
    List<KnowledgePoint> searchByKeyword(@Param("knowledgeBaseId") Long knowledgeBaseId, 
                                         @Param("keyword") String keyword);
    
    /**
     * 统计知识库中的知识点数量
     */
    Long countByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 统计指定文档的知识点数量
     */
    Long countBySourceDocumentId(Long sourceDocumentId);
    
    /**
     * 根据向量ID查询知识点
     */
    List<KnowledgePoint> findByVectorId(String vectorId);
    
    /**
     * 删除知识库下的所有知识点
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}

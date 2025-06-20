package com.teachhelper.repository;

import com.teachhelper.entity.KnowledgeBaseFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库收藏Repository
 */
@Repository
public interface KnowledgeBaseFavoriteRepository extends JpaRepository<KnowledgeBaseFavorite, Long> {
    
    /**
     * 检查用户是否收藏了指定知识库
     */
    boolean existsByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId);
    
    /**
     * 根据用户ID和知识库ID查找收藏记录
     */
    Optional<KnowledgeBaseFavorite> findByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId);
    
    /**
     * 获取用户收藏的所有知识库ID列表
     */
    @Query("SELECT f.knowledgeBaseId FROM KnowledgeBaseFavorite f WHERE f.userId = :userId")
    List<Long> findKnowledgeBaseIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 统计知识库的收藏数量
     */
    long countByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 删除收藏记录
     */
    void deleteByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId);
    
    /**
     * 获取用户收藏的知识库列表（带详细信息）
     */
    @Query("SELECT f FROM KnowledgeBaseFavorite f " +
           "JOIN KnowledgeBase kb ON f.knowledgeBaseId = kb.id " +
           "WHERE f.userId = :userId AND kb.isActive = true " +
           "ORDER BY f.createdAt DESC")
    List<KnowledgeBaseFavorite> findUserFavoritesWithKnowledgeBase(@Param("userId") Long userId);
}

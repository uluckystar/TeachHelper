package com.teachhelper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.UserAIConfig;

@Repository
public interface UserAIConfigRepository extends JpaRepository<UserAIConfig, Long> {
    
    /**
     * 根据用户ID查找所有AI配置（包括未激活的）
     */
    List<UserAIConfig> findByUserId(Long userId);
    
    /**
     * 根据用户ID查找所有激活的AI配置
     */
    List<UserAIConfig> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * 根据用户ID查找默认AI配置
     */
    Optional<UserAIConfig> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);
    
    /**
     * 根据用户ID和提供商查找配置
     */
    Optional<UserAIConfig> findByUserIdAndProviderAndIsActiveTrue(Long userId, AIProvider provider);
    
    /**
     * 检查用户是否已有该提供商的配置
     */
    boolean existsByUserIdAndProvider(Long userId, AIProvider provider);
    
    /**
     * 清除用户的所有默认配置
     */
    @Modifying
    @Query("UPDATE UserAIConfig u SET u.isDefault = false WHERE u.userId = :userId")
    void clearDefaultForUser(@Param("userId") Long userId);
    
    /**
     * 统计用户的AI配置数量
     */
    long countByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * 统计所有活跃的AI配置数量
     */
    @Query("SELECT COUNT(u) FROM UserAIConfig u WHERE u.isActive = true")
    long countByIsActiveTrue();
    
    /**
     * 根据用户ID删除所有配置
     */
    void deleteByUserId(Long userId);
}

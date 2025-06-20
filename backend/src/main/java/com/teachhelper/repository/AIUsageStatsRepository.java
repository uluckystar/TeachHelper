package com.teachhelper.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.AIUsageStats;

@Repository
public interface AIUsageStatsRepository extends JpaRepository<AIUsageStats, Long> {
    
    /**
     * 根据用户ID查找使用统计
     */
    List<AIUsageStats> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户ID和时间范围查找统计
     */
    List<AIUsageStats> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据用户ID和提供商查找统计
     */
    List<AIUsageStats> findByUserIdAndProviderOrderByCreatedAtDesc(Long userId, AIProvider provider);
    
    /**
     * 统计用户的总token使用量
     */
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM AIUsageStats u WHERE u.userId = :userId")
    Long getTotalTokensByUser(@Param("userId") Long userId);
    
    /**
     * 统计用户的总费用
     */
    @Query("SELECT COALESCE(SUM(u.totalCost), 0) FROM AIUsageStats u WHERE u.userId = :userId")
    BigDecimal getTotalCostByUser(@Param("userId") Long userId);
    
    /**
     * 统计用户的请求次数
     */
    @Query("SELECT COUNT(u) FROM AIUsageStats u WHERE u.userId = :userId")
    Long getTotalRequestsByUser(@Param("userId") Long userId);
    
    /**
     * 统计用户特定时间范围内的使用情况
     */
    @Query("SELECT new com.teachhelper.dto.response.AIUsageSummaryDTO(" +
           "u.provider, " +
           "COUNT(u), " +
           "COALESCE(SUM(u.inputTokens), 0), " +
           "COALESCE(SUM(u.outputTokens), 0), " +
           "COALESCE(SUM(u.totalTokens), 0), " +
           "COALESCE(SUM(u.totalCost), 0)) " +
           "FROM AIUsageStats u " +
           "WHERE u.userId = :userId AND u.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY u.provider")
    List<com.teachhelper.dto.response.AIUsageSummaryDTO> getUsageSummaryByUserAndDateRange(
        @Param("userId") Long userId, 
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户每日使用情况
     */
    @Query("SELECT DATE(u.createdAt) as date, " +
           "COUNT(u) as requestCount, " +
           "COALESCE(SUM(u.totalTokens), 0) as totalTokens, " +
           "COALESCE(SUM(u.totalCost), 0) as totalCost " +
           "FROM AIUsageStats u " +
           "WHERE u.userId = :userId AND u.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(u.createdAt) " +
           "ORDER BY DATE(u.createdAt) DESC")
    List<Object[]> getDailyUsageStats(@Param("userId") Long userId, 
                                     @Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据用户ID删除所有统计
     */
    void deleteByUserId(Long userId);
    
    /**
     * 统计指定配置在时间范围内的请求数
     */
    Long countByConfigIdAndCreatedAtBetween(Long configId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算指定配置在时间范围内的总费用
     */
    @Query("SELECT COALESCE(SUM(u.totalCost), 0) FROM AIUsageStats u WHERE u.configId = :configId AND u.createdAt BETWEEN :startTime AND :endTime")
    BigDecimal sumTotalCostByConfigIdAndCreatedAtBetween(@Param("configId") Long configId, 
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);
}

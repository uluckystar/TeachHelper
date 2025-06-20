package com.teachhelper.service;

import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.AIUsageStats;
import com.teachhelper.repository.AIUsageStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI使用统计服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIUsageStatsService {
    
    private final AIUsageStatsRepository aiUsageStatsRepository;
    
    /**
     * 记录AI使用统计
     */
    @Transactional
    public void recordUsage(Long configId, Long userId, String provider, String model, 
                           Integer inputTokens, Integer outputTokens, Integer totalTokens, 
                           Double cost, Long durationMs) {
        try {
            AIUsageStats stats = new AIUsageStats();
            stats.setConfigId(configId);
            stats.setUserId(userId);
            stats.setProvider(AIProvider.valueOf(provider));
            stats.setModelName(model);
            stats.setInputTokens(inputTokens);
            stats.setOutputTokens(outputTokens);
            stats.setTotalTokens(totalTokens);
            stats.setTotalCost(BigDecimal.valueOf(cost));
            stats.setRequestDurationMs(durationMs);
            stats.setRequestType("TEST");
            stats.setStatus("SUCCESS");
            stats.setCreatedAt(LocalDateTime.now());
            
            aiUsageStatsRepository.save(stats);
            
            log.debug("Recorded AI usage stats: configId={}, userId={}, provider={}, tokens={}, cost={}", 
                configId, userId, provider, totalTokens, cost);
                
        } catch (Exception e) {
            log.error("Failed to record AI usage stats", e);
            // 不抛出异常，避免影响主要业务流程
        }
    }
    
    /**
     * 获取用户总使用统计
     */
    public AIUsageStats getUserTotalStats(Long userId) {
        // 这里可以实现聚合查询，返回用户的总统计信息
        // 暂时返回null，后续可以扩展
        return null;
    }
    
    /**
     * 获取配置使用统计
     */
    public AIUsageStats getConfigStats(Long configId) {
        // 这里可以实现聚合查询，返回配置的统计信息
        // 暂时返回null，后续可以扩展
        return null;
    }
}
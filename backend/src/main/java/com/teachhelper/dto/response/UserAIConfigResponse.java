package com.teachhelper.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.teachhelper.entity.AIProvider;

import lombok.Data;

@Data
public class UserAIConfigResponse {
    private Long id;
    private AIProvider provider;
    private String providerDisplayName;
    private String maskedApiKey;  // 改为maskedApiKey，更清晰
    private String apiEndpoint;
    private String modelName;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Boolean isActive;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UsageStats usageStats;  // 添加使用统计
    private String description;  // 添加描述字段
    
    public UserAIConfigResponse() {}
    
    public UserAIConfigResponse(Long id, AIProvider provider, String maskedApiKey, String apiEndpoint,
            String modelName, Integer maxTokens, BigDecimal temperature, Boolean isActive,
            Boolean isDefault, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.provider = provider;
        this.providerDisplayName = provider != null ? provider.getDisplayName() : "";
        this.maskedApiKey = maskedApiKey;
        this.apiEndpoint = apiEndpoint;
        this.modelName = modelName;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.isActive = isActive;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * 使用统计内部类
     */
    @Data
    public static class UsageStats {
        private Long totalRequests;
        private BigDecimal totalCost;
        
        public UsageStats() {}
        
        public UsageStats(Long totalRequests, BigDecimal totalCost) {
            this.totalRequests = totalRequests;
            this.totalCost = totalCost;
        }
    }
}

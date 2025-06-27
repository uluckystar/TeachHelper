package com.teachhelper.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.teachhelper.entity.AIProvider;

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
    
    // Getter方法
    public Long getId() {
        return id;
    }
    
    public AIProvider getProvider() {
        return provider;
    }
    
    public String getProviderDisplayName() {
        return providerDisplayName;
    }
    
    public String getMaskedApiKey() {
        return maskedApiKey;
    }
    
    public String getApiEndpoint() {
        return apiEndpoint;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public BigDecimal getTemperature() {
        return temperature;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public UsageStats getUsageStats() {
        return usageStats;
    }
    
    public String getDescription() {
        return description;
    }
    
    // Setter方法
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setProvider(AIProvider provider) {
        this.provider = provider;
    }
    
    public void setProviderDisplayName(String providerDisplayName) {
        this.providerDisplayName = providerDisplayName;
    }
    
    public void setMaskedApiKey(String maskedApiKey) {
        this.maskedApiKey = maskedApiKey;
    }
    
    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setUsageStats(UsageStats usageStats) {
        this.usageStats = usageStats;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
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
    public static class UsageStats {
        private Long totalRequests;
        private BigDecimal totalCost;
        
        public UsageStats() {}
        
        public UsageStats(Long totalRequests, BigDecimal totalCost) {
            this.totalRequests = totalRequests;
            this.totalCost = totalCost;
        }
        
        // Getter方法
        public Long getTotalRequests() {
            return totalRequests;
        }
        
        public BigDecimal getTotalCost() {
            return totalCost;
        }
        
        // Setter方法
        public void setTotalRequests(Long totalRequests) {
            this.totalRequests = totalRequests;
        }
        
        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }
        
        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UsageStats that = (UsageStats) o;
            return Objects.equals(totalRequests, that.totalRequests) &&
                   Objects.equals(totalCost, that.totalCost);
        }
        
        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(totalRequests, totalCost);
        }
        
        // toString方法
        @Override
        public String toString() {
            return "UsageStats{" +
                    "totalRequests=" + totalRequests +
                    ", totalCost=" + totalCost +
                    '}';
        }
    }
}

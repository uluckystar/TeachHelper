package com.teachhelper.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * AI使用统计实体
 */
@Entity
@Table(name = "ai_usage_stats")
public class AIUsageStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "config_id", nullable = false)
    private Long configId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AIProvider provider;
    
    @Column(name = "model_name", length = 100)
    private String modelName;
    
    @Column(name = "request_type", length = 50)
    private String requestType; // EVALUATION, RUBRIC_GENERATION, CHAT
    
    @Column(name = "input_tokens")
    private Integer inputTokens = 0;
    
    @Column(name = "output_tokens")
    private Integer outputTokens = 0;
    
    @Column(name = "total_tokens")
    private Integer totalTokens = 0;
    
    @Column(name = "input_cost", precision = 10, scale = 6)
    private BigDecimal inputCost = BigDecimal.ZERO;
    
    @Column(name = "output_cost", precision = 10, scale = 6)
    private BigDecimal outputCost = BigDecimal.ZERO;
    
    @Column(name = "total_cost", precision = 10, scale = 6)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(name = "request_duration_ms")
    private Long requestDurationMs;
    
    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILED, TIMEOUT
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (totalTokens == null) {
            totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
        }
        if (totalCost == null) {
            totalCost = (inputCost != null ? inputCost : BigDecimal.ZERO)
                       .add(outputCost != null ? outputCost : BigDecimal.ZERO);
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getConfigId() {
        return configId;
    }
    
    public void setConfigId(Long configId) {
        this.configId = configId;
    }
    
    public AIProvider getProvider() {
        return provider;
    }
    
    public void setProvider(AIProvider provider) {
        this.provider = provider;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getRequestType() {
        return requestType;
    }
    
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    
    public Integer getInputTokens() {
        return inputTokens;
    }
    
    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }
    
    public Integer getOutputTokens() {
        return outputTokens;
    }
    
    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }
    
    public Integer getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public BigDecimal getInputCost() {
        return inputCost;
    }
    
    public void setInputCost(BigDecimal inputCost) {
        this.inputCost = inputCost;
    }
    
    public BigDecimal getOutputCost() {
        return outputCost;
    }
    
    public void setOutputCost(BigDecimal outputCost) {
        this.outputCost = outputCost;
    }
    
    public BigDecimal getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
    
    public Long getRequestDurationMs() {
        return requestDurationMs;
    }
    
    public void setRequestDurationMs(Long requestDurationMs) {
        this.requestDurationMs = requestDurationMs;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

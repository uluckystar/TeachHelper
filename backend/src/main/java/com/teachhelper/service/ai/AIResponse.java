package com.teachhelper.service.ai;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * AI响应结果包装类
 */
public class AIResponse {
    private String content;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Long durationMs;
    private boolean success;
    private String errorMessage;
    private String errorCode;
    private String model;
    private String provider;
    private LocalDateTime responseTime = LocalDateTime.now();
    private String requestId;
    private Map<String, Object> metadata = new HashMap<>();
    
    public AIResponse() {}
    
    public AIResponse(String content, Integer inputTokens, Integer outputTokens, Long durationMs) {
        this.content = content;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
        this.durationMs = durationMs;
        this.success = true;
    }
    
    public static AIResponse error(String errorMessage, Long durationMs) {
        AIResponse response = new AIResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        response.durationMs = durationMs;
        return response;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
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
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public LocalDateTime getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // 便捷方法
    public static AIResponse success(String content) {
        AIResponse response = new AIResponse();
        response.setContent(content);
        response.setSuccess(true);
        return response;
    }
    
    public static AIResponse failure(String errorMessage) {
        AIResponse response = new AIResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    public static AIResponse failure(String errorCode, String errorMessage) {
        AIResponse response = new AIResponse();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    public AIResponse withTokens(Integer inputTokens, Integer outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
        return this;
    }
    
    public AIResponse withDuration(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }
    
    public AIResponse withModel(String provider, String model) {
        this.provider = provider;
        this.model = model;
        return this;
    }
    
    public AIResponse withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}

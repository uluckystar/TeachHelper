package com.teachhelper.service.ai;

import java.util.Objects;

import java.util.Map;
import java.util.HashMap;

/**
 * 统一AI请求DTO
 */
public class AIRequest {
    
    /**
     * 用户提示内容
     */
    private String prompt;
    
    /**
     * 系统提示（可选）
     */
    private String systemPrompt;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * AI提供商
     */
    private String provider;
    
    /**
     * 最大token数
     */
    private Integer maxTokens = 2000;
    
    /**
     * 温度参数 (0.0-2.0)
     */
    private Double temperature = 0.7;
    
    /**
     * 用户ID（用于配置选择和统计）
     */
    private Long userId;
    
    /**
     * 请求类型（用于统计分类）
     */
    private String requestType = "CHAT";
    
    /**
     * 是否启用流式响应
     */
    private Boolean stream = false;
    
    /**
     * 自定义参数
     */
    private Map<String, Object> customParams = new HashMap<>();
    
    // 构造函数
    public AIRequest() {}
    
    public AIRequest(String prompt, String systemPrompt, String model, String provider, 
                    Integer maxTokens, Double temperature, Long userId, String requestType,
                    Boolean stream, Map<String, Object> customParams) {
        this.prompt = prompt;
        this.systemPrompt = systemPrompt;
        this.model = model;
        this.provider = provider;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.userId = userId;
        this.requestType = requestType;
        this.stream = stream;
        this.customParams = customParams != null ? customParams : new HashMap<>();
    }
    
    // Getter and Setter methods
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
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
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getRequestType() {
        return requestType;
    }
    
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    public Map<String, Object> getCustomParams() {
        return customParams;
    }
    
    public void setCustomParams(Map<String, Object> customParams) {
        this.customParams = customParams != null ? customParams : new HashMap<>();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIRequest aiRequest = (AIRequest) o;
        return Objects.equals(prompt, aiRequest.prompt) &&
               Objects.equals(systemPrompt, aiRequest.systemPrompt) &&
               Objects.equals(model, aiRequest.model) &&
               Objects.equals(provider, aiRequest.provider) &&
               Objects.equals(maxTokens, aiRequest.maxTokens) &&
               Objects.equals(temperature, aiRequest.temperature) &&
               Objects.equals(userId, aiRequest.userId) &&
               Objects.equals(requestType, aiRequest.requestType) &&
               Objects.equals(stream, aiRequest.stream) &&
               Objects.equals(customParams, aiRequest.customParams);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(prompt, systemPrompt, model, provider, maxTokens, temperature,
                          userId, requestType, stream, customParams);
    }
    
    @Override
    public String toString() {
        return "AIRequest{" +
               "prompt='" + prompt + '\'' +
               ", systemPrompt='" + systemPrompt + '\'' +
               ", model='" + model + '\'' +
               ", provider='" + provider + '\'' +
               ", maxTokens=" + maxTokens +
               ", temperature=" + temperature +
               ", userId=" + userId +
               ", requestType='" + requestType + '\'' +
               ", stream=" + stream +
               ", customParams=" + customParams +
               '}';
    }
    
    /**
     * 构建简单的聊天请求
     */
    public static AIRequest chat(String prompt) {
        AIRequest request = new AIRequest();
        request.setPrompt(prompt);
        request.setRequestType("CHAT");
        return request;
    }
    
    /**
     * 构建知识提取请求
     */
    public static AIRequest knowledgeExtraction(String content) {
        AIRequest request = new AIRequest();
        request.setPrompt(content);
        request.setRequestType("KNOWLEDGE_EXTRACTION");
        request.setSystemPrompt("你是一个专业的知识点提取助手，请从给定内容中提取关键知识点。");
        return request;
    }
    
    /**
     * 构建题目生成请求
     */
    public static AIRequest questionGeneration(String content, String questionType) {
        AIRequest request = new AIRequest();
        request.setPrompt(content);
        request.setRequestType("QUESTION_GENERATION");
        request.setSystemPrompt("你是一个专业的题目生成助手，请根据给定内容生成" + questionType + "题目。");
        return request;
    }
    
    /**
     * 构建评分请求
     */
    public static AIRequest evaluation(String question, String answer) {
        AIRequest request = new AIRequest();
        request.setPrompt("题目：" + question + "\n答案：" + answer);
        request.setRequestType("EVALUATION");
        request.setSystemPrompt("你是一个专业的答案评分助手，请对学生答案进行客观评分。");
        return request;
    }
}
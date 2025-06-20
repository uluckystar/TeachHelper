package com.teachhelper.service.ai;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * 统一AI请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
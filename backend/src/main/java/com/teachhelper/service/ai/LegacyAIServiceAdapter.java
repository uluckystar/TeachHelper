package com.teachhelper.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 旧AI服务适配器 - 用于兼容现有的AI服务调用
 */
@Service
public class LegacyAIServiceAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(LegacyAIServiceAdapter.class);
    
    /**
     * 适配旧的AI调用方式
     */
    public String callAI(String prompt, String model) {
        try {
            // 临时实现，返回默认响应
            log.info("Legacy AI service called with model: {}", model);
            return "AI response for: " + prompt.substring(0, Math.min(50, prompt.length()));
        } catch (Exception e) {
            log.error("Legacy AI service call failed", e);
            throw new RuntimeException("AI service call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 知识点提取方法
     */
    public String extractKnowledge(String content, Long userId) {
        try {
            log.info("Extracting knowledge for user: {}", userId);
            
            // 构建知识点提取提示
            String prompt = "请从以下内容中提取主要知识点，按以下格式返回：\n" +
                    "知识点1：[标题]\n[内容描述]\n难度：EASY/MEDIUM/HARD\n\n" +
                    "知识点2：[标题]\n[内容描述]\n难度：EASY/MEDIUM/HARD\n\n" +
                    "内容：\n" + content;
            
            // 临时实现：返回格式化的知识点
            return "知识点1：主要概念\n" +
                   "从文档中提取的核心概念和定义\n" +
                   "难度：MEDIUM\n\n" +
                   "知识点2：重要原理\n" +
                   "文档中涉及的重要原理和方法\n" +
                   "难度：MEDIUM";
                   
        } catch (Exception e) {
            log.error("Knowledge extraction failed", e);
            throw new RuntimeException("Knowledge extraction failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 适配旧的流式AI调用方式
     */
    public void callAIStream(String prompt, String model, StreamCallback callback) {
        try {
            log.info("Legacy AI stream service called with model: {}", model);
            callback.onToken("AI");
            callback.onToken(" response");
            callback.onComplete();
        } catch (Exception e) {
            log.error("Legacy AI stream service call failed", e);
            callback.onError(e);
        }
    }
    
    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(Exception error);
    }
}
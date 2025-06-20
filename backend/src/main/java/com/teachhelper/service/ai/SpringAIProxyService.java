package com.teachhelper.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.service.UserAIConfigService;
import com.teachhelper.service.AIUsageStatsService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Spring AI统一代理服务
 * 支持多AI提供商、动态配置、流式响应
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringAIProxyService {
    
    @Autowired
    private UserAIConfigService userAIConfigService;
    
    @Autowired
    private AIClientFactory aiClientFactory;
    
    /**
     * 同步聊天请求
     */
    public AIResponse chatSync(AIRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取用户配置
            UserAIConfig config = getUserConfig(request);
            if (config == null) {
                throw new RuntimeException("No AI configuration found for user: " + request.getUserId());
            }
            
            // 获取AI客户端
            AIClient client = aiClientFactory.getClient(config.getProvider());
            
            // 构建提示
            String fullPrompt = buildFullPrompt(request);
            
            // 执行请求
            AIResponse response = client.chat(fullPrompt, config);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 设置额外信息
            response.withDuration(duration)
                   .withModel(config.getProvider().name(), "default-model");
            
            // 记录使用统计
            recordUsage(request, response);
            
            return response;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("AI chat request failed", e);
            
            AIResponse errorResponse = AIResponse.failure("AI_REQUEST_FAILED", e.getMessage())
                    .withDuration(duration);
            
            return errorResponse;
        }
    }
    
    /**
     * 流式聊天请求
     */
    public void chatStream(AIRequest request, 
                          Consumer<String> onToken, 
                          Runnable onComplete, 
                          Consumer<Exception> onError) {
        // 暂时使用异步方式模拟流式响应
        new Thread(() -> {
            try {
                AIResponse response = chatSync(request);
                if (response.isSuccess()) {
                    // 模拟流式输出
                    String content = response.getContent();
                    String[] words = content.split(" ");
                    for (String word : words) {
                        onToken.accept(word + " ");
                        Thread.sleep(50); // 模拟延迟
                    }
                    onComplete.run();
                } else {
                    onError.accept(new RuntimeException(response.getErrorMessage()));
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        }).start();
    }
    
    /**
     * 异步聊天请求
     */
    public void chatAsync(AIRequest request, Consumer<AIResponse> callback) {
        new Thread(() -> {
            AIResponse response = chatSync(request);
            callback.accept(response);
        }).start();
    }
    
    /**
     * 构建完整提示
     */
    private String buildFullPrompt(AIRequest request) {
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            return "System: " + request.getSystemPrompt() + "\n\nUser: " + request.getPrompt();
        } else {
            return request.getPrompt();
        }
    }
    
    /**
     * 获取用户配置
     */
    private UserAIConfig getUserConfig(AIRequest request) {
        if (request.getUserId() != null) {
            // 使用默认配置
            return userAIConfigService.getUserDefaultAIConfig(request.getUserId()).orElse(null);
        }
        return null;
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(AIRequest request) {
        return String.format("%s_%s_%s_%s", 
            request.getUserId(),
            request.getProvider(),
            request.getModel(),
            System.currentTimeMillis() / (1000 * 60 * 5) // 5分钟缓存
        );
    }
    
    @Autowired
    private AIUsageStatsService aiUsageStatsService;
    
    /**
     * 记录使用统计
     */
    private void recordUsage(AIRequest request, AIResponse response) {
        try {
            if (request.getUserId() != null && response.getProvider() != null) {
                // 获取用户配置
                UserAIConfig config = getUserConfig(request);
                if (config != null) {
                    // 计算费用
                    double cost = calculateCost(response);
                    
                    // 记录使用统计到数据库
                    aiUsageStatsService.recordUsage(
                        config.getId(),
                        request.getUserId(),
                        response.getProvider(),
                        response.getModel() != null ? response.getModel() : "default-model",
                        response.getInputTokens() != null ? response.getInputTokens() : 0,
                        response.getOutputTokens() != null ? response.getOutputTokens() : 0,
                        response.getTotalTokens() != null ? response.getTotalTokens() : 0,
                        cost,
                        response.getDurationMs() != null ? response.getDurationMs() : 0L
                    );
                    
                    log.debug("Recorded AI usage for user: {}, provider: {}, tokens: {}, cost: {}", 
                        request.getUserId(), response.getProvider(), response.getTotalTokens(), cost);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to record AI usage", e);
        }
    }
    
    /**
     * 计算费用
     */
    private double calculateCost(AIResponse response) {
        if (response.getInputTokens() == null || response.getOutputTokens() == null) {
            return 0.0;
        }
        
        // 根据提供商计算费用（美元）
        double inputPrice = 0.0015; // 默认价格 per 1K tokens
        double outputPrice = 0.002;
        
        if (response.getProvider() != null) {
            switch (response.getProvider()) {
                case "DEEPSEEK":
                    inputPrice = 0.0007;
                    outputPrice = 0.0014;
                    break;
                case "CLAUDE":
                    inputPrice = 0.008;
                    outputPrice = 0.024;
                    break;
                case "OPENAI":
                default:
                    inputPrice = 0.0015;
                    outputPrice = 0.002;
                    break;
            }
        }
        
        double inputCost = (response.getInputTokens() / 1000.0) * inputPrice;
        double outputCost = (response.getOutputTokens() / 1000.0) * outputPrice;
        
        return inputCost + outputCost;
    }
}
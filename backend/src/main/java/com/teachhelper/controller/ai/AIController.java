package com.teachhelper.controller.ai;

import com.teachhelper.service.ai.AIRequest;
import com.teachhelper.service.ai.AIResponse;
import com.teachhelper.service.ai.SpringAIProxyService;
import com.teachhelper.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一AI服务控制器
 * 支持同步、异步、流式三种响应模式
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "统一AI服务", description = "基于Spring AI的统一AI服务接口")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final SpringAIProxyService springAIProxyService;
    private final AuthService authService;

    /**
     * 同步AI聊天
     */
    @PostMapping("/chat/sync")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "同步AI聊天", description = "发送消息并同步等待AI响应")
    public ResponseEntity<AIResponse> chatSync(@Valid @RequestBody AIRequest request) {
        try {
            // 设置当前用户ID
            Long userId = authService.getCurrentUser().getId();
            request.setUserId(userId);
            
            log.info("Sync AI chat request from user: {}", userId);
            
            AIResponse response = springAIProxyService.chatSync(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Sync AI chat failed", e);
            AIResponse errorResponse = AIResponse.failure("SYNC_CHAT_FAILED", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 测试AI配置 - 流式响应
     */
    @PostMapping(value = "/test-config", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "测试AI配置", description = "测试用户的AI配置是否正常工作，支持流式响应")
    public SseEmitter testConfigStream(@RequestBody(required = false) Map<String, Object> requestBody) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时
        
        try {
            // 在主线程中获取用户信息，避免异步线程中的安全上下文问题
            Long userId = authService.getCurrentUser().getId();
            
            // 从请求体中获取测试提示词，如果没有则使用默认的
            String testPrompt = "请简单介绍一下人工智能的定义和应用领域。";
            Integer maxTokens = 500;
            
            if (requestBody != null) {
                if (requestBody.containsKey("prompt")) {
                    testPrompt = (String) requestBody.get("prompt");
                }
                if (requestBody.containsKey("maxTokens")) {
                    maxTokens = (Integer) requestBody.get("maxTokens");
                }
            }
            
            final String finalTestPrompt = testPrompt;
            final Integer finalMaxTokens = maxTokens;
            
            // 保存当前的安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();
            
            CompletableFuture.runAsync(() -> {
                // 在异步线程中设置安全上下文
                SecurityContextHolder.setContext(securityContext);
                try {
                    AIRequest request = AIRequest.chat(finalTestPrompt);
                    request.setUserId(userId);
                    request.setMaxTokens(finalMaxTokens);
                    
                    log.info("AI config test stream request from user: {}, prompt: {}", userId, finalTestPrompt);
                    
                    log.info("Starting stream test for user: {}", userId);
                    
                    // 发送开始事件
                    emitter.send("data: {\"event\":\"start\",\"message\":\"开始测试AI配置...\",\"timestamp\":" + System.currentTimeMillis() + "}\n\n");
                    
                    long startTime = System.currentTimeMillis();
                    
                    // 直接使用同步方式获取完整响应，然后模拟流式发送
                    AIResponse response = springAIProxyService.chatSync(request);
                    
                    if (response.isSuccess()) {
                        log.info("AI response received, content length: {}", response.getContent().length());
                        
                        // 模拟流式发送内容
                        String content = response.getContent();
                        String[] words = content.split("(?<=\\s)|(?=\\s)"); // 按空格分割但保留空格
                        
                        for (int i = 0; i < words.length; i++) {
                            try {
                                String tokenData = "data: {\"event\":\"token\",\"content\":\"" + escapeJson(words[i]) + "\"}\n\n";
                                emitter.send(tokenData);
                                
                                // 添加延迟模拟真实流式效果
                                Thread.sleep(50);
                            } catch (IOException e) {
                                log.error("Failed to send token: {}", e.getMessage());
                                break;
                            } catch (InterruptedException e) {
                                log.warn("Stream interrupted");
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                        
                        // 发送完成事件
                        long duration = System.currentTimeMillis() - startTime;
                        String completeData = String.format(
                            "data: {\"event\":\"complete\",\"success\":true,\"message\":\"AI配置测试成功\",\"duration\":%d,\"provider\":\"%s\",\"model\":\"%s\",\"inputTokens\":%d,\"outputTokens\":%d,\"totalTokens\":%d,\"estimatedCost\":%f}\n\n",
                            duration,
                            response.getProvider() != null ? response.getProvider() : "未知",
                            response.getModel() != null ? response.getModel() : "未知",
                            response.getInputTokens() != null ? response.getInputTokens() : 0,
                            response.getOutputTokens() != null ? response.getOutputTokens() : 0,
                            response.getTotalTokens() != null ? response.getTotalTokens() : 0,
                            calculateEstimatedCost(response)
                        );
                        
                        emitter.send(completeData);
                        
                        log.info("Stream test completed successfully for user: {}", userId);
                        
                    } else {
                        // 发送错误事件
                        long duration = System.currentTimeMillis() - startTime;
                        String errorData = String.format(
                            "data: {\"event\":\"error\",\"success\":false,\"message\":\"AI配置测试失败: %s\",\"error\":\"%s\",\"duration\":%d}\n\n",
                            escapeJson(response.getErrorMessage()),
                            escapeJson(response.getErrorMessage()),
                            duration
                        );
                        
                        emitter.send(errorData);
                        
                        log.error("Stream test failed for user: {}, error: {}", userId, response.getErrorMessage());
                    }
                    
                    emitter.complete();
                    
                } catch (Exception e) {
                    log.error("AI config test stream failed", e);
                    try {
                        emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                "success", false,
                                "message", "AI配置测试失败: " + e.getMessage(),
                                "error", e.getMessage()
                            )));
                        emitter.completeWithError(e);
                    } catch (IOException ioException) {
                        log.error("Failed to send error event", ioException);
                        emitter.completeWithError(ioException);
                    }
                } finally {
                    // 清理安全上下文
                    SecurityContextHolder.clearContext();
                }
            });
            
        } catch (Exception e) {
            log.error("AI config test stream initialization failed", e);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of(
                        "success", false,
                        "message", "AI配置测试初始化失败: " + e.getMessage(),
                        "error", e.getMessage()
                    )));
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("Failed to send initialization error event", ioException);
                emitter.completeWithError(ioException);
            }
        }
        
        return emitter;
    }
    
    /**
     * 测试AI配置 - 同步版本（保留兼容性）
     */
    @PostMapping("/test-config-sync")
    @Operation(summary = "测试AI配置(同步)", description = "测试用户的AI配置是否正常工作，同步响应")
    public ResponseEntity<Map<String, Object>> testConfigSync(@RequestBody(required = false) Map<String, Object> requestBody) {
        try {
            // 设置当前用户ID
            Long userId = authService.getCurrentUser().getId();
            
            // 从请求体中获取测试提示词，如果没有则使用默认的
            String testPrompt = "请简单介绍一下人工智能的定义和应用领域。";
            Integer maxTokens = 500;
            
            if (requestBody != null) {
                if (requestBody.containsKey("prompt")) {
                    testPrompt = (String) requestBody.get("prompt");
                }
                if (requestBody.containsKey("maxTokens")) {
                    maxTokens = (Integer) requestBody.get("maxTokens");
                }
            }
            
            AIRequest request = AIRequest.chat(testPrompt);
            request.setUserId(userId);
            request.setMaxTokens(maxTokens);
            
            log.info("AI config test sync request from user: {}, prompt: {}", userId, testPrompt);
            
            AIResponse response = springAIProxyService.chatSync(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "AI配置测试成功",
                    "response", response.getContent(),
                    "duration", response.getDurationMs(),
                    "provider", response.getProvider() != null ? response.getProvider() : "未知",
                    "model", response.getModel() != null ? response.getModel() : "未知",
                    "inputTokens", response.getInputTokens() != null ? response.getInputTokens() : 0,
                    "outputTokens", response.getOutputTokens() != null ? response.getOutputTokens() : 0,
                    "totalTokens", response.getTotalTokens() != null ? response.getTotalTokens() : 0,
                    "estimatedCost", calculateEstimatedCost(response)
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "AI配置测试失败: " + response.getErrorMessage(),
                    "error", response.getErrorMessage(),
                    "duration", response.getDurationMs()
                ));
            }
            
        } catch (Exception e) {
            log.error("AI config test sync failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "AI配置测试失败: " + e.getMessage(),
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 计算估算费用
     */
    private double calculateEstimatedCost(AIResponse response) {
        if (response.getInputTokens() == null || response.getOutputTokens() == null) {
            return 0.0;
        }
        
        // 根据提供商计算大概费用（美元）
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
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
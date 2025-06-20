package com.teachhelper.service.ai.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.service.ai.AIClient;
import com.teachhelper.service.ai.AIResponse;

/**
 * DeepSeek客户端实现
 */
@Service
public class DeepSeekClient implements AIClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public DeepSeekClient(@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate) {
        this.restTemplate = aiRestTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public AIResponse chat(String prompt, UserAIConfig config) {
        long startTime = System.currentTimeMillis();
        
        try {
            // DeepSeek使用类似OpenAI的API格式
            String endpoint = config.getApiEndpoint() != null ? 
                config.getApiEndpoint() : AIProvider.DEEPSEEK.getDefaultEndpoint();
            String model = config.getModelName() != null ? 
                config.getModelName() : AIProvider.DEEPSEEK.getDefaultModel();
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature().doubleValue());
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            
            // 添加自定义头部
            if (config.getCustomHeaders() != null && !config.getCustomHeaders().trim().isEmpty()) {
                try {
                    JsonNode customHeadersNode = objectMapper.readTree(config.getCustomHeaders());
                    customHeadersNode.fields().forEachRemaining(entry -> {
                        headers.add(entry.getKey(), entry.getValue().asText());
                    });
                } catch (Exception e) {
                    System.out.println("解析自定义头部失败: " + e.getMessage());
                }
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseDeepSeekResponse(response.getBody(), startTime);
            } else {
                return AIResponse.error("DeepSeek API请求失败: " + response.getStatusCode(), 
                                      System.currentTimeMillis() - startTime);
            }
            
        } catch (Exception e) {
            return AIResponse.error("DeepSeek API调用异常: " + e.getMessage(), 
                                  System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public boolean validateConfig(UserAIConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            return false;
        }
        
        try {
            AIResponse response = chat("测试连接", config);
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return AIProvider.DEEPSEEK.name();
    }
    
    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // DeepSeek的token计算与OpenAI类似
        int chineseChars = 0;
        int otherChars = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        
        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }
    
    private AIResponse parseDeepSeekResponse(String responseBody, long startTime) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // 提取内容
            String content = rootNode.path("choices").path(0).path("message").path("content").asText();
            
            // 提取token使用情况
            JsonNode usageNode = rootNode.path("usage");
            Integer inputTokens = usageNode.path("prompt_tokens").asInt(0);
            Integer outputTokens = usageNode.path("completion_tokens").asInt(0);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new AIResponse(content, inputTokens, outputTokens, duration);
            
        } catch (Exception e) {
            return AIResponse.error("解析DeepSeek响应失败: " + e.getMessage(), 
                                  System.currentTimeMillis() - startTime);
        }
    }
}

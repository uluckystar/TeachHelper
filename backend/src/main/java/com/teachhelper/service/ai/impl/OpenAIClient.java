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
 * OpenAI客户端实现
 */
@Service
public class OpenAIClient implements AIClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenAIClient(@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate) {
        this.restTemplate = aiRestTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public AIResponse chat(String prompt, UserAIConfig config) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建请求
            String endpoint = config.getApiEndpoint() != null ? 
                config.getApiEndpoint() : AIProvider.OPENAI.getDefaultEndpoint();
            String model = config.getModelName() != null ? 
                config.getModelName() : AIProvider.OPENAI.getDefaultModel();
            
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
                return parseOpenAIResponse(response.getBody(), startTime);
            } else {
                return AIResponse.error("OpenAI API请求失败: " + response.getStatusCode(), 
                                      System.currentTimeMillis() - startTime);
            }
            
        } catch (Exception e) {
            return AIResponse.error("OpenAI API调用异常: " + e.getMessage(), 
                                  System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public boolean validateConfig(UserAIConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            return false;
        }
        
        // 可以做一个简单的API调用来验证
        try {
            AIResponse response = chat("测试连接", config);
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return AIProvider.OPENAI.name();
    }
    
    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：英文大约4个字符1个token，中文大约1.5个字符1个token
        int chineseChars = 0;
        int otherChars = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        
        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }
    
    private AIResponse parseOpenAIResponse(String responseBody, long startTime) {
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
            return AIResponse.error("解析OpenAI响应失败: " + e.getMessage(), 
                                  System.currentTimeMillis() - startTime);
        }
    }
}

package com.teachhelper.service.ai.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.service.ai.AIClient;
import com.teachhelper.service.ai.AIResponse;

/**
 * 通用AI客户端实现（用于支持其他提供商）
 */
public class GenericAIClient implements AIClient {
    
    private final AIProvider provider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public GenericAIClient(AIProvider provider) {
        this.provider = provider;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public GenericAIClient(AIProvider provider, RestTemplate restTemplate) {
        this.provider = provider;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public AIResponse chat(String prompt, UserAIConfig config) {
        long startTime = System.currentTimeMillis();
        
        try {
            String endpoint = config.getApiEndpoint() != null ? 
                config.getApiEndpoint() : provider.getDefaultEndpoint();
            String model = config.getModelName() != null ? 
                config.getModelName() : provider.getDefaultModel();
            
            // 构建通用请求体
            Map<String, Object> requestBody = buildRequestBody(prompt, model, config);
            
            // 设置请求头
            HttpHeaders headers = buildHeaders(config);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseGenericResponse(response.getBody(), startTime);
            } else {
                return AIResponse.error(provider.getDisplayName() + " API请求失败: " + response.getStatusCode(), 
                                      System.currentTimeMillis() - startTime);
            }
            
        } catch (Exception e) {
            return AIResponse.error(provider.getDisplayName() + " API调用异常: " + e.getMessage(), 
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
        return provider.name();
    }
    
    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 通用token估算
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
    
    private Map<String, Object> buildRequestBody(String prompt, String model, UserAIConfig config) {
        Map<String, Object> requestBody = new HashMap<>();
        
        switch (provider) {
            case ALIBABA_TONGYI:
                // 通义千问格式
                requestBody.put("model", model);
                Map<String, Object> input = new HashMap<>();
                input.put("prompt", prompt);
                requestBody.put("input", input);
                
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("max_tokens", config.getMaxTokens());
                parameters.put("temperature", config.getTemperature().doubleValue());
                requestBody.put("parameters", parameters);
                break;
                
            case BAIDU_ERNIE:
                // 文心一言格式
                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                messages.add(message);
                requestBody.put("messages", messages);
                requestBody.put("max_output_tokens", config.getMaxTokens());
                requestBody.put("temperature", config.getTemperature().doubleValue());
                break;
                
            default:
                // 默认OpenAI格式
                requestBody.put("model", model);
                requestBody.put("max_tokens", config.getMaxTokens());
                requestBody.put("temperature", config.getTemperature().doubleValue());
                
                List<Map<String, String>> defaultMessages = new ArrayList<>();
                Map<String, String> defaultMessage = new HashMap<>();
                defaultMessage.put("role", "user");
                defaultMessage.put("content", prompt);
                defaultMessages.add(defaultMessage);
                requestBody.put("messages", defaultMessages);
                break;
        }
        
        return requestBody;
    }
    
    private HttpHeaders buildHeaders(UserAIConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        switch (provider) {
            case ALIBABA_TONGYI:
                headers.setBearerAuth(config.getApiKey());
                break;
            case BAIDU_ERNIE:
                headers.add("Authorization", "Bearer " + config.getApiKey());
                break;
            case TENCENT_HUNYUAN:
                headers.add("Authorization", "Bearer " + config.getApiKey());
                break;
            default:
                headers.setBearerAuth(config.getApiKey());
                break;
        }
        
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
        
        return headers;
    }
    
    private AIResponse parseGenericResponse(String responseBody, long startTime) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String content = "";
            Integer inputTokens = 0;
            Integer outputTokens = 0;
            
            switch (provider) {
                case ALIBABA_TONGYI:
                    content = rootNode.path("output").path("text").asText();
                    inputTokens = rootNode.path("usage").path("input_tokens").asInt(0);
                    outputTokens = rootNode.path("usage").path("output_tokens").asInt(0);
                    break;
                    
                case BAIDU_ERNIE:
                    content = rootNode.path("result").asText();
                    inputTokens = rootNode.path("usage").path("prompt_tokens").asInt(0);
                    outputTokens = rootNode.path("usage").path("completion_tokens").asInt(0);
                    break;
                    
                default:
                    // 尝试多种可能的响应格式
                    if (rootNode.has("choices")) {
                        content = rootNode.path("choices").path(0).path("message").path("content").asText();
                    } else if (rootNode.has("content")) {
                        content = rootNode.path("content").asText();
                    } else if (rootNode.has("text")) {
                        content = rootNode.path("text").asText();
                    }
                    
                    JsonNode usageNode = rootNode.path("usage");
                    inputTokens = usageNode.path("prompt_tokens").asInt(0);
                    outputTokens = usageNode.path("completion_tokens").asInt(0);
                    break;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return new AIResponse(content, inputTokens, outputTokens, duration);
            
        } catch (Exception e) {
            return AIResponse.error("解析" + provider.getDisplayName() + "响应失败: " + e.getMessage(), 
                                  System.currentTimeMillis() - startTime);
        }
    }
}

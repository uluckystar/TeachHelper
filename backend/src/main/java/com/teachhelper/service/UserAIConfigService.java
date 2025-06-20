package com.teachhelper.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.request.AIConfigTestRequest;
import com.teachhelper.dto.request.UserAIConfigRequest;
import com.teachhelper.dto.response.AIConfigTestResponse;
import com.teachhelper.dto.response.AIProviderInfo;
import com.teachhelper.dto.response.AIUsageStatsDTO;
import com.teachhelper.dto.response.AIUsageSummaryDTO;
import com.teachhelper.dto.response.UserAIConfigResponse;
import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.AIUsageStats;
import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.repository.AIUsageStatsRepository;
import com.teachhelper.repository.UserAIConfigRepository;
import com.teachhelper.service.ai.AIClient;
import com.teachhelper.service.ai.AIClientFactory;
import com.teachhelper.service.ai.AIResponse;

/**
 * 用户AI配置管理服务
 */
@Service
@Transactional
public class UserAIConfigService {
    
    @Autowired
    private UserAIConfigRepository userAIConfigRepository;
    
    @Autowired
    private AIUsageStatsRepository aiUsageStatsRepository;
    
    @Autowired
    private AIClientFactory aiClientFactory;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 获取用户的所有AI配置
     */
    public List<UserAIConfigResponse> getUserAIConfigs(Long userId) {
        // 获取所有配置，包括未激活的
        List<UserAIConfig> configs = userAIConfigRepository.findByUserId(userId);
        return configs.stream()
                .map(config -> convertToResponseWithStats(config, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户的默认AI配置
     */
    public Optional<UserAIConfig> getUserDefaultAIConfig(Long userId) {
        return userAIConfigRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
    }
    
    /**
     * 创建或更新AI配置
     */
    public UserAIConfigResponse saveAIConfig(Long userId, UserAIConfigRequest request) {
        // 检查是否已存在该提供商的配置
        Optional<UserAIConfig> existingConfig = userAIConfigRepository
                .findByUserIdAndProviderAndIsActiveTrue(userId, request.getProvider());
        
        UserAIConfig config;
        if (existingConfig.isPresent()) {
            // 更新现有配置
            config = existingConfig.get();
            updateConfigFromRequest(config, request);
        } else {
            // 创建新配置
            config = createConfigFromRequest(userId, request);
        }
        
        // 如果设置为默认配置，先清除其他默认配置
        if (request.getIsDefault() != null && request.getIsDefault()) {
            userAIConfigRepository.clearDefaultForUser(userId);
            config.setIsDefault(true);
        }
        
        // 基础配置验证（不进行网络请求）
        try {
            // 只验证客户端工厂是否支持该提供商
            aiClientFactory.getClient(request.getProvider());
            // 验证基本配置参数
            if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
                throw new RuntimeException("API密钥不能为空");
            }
        } catch (RuntimeException e) {
            System.out.println("AI配置基础验证异常: " + e.getMessage());
            // 基础验证失败则抛出异常
            throw new RuntimeException("AI配置验证失败: " + e.getMessage());
        }
        
        config = userAIConfigRepository.save(config);
        return convertToResponse(config);
    }
    
    /**
     * 删除AI配置
     */
    public void deleteAIConfig(Long userId, Long configId) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (configOpt.isPresent() && configOpt.get().getUserId().equals(userId)) {
            UserAIConfig config = configOpt.get();
            config.setIsActive(false);
            userAIConfigRepository.save(config);
        } else {
            throw new RuntimeException("AI配置不存在或无权限删除");
        }
    }
    
    /**
     * 设置默认AI配置
     */
    public void setDefaultAIConfig(Long userId, Long configId) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (configOpt.isPresent() && configOpt.get().getUserId().equals(userId)) {
            // 清除所有默认配置
            userAIConfigRepository.clearDefaultForUser(userId);
            
            // 设置新的默认配置
            UserAIConfig config = configOpt.get();
            config.setIsDefault(true);
            userAIConfigRepository.save(config);
        } else {
            throw new RuntimeException("AI配置不存在或无权限修改");
        }
    }
    
    /**
     * 测试AI配置连接
     */
    public boolean testAIConfig(Long userId, Long configId) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (configOpt.isPresent() && configOpt.get().getUserId().equals(userId)) {
            UserAIConfig config = configOpt.get();
            try {
                AIClient client = aiClientFactory.getClient(config.getProvider());
                return client.validateConfig(config);
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * 记录AI使用统计
     */
    public void recordAIUsage(Long userId, Long configId, AIProvider provider, 
                             String modelName, String requestType, 
                             AIResponse response) {
        AIUsageStats stats = new AIUsageStats();
        stats.setUserId(userId);
        stats.setConfigId(configId);
        stats.setProvider(provider);
        stats.setModelName(modelName);
        stats.setRequestType(requestType);
        stats.setRequestDurationMs(response.getDurationMs());
        
        if (response.isSuccess()) {
            stats.setInputTokens(response.getInputTokens());
            stats.setOutputTokens(response.getOutputTokens());
            stats.setTotalTokens(response.getTotalTokens());
            
            // 计算费用
            BigDecimal inputCost = calculateInputCost(provider, response.getInputTokens());
            BigDecimal outputCost = calculateOutputCost(provider, response.getOutputTokens());
            stats.setInputCost(inputCost);
            stats.setOutputCost(outputCost);
            stats.setTotalCost(inputCost.add(outputCost));
            
            stats.setStatus("SUCCESS");
        } else {
            stats.setStatus("FAILED");
            stats.setErrorMessage(response.getErrorMessage());
        }
        
        aiUsageStatsRepository.save(stats);
    }
    
    /**
     * 获取用户AI使用统计汇总
     */
    public List<AIUsageSummaryDTO> getUserUsageSummary(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return aiUsageStatsRepository.getUsageSummaryByUserAndDateRange(userId, startTime, endTime);
    }
    
    /**
     * 获取用户总使用统计
     */
    public Map<String, Object> getUserTotalStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTokens", aiUsageStatsRepository.getTotalTokensByUser(userId));
        stats.put("totalCost", aiUsageStatsRepository.getTotalCostByUser(userId));
        stats.put("totalRequests", aiUsageStatsRepository.getTotalRequestsByUser(userId));
        return stats;
    }
    
    /**
     * 获取用户每日使用统计
     */
    public List<Map<String, Object>> getUserDailyStats(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = aiUsageStatsRepository.getDailyUsageStats(userId, startTime, endTime);
        return results.stream().map(row -> {
            Map<String, Object> dailyStats = new HashMap<>();
            dailyStats.put("date", row[0]);
            dailyStats.put("requestCount", row[1]);
            dailyStats.put("totalTokens", row[2]);
            dailyStats.put("totalCost", row[3]);
            return dailyStats;
        }).collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取用户AI配置
     */
    public UserAIConfigResponse getAIConfigById(Long userId, Long configId) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (configOpt.isPresent() && configOpt.get().getUserId().equals(userId) && configOpt.get().getIsActive()) {
            return convertToResponse(configOpt.get());
        } else {
            throw new RuntimeException("AI配置不存在或无权限访问");
        }
    }
    
    /**
     * 更新AI配置
     */
    public UserAIConfigResponse updateAIConfig(Long userId, Long configId, UserAIConfigRequest request) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (configOpt.isPresent() && configOpt.get().getUserId().equals(userId)) {
            UserAIConfig config = configOpt.get();
            updateConfigFromRequest(config, request);
            
            // 如果设置为默认配置，先清除其他默认配置
            if (request.getIsDefault() != null && request.getIsDefault()) {
                userAIConfigRepository.clearDefaultForUser(userId);
                config.setIsDefault(true);
            }
            
            config = userAIConfigRepository.save(config);
            return convertToResponse(config);
        } else {
            throw new RuntimeException("AI配置不存在或无权限修改");
        }
    }
    
    /**
     * 切换AI配置激活状态
     */
    public UserAIConfigResponse toggleAIConfig(Long userId, Long configId, Boolean isActive) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (configOpt.isPresent() && configOpt.get().getUserId().equals(userId)) {
            UserAIConfig config = configOpt.get();
            config.setIsActive(isActive);
            config = userAIConfigRepository.save(config);
            return convertToResponse(config);
        } else {
            throw new RuntimeException("AI配置不存在或无权限修改");
        }
    }
    
    /**
     * 高级AI配置测试
     */
    public AIConfigTestResponse testAIConfigAdvanced(Long userId, Long configId, AIConfigTestRequest testRequest) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (!configOpt.isPresent() || !configOpt.get().getUserId().equals(userId)) {
            return AIConfigTestResponse.failure("AI配置不存在或无权限访问", 0L);
        }
        
        UserAIConfig config = configOpt.get();
        long startTime = System.currentTimeMillis();
        
        try {
            AIClient client = aiClientFactory.getClient(config.getProvider());
            
            // 使用测试请求中的提示词
            String testMessage = testRequest.getPrompt();
            
            // 创建测试用的配置副本，使用测试请求中的参数
            UserAIConfig testConfig = new UserAIConfig();
            testConfig.setProvider(config.getProvider());
            testConfig.setApiKey(config.getApiKey());
            testConfig.setApiEndpoint(config.getApiEndpoint());
            testConfig.setModelName(config.getModelName());
            testConfig.setCustomHeaders(config.getCustomHeaders());
            testConfig.setTemperature(config.getTemperature());
            
            // 使用测试请求中的maxTokens，如果没有则使用配置中的默认值
            Integer maxTokens = testRequest.getMaxTokens() != null ? 
                testRequest.getMaxTokens() : config.getMaxTokens();
            testConfig.setMaxTokens(maxTokens);
            
            // 执行真实的AI对话测试
            AIResponse aiResponse = client.chat(testMessage, testConfig);
            long duration = System.currentTimeMillis() - startTime;
            
            if (!aiResponse.isSuccess()) {
                return AIConfigTestResponse.failure(
                    "AI配置测试失败: " + aiResponse.getContent(),
                    duration
                );
            }
            
            String response = aiResponse.getContent();
            
            // 使用AI响应中的Token信息，如果没有则进行估算
            Integer inputTokensObj = aiResponse.getInputTokens();
            Integer outputTokensObj = aiResponse.getOutputTokens();
            int inputTokens = inputTokensObj != null ? inputTokensObj : client.estimateTokens(testMessage);
            int outputTokens = outputTokensObj != null ? outputTokensObj : client.estimateTokens(response);
            int totalTokens = inputTokens + outputTokens;
            
            // 根据提供商计算大概成本
            BigDecimal estimatedCost = calculateEstimatedCost(config.getProvider(), 
                inputTokens, outputTokens);
            
            return AIConfigTestResponse.success(
                response != null ? response : "AI返回了空响应", 
                duration,
                inputTokens,
                outputTokens,
                totalTokens,
                estimatedCost
            );
            
        } catch (RuntimeException e) {
            long duration = System.currentTimeMillis() - startTime;
            return AIConfigTestResponse.failure(
                "AI配置测试失败: " + e.getMessage(),
                duration
            );
        }
    }
    
    /**
     * 计算估算成本
     */
    private BigDecimal calculateEstimatedCost(AIProvider provider, int inputTokens, int outputTokens) {
        // 根据不同提供商的价格计算成本
        BigDecimal inputPrice;
        BigDecimal outputPrice;
        
        switch (provider) {
            case OPENAI -> {
                inputPrice = new BigDecimal("0.0015"); // GPT-3.5-turbo input price per 1K tokens
                outputPrice = new BigDecimal("0.002");  // GPT-3.5-turbo output price per 1K tokens
            }
            case CLAUDE -> {
                inputPrice = new BigDecimal("0.008");   // Claude-3 input price per 1K tokens
                outputPrice = new BigDecimal("0.024");  // Claude-3 output price per 1K tokens
            }
            case DEEPSEEK -> {
                inputPrice = new BigDecimal("0.0007");  // DeepSeek input price per 1K tokens
                outputPrice = new BigDecimal("0.0014"); // DeepSeek output price per 1K tokens
            }
            default -> {
                inputPrice = new BigDecimal("0.002");   // Default price per 1K tokens
                outputPrice = new BigDecimal("0.002");  // Default price per 1K tokens
            }
        }
        
        BigDecimal inputCost = new BigDecimal(inputTokens).divide(new BigDecimal(1000)).multiply(inputPrice);
        BigDecimal outputCost = new BigDecimal(outputTokens).divide(new BigDecimal(1000)).multiply(outputPrice);
        
        return inputCost.add(outputCost);
    }
    
    /**
     * 获取配置使用统计
     */
    public AIUsageStatsDTO getConfigUsageStats(Long userId, Long configId, LocalDateTime startTime, LocalDateTime endTime) {
        // 验证配置权限
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (!configOpt.isPresent() || !configOpt.get().getUserId().equals(userId)) {
            throw new RuntimeException("AI配置不存在或无权限访问");
        }
        
        // 注意：由于actualStartTime和actualEndTime当前未被使用，
        // 将来可以在实际的数据库查询中使用这些值
        // LocalDateTime actualStartTime = startTime != null ? startTime : LocalDateTime.now().minusDays(30);
        // LocalDateTime actualEndTime = endTime != null ? endTime : LocalDateTime.now();
        
        // 创建一个基本的统计DTO，后续可以扩展
        AIUsageStatsDTO stats = new AIUsageStatsDTO();
        stats.setTotalRequests(0L);
        stats.setTotalTokens(0L);
        stats.setTotalCost(BigDecimal.ZERO);
        stats.setAverageResponseTime(0L);
        stats.setSuccessRate(0.0);
        stats.setLastUsed(null);
        
        return stats;
    }
    
    /**
     * 获取可用的AI提供商信息
     */
    public List<AIProviderInfo> getAvailableProviders() {
        List<AIProviderInfo> providers = new ArrayList<>();
        
        for (AIProvider provider : AIProvider.values()) {
            AIProviderInfo providerInfo = new AIProviderInfo(provider);
            providers.add(providerInfo);
        }
        
        return providers;
    }
    
    /**
     * 重置配置统计
     */
    public void resetConfigStats(Long userId, Long configId) {
        // 验证配置权限
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (!configOpt.isPresent() || !configOpt.get().getUserId().equals(userId)) {
            throw new RuntimeException("AI配置不存在或无权限访问");
        }
        
        // 这里可以实现删除统计数据的逻辑
        // 暂时留空，后续根据实际repository方法实现
    }
    
    /**
     * 导入配置
     */
    public UserAIConfigResponse importConfig(Long userId, Map<String, Object> configData) {
        UserAIConfigRequest request = new UserAIConfigRequest();
        
        // 从Map中提取配置数据
        if (configData.containsKey("provider")) {
            request.setProvider(AIProvider.valueOf((String) configData.get("provider")));
        }
        if (configData.containsKey("apiKey")) {
            request.setApiKey((String) configData.get("apiKey"));
        }
        if (configData.containsKey("apiEndpoint")) {
            request.setApiEndpoint((String) configData.get("apiEndpoint"));
        }
        if (configData.containsKey("modelName")) {
            request.setModelName((String) configData.get("modelName"));
        }
        if (configData.containsKey("maxTokens")) {
            request.setMaxTokens((Integer) configData.get("maxTokens"));
        }
        if (configData.containsKey("temperature")) {
            Object temp = configData.get("temperature");
            if (temp instanceof Number number) {
                request.setTemperature(BigDecimal.valueOf(number.doubleValue()));
            }
        }
        if (configData.containsKey("isDefault")) {
            request.setIsDefault((Boolean) configData.get("isDefault"));
        }
        
        return saveAIConfig(userId, request);
    }
    
    /**
     * 导出配置
     */
    public Map<String, Object> exportConfig(Long userId, Long configId) {
        Optional<UserAIConfig> configOpt = userAIConfigRepository.findById(configId);
        if (!configOpt.isPresent() || !configOpt.get().getUserId().equals(userId)) {
            throw new RuntimeException("AI配置不存在或无权限访问");
        }
        
        UserAIConfig config = configOpt.get();
        Map<String, Object> exportData = new HashMap<>();
        
        exportData.put("provider", config.getProvider().name());
        exportData.put("apiEndpoint", config.getApiEndpoint());
        exportData.put("modelName", config.getModelName());
        exportData.put("maxTokens", config.getMaxTokens());
        exportData.put("temperature", config.getTemperature());
        exportData.put("isDefault", config.getIsDefault());
        exportData.put("customHeaders", config.getCustomHeaders());
        exportData.put("exportedAt", LocalDateTime.now());
        
        // 不导出敏感信息如API密钥
        exportData.put("note", "API密钥等敏感信息不会被导出，需要重新配置");
        
        return exportData;
    }

    // 私有辅助方法
    private UserAIConfig createConfigFromRequest(Long userId, UserAIConfigRequest request) {
        UserAIConfig config = new UserAIConfig();
        config.setUserId(userId);
        updateConfigFromRequest(config, request);
        return config;
    }
    
    private void updateConfigFromRequest(UserAIConfig config, UserAIConfigRequest request) {
        config.setProvider(request.getProvider());
        config.setApiKey(request.getApiKey());
        config.setApiEndpoint(request.getApiEndpoint());
        config.setModelName(request.getModelName());
        config.setMaxTokens(request.getMaxTokens());
        config.setTemperature(request.getTemperature());
        
        // 将Map转换为JSON字符串
        if (request.getCustomHeaders() != null && !request.getCustomHeaders().isEmpty()) {
            try {
                String customHeadersJson = objectMapper.writeValueAsString(request.getCustomHeaders());
                config.setCustomHeaders(customHeadersJson);
            } catch (Exception e) {
                config.setCustomHeaders(null);
            }
        } else {
            config.setCustomHeaders(null);
        }
        
        if (request.getIsDefault() != null) {
            config.setIsDefault(request.getIsDefault());
        }
    }
    
    private UserAIConfigResponse convertToResponse(UserAIConfig config) {
        // 屏蔽API Key敏感信息
        String maskedApiKey = config.getApiKey() != null ? 
            config.getApiKey().substring(0, Math.min(8, config.getApiKey().length())) + "****" : null;
            
        return new UserAIConfigResponse(
            config.getId(),
            config.getProvider(),
            maskedApiKey,
            config.getApiEndpoint(),
            config.getModelName(),
            config.getMaxTokens(),
            config.getTemperature(),
            config.getIsActive(),
            config.getIsDefault(),
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }
    
    private UserAIConfigResponse convertToResponseWithStats(UserAIConfig config, Long userId) {
        UserAIConfigResponse response = convertToResponse(config);
        
        // 获取使用统计
        try {
            // 获取最近30天的统计数据
            LocalDateTime startTime = LocalDateTime.now().minusDays(30);
            LocalDateTime endTime = LocalDateTime.now();
            
            // 获取该配置的使用统计
            Long totalRequests = aiUsageStatsRepository.countByConfigIdAndCreatedAtBetween(
                config.getId(), startTime, endTime);
            BigDecimal totalCost = aiUsageStatsRepository.sumTotalCostByConfigIdAndCreatedAtBetween(
                config.getId(), startTime, endTime);
            
            // 设置统计信息
            response.setUsageStats(new UserAIConfigResponse.UsageStats(
                totalRequests != null ? totalRequests : 0L,
                totalCost != null ? totalCost : BigDecimal.ZERO
            ));
        } catch (Exception e) {
            // 如果获取统计失败，设置默认值
            response.setUsageStats(new UserAIConfigResponse.UsageStats(0L, BigDecimal.ZERO));
        }
        
        return response;
    }
    
    private BigDecimal calculateInputCost(AIProvider provider, Integer tokens) {
        if (tokens == null || tokens == 0) return BigDecimal.ZERO;
        double costPer1K = provider.getInputTokenPrice();
        return BigDecimal.valueOf(tokens * costPer1K / 1000.0).setScale(6, BigDecimal.ROUND_HALF_UP);
    }
    
    private BigDecimal calculateOutputCost(AIProvider provider, Integer tokens) {
        if (tokens == null || tokens == 0) return BigDecimal.ZERO;
        double costPer1K = provider.getOutputTokenPrice();
        return BigDecimal.valueOf(tokens * costPer1K / 1000.0).setScale(6, BigDecimal.ROUND_HALF_UP);
    }
}

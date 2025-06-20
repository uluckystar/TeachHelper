package com.teachhelper.service.dev;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.AIProvider;
import com.teachhelper.entity.AIUsageStats;
import com.teachhelper.entity.User;
import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.repository.AIUsageStatsRepository;
import com.teachhelper.repository.UserAIConfigRepository;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境AI配置数据服务
 * 负责创建AI配置和使用统计的示例数据
 */
@Service
public class DevAIDataService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserAIConfigRepository userAIConfigRepository;
    
    @Autowired
    private AIUsageStatsRepository aiUsageStatsRepository;
    
    /**
     * 创建AI配置数据
     */
    @Transactional
    public void createAIConfigs() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new RuntimeException("请先创建用户数据");
        }
        
        createAIConfigsForUsers(users);
    }
    
    /**
     * 创建AI使用统计数据
     */
    @Transactional
    public void createAIUsageStats() {
        List<UserAIConfig> configs = userAIConfigRepository.findAll();
        if (configs.isEmpty()) {
            throw new RuntimeException("请先创建AI配置数据");
        }
        
        createUsageStatsForConfigs(configs);
    }
    
    /**
     * 清空AI相关数据
     */
    @Transactional
    public void clearAIData() {
        aiUsageStatsRepository.deleteAll();
        userAIConfigRepository.deleteAll();
    }
    
    /**
     * 为用户创建AI配置
     */
    private void createAIConfigsForUsers(List<User> users) {
        List<UserAIConfig> configs = new ArrayList<>();
        
        // 为管理员创建多个AI配置
        User admin = findUserByUsername(users, "admin");
        if (admin != null) {
            configs.addAll(createAdminConfigs(admin));
        }
        
        // 为教师创建AI配置
        for (int i = 1; i <= 4; i++) {
            User teacher = findUserByUsername(users, "teacher" + i);
            if (teacher != null) {
                configs.addAll(createTeacherConfigs(teacher, i));
            }
        }
        
        // 为部分学生创建配置（学生一般使用较少的AI配置）
        User student1 = findUserByUsername(users, "student1");
        if (student1 != null) {
            configs.add(createStudentConfig(student1));
        }
        
        User student3 = findUserByUsername(users, "student3");
        if (student3 != null) {
            configs.add(createStudentConfig(student3));
        }
        
        userAIConfigRepository.saveAll(configs);
    }
    
    /**
     * 为管理员创建AI配置
     */
    private List<UserAIConfig> createAdminConfigs(User admin) {
        List<UserAIConfig> configs = new ArrayList<>();
        
        // OpenAI配置
        UserAIConfig openaiConfig = new UserAIConfig();
        openaiConfig.setUserId(admin.getId());
        openaiConfig.setProvider(AIProvider.OPENAI);
        openaiConfig.setApiKey("sk-demo-key-openai-1234567890");
        openaiConfig.setApiEndpoint("https://api.openai.com/v1/chat/completions");
        openaiConfig.setModelName("gpt-3.5-turbo");
        openaiConfig.setMaxTokens(4000);
        openaiConfig.setTemperature(new BigDecimal("0.7"));
        openaiConfig.setIsActive(true);
        openaiConfig.setIsDefault(true);
        configs.add(openaiConfig);
        
        // DeepSeek配置
        UserAIConfig deepseekConfig = new UserAIConfig();
        deepseekConfig.setUserId(admin.getId());
        deepseekConfig.setProvider(AIProvider.DEEPSEEK);
        deepseekConfig.setApiKey("sk-demo-key-deepseek-abcdefg");
        deepseekConfig.setApiEndpoint("https://api.deepseek.com/v1/chat/completions");
        deepseekConfig.setModelName("deepseek-chat");
        deepseekConfig.setMaxTokens(3000);
        deepseekConfig.setTemperature(new BigDecimal("0.5"));
        deepseekConfig.setIsActive(true);
        deepseekConfig.setIsDefault(false);
        configs.add(deepseekConfig);
        
        // Claude配置
        UserAIConfig claudeConfig = new UserAIConfig();
        claudeConfig.setUserId(admin.getId());
        claudeConfig.setProvider(AIProvider.CLAUDE);
        claudeConfig.setApiKey("sk-demo-key-claude-admin");
        claudeConfig.setApiEndpoint("https://api.anthropic.com/v1/messages");
        claudeConfig.setModelName("claude-3-sonnet-20240229");
        claudeConfig.setMaxTokens(3500);
        claudeConfig.setTemperature(new BigDecimal("0.3"));
        claudeConfig.setIsActive(true);
        claudeConfig.setIsDefault(false);
        configs.add(claudeConfig);
        
        return configs;
    }
    
    /**
     * 为教师创建AI配置
     */
    private List<UserAIConfig> createTeacherConfigs(User teacher, int teacherIndex) {
        List<UserAIConfig> configs = new ArrayList<>();
        
        switch (teacherIndex) {
            case 1:
                // 教师1 - 阿里通义千问
                UserAIConfig teacher1Config = new UserAIConfig();
                teacher1Config.setUserId(teacher.getId());
                teacher1Config.setProvider(AIProvider.ALIBABA_TONGYI);
                teacher1Config.setApiKey("sk-demo-key-tongyi-teacher1");
                teacher1Config.setApiEndpoint("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation");
                teacher1Config.setModelName("qwen-turbo");
                teacher1Config.setMaxTokens(2000);
                teacher1Config.setTemperature(new BigDecimal("0.6"));
                teacher1Config.setIsActive(true);
                teacher1Config.setIsDefault(true);
                configs.add(teacher1Config);
                break;
                
            case 2:
                // 教师2 - 百度文心一言
                UserAIConfig teacher2Config1 = new UserAIConfig();
                teacher2Config1.setUserId(teacher.getId());
                teacher2Config1.setProvider(AIProvider.BAIDU_ERNIE);
                teacher2Config1.setApiKey("sk-demo-key-ernie-teacher2");
                teacher2Config1.setApiEndpoint("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions");
                teacher2Config1.setModelName("ernie-bot-turbo");
                teacher2Config1.setMaxTokens(2500);
                teacher2Config1.setTemperature(new BigDecimal("0.8"));
                teacher2Config1.setIsActive(true);
                teacher2Config1.setIsDefault(true);
                configs.add(teacher2Config1);
                
                // 教师2的第二个配置 - Claude
                UserAIConfig teacher2Config2 = new UserAIConfig();
                teacher2Config2.setUserId(teacher.getId());
                teacher2Config2.setProvider(AIProvider.CLAUDE);
                teacher2Config2.setApiKey("sk-demo-key-claude-teacher2");
                teacher2Config2.setApiEndpoint("https://api.anthropic.com/v1/messages");
                teacher2Config2.setModelName("claude-3-sonnet-20240229");
                teacher2Config2.setMaxTokens(3500);
                teacher2Config2.setTemperature(new BigDecimal("0.4"));
                teacher2Config2.setIsActive(true);
                teacher2Config2.setIsDefault(false);
                configs.add(teacher2Config2);
                break;
                
            case 3:
                // 教师3 - 腾讯混元
                UserAIConfig teacher3Config = new UserAIConfig();
                teacher3Config.setUserId(teacher.getId());
                teacher3Config.setProvider(AIProvider.TENCENT_HUNYUAN);
                teacher3Config.setApiKey("sk-demo-key-hunyuan-teacher3");
                teacher3Config.setApiEndpoint("https://hunyuan.tencentcloudapi.com/");
                teacher3Config.setModelName("hunyuan-lite");
                teacher3Config.setMaxTokens(2800);
                teacher3Config.setTemperature(new BigDecimal("0.7"));
                teacher3Config.setIsActive(true);
                teacher3Config.setIsDefault(true);
                configs.add(teacher3Config);
                break;
                
            case 4:
                // 教师4 - 自定义配置
                UserAIConfig teacher4Config = new UserAIConfig();
                teacher4Config.setUserId(teacher.getId());
                teacher4Config.setProvider(AIProvider.CUSTOM);
                teacher4Config.setApiKey("sk-demo-key-custom-teacher4");
                teacher4Config.setApiEndpoint("https://api.custom-ai.com/v1/chat/completions");
                teacher4Config.setModelName("custom-model-v1");
                teacher4Config.setMaxTokens(3200);
                teacher4Config.setTemperature(new BigDecimal("0.6"));
                teacher4Config.setIsActive(true);
                teacher4Config.setIsDefault(true);
                configs.add(teacher4Config);
                break;
            default:
                // 对于其他教师索引，不创建配置
                break;
        }
        
        return configs;
    }
    
    /**
     * 为学生创建AI配置
     */
    private UserAIConfig createStudentConfig(User student) {
        UserAIConfig config = new UserAIConfig();
        config.setUserId(student.getId());
        config.setProvider(AIProvider.OPENAI);
        config.setApiKey("sk-demo-key-student-basic");
        config.setApiEndpoint("https://api.openai.com/v1/chat/completions");
        config.setModelName("gpt-3.5-turbo");
        config.setMaxTokens(2000);
        config.setTemperature(new BigDecimal("0.7"));
        config.setIsActive(true);
        config.setIsDefault(true);
        return config;
    }
    
    /**
     * 为配置创建使用统计
     */
    private void createUsageStatsForConfigs(List<UserAIConfig> configs) {
        for (UserAIConfig config : configs) {
            // 根据用户类型创建不同数量的使用记录
            User user = userRepository.findById(config.getUserId()).orElse(null);
            if (user == null) continue;
            
            if ("admin".equals(user.getUsername())) {
                // 管理员使用频率最高
                createUsageStatsForConfig(config, "EVALUATION", 8);
                createUsageStatsForConfig(config, "RUBRIC_GENERATION", 5);
                createUsageStatsForConfig(config, "CHAT", 6);
            } else if (user.getUsername().startsWith("teacher")) {
                // 教师使用频率中等
                createUsageStatsForConfig(config, "EVALUATION", 5);
                createUsageStatsForConfig(config, "RUBRIC_GENERATION", 3);
                createUsageStatsForConfig(config, "CHAT", 2);
            } else if (user.getUsername().startsWith("student")) {
                // 学生使用频率较低
                createUsageStatsForConfig(config, "CHAT", 2);
                createUsageStatsForConfig(config, "EVALUATION", 1);
            }
        }
    }
    
    /**
     * 为特定配置创建使用统计
     */
    private void createUsageStatsForConfig(UserAIConfig config, String requestType, int count) {
        for (int i = 0; i < count; i++) {
            AIUsageStats stats = new AIUsageStats();
            stats.setUserId(config.getUserId());
            stats.setConfigId(config.getId());
            stats.setProvider(config.getProvider());
            stats.setModelName(config.getModelName());
            stats.setRequestType(requestType);
            
            // 模拟不同的token使用量
            int baseInputTokens = getBaseInputTokens(requestType);
            int baseOutputTokens = getBaseOutputTokens(requestType);
            
            int inputTokens = baseInputTokens + (i * 50) + (int)(Math.random() * 100);
            int outputTokens = baseOutputTokens + (i * 30) + (int)(Math.random() * 80);
            
            stats.setInputTokens(inputTokens);
            stats.setOutputTokens(outputTokens);
            stats.setTotalTokens(inputTokens + outputTokens);
            
            // 计算费用
            double inputCost = inputTokens * config.getProvider().getInputTokenPrice() / 1000.0;
            double outputCost = outputTokens * config.getProvider().getOutputTokenPrice() / 1000.0;
            stats.setInputCost(BigDecimal.valueOf(inputCost).setScale(6, BigDecimal.ROUND_HALF_UP));
            stats.setOutputCost(BigDecimal.valueOf(outputCost).setScale(6, BigDecimal.ROUND_HALF_UP));
            stats.setTotalCost(stats.getInputCost().add(stats.getOutputCost()));
            
            // 模拟请求时间
            stats.setRequestDurationMs((long)(1000 + Math.random() * 3000));
            
            // 大部分请求成功，少数失败
            stats.setStatus(i < count - 1 || Math.random() > 0.1 ? "SUCCESS" : "FAILED");
            if ("FAILED".equals(stats.getStatus())) {
                stats.setErrorMessage(getRandomErrorMessage());
            }
            
            // 设置创建时间为过去几天内的随机时间
            LocalDateTime baseTime = LocalDateTime.now().minusDays(7);
            stats.setCreatedAt(baseTime.plusHours((long)(Math.random() * 168))); // 168小时=7天
            
            aiUsageStatsRepository.save(stats);
        }
    }
    
    /**
     * 根据请求类型获取基础输入token数
     */
    private int getBaseInputTokens(String requestType) {
        return switch (requestType) {
            case "EVALUATION" -> 300;  // 评估需要较多输入
            case "RUBRIC_GENERATION" -> 250;  // 生成评分标准
            case "CHAT" -> 150;  // 普通对话
            default -> 200;
        };
    }
    
    /**
     * 根据请求类型获取基础输出token数
     */
    private int getBaseOutputTokens(String requestType) {
        return switch (requestType) {
            case "EVALUATION" -> 200;  // 评估输出中等
            case "RUBRIC_GENERATION" -> 300;  // 生成评分标准输出较多
            case "CHAT" -> 100;  // 普通对话输出较少
            default -> 150;
        };
    }
    
    /**
     * 获取随机错误消息
     */
    private String getRandomErrorMessage() {
        String[] errors = {
            "API rate limit exceeded",
            "Connection timeout",
            "Invalid API key",
            "Model temporarily unavailable",
            "Request too large"
        };
        return errors[(int)(Math.random() * errors.length)];
    }
    
    /**
     * 根据用户名查找用户
     */
    private User findUserByUsername(List<User> users, String username) {
        return users.stream()
            .filter(user -> username.equals(user.getUsername()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取AI配置统计信息
     */
    public AIConfigStats getAIConfigStats() {
        long totalConfigs = userAIConfigRepository.count();
        long activeConfigs = userAIConfigRepository.countByIsActiveTrue();
        long totalUsageRecords = aiUsageStatsRepository.count();
        
        // 按提供商统计配置数量
        List<Object[]> providerStats = userAIConfigRepository.findAll().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                config -> config.getProvider().name(),
                java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
            .toList();
        
        return new AIConfigStats(totalConfigs, activeConfigs, totalUsageRecords, providerStats);
    }
    
    /**
     * AI配置统计信息类
     */
    public static class AIConfigStats {
        private final long totalConfigs;
        private final long activeConfigs;
        private final long totalUsageRecords;
        private final List<Object[]> providerStats;
        
        public AIConfigStats(long totalConfigs, long activeConfigs, long totalUsageRecords, 
                           List<Object[]> providerStats) {
            this.totalConfigs = totalConfigs;
            this.activeConfigs = activeConfigs;
            this.totalUsageRecords = totalUsageRecords;
            this.providerStats = providerStats;
        }
        
        public long getTotalConfigs() { return totalConfigs; }
        public long getActiveConfigs() { return activeConfigs; }
        public long getTotalUsageRecords() { return totalUsageRecords; }
        public List<Object[]> getProviderStats() { return providerStats; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AI配置统计:\n");
            sb.append("  总配置数: ").append(totalConfigs).append("\n");
            sb.append("  活跃配置数: ").append(activeConfigs).append("\n");
            sb.append("  使用记录数: ").append(totalUsageRecords).append("\n");
            sb.append("  提供商分布:\n");
            for (Object[] stat : providerStats) {
                sb.append("    ").append(stat[0]).append(": ").append(stat[1]).append("个\n");
            }
            return sb.toString();
        }
    }
}

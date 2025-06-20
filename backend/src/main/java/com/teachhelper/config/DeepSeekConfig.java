package com.teachhelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek AI 配置
 * 暂时禁用自定义配置，使用application.yml中的标准OpenAI配置
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.deepseek.api-key")
public class DeepSeekConfig {

    @Value("${spring.ai.deepseek.api-key}")
    private String apiKey;

    // 配置通过application.yml处理
    // 暂时禁用自定义Bean创建，避免API兼容性问题
}

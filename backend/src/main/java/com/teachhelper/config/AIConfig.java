package com.teachhelper.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 * - Chat使用OpenAI API (DeepSeek兼容)
 * - Embedding使用Ollama本地服务 (自动配置)
 * - VectorStore使用Spring AI官方PgVector自动配置
 * 
 * 参考文档：https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html
 */
@Configuration
public class AIConfig {
    
    // Spring AI会自动配置PgVectorStore
    // 只需要确保：
    // 1. 有名为"vectorDataSource"的数据源Bean
    // 2. 有EmbeddingModel Bean (Ollama已自动配置)
    // 3. 在application.yml中配置spring.ai.vectorstore.pgvector.*属性
    
}

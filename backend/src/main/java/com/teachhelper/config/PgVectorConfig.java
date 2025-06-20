package com.teachhelper.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PgVector向量存储配置
 * 基于 Spring AI 1.1-SNAPSHOT 官方文档的最佳实践
 * 参考: https://docs.spring.io/spring-ai/reference/1.1-SNAPSHOT/api/vectordbs/pgvector.html
 */
@Configuration
public class PgVectorConfig {

    @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}")
    private String tableName;
    
    @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
    private String schemaName;
    
    @Value("${spring.ai.vectorstore.pgvector.dimension:768}")
    private int dimensions;
    
    @Value("${spring.ai.vectorstore.pgvector.distance-type:COSINE_DISTANCE}")
    private String distanceType;
    
    @Value("${spring.ai.vectorstore.pgvector.remove-existing-vector-store-table:false}")
    private boolean removeExistingVectorStoreTable;
    
    @Value("${spring.ai.vectorstore.pgvector.index-type:HNSW}")
    private String indexType;
    
    @Value("${spring.ai.vectorstore.pgvector.schema-validation:true}")
    private boolean schemaValidation;

    /**
     * 创建PgVector VectorStore
     * 使用Spring AI 1.0.0-SNAPSHOT兼容的构建器模式
     * 明确指定使用Ollama的embedding模型
     */
    @Bean
    public VectorStore vectorStore(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        
        // 使用简化的构建器，只传入必要参数
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }
}

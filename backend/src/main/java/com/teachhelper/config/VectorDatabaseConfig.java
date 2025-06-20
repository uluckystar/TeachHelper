package com.teachhelper.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * 数据库配置类
 * 配置双数据源：MySQL(主要业务数据) + PostgreSQL(向量数据)
 * 
 * Spring AI PgVector需要明确指定数据源，避免与主数据源冲突
 */
@Configuration
public class VectorDatabaseConfig {

    @Value("${spring.datasource.url}")
    private String mainDbUrl;

    @Value("${spring.datasource.username}")
    private String mainDbUsername;

    @Value("${spring.datasource.password}")
    private String mainDbPassword;

    @Value("${spring.datasource.driverClassName}")
    private String mainDbDriver;

    @Value("${spring.ai.vectorstore.pgvector.host:localhost}")
    private String vectorDbHost;

    @Value("${spring.ai.vectorstore.pgvector.port:5432}")
    private String vectorDbPort;

    @Value("${spring.ai.vectorstore.pgvector.database:teach_helper_vector}")
    private String vectorDbName;

    @Value("${spring.ai.vectorstore.pgvector.username:jiangjiaxing}")
    private String vectorDbUsername;

    @Value("${spring.ai.vectorstore.pgvector.password:12345}")
    private String vectorDbPassword;

    /**
     * 主数据源 - MySQL
     * 用于用户、题目、答案等业务数据
     */
    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(mainDbDriver);
        dataSource.setUrl(mainDbUrl);
        dataSource.setUsername(mainDbUsername);
        dataSource.setPassword(mainDbPassword);
        return dataSource;
    }

    /**
     * 向量数据库数据源 - PostgreSQL
     * 专门用于Spring AI PgVector
     * 符合 Spring AI 1.1-SNAPSHOT 官方文档规范
     */
    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(String.format("jdbc:postgresql://%s:%s/%s", vectorDbHost, vectorDbPort, vectorDbName))
                .username(vectorDbUsername)
                .password(vectorDbPassword)
                .build();
    }

    /**
     * 向量数据库JdbcTemplate
     * 用于PgVector操作
     */
    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

package com.teachhelper.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类 - 设置HTTP客户端超时
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))  // 连接超时30秒
                .setReadTimeout(Duration.ofSeconds(120))    // 读取超时120秒 (AI请求可能需要更长时间)
                .build();
    }

    @Bean
    @Qualifier("aiRestTemplate")
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder) {
        // 专门为AI请求使用的RestTemplate，超时时间更长
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))  // 30秒连接超时
                .setReadTimeout(Duration.ofSeconds(120))    // 120秒读取超时
                .build();
    }
}

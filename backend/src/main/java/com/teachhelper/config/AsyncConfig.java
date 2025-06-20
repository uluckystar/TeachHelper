package com.teachhelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置类
 * 确保SecurityContext在异步方法中得到正确传播
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 配置异步执行器，支持SecurityContext传播
     */
    @Bean(name = "securityContextTaskExecutor")
    public Executor securityContextTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("SecurityContextAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        // 使用DelegatingSecurityContextExecutor包装，确保SecurityContext传播
        return new DelegatingSecurityContextExecutor(executor);
    }
    
    /**
     * 默认异步执行器，使用支持SecurityContext传播的执行器
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return securityContextTaskExecutor();
    }
}

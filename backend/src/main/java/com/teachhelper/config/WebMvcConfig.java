package com.teachhelper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 用于配置静态资源处理和其他Web相关设置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理，确保不与API路径冲突
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
                
        // 添加文档资源处理
        registry.addResourceHandler("/docs/**")
                .addResourceLocations("classpath:/docs/");
                
        // 确保API路径不被静态资源处理器拦截
        // 通过设置order确保API Controller优先处理
    }
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 确保API路径优先处理，避免被静态资源拦截
        configurer.setUseTrailingSlashMatch(false);
    }
}

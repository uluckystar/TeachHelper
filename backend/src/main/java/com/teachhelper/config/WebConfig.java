package com.teachhelper.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 包含全局的数据转换器和格式化器配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 添加自定义格式转换器
     * 主要用于URL参数的日期时间格式转换
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 添加LocalDateTime格式转换器，支持"yyyy-MM-dd HH:mm:ss"格式
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            try {
                // 支持多种日期时间格式
                DateTimeFormatter formatter;
                if (source.contains("T")) {
                    // ISO-8601格式：2025-06-23T00:00:00
                    formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                } else if (source.length() == 19) {
                    // 标准格式：2025-06-23 00:00:00
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                } else if (source.length() == 16) {
                    // 无秒格式：2025-06-23 00:00
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                } else if (source.length() == 10) {
                    // 仅日期格式：2025-06-23 (默认时间为00:00:00)
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    return LocalDateTime.parse(source + " 00:00:00", 
                                             DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else {
                    // 尝试使用标准格式
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                }
                return LocalDateTime.parse(source, formatter);
            } catch (Exception e) {
                throw new IllegalArgumentException("无法解析日期时间格式: " + source + 
                    "。支持的格式: yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm, yyyy-MM-dd, ISO-8601", e);
            }
        });
    }
} 
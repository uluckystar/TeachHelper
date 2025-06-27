package com.teachhelper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.document.parsing")
public class DocumentParsingConfig {
    
    /**
     * 是否启用LibreOffice转换功能
     */
    private boolean enableLibreOfficeConversion = true;
    
    /**
     * LibreOffice转换超时时间（秒）
     */
    private int libreOfficeTimeout = 30;
    
    /**
     * 自定义LibreOffice可执行文件路径
     */
    private String libreOfficePath;
    
    /**
     * 临时文件保留时间（分钟）
     */
    private int tempFileRetentionMinutes = 60;
    
    /**
     * 是否启用Apache Tika解析
     */
    private boolean enableTikaParser = true;
    
    /**
     * 是否启用纯文本回退解析
     */
    private boolean enablePlainTextFallback = true;
    
    /**
     * 最大文件大小限制（MB）
     */
    private int maxFileSizeMB = 50;
    
    /**
     * 并行解析的最大线程数
     */
    private int maxParsingThreads = 5;
} 
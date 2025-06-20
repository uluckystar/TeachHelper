package com.teachhelper.service.knowledge.processors;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档处理器接口
 */
public interface DocumentProcessor {
    
    /**
     * 检查是否支持该文件类型
     */
    boolean supports(String fileExtension);
    
    /**
     * 提取文档文本内容
     */
    String extractText(MultipartFile file) throws Exception;
    
    /**
     * 提取文档文本内容（从文件路径）
     */
    String extractText(String filePath) throws Exception;
    
    /**
     * 获取支持的文件扩展名
     */
    String[] getSupportedExtensions();
}

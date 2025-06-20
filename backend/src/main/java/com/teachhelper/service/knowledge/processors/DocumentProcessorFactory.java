package com.teachhelper.service.knowledge.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 文档处理器工厂
 */
@Component
public class DocumentProcessorFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessorFactory.class);
    
    @Autowired
    private List<DocumentProcessor> processors;
    
    /**
     * 根据文件扩展名获取合适的处理器
     */
    public Optional<DocumentProcessor> getProcessor(String fileExtension) {
        log.debug("Looking for processor for file extension: {}", fileExtension);
        
        return processors.stream()
            .filter(processor -> processor.supports(fileExtension))
            .findFirst();
    }
    
    /**
     * 检查文件类型是否支持
     */
    public boolean isSupported(String fileExtension) {
        return processors.stream()
            .anyMatch(processor -> processor.supports(fileExtension));
    }
    
    /**
     * 获取所有支持的文件类型
     */
    public String[] getSupportedFileTypes() {
        return processors.stream()
            .flatMap(processor -> Arrays.stream(processor.getSupportedExtensions()))
            .distinct()
            .toArray(String[]::new);
    }
    
    /**
     * 获取支持的文件类型描述
     */
    public String getSupportedFileTypesDescription() {
        String[] types = getSupportedFileTypes();
        return String.join(", ", types);
    }
    
    /**
     * 获取已注册的处理器数量
     */
    public int getProcessorCount() {
        return processors.size();
    }
    
    /**
     * 记录所有已注册的处理器
     */
    public void logRegisteredProcessors() {
        log.info("Registered document processors:");
        for (DocumentProcessor processor : processors) {
            String extensions = String.join(", ", processor.getSupportedExtensions());
            log.info("  {} supports: {}", processor.getClass().getSimpleName(), extensions);
        }
    }
}

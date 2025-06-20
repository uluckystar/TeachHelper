package com.teachhelper.service.knowledge.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 文本文档处理器
 */
@Component
public class TextDocumentProcessor implements DocumentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(TextDocumentProcessor.class);

    @Override
    public boolean supports(String fileExtension) {
        return ".txt".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        log.info("Processing text document: {}", file.getOriginalFilename());
        
        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            log.info("Successfully read text file: {} ({} characters)", 
                file.getOriginalFilename(), content.length());
            
            return content;
        } catch (IOException e) {
            log.error("Failed to read text file: {}", file.getOriginalFilename(), e);
            throw new Exception("Text file reading failed", e);
        }
    }

    @Override
    public String extractText(String filePath) throws Exception {
        log.info("Processing text document from path: {}", filePath);
        
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            log.info("Successfully read text file: {} ({} characters)", 
                filePath, content.length());
            
            return content;
        } catch (IOException e) {
            log.error("Failed to read text file: {}", filePath, e);
            throw new Exception("Text file reading failed", e);
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".txt"};
    }
}

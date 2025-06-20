package com.teachhelper.service.knowledge.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * PDF文档处理器，使用Spring AI的PDF Reader
 */
@Component
public class PdfDocumentProcessor implements DocumentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(PdfDocumentProcessor.class);

    @Override
    public boolean supports(String fileExtension) {
        return ".pdf".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        log.info("Processing PDF document: {}", file.getOriginalFilename());
        
        // 创建临时文件
        Path tempFile = Files.createTempFile("pdf_", ".pdf");
        try {
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            return extractTextFromFile(tempFile.toString());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public String extractText(String filePath) throws Exception {
        log.info("Processing PDF document from path: {}", filePath);
        return extractTextFromFile(filePath);
    }

    private String extractTextFromFile(String filePath) throws Exception {
        try {
            Resource resource = new FileSystemResource(filePath);
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            
            List<Document> documents = pdfReader.get();
            StringBuilder content = new StringBuilder();
            
            for (Document doc : documents) {
                content.append(doc.getFormattedContent()).append("\n");
            }
            
            String text = content.toString().trim();
            log.info("Successfully extracted text from PDF: {} characters", text.length());
            
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", filePath, e);
            throw new Exception("PDF text extraction failed", e);
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".pdf"};
    }
}
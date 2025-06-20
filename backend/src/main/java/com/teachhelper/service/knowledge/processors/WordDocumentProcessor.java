package com.teachhelper.service.knowledge.processors;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Word文档处理器
 */
@Component
@Slf4j
public class WordDocumentProcessor implements DocumentProcessor {

    @Override
    public boolean supports(String fileExtension) {
        return ".doc".equalsIgnoreCase(fileExtension) || 
               ".docx".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new Exception("File name is null");
        }

        try {
            if (fileName.toLowerCase().endsWith(".docx")) {
                return extractFromDocx(file);
            } else if (fileName.toLowerCase().endsWith(".doc")) {
                return extractFromDoc(file);
            }
            throw new Exception("Unsupported Word document format");
        } catch (IOException e) {
            log.error("Failed to extract text from Word document: {}", fileName, e);
            throw new Exception("Word document text extraction failed", e);
        }
    }

    @Override
    public String extractText(String filePath) throws Exception {
        try {
            if (filePath.toLowerCase().endsWith(".docx")) {
                return extractFromDocx(filePath);
            } else if (filePath.toLowerCase().endsWith(".doc")) {
                return extractFromDoc(filePath);
            }
            throw new Exception("Unsupported Word document format");
        } catch (IOException e) {
            log.error("Failed to extract text from Word document: {}", filePath, e);
            throw new Exception("Word document text extraction failed", e);
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractFromDoc(MultipartFile file) throws IOException {
        try (HWPFDocument document = new HWPFDocument(file.getInputStream());
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractFromDocx(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractFromDoc(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".doc", ".docx"};
    }
}

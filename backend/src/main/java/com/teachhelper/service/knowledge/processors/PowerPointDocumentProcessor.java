package com.teachhelper.service.knowledge.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * PowerPoint文档处理器
 */
@Component
public class PowerPointDocumentProcessor implements DocumentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(PowerPointDocumentProcessor.class);

    @Override
    public boolean supports(String fileExtension) {
        return ".ppt".equalsIgnoreCase(fileExtension) || 
               ".pptx".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new Exception("File name is null");
        }

        try {
            if (fileName.toLowerCase().endsWith(".pptx")) {
                return extractFromPptx(file);
            } else if (fileName.toLowerCase().endsWith(".ppt")) {
                return extractFromPpt(file);
            }
            throw new Exception("Unsupported PowerPoint document format");
        } catch (IOException e) {
            log.error("Failed to extract text from PowerPoint document: {}", fileName, e);
            throw new Exception("PowerPoint document text extraction failed", e);
        }
    }

    @Override
    public String extractText(String filePath) throws Exception {
        try {
            if (filePath.toLowerCase().endsWith(".pptx")) {
                return extractFromPptx(filePath);
            } else if (filePath.toLowerCase().endsWith(".ppt")) {
                return extractFromPpt(filePath);
            }
            throw new Exception("Unsupported PowerPoint document format");
        } catch (IOException e) {
            log.error("Failed to extract text from PowerPoint document: {}", filePath, e);
            throw new Exception("PowerPoint document text extraction failed", e);
        }
    }

    private String extractFromPptx(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (XMLSlideShow slideShow = new XMLSlideShow(file.getInputStream())) {
            for (XSLFSlide slide : slideShow.getSlides()) {
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        org.apache.poi.xslf.usermodel.XSLFTextShape textShape = 
                            (org.apache.poi.xslf.usermodel.XSLFTextShape) shape;
                        content.append(textShape.getText()).append("\n");
                    }
                });
            }
        }
        return content.toString();
    }

    private String extractFromPpt(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (HSLFSlideShow slideShow = new HSLFSlideShow(file.getInputStream())) {
            for (HSLFSlide slide : slideShow.getSlides()) {
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.hslf.usermodel.HSLFTextShape) {
                        org.apache.poi.hslf.usermodel.HSLFTextShape textShape = 
                            (org.apache.poi.hslf.usermodel.HSLFTextShape) shape;
                        content.append(textShape.getText()).append("\n");
                    }
                });
            }
        }
        return content.toString();
    }

    private String extractFromPptx(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XMLSlideShow slideShow = new XMLSlideShow(fis)) {
            for (XSLFSlide slide : slideShow.getSlides()) {
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        org.apache.poi.xslf.usermodel.XSLFTextShape textShape = 
                            (org.apache.poi.xslf.usermodel.XSLFTextShape) shape;
                        content.append(textShape.getText()).append("\n");
                    }
                });
            }
        }
        return content.toString();
    }

    private String extractFromPpt(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             HSLFSlideShow slideShow = new HSLFSlideShow(fis)) {
            for (HSLFSlide slide : slideShow.getSlides()) {
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.hslf.usermodel.HSLFTextShape) {
                        org.apache.poi.hslf.usermodel.HSLFTextShape textShape = 
                            (org.apache.poi.hslf.usermodel.HSLFTextShape) shape;
                        content.append(textShape.getText()).append("\n");
                    }
                });
            }
        }
        return content.toString();
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".ppt", ".pptx"};
    }
}

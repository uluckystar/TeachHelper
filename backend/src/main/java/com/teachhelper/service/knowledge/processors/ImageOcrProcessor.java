package com.teachhelper.service.knowledge.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 图片OCR处理器
 */
@Component
public class ImageOcrProcessor implements DocumentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ImageOcrProcessor.class);
    
    @Value("${app.ocr.tesseract.data-path:tessdata}")
    private String tesseractDataPath;
    
    @Value("${app.ocr.tesseract.language:chi_sim+eng}")
    private String ocrLanguage;

    @Override
    public boolean supports(String fileExtension) {
        return ".jpg".equalsIgnoreCase(fileExtension) || 
               ".jpeg".equalsIgnoreCase(fileExtension) || 
               ".png".equalsIgnoreCase(fileExtension) ||
               ".gif".equalsIgnoreCase(fileExtension) ||
               ".bmp".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        log.info("Processing image with OCR: {}", file.getOriginalFilename());
        
        // 创建临时文件
        Path tempFile = Files.createTempFile("image_", getFileExtension(file.getOriginalFilename()));
        try {
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            return extractTextFromFile(tempFile.toString());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public String extractText(String filePath) throws Exception {
        log.info("Processing image with OCR from path: {}", filePath);
        return extractTextFromFile(filePath);
    }

    private String extractTextFromFile(String filePath) throws Exception {
        try {
            // 读取图片
            File imageFile = new File(filePath);
            BufferedImage image = ImageIO.read(imageFile);
            
            if (image == null) {
                throw new IOException("Failed to read image file: " + filePath);
            }
            
            // 初始化Tesseract
            Tesseract tesseract = new Tesseract();
            
            // 检查Tesseract数据路径是否存在
            File dataPathFile = new File(tesseractDataPath);
            if (dataPathFile.exists()) {
                tesseract.setDatapath(tesseractDataPath);
            } else {
                log.warn("Tesseract data path not found: {}, using default", tesseractDataPath);
            }
            
            tesseract.setLanguage(ocrLanguage);
            
            // 设置OCR参数
            tesseract.setPageSegMode(1);  // 自动页面分割模式
            tesseract.setOcrEngineMode(1); // 使用LSTM OCR引擎
            
            // 执行OCR
            String text = tesseract.doOCR(image);
            
            log.info("Successfully extracted text from image: {} ({} characters)", 
                filePath, text.length());
            
            return text;
            
        } catch (TesseractException e) {
            log.error("OCR processing failed for image: {}", filePath, e);
            throw new Exception("OCR processing failed: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to read image file: {}", filePath, e);
            throw new Exception("Image reading failed", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".jpg", ".jpeg", ".png", ".gif", ".bmp"};
    }
}

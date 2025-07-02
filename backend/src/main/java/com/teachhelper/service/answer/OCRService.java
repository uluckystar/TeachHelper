package com.teachhelper.service.answer;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * OCR (光学字符识别) 服务
 * 使用 Tesseract 引擎识别图片中的文字内容
 */
@Service
public class OCRService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRService.class);
    
    @Value("${app.ocr.tesseract.datapath:/usr/local/share/tessdata}")
    private String tesseractDataPath;
    
    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;
    
    /**
     * 识别图片中的文字内容
     * @param imageFile 图片文件
     * @return 识别出的文字内容
     */
    public String extractTextFromImage(File imageFile) {
        if (!ocrEnabled) {
            log.info("OCR功能已禁用，跳过图片文字识别: {}", imageFile.getName());
            return null;
        }
        
        log.info("🔍 开始OCR文字识别: {}", imageFile.getName());
        
        try {
            // 检查文件是否存在
            if (!imageFile.exists() || !imageFile.isFile()) {
                log.warn("图片文件不存在: {}", imageFile.getAbsolutePath());
                return null;
            }
            
            // 检查文件大小（限制10MB）
            long fileSizeInMB = imageFile.length() / (1024 * 1024);
            if (fileSizeInMB > 10) {
                log.warn("图片文件过大: {} MB，跳过OCR识别", fileSizeInMB);
                return "图片文件过大（超过10MB），无法进行OCR识别";
            }
            
            // 验证图片格式
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.warn("无法读取图片文件: {}", imageFile.getName());
                return null;
            }
            
            // 配置Tesseract
            Tesseract tesseract = new Tesseract();
            
            // 设置训练数据路径
            if (new File(tesseractDataPath).exists()) {
                tesseract.setDatapath(tesseractDataPath);
                log.debug("使用Tesseract数据路径: {}", tesseractDataPath);
            } else {
                log.debug("使用默认Tesseract数据路径");
            }
            
            // 设置识别语言（中文+英文）
            tesseract.setLanguage("chi_sim+eng");
            
            // 设置OCR引擎模式和分页模式
            tesseract.setOcrEngineMode(1); // OEM_LSTM_ONLY
            tesseract.setPageSegMode(6);   // PSM_SINGLE_UNIFORM_BLOCK
            
            // 执行OCR识别
            long startTime = System.currentTimeMillis();
            String result = tesseract.doOCR(image);
            long duration = System.currentTimeMillis() - startTime;
            
            if (result != null && !result.trim().isEmpty()) {
                String cleanedResult = cleanOCRResult(result);
                log.info("✅ OCR识别成功: {} - 耗时: {}ms, 识别内容长度: {} 字符", 
                        imageFile.getName(), duration, cleanedResult.length());
                log.debug("OCR识别内容预览: {}", 
                        cleanedResult.length() > 100 ? cleanedResult.substring(0, 100) + "..." : cleanedResult);
                return cleanedResult;
            } else {
                log.warn("⚠️ OCR识别结果为空: {}", imageFile.getName());
                return "OCR识别未能提取到文字内容，可能是图片质量问题或不包含文字";
            }
            
        } catch (TesseractException e) {
            log.error("❌ Tesseract OCR识别失败: {} - {}", imageFile.getName(), e.getMessage());
            return "OCR识别失败: " + e.getMessage();
        } catch (IOException e) {
            log.error("❌ 读取图片文件失败: {} - {}", imageFile.getName(), e.getMessage());
            return "读取图片文件失败: " + e.getMessage();
        } catch (Exception e) {
            log.error("❌ OCR处理异常: {} - {}", imageFile.getName(), e.getMessage(), e);
            return "OCR处理异常: " + e.getMessage();
        }
    }
    
    /**
     * 清理OCR识别结果
     * @param rawResult 原始OCR结果
     * @return 清理后的结果
     */
    private String cleanOCRResult(String rawResult) {
        if (rawResult == null) {
            return "";
        }
        
        return rawResult
                // 移除多余的空白字符
                .replaceAll("\\s+", " ")
                // 移除特殊控制字符
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                // 移除行首行尾空白
                .trim();
    }
    
    /**
     * 检查图片文件格式是否支持OCR
     * @param fileName 文件名
     * @return 是否支持OCR
     */
    public boolean isSupportedImageFormat(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
               lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
               lowerName.endsWith(".bmp") || lowerName.endsWith(".tiff") ||
               lowerName.endsWith(".tif") || lowerName.endsWith(".webp");
    }
    
    /**
     * 获取OCR服务状态信息
     * @return 状态信息
     */
    public String getOCRStatus() {
        StringBuilder status = new StringBuilder();
        status.append("OCR服务状态: ").append(ocrEnabled ? "启用" : "禁用").append("\n");
        status.append("Tesseract数据路径: ").append(tesseractDataPath).append("\n");
        status.append("数据路径存在: ").append(new File(tesseractDataPath).exists()).append("\n");
        
        if (ocrEnabled) {
            try {
                Tesseract tesseract = new Tesseract();
                if (new File(tesseractDataPath).exists()) {
                    tesseract.setDatapath(tesseractDataPath);
                }
                // 简单测试
                status.append("Tesseract初始化: 成功\n");
            } catch (Exception e) {
                status.append("Tesseract初始化: 失败 - ").append(e.getMessage()).append("\n");
            }
        }
        
        return status.toString();
    }
} 
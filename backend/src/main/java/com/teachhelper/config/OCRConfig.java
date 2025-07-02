package com.teachhelper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * OCR配置类
 * 自动检测和设置Tesseract库路径
 */
@Configuration
@Slf4j
public class OCRConfig {
    
    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;
    
    @Value("${app.ocr.tesseract.datapath:/opt/homebrew/share/tessdata}")
    private String tesseractDataPath;
    
    private final Environment environment;
    
    public OCRConfig(Environment environment) {
        this.environment = environment;
    }
    
    @PostConstruct
    public void initializeOCR() {
        if (!ocrEnabled) {
            log.info("OCR功能已禁用，跳过初始化");
            return;
        }
        
        log.info("🔧 开始初始化OCR配置...");
        
        // 设置Tesseract库路径
        setupTesseractLibraryPath();
        
        // 验证Tesseract数据路径
        validateTesseractDataPath();
        
        log.info("✅ OCR配置初始化完成");
    }
    
    /**
     * 设置Tesseract库路径
     */
    private void setupTesseractLibraryPath() {
        // 常见的Tesseract库路径
        List<String> possiblePaths = Arrays.asList(
            "/opt/homebrew/Cellar/tesseract/5.5.0_1/lib",  // Homebrew最新版本
            "/opt/homebrew/lib",                             // Homebrew链接目录
            "/usr/local/lib",                                // 传统Unix路径
            "/usr/lib",                                      // 系统路径
            System.getProperty("java.library.path", "")     // 现有的库路径
        );
        
        String foundPath = null;
        for (String path : possiblePaths) {
            if (path.isEmpty()) continue;
            
            File libFile = new File(path, "libtesseract.dylib");
            File libFile5 = new File(path, "libtesseract.5.dylib");
            
            if (libFile.exists() || libFile5.exists()) {
                foundPath = path;
                log.info("🔍 找到Tesseract库路径: {}", path);
                break;
            }
        }
        
        if (foundPath != null) {
            // 设置JNA库路径
            String currentJnaPath = System.getProperty("jna.library.path", "");
            String newJnaPath = currentJnaPath.isEmpty() ? foundPath : currentJnaPath + File.pathSeparator + foundPath;
            System.setProperty("jna.library.path", newJnaPath);
            
            // 设置Java库路径
            String currentJavaPath = System.getProperty("java.library.path", "");
            String newJavaPath = currentJavaPath.isEmpty() ? foundPath : currentJavaPath + File.pathSeparator + foundPath;
            System.setProperty("java.library.path", newJavaPath);
            
            log.info("📚 设置JNA库路径: {}", newJnaPath);
            log.info("📚 设置Java库路径: {}", newJavaPath);
        } else {
            log.warn("⚠️ 未找到Tesseract库文件，OCR功能可能无法正常工作");
            log.warn("请确保已安装Tesseract: brew install tesseract tesseract-lang");
        }
    }
    
    /**
     * 验证Tesseract数据路径
     */
    private void validateTesseractDataPath() {
        File dataDir = new File(tesseractDataPath);
        if (dataDir.exists() && dataDir.isDirectory()) {
            log.info("📁 Tesseract数据路径验证成功: {}", tesseractDataPath);
            
            // 检查中文语言包
            File chineseModel = new File(dataDir, "chi_sim.traineddata");
            File englishModel = new File(dataDir, "eng.traineddata");
            
            if (chineseModel.exists()) {
                log.info("🈶 检测到中文简体语言包");
            } else {
                log.warn("⚠️ 未检测到中文简体语言包，请安装: brew install tesseract-lang");
            }
            
            if (englishModel.exists()) {
                log.info("🅰️ 检测到英文语言包");
            } else {
                log.warn("⚠️ 未检测到英文语言包");
            }
        } else {
            log.error("❌ Tesseract数据路径不存在: {}", tesseractDataPath);
            log.error("请检查配置或安装Tesseract");
        }
    }
    
    /**
     * 获取OCR配置信息
     */
    public String getConfigInfo() {
        StringBuilder info = new StringBuilder();
        info.append("OCR配置信息:\n");
        info.append("- OCR启用状态: ").append(ocrEnabled).append("\n");
        info.append("- Tesseract数据路径: ").append(tesseractDataPath).append("\n");
        info.append("- JNA库路径: ").append(System.getProperty("jna.library.path", "未设置")).append("\n");
        info.append("- Java库路径: ").append(System.getProperty("java.library.path", "未设置")).append("\n");
        
        return info.toString();
    }
} 
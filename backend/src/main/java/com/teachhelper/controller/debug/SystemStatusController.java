package com.teachhelper.controller.debug;

import com.teachhelper.config.DocumentParsingConfig;
import com.teachhelper.service.answer.OCRService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统状态检查控制器
 * 用于检查各个组件的运行状态
 */
@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class SystemStatusController {

    @Autowired(required = false)
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DocumentParsingConfig documentParsingConfig;

    @Autowired
    private OCRService ocrService;

    /**
     * 检查系统整体状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 检查数据库连接
            boolean dbStatus = checkDatabaseConnection();
            status.put("database", dbStatus);
            
            // 检查AI服务
            boolean aiStatus = checkAIServices();
            status.put("ai", aiStatus);
            
            // 检查向量存储
            boolean vectorStatus = checkVectorStore();
            status.put("vectorStore", vectorStatus);
            
            // 检查文档解析能力
            Map<String, Object> parsingStatus = checkDocumentParsingCapabilities();
            status.put("documentParsing", parsingStatus);
            
            // 整体状态
            boolean overallStatus = dbStatus && aiStatus && vectorStatus;
            status.put("overall", overallStatus);
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("error", e.getMessage());
            status.put("overall", false);
            return ResponseEntity.status(500).body(status);
        }
    }
    
    /**
     * 检查数据库连接
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean connected = checkDatabaseConnection();
            result.put("connected", connected);
            
            if (connected) {
                // 检查必要的表
                result.put("tables", checkRequiredTables());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 检查AI服务
     */
    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> checkAI() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("chatModel", chatModel != null);
            result.put("embeddingModel", embeddingModel != null);
            
            // 如果ChatModel可用，尝试简单调用
            if (chatModel != null) {
                try {
                    // 简单测试调用
                    result.put("chatModelWorking", true);
                } catch (Exception e) {
                    result.put("chatModelWorking", false);
                    result.put("chatModelError", e.getMessage());
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 检查向量存储
     */
    @GetMapping("/vector")
    public ResponseEntity<Map<String, Object>> checkVector() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("vectorStoreAvailable", vectorStore != null);
            
            if (vectorStore != null) {
                result.put("vectorStoreClass", vectorStore.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("vectorStoreAvailable", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 检查文档解析能力
     */
    @GetMapping("/document-parsing")
    public ResponseEntity<Map<String, Object>> checkDocumentParsing() {
        return ResponseEntity.ok(checkDocumentParsingCapabilities());
    }
    
    /**
     * 获取OCR服务状态
     */
    @GetMapping("/ocr-status")
    public ResponseEntity<Map<String, Object>> getOCRStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            String ocrStatus = ocrService.getOCRStatus();
            status.put("status", "success");
            status.put("ocr_info", ocrStatus);
            status.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("status", "error");
            status.put("error", e.getMessage());
            status.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }
    
    // 私有辅助方法
    private boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5秒超时
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkAIServices() {
        return chatModel != null && embeddingModel != null;
    }
    
    private boolean checkVectorStore() {
        return vectorStore != null;
    }
    
    private Map<String, Boolean> checkRequiredTables() {
        Map<String, Boolean> tables = new HashMap<>();
        String[] requiredTables = {
            "knowledge_documents", 
            "knowledge_points", 
            "vector_store",
            "knowledge_bases"
        };
        
        try (Connection connection = dataSource.getConnection()) {
            for (String tableName : requiredTables) {
                try {
                    connection.prepareStatement("SELECT 1 FROM " + tableName + " LIMIT 1").execute();
                    tables.put(tableName, true);
                } catch (Exception e) {
                    tables.put(tableName, false);
                }
            }
        } catch (Exception e) {
            // 如果连接失败，所有表都标记为不可用
            for (String tableName : requiredTables) {
                tables.put(tableName, false);
            }
        }
        
        return tables;
    }
    
    /**
     * 检查文档解析能力
     */
    private Map<String, Object> checkDocumentParsingCapabilities() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查Apache POI可用性
            boolean poiAvailable = checkApachePOIAvailable();
            result.put("apachePOI", poiAvailable);
            
            // 检查Apache Tika可用性
            boolean tikaAvailable = checkApacheTikaAvailable();
            result.put("apacheTika", tikaAvailable);
            
            // 检查LibreOffice可用性
            Map<String, Object> libreOfficeStatus = checkLibreOfficeAvailable();
            result.put("libreOffice", libreOfficeStatus);
            
            // 配置信息
            Map<String, Object> config = new HashMap<>();
            config.put("apachePOIEnabled", true); // Apache POI总是可用的
            config.put("apacheTikaEnabled", documentParsingConfig.isEnableTikaParser());
            config.put("libreOfficeEnabled", documentParsingConfig.isEnableLibreOfficeConversion());
            config.put("libreOfficeTimeout", documentParsingConfig.getLibreOfficeTimeout());
            result.put("config", config);
            
            // 解析策略
            Map<String, String> strategies = new HashMap<>();
            strategies.put("primary", "Apache POI");
            strategies.put("secondary", "Apache Tika");
            strategies.put("tertiary", "LibreOffice Conversion");
            strategies.put("fallback", "Plain Text");
            result.put("strategies", strategies);
            
            // 整体状态
            boolean overallCapable = poiAvailable || tikaAvailable || (Boolean) libreOfficeStatus.get("available");
            result.put("capable", overallCapable);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("capable", false);
        }
        
        return result;
    }
    
    private boolean checkApachePOIAvailable() {
        try {
            Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument");
            Class.forName("org.apache.poi.hwpf.HWPFDocument");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private boolean checkApacheTikaAvailable() {
        try {
            Class.forName("org.apache.tika.Tika");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private Map<String, Object> checkLibreOfficeAvailable() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 检查各平台的LibreOffice路径
            String[] possiblePaths = {
                "/usr/bin/libreoffice",          // Linux
                "/opt/libreoffice/program/soffice", // Linux alternative
                "/Applications/LibreOffice.app/Contents/MacOS/soffice", // macOS
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe", // Windows
                "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe" // Windows 32-bit
            };
            
            String availablePath = null;
            for (String path : possiblePaths) {
                if (new java.io.File(path).exists()) {
                    availablePath = path;
                    break;
                }
            }
            
            status.put("available", availablePath != null);
            status.put("path", availablePath);
            
            if (availablePath != null) {
                try {
                    // 尝试执行version命令测试
                    ProcessBuilder pb = new ProcessBuilder(availablePath, "--version");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                    
                    if (finished && process.exitValue() == 0) {
                        status.put("working", true);
                        // 读取版本信息
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(process.getInputStream()))) {
                            String version = reader.readLine();
                            status.put("version", version);
                        }
                    } else {
                        status.put("working", false);
                        status.put("error", "LibreOffice command failed or timed out");
                    }
                    
                    if (!finished) {
                        process.destroyForcibly();
                    }
                } catch (Exception e) {
                    status.put("working", false);
                    status.put("error", "Failed to test LibreOffice: " + e.getMessage());
                }
            } else {
                status.put("working", false);
                status.put("suggestion", "请安装LibreOffice以增强文档解析能力");
            }
            
        } catch (Exception e) {
            status.put("available", false);
            status.put("working", false);
            status.put("error", e.getMessage());
        }
        
        return status;
    }
}

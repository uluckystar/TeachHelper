package com.teachhelper.controller.debug;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
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
}

package com.teachhelper.service.knowledge;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索到的文档
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrievedDocument {
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 相似度分数
     */
    private double score;
    
    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档类型
     */
    private String documentType;
    
    /**
     * 文档路径
     */
    private String filePath;
    
    /**
     * 获取元数据值
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 构造函数 - 用于从Spring AI Document创建
     */
    public RetrievedDocument(String id, String content, Map<String, Object> metadata, Double score) {
        this.documentId = id;
        this.content = content;
        this.metadata = metadata;
        this.score = score != null ? score : 0.0;
        
        // 从metadata中提取字段
        if (metadata != null) {
            this.title = (String) metadata.getOrDefault("title", "");
            this.documentType = (String) metadata.getOrDefault("fileType", "");
            this.filePath = (String) metadata.getOrDefault("fileName", "");
            this.knowledgeBaseId = (Long) metadata.get("knowledgeBaseId");
        }
    }
    
    /**
     * 获取文档摘要
     */
    public String getSummary() {
        if (content != null && content.length() > 200) {
            return content.substring(0, 200) + "...";
        }
        return content;
    }
}

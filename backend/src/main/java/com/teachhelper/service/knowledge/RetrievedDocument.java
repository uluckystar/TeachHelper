package com.teachhelper.service.knowledge;

import java.util.Objects;

import java.util.Map;

/**
 * 检索到的文档
 */
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
    
    // 构造函数
    public RetrievedDocument() {}
    
    public RetrievedDocument(String content, double score, Map<String, Object> metadata, 
                           String documentId, Long knowledgeBaseId, String title, 
                           String documentType, String filePath) {
        this.content = content;
        this.score = score;
        this.metadata = metadata;
        this.documentId = documentId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.title = title;
        this.documentType = documentType;
        this.filePath = filePath;
    }
    
    // Getter and Setter methods
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }
    
    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetrievedDocument that = (RetrievedDocument) o;
        return Double.compare(that.score, score) == 0 &&
               Objects.equals(content, that.content) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(documentId, that.documentId) &&
               Objects.equals(knowledgeBaseId, that.knowledgeBaseId) &&
               Objects.equals(title, that.title) &&
               Objects.equals(documentType, that.documentType) &&
               Objects.equals(filePath, that.filePath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(content, score, metadata, documentId, knowledgeBaseId, 
                          title, documentType, filePath);
    }
    
    @Override
    public String toString() {
        return "RetrievedDocument{" +
               "content='" + getSummary() + '\'' +
               ", score=" + score +
               ", documentId='" + documentId + '\'' +
               ", knowledgeBaseId=" + knowledgeBaseId +
               ", title='" + title + '\'' +
               ", documentType='" + documentType + '\'' +
               ", filePath='" + filePath + '\'' +
               '}';
    }
    
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

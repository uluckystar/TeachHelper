package com.teachhelper.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识点响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointResponse {

    private Long id;
    
    private Long knowledgeBaseId;
    
    private String title;
    
    private String content;
    
    private String keywords;
    
    private String difficultyLevel;
    
    private Long sourceDocumentId;
    
    private Integer sourcePageNumber;
    
    private String vectorId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // 便于前端使用的字段
    private String difficulty; // 前端期望的格式：'EASY' | 'MEDIUM' | 'HARD'
    
    private String[] tags; // 解析keywords为数组
    
    private String summary; // 知识点摘要
    
    private Boolean isAIGenerated; // 是否AI生成
    
    // 统计信息
    private Integer relatedDocumentsCount;
    
    private Integer relatedQuestionsCount;
    
    // Manual setters (fallback for Lombok issues)
    public void setTags(String[] tags) { this.tags = tags; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setIsAIGenerated(Boolean isAIGenerated) { this.isAIGenerated = isAIGenerated; }
    public void setRelatedDocumentsCount(Integer relatedDocumentsCount) { this.relatedDocumentsCount = relatedDocumentsCount; }
    public void setRelatedQuestionsCount(Integer relatedQuestionsCount) { this.relatedQuestionsCount = relatedQuestionsCount; }
}

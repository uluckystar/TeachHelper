package com.teachhelper.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeBaseResponse {
    private Long id;
    private String name;
    private String description;
    private String subject;
    private String gradeLevel;
    private Long createdBy;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private Long documentCount = 0L;
    private Long knowledgePointCount = 0L;
    private Long favoriteCount = 0L;
    private Boolean isFavorited = false; // 当前用户是否已收藏
    
    // Manual setters (fallback for Lombok issues)
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setDocumentCount(Long documentCount) { this.documentCount = documentCount; }
    public void setKnowledgePointCount(Long knowledgePointCount) { this.knowledgePointCount = knowledgePointCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
    public void setIsFavorited(Boolean isFavorited) { this.isFavorited = isFavorited; }
}

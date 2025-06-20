package com.teachhelper.dto.response;

import java.time.LocalDateTime;

/**
 * 试卷生成模板响应DTO
 */
public class PaperGenerationTemplateResponse {
    
    private Long id;
    private String name;
    private String description;
    private String subject;
    private String gradeLevel;
    private Integer totalScore;
    private Integer timeLimit;
    private String questionConfig;
    private String difficultyConfig;
    private String knowledgeBaseConfig;
    private Long createdBy;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isPublic;
    
    // Constructors
    public PaperGenerationTemplateResponse() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
    
    public String getQuestionConfig() { return questionConfig; }
    public void setQuestionConfig(String questionConfig) { this.questionConfig = questionConfig; }
    
    public String getDifficultyConfig() { return difficultyConfig; }
    public void setDifficultyConfig(String difficultyConfig) { this.difficultyConfig = difficultyConfig; }
    
    public String getKnowledgeBaseConfig() { return knowledgeBaseConfig; }
    public void setKnowledgeBaseConfig(String knowledgeBaseConfig) { this.knowledgeBaseConfig = knowledgeBaseConfig; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}

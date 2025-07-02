package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷模板响应DTO
 */
public class ExamPaperTemplateResponse {
    
    private Long id;
    private String name;
    private String description;
    private String subject;
    private String gradeLevel;
    private Integer totalScore;
    private Integer duration;
    private String status;
    private String templateType;
    private String questionTypeConfig;
    private String difficultyConfig;
    private String knowledgeBaseConfig;
    private String tags;
    private Boolean isPublic;
    private Integer usageCount;
    private Long createdBy;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUsedAt;
    private Integer questionCount;
    private Integer configuredQuestionCount;
    private Boolean isComplete;
    private Boolean isUsable;
    private List<ExamPaperTemplateQuestionResponse> questions;
    
    // Constructors
    public ExamPaperTemplateResponse() {}
    
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
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    
    public String getQuestionTypeConfig() { return questionTypeConfig; }
    public void setQuestionTypeConfig(String questionTypeConfig) { this.questionTypeConfig = questionTypeConfig; }
    
    public String getDifficultyConfig() { return difficultyConfig; }
    public void setDifficultyConfig(String difficultyConfig) { this.difficultyConfig = difficultyConfig; }
    
    public String getKnowledgeBaseConfig() { return knowledgeBaseConfig; }
    public void setKnowledgeBaseConfig(String knowledgeBaseConfig) { this.knowledgeBaseConfig = knowledgeBaseConfig; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
    
    public Integer getConfiguredQuestionCount() { return configuredQuestionCount; }
    public void setConfiguredQuestionCount(Integer configuredQuestionCount) { this.configuredQuestionCount = configuredQuestionCount; }
    
    public Boolean getIsComplete() { return isComplete; }
    public void setIsComplete(Boolean isComplete) { this.isComplete = isComplete; }
    
    public Boolean getIsUsable() { return isUsable; }
    public void setIsUsable(Boolean isUsable) { this.isUsable = isUsable; }
    
    public List<ExamPaperTemplateQuestionResponse> getQuestions() { return questions; }
    public void setQuestions(List<ExamPaperTemplateQuestionResponse> questions) { this.questions = questions; }
} 
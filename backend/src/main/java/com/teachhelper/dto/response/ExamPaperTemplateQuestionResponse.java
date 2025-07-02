package com.teachhelper.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷模板题目响应DTO
 */
public class ExamPaperTemplateQuestionResponse {
    
    private Long id;
    private Long templateId;
    private Integer questionOrder;
    private String questionType;
    private String questionContent;
    private Long questionId;
    private BigDecimal score;
    private String difficultyLevel;
    private String knowledgeTags;
    private String questionConfig;
    private Boolean isRequired;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isConfigured;
    private Boolean isReady;
    
    // 如果引用了现有题目，可能包含题目的详细信息
    private QuestionSummary referencedQuestion;
    
    // 内部类：题目摘要
    public static class QuestionSummary {
        private Long id;
        private String content;
        private String questionType;
        private BigDecimal score;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }
        
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
    }
    
    // Constructors
    public ExamPaperTemplateQuestionResponse() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    
    public String getQuestionContent() { return questionContent; }
    public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
    
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public String getKnowledgeTags() { return knowledgeTags; }
    public void setKnowledgeTags(String knowledgeTags) { this.knowledgeTags = knowledgeTags; }
    
    public String getQuestionConfig() { return questionConfig; }
    public void setQuestionConfig(String questionConfig) { this.questionConfig = questionConfig; }
    
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getIsConfigured() { return isConfigured; }
    public void setIsConfigured(Boolean isConfigured) { this.isConfigured = isConfigured; }
    
    public Boolean getIsReady() { return isReady; }
    public void setIsReady(Boolean isReady) { this.isReady = isReady; }
    
    public QuestionSummary getReferencedQuestion() { return referencedQuestion; }
    public void setReferencedQuestion(QuestionSummary referencedQuestion) { this.referencedQuestion = referencedQuestion; }
} 
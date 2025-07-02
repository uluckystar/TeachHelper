package com.teachhelper.dto.request;

import jakarta.validation.constraints.*;

/**
 * 试卷模板题目请求DTO
 */
public class ExamPaperTemplateQuestionRequest {
    
    private Long id;
    
    @NotNull(message = "题目序号不能为空")
    @Min(value = 1, message = "题目序号必须大于0")
    private Integer questionOrder;
    
    @NotBlank(message = "题目类型不能为空")
    private String questionType;
    
    private String questionContent;
    
    private Long questionId;
    
    @DecimalMin(value = "0.0", message = "分数不能为负数")
    @DecimalMax(value = "100.0", message = "分数不能超过100")
    private Double score;
    
    private String difficultyLevel;
    
    private String knowledgeTags;
    
    private String questionConfig;
    
    private Boolean isRequired = true;
    
    private String status = "DRAFT";
    
    // Constructors
    public ExamPaperTemplateQuestionRequest() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    
    public String getQuestionContent() { return questionContent; }
    public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
    
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
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
} 
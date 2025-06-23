package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.teachhelper.dto.QuestionOptionDTO;
import com.teachhelper.entity.QuestionType;

public class QuestionResponse {
    
    private Long id;
    private Long examId;
    private String examTitle;
    private String title;
    private String content;
    private QuestionType questionType;
    private Double maxScore;
    private String referenceAnswer;
    private List<QuestionOptionDTO> options;
    private List<RubricCriterionResponse> rubricCriteria;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long totalAnswers;
    private long evaluatedAnswers;
    private double averageScore;
    
    // 题目来源相关字段
    private String sourceType;
    private Long sourceKnowledgeBaseId;
    private String sourceKnowledgeBaseName;
    private Long sourceKnowledgePointId;
    private String sourceKnowledgePointName;
    private Long questionBankId;
    private String questionBankName;
    private String difficulty;
    private String keywords;
    private Boolean isConfirmed; // AI整理题目的确认状态
    
    public QuestionResponse() {}
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getExamId() {
        return examId;
    }
    
    public void setExamId(Long examId) {
        this.examId = examId;
    }
    
    public String getExamTitle() {
        return examTitle;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public QuestionType getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
    
    public Double getMaxScore() {
        return maxScore;
    }
    
    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }
    
    public String getReferenceAnswer() {
        return referenceAnswer;
    }
    
    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }
    
    public List<QuestionOptionDTO> getOptions() {
        return options;
    }
    
    public void setOptions(List<QuestionOptionDTO> options) {
        this.options = options;
    }
    
    public List<RubricCriterionResponse> getRubricCriteria() {
        return rubricCriteria;
    }
    
    public void setRubricCriteria(List<RubricCriterionResponse> rubricCriteria) {
        this.rubricCriteria = rubricCriteria;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public long getTotalAnswers() {
        return totalAnswers;
    }
    
    public void setTotalAnswers(long totalAnswers) {
        this.totalAnswers = totalAnswers;
    }
    
    public long getEvaluatedAnswers() {
        return evaluatedAnswers;
    }
    
    public void setEvaluatedAnswers(long evaluatedAnswers) {
        this.evaluatedAnswers = evaluatedAnswers;
    }
    
    public double getAverageScore() {
        return averageScore;
    }
    
    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }
    
    // 题目来源相关字段的getter和setter
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public Long getSourceKnowledgeBaseId() {
        return sourceKnowledgeBaseId;
    }
    
    public void setSourceKnowledgeBaseId(Long sourceKnowledgeBaseId) {
        this.sourceKnowledgeBaseId = sourceKnowledgeBaseId;
    }
    
    public String getSourceKnowledgeBaseName() {
        return sourceKnowledgeBaseName;
    }
    
    public void setSourceKnowledgeBaseName(String sourceKnowledgeBaseName) {
        this.sourceKnowledgeBaseName = sourceKnowledgeBaseName;
    }
    
    public Long getSourceKnowledgePointId() {
        return sourceKnowledgePointId;
    }
    
    public void setSourceKnowledgePointId(Long sourceKnowledgePointId) {
        this.sourceKnowledgePointId = sourceKnowledgePointId;
    }
    
    public String getSourceKnowledgePointName() {
        return sourceKnowledgePointName;
    }
    
    public void setSourceKnowledgePointName(String sourceKnowledgePointName) {
        this.sourceKnowledgePointName = sourceKnowledgePointName;
    }
    
    public Long getQuestionBankId() {
        return questionBankId;
    }
    
    public void setQuestionBankId(Long questionBankId) {
        this.questionBankId = questionBankId;
    }
    
    public String getQuestionBankName() {
        return questionBankName;
    }
    
    public void setQuestionBankName(String questionBankName) {
        this.questionBankName = questionBankName;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public Boolean getIsConfirmed() {
        return isConfirmed;
    }
    
    public void setIsConfirmed(Boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }
    
    public static class RubricCriterionResponse {
        private Long id;
        private String criterionText;
        private Double points;
        
        public RubricCriterionResponse() {}
        
        public RubricCriterionResponse(Long id, String criterionText, Double points) {
            this.id = id;
            this.criterionText = criterionText;
            this.points = points;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getCriterionText() {
            return criterionText;
        }
        
        public void setCriterionText(String criterionText) {
            this.criterionText = criterionText;
        }
        
        public Double getPoints() {
            return points;
        }
        
        public void setPoints(Double points) {
            this.points = points;
        }
    }
}

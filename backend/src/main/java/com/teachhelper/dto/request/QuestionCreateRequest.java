package com.teachhelper.dto.request;

import java.util.List;

import com.teachhelper.dto.QuestionOptionDTO;
import com.teachhelper.entity.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class QuestionCreateRequest {
    
    @NotNull(message = "Exam ID is required")
    private Long examId;
    
    @NotBlank(message = "Question title is required")
    @Size(max = 200, message = "Question title must not exceed 200 characters")
    private String title;
    
    @NotBlank(message = "Question content is required")
    @Size(max = 2000, message = "Question content must not exceed 2000 characters")
    private String content;
    
    @NotNull(message = "Question type is required")
    private QuestionType questionType;
    
    @NotNull(message = "Maximum score is required")
    @Positive(message = "Maximum score must be positive")
    private Double maxScore;
    
    private String referenceAnswer;
    
    @Valid
    private List<QuestionOptionDTO> options;
    
    @Valid
    private List<RubricCriterionRequest> rubricCriteria;
    
    public QuestionCreateRequest() {}
    
    public Long getExamId() {
        return examId;
    }
    
    public void setExamId(Long examId) {
        this.examId = examId;
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
    
    public List<RubricCriterionRequest> getRubricCriteria() {
        return rubricCriteria;
    }
    
    public void setRubricCriteria(List<RubricCriterionRequest> rubricCriteria) {
        this.rubricCriteria = rubricCriteria;
    }
    
    public static class RubricCriterionRequest {
        @NotBlank(message = "Criterion text is required")
        @Size(max = 500, message = "Criterion text must not exceed 500 characters")
        private String criterionText;
        
        @NotNull(message = "Points are required")
        @Positive(message = "Points must be positive")
        private Double points;
        
        public RubricCriterionRequest() {}
        
        public RubricCriterionRequest(String criterionText, Double points) {
            this.criterionText = criterionText;
            this.points = points;
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

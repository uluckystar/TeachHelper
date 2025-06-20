package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.teachhelper.entity.ExamStatus;
import com.teachhelper.entity.QuestionType;

public class ExamResponse {
    
    private Long id;
    private String title;
    private String description;
    private ExamStatus status;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<QuestionSummary> questions;
    private long totalQuestions;
    private long totalAnswers;
    private long evaluatedAnswers;
    private int questionCount;
    private double totalScore;
    
    public ExamResponse() {}
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ExamStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExamStatus status) {
        this.status = status;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getSettings() {
        return settings;
    }
    
    public void setSettings(String settings) {
        this.settings = settings;
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
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public List<QuestionSummary> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<QuestionSummary> questions) {
        this.questions = questions;
    }
    
    public long getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
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
    
    public int getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }
    
    public double getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }
    
    public static class QuestionSummary {
        private Long id;
        private String title;
        private QuestionType questionType;
        private Double maxScore;
        private long answerCount;
        private long evaluatedCount;
        
        public QuestionSummary() {}
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
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
        
        public long getAnswerCount() {
            return answerCount;
        }
        
        public void setAnswerCount(long answerCount) {
            this.answerCount = answerCount;
        }
        
        public long getEvaluatedCount() {
            return evaluatedCount;
        }
        
        public void setEvaluatedCount(long evaluatedCount) {
            this.evaluatedCount = evaluatedCount;
        }
    }
}

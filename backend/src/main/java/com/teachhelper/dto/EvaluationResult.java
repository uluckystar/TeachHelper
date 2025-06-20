package com.teachhelper.dto;

import java.math.BigDecimal;

/**
 * AI评估结果DTO
 */
public class EvaluationResult {
    
    private BigDecimal score;
    private BigDecimal maxScore;
    private String feedback;
    private String reasoning;
    private boolean success;
    private String errorMessage;
    
    public EvaluationResult() {}
    
    public EvaluationResult(BigDecimal score, BigDecimal maxScore, String feedback) {
        this.score = score;
        this.maxScore = maxScore;
        this.feedback = feedback;
        this.success = true;
    }
    
    public EvaluationResult(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
        this.score = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public BigDecimal getScore() {
        return score;
    }
    
    public void setScore(BigDecimal score) {
        this.score = score;
    }
    
    public BigDecimal getMaxScore() {
        return maxScore;
    }
    
    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

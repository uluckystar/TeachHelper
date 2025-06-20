package com.teachhelper.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ManualEvaluationRequest {
    
    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be at least 0")
    @DecimalMax(value = "100.0", message = "Score must not exceed 100")
    private Double score;
    
    @Size(max = 1000, message = "Feedback must not exceed 1000 characters")
    private String feedback;
    
    public ManualEvaluationRequest() {}
    
    public ManualEvaluationRequest(Double score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}

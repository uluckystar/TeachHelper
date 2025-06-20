package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BatchEvaluationResult {
    
    private String taskId;
    private int totalAnswers;
    private int successfulEvaluations;
    private int failedEvaluations;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> errors;
    private String status;
    
    public BatchEvaluationResult() {
        this.errors = new ArrayList<>();
        this.status = "PENDING";
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public int getTotalAnswers() {
        return totalAnswers;
    }
    
    public void setTotalAnswers(int totalAnswers) {
        this.totalAnswers = totalAnswers;
    }
    
    public int getSuccessfulEvaluations() {
        return successfulEvaluations;
    }
    
    public void setSuccessfulEvaluations(int successfulEvaluations) {
        this.successfulEvaluations = successfulEvaluations;
    }
    
    public int getFailedEvaluations() {
        return failedEvaluations;
    }
    
    public void setFailedEvaluations(int failedEvaluations) {
        this.failedEvaluations = failedEvaluations;
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
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public double getSuccessRate() {
        if (totalAnswers == 0) return 0.0;
        return (double) successfulEvaluations / totalAnswers * 100;
    }
    
    public double getProgress() {
        if (totalAnswers == 0) return 100.0;
        int processedAnswers = successfulEvaluations + failedEvaluations;
        return (double) processedAnswers / totalAnswers * 100.0;
    }
    
    public long getDurationInMillis() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
}

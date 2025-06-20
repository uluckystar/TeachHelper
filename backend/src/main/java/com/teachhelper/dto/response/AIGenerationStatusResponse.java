package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class AIGenerationStatusResponse {
    
    public enum GenerationStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
    
    private String taskId;
    private GenerationStatus status;
    private Long questionId;
    private Integer progress;
    private String message;
    private String error;
    private LocalDateTime updatedAt;
    private Long processingTimeMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private List<RubricSuggestionResponse> suggestions;
    
    public AIGenerationStatusResponse() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public AIGenerationStatusResponse(String taskId, GenerationStatus status, Long questionId) {
        this.taskId = taskId;
        this.status = status;
        this.questionId = questionId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public GenerationStatus getStatus() { return status; }
    public void setStatus(GenerationStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }

    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }

    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }

    public List<RubricSuggestionResponse> getSuggestions() { return suggestions; }
    public void setSuggestions(List<RubricSuggestionResponse> suggestions) { this.suggestions = suggestions; }
}

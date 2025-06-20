package com.teachhelper.dto.response;

import lombok.Data;

@Data
public class AIGenerationTaskResponse {
    private String taskId;
    private Long questionId;
    private String status;
    private String message;
    
    public AIGenerationTaskResponse() {}
    
    public AIGenerationTaskResponse(String taskId, Long questionId) {
        this.taskId = taskId;
        this.questionId = questionId;
        this.status = "PENDING";
        this.message = "任务已创建";
    }
    
    // Explicit setters to fix compilation issues
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // Explicit getters
    public String getTaskId() {
        return taskId;
    }
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
}

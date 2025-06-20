package com.teachhelper.dto.response;

/**
 * 评估进度响应DTO
 */
public class EvaluationProgress {
    private String taskId;
    private String status;
    private int totalAnswers;
    private int evaluatedAnswers;
    private String startTime;
    private String endTime;
    private String message;
    private double progress;

    // 默认构造函数
    public EvaluationProgress() {}

    // 构造函数
    public EvaluationProgress(String taskId, String status, int totalAnswers, int evaluatedAnswers, 
                             String startTime, String endTime, String message, double progress) {
        this.taskId = taskId;
        this.status = status;
        this.totalAnswers = totalAnswers;
        this.evaluatedAnswers = evaluatedAnswers;
        this.startTime = startTime;
        this.endTime = endTime;
        this.message = message;
        this.progress = progress;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalAnswers() {
        return totalAnswers;
    }

    public void setTotalAnswers(int totalAnswers) {
        this.totalAnswers = totalAnswers;
    }

    public int getEvaluatedAnswers() {
        return evaluatedAnswers;
    }

    public void setEvaluatedAnswers(int evaluatedAnswers) {
        this.evaluatedAnswers = evaluatedAnswers;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}

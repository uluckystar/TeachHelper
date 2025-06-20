package com.teachhelper.exception.evaluation;

/**
 * 批量评估异常类
 */
public class BatchEvaluationException extends RuntimeException {
    
    private final String taskId;
    
    public BatchEvaluationException(String message, String taskId) {
        super(message);
        this.taskId = taskId;
    }
    
    public BatchEvaluationException(String message, String taskId, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
    }
    
    public String getTaskId() {
        return taskId;
    }
}

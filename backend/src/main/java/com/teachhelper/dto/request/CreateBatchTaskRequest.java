package com.teachhelper.dto.request;

/**
 * 创建批量评估任务请求
 */
public class CreateBatchTaskRequest {
    
    private Long examId;
    private Long questionId;
    private String taskType; // FULL_EVALUATION, SELECTIVE_EVALUATION等
    private String description;
    
    public CreateBatchTaskRequest() {}
    
    public Long getExamId() {
        return examId;
    }
    
    public void setExamId(Long examId) {
        this.examId = examId;
    }
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}

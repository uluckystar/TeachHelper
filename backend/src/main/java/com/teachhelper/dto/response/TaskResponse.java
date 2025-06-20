package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务响应DTO
 */
@Schema(description = "任务信息响应")
public class TaskResponse {

    @Schema(description = "任务ID", example = "task-2024-001")
    private String taskId;

    @Schema(description = "任务类型", example = "BATCH_EVALUATION")
    private String type;

    @Schema(description = "任务名称", example = "批量评估任务")
    private String name;

    @Schema(description = "任务描述", example = "对考试的所有答案进行批量评估")
    private String description;

    @Schema(description = "任务状态", example = "RUNNING")
    private String status;

    @Schema(description = "进度百分比", example = "65")
    private Integer progress;

    @Schema(description = "已处理数量", example = "26")
    private Integer processedCount;

    @Schema(description = "总数量", example = "40")
    private Integer totalCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "优先级", example = "NORMAL")
    private String priority;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "任务配置")
    private Map<String, Object> config;

    @Schema(description = "任务结果摘要")
    private Map<String, Object> resultSummary;

    @Schema(description = "执行日志")
    private List<TaskLogEntry> logs;

    @Schema(description = "创建者用户ID")
    private Long createdBy;

    @Schema(description = "创建者用户名")
    private String createdByName;

    // 内部类：任务日志条目
    @Schema(description = "任务日志条目")
    public static class TaskLogEntry {
        @Schema(description = "时间戳")
        private LocalDateTime timestamp;

        @Schema(description = "日志级别", example = "INFO")
        private String level;

        @Schema(description = "日志消息")
        private String message;

        // Constructors
        public TaskLogEntry() {}

        public TaskLogEntry(LocalDateTime timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // Constructors
    public TaskResponse() {}

    public TaskResponse(String taskId, String type, String name, String status) {
        this.taskId = taskId;
        this.type = type;
        this.name = name;
        this.status = status;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(Integer processedCount) {
        this.processedCount = processedCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(Map<String, Object> resultSummary) {
        this.resultSummary = resultSummary;
    }

    public List<TaskLogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<TaskLogEntry> logs) {
        this.logs = logs;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}

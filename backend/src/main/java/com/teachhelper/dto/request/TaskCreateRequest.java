package com.teachhelper.dto.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 任务创建请求DTO
 */
@Schema(description = "任务创建请求")
public class TaskCreateRequest {

    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型", example = "BATCH_EVALUATION", 
            allowableValues = {"BATCH_EVALUATION", "AI_GENERATION", "KNOWLEDGE_PROCESSING", "PAPER_GENERATION"})
    private String type;

    @Schema(description = "任务名称", example = "批量评估任务")
    private String name;

    @Schema(description = "任务描述", example = "对考试的所有答案进行批量评估")
    private String description;

    @NotNull(message = "任务配置不能为空")
    @Schema(description = "任务配置参数")
    private Map<String, Object> config;

    @Schema(description = "优先级", example = "NORMAL", 
            allowableValues = {"LOW", "NORMAL", "HIGH"})
    private String priority = "NORMAL";

    @Schema(description = "是否立即开始", example = "true")
    @JsonProperty("autoStart")
    private Boolean autoStart = true;

    // Constructors
    public TaskCreateRequest() {}

    public TaskCreateRequest(String type, String name, String description, Map<String, Object> config) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.config = config;
    }

    // Getters and Setters
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

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart) {
        this.autoStart = autoStart;
    }
}

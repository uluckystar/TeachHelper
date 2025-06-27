 package com.teachhelper.entity;

import com.teachhelper.enums.PromptName;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提示词实体
 * 存储系统级提示词模板，支持变量替换
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "prompts")
public class Prompt extends BaseEntity {
    
    /**
     * 提示词名称（枚举类型）
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 50, nullable = false, unique = true)
    private PromptName name;
    
    /**
     * 提示词描述
     */
    @NotBlank
    @Column(name = "description", length = 500, nullable = false)
    private String description;
    
    /**
     * 系统消息（角色定义）
     */
    @Column(name = "system_message", columnDefinition = "TEXT")
    private String systemMessage;
    
    /**
     * 用户提示词模板（支持变量占位符）
     */
    @NotBlank
    @Column(name = "user_prompt_template", columnDefinition = "TEXT", nullable = false)
    private String userPromptTemplate;
    
    /**
     * 提示词版本
     */
    @Column(name = "version", length = 20)
    private String version = "1.0";
    
    /**
     * 是否激活
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    /**
     * 创建者备注
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * 使用次数统计
     */
    @Column(name = "usage_count")
    private Long usageCount = 0L;
    
    /**
     * 模板中的变量列表（JSON格式存储）
     */
    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;
    
    // 构造器
    public Prompt() {}
    
    public Prompt(PromptName name, String description, String userPromptTemplate) {
        this.name = name;
        this.description = description;
        this.userPromptTemplate = userPromptTemplate;
    }
    
    public Prompt(PromptName name, String description, String systemMessage, String userPromptTemplate) {
        this.name = name;
        this.description = description;
        this.systemMessage = systemMessage;
        this.userPromptTemplate = userPromptTemplate;
    }
    
    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0L : this.usageCount) + 1;
    }
    
    /**
     * 获取完整的提示词内容（系统消息 + 用户模板）
     */
    public String getFullPromptTemplate() {
        StringBuilder fullPrompt = new StringBuilder();
        
        if (systemMessage != null && !systemMessage.trim().isEmpty()) {
            fullPrompt.append("系统角色：").append(systemMessage).append("\n\n");
        }
        
        fullPrompt.append(userPromptTemplate);
        
        return fullPrompt.toString();
    }
    
    /**
     * 检查是否包含指定变量
     */
    public boolean containsVariable(String variableName) {
        String placeholder = "{" + variableName + "}";
        return userPromptTemplate != null && userPromptTemplate.contains(placeholder);
    }
}
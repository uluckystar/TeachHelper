package com.teachhelper.dto.request;

import jakarta.validation.constraints.*;

/**
 * 试卷生成模板请求DTO
 */
public class PaperGenerationTemplateRequest {
    
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 200, message = "模板名称不能超过200个字符")
    private String name;
    
    @Size(max = 1000, message = "模板描述不能超过1000个字符")
    private String description;
    
    @NotBlank(message = "科目不能为空")
    @Size(max = 100, message = "科目名称不能超过100个字符")
    private String subject;
    
    @NotBlank(message = "年级不能为空")
    @Size(max = 50, message = "年级不能超过50个字符")
    private String gradeLevel;
    
    @NotNull(message = "总分不能为空")
    @Min(value = 1, message = "总分必须大于0")
    @Max(value = 1000, message = "总分不能超过1000")
    private Integer totalScore = 100;
    
    @Min(value = 1, message = "时间限制必须大于0")
    @Max(value = 600, message = "时间限制不能超过600分钟")
    private Integer timeLimit;
    
    @NotBlank(message = "题型配置不能为空")
    private String questionConfig;
    
    private String difficultyConfig;
    
    private String knowledgeBaseConfig;
    
    // Constructors
    public PaperGenerationTemplateRequest() {}
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
    
    public String getQuestionConfig() { return questionConfig; }
    public void setQuestionConfig(String questionConfig) { this.questionConfig = questionConfig; }
    
    public String getDifficultyConfig() { return difficultyConfig; }
    public void setDifficultyConfig(String difficultyConfig) { this.difficultyConfig = difficultyConfig; }
    
    public String getKnowledgeBaseConfig() { return knowledgeBaseConfig; }
    public void setKnowledgeBaseConfig(String knowledgeBaseConfig) { this.knowledgeBaseConfig = knowledgeBaseConfig; }
}

package com.teachhelper.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * 试卷模板请求DTO
 */
public class ExamPaperTemplateRequest {
    
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
    
    @Min(value = 1, message = "考试时长必须大于0")
    @Max(value = 600, message = "考试时长不能超过600分钟")
    private Integer duration;
    
    private String status = "DRAFT";
    
    private String templateType = "MANUAL";
    
    private String questionTypeConfig;
    
    private String difficultyConfig;
    
    private String knowledgeBaseConfig;
    
    private String tags;
    
    private Boolean isPublic = false;
    
    private List<ExamPaperTemplateQuestionRequest> questions;
    
    // Constructors
    public ExamPaperTemplateRequest() {}
    
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
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    
    public String getQuestionTypeConfig() { return questionTypeConfig; }
    public void setQuestionTypeConfig(String questionTypeConfig) { this.questionTypeConfig = questionTypeConfig; }
    
    public String getDifficultyConfig() { return difficultyConfig; }
    public void setDifficultyConfig(String difficultyConfig) { this.difficultyConfig = difficultyConfig; }
    
    public String getKnowledgeBaseConfig() { return knowledgeBaseConfig; }
    public void setKnowledgeBaseConfig(String knowledgeBaseConfig) { this.knowledgeBaseConfig = knowledgeBaseConfig; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public List<ExamPaperTemplateQuestionRequest> getQuestions() { return questions; }
    public void setQuestions(List<ExamPaperTemplateQuestionRequest> questions) { this.questions = questions; }
} 
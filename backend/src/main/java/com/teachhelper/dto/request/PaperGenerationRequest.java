package com.teachhelper.dto.request;

import com.teachhelper.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * 试卷生成请求DTO
 */
public class PaperGenerationRequest {
    
    @NotBlank(message = "试卷标题不能为空")
    @Size(max = 200, message = "试卷标题不能超过200个字符")
    private String title;
    
    @Size(max = 1000, message = "试卷描述不能超过1000个字符")
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
    
    @NotEmpty(message = "题型配置不能为空")
    @Valid
    private List<QuestionTypeConfigRequest> questionTypes;
    
    @Valid
    private DifficultyConfigRequest difficulty;
    
    private List<String> knowledgeBaseIds;
    
    @Size(max = 500, message = "自定义要求不能超过500个字符")
    private String customRequirements;
    
    // Constructors
    public PaperGenerationRequest() {}
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
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
    
    public List<QuestionTypeConfigRequest> getQuestionTypes() { return questionTypes; }
    public void setQuestionTypes(List<QuestionTypeConfigRequest> questionTypes) { this.questionTypes = questionTypes; }
    
    public DifficultyConfigRequest getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyConfigRequest difficulty) { this.difficulty = difficulty; }
    
    public List<String> getKnowledgeBaseIds() { return knowledgeBaseIds; }
    public void setKnowledgeBaseIds(List<String> knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
    
    public String getCustomRequirements() { return customRequirements; }
    public void setCustomRequirements(String customRequirements) { this.customRequirements = customRequirements; }
    
    /**
     * 题型配置请求
     */
    public static class QuestionTypeConfigRequest {
        
        @NotNull(message = "题型不能为空")
        private QuestionType type;
        
        @NotNull(message = "题目数量不能为空")
        @Min(value = 1, message = "题目数量必须大于0")
        @Max(value = 50, message = "单种题型数量不能超过50")
        private Integer count;
        
        @NotNull(message = "每题分值不能为空")
        @Min(value = 1, message = "每题分值必须大于0")
        @Max(value = 100, message = "每题分值不能超过100")
        private Integer scorePerQuestion;
        
        @Size(max = 200, message = "题型要求不能超过200个字符")
        private String requirements;
        
        // Constructors
        public QuestionTypeConfigRequest() {}
        
        public QuestionTypeConfigRequest(QuestionType type, Integer count, Integer scorePerQuestion) {
            this.type = type;
            this.count = count;
            this.scorePerQuestion = scorePerQuestion;
        }
        
        // Getters and Setters
        public QuestionType getType() { return type; }
        public void setType(QuestionType type) { this.type = type; }
        
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        
        public Integer getScorePerQuestion() { return scorePerQuestion; }
        public void setScorePerQuestion(Integer scorePerQuestion) { this.scorePerQuestion = scorePerQuestion; }
        
        public String getRequirements() { return requirements; }
        public void setRequirements(String requirements) { this.requirements = requirements; }
    }
    
    /**
     * 难度配置请求
     */
    public static class DifficultyConfigRequest {
        
        @NotNull(message = "简单题比例不能为空")
        @DecimalMin(value = "0.0", message = "简单题比例不能小于0")
        @DecimalMax(value = "1.0", message = "简单题比例不能大于1")
        private Double easyRatio = 0.3;
        
        @NotNull(message = "中等题比例不能为空")
        @DecimalMin(value = "0.0", message = "中等题比例不能小于0")
        @DecimalMax(value = "1.0", message = "中等题比例不能大于1")
        private Double mediumRatio = 0.5;
        
        @NotNull(message = "困难题比例不能为空")
        @DecimalMin(value = "0.0", message = "困难题比例不能小于0")
        @DecimalMax(value = "1.0", message = "困难题比例不能大于1")
        private Double hardRatio = 0.2;
        
        // Constructors
        public DifficultyConfigRequest() {}
        
        public DifficultyConfigRequest(Double easyRatio, Double mediumRatio, Double hardRatio) {
            this.easyRatio = easyRatio;
            this.mediumRatio = mediumRatio;
            this.hardRatio = hardRatio;
        }
        
        // Getters and Setters
        public Double getEasyRatio() { return easyRatio; }
        public void setEasyRatio(Double easyRatio) { this.easyRatio = easyRatio; }
        
        public Double getMediumRatio() { return mediumRatio; }
        public void setMediumRatio(Double mediumRatio) { this.mediumRatio = mediumRatio; }
        
        public Double getHardRatio() { return hardRatio; }
        public void setHardRatio(Double hardRatio) { this.hardRatio = hardRatio; }
        
        /**
         * 验证比例总和是否为1
         */
        public boolean isValidRatios() {
            double total = (easyRatio != null ? easyRatio : 0) + 
                          (mediumRatio != null ? mediumRatio : 0) + 
                          (hardRatio != null ? hardRatio : 0);
            return Math.abs(total - 1.0) < 0.001; // 允许小的浮点误差
        }
    }
}

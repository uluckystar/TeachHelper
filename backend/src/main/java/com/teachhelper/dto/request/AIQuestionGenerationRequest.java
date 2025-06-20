package com.teachhelper.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * AI题目生成请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIQuestionGenerationRequest {
    
    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
    
    /**
     * 题目类型列表
     */
    @NotEmpty(message = "至少选择一种题目类型")
    private List<String> questionTypes;
    
    /**
     * 难度分布
     */
    @NotNull(message = "难度分布不能为空")
    private Map<String, Integer> difficultyDistribution;
    
    /**
     * 知识点ID列表（可选，空则使用全部知识点）
     */
    private List<Long> knowledgePointIds;
    
    /**
     * 生成策略
     */
    @NotNull(message = "生成策略不能为空")
    private String generationStrategy;
    
    /**
     * AI配置ID
     */
    @NotNull(message = "AI配置ID不能为空")
    private Long aiConfigId;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 附加要求
     */
    private String additionalRequirements;
    
    /**
     * 高级配置选项
     */
    private Map<String, Object> advancedOptions;
    
    // Manual getters and setters (fallback for Lombok issues)
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    
    public List<String> getQuestionTypes() { return questionTypes; }
    public void setQuestionTypes(List<String> questionTypes) { this.questionTypes = questionTypes; }
    
    public Map<String, Integer> getDifficultyDistribution() { return difficultyDistribution; }
    public void setDifficultyDistribution(Map<String, Integer> difficultyDistribution) { this.difficultyDistribution = difficultyDistribution; }
    
    public List<Long> getKnowledgePointIds() { return knowledgePointIds; }
    public void setKnowledgePointIds(List<Long> knowledgePointIds) { this.knowledgePointIds = knowledgePointIds; }
    
    public String getGenerationStrategy() { return generationStrategy; }
    public void setGenerationStrategy(String generationStrategy) { this.generationStrategy = generationStrategy; }
    
    public Long getAiConfigId() { return aiConfigId; }
    public void setAiConfigId(Long aiConfigId) { this.aiConfigId = aiConfigId; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getAdditionalRequirements() { return additionalRequirements; }
    public void setAdditionalRequirements(String additionalRequirements) { this.additionalRequirements = additionalRequirements; }
    
    public Map<String, Object> getAdvancedOptions() { return advancedOptions; }
    public void setAdvancedOptions(Map<String, Object> advancedOptions) { this.advancedOptions = advancedOptions; }
    
    /**
     * 难度分布内嵌类
     */
    public static class DifficultyDistribution {
        @Min(value = 0, message = "简单题目数量不能为负数")
        private Integer EASY = 0;
        
        @Min(value = 0, message = "中等题目数量不能为负数")
        private Integer MEDIUM = 0;
        
        @Min(value = 0, message = "困难题目数量不能为负数")
        private Integer HARD = 0;
        
        public DifficultyDistribution() {}
        
        public DifficultyDistribution(Integer easy, Integer medium, Integer hard) {
            this.EASY = easy;
            this.MEDIUM = medium;
            this.HARD = hard;
        }
        
        public Integer getEASY() { return EASY; }
        public void setEASY(Integer EASY) { this.EASY = EASY; }
        
        public Integer getMEDIUM() { return MEDIUM; }
        public void setMEDIUM(Integer MEDIUM) { this.MEDIUM = MEDIUM; }
        
        public Integer getHARD() { return HARD; }
        public void setHARD(Integer HARD) { this.HARD = HARD; }
        
        /**
         * 获取总题目数
         */
        public int getTotalCount() {
            return (EASY != null ? EASY : 0) + 
                   (MEDIUM != null ? MEDIUM : 0) + 
                   (HARD != null ? HARD : 0);
        }
        
        /**
         * 获取指定难度的数量
         */
        public Integer get(String difficulty) {
            switch (difficulty.toUpperCase()) {
                case "EASY": return EASY;
                case "MEDIUM": return MEDIUM;
                case "HARD": return HARD;
                default: return 0;
            }
        }
        
        /**
         * 获取所有难度级别的值
         */
        public java.util.stream.Stream<Integer> values() {
            return java.util.stream.Stream.of(EASY != null ? EASY : 0, 
                                             MEDIUM != null ? MEDIUM : 0, 
                                             HARD != null ? HARD : 0);
        }
    }
}

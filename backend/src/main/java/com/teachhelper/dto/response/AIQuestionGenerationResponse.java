package com.teachhelper.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI题目生成响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIQuestionGenerationResponse {
    
    /**
     * 任务ID（用于异步处理）
     */
    private String taskId;
    
    /**
     * 生成状态
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 生成的题目列表
     */
    private List<GeneratedQuestion> questions;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 使用的Token数量
     */
    private Long totalTokens;
    
    /**
     * 处理时间（毫秒）
     */
    private Long processingTimeMs;
    
    /**
     * 生成的题目数量
     */
    private Integer generatedCount;
    
    /**
     * 生成配置摘要
     */
    private String configSummary;
    
    /**
     * 生成策略
     */
    private String sourceStrategy;
    
    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 警告信息
     */
    private List<String> warnings;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 获取总生成数量（向后兼容）
     */
    public Integer getTotalGenerated() {
        return generatedCount;
    }
    
    // Manual methods for Lombok @Builder issues
    public static AIQuestionGenerationResponseBuilder builder() {
        return new AIQuestionGenerationResponseBuilder();
    }
    
    // Manual getters and setters (fallback for Lombok issues)
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    
    public List<GeneratedQuestion> getQuestions() { return questions; }
    public void setQuestions(List<GeneratedQuestion> questions) { this.questions = questions; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public Integer getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(Integer generatedCount) { this.generatedCount = generatedCount; }
    
    public String getConfigSummary() { return configSummary; }
    public void setConfigSummary(String configSummary) { this.configSummary = configSummary; }
    
    public String getSourceStrategy() { return sourceStrategy; }
    public void setSourceStrategy(String sourceStrategy) { this.sourceStrategy = sourceStrategy; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Builder class
    public static class AIQuestionGenerationResponseBuilder {
        private String taskId;
        private String status;
        private Integer progress;
        private List<GeneratedQuestion> questions;
        private String error;
        private Long totalTokens;
        private Long processingTimeMs;
        private Integer generatedCount;
        private String configSummary;
        private String sourceStrategy;
        private LocalDateTime generatedAt;
        private List<String> warnings;
        private Map<String, Object> metadata;
        
        AIQuestionGenerationResponseBuilder() {}
        
        public AIQuestionGenerationResponseBuilder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder progress(Integer progress) {
            this.progress = progress;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder questions(List<GeneratedQuestion> questions) {
            this.questions = questions;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder error(String error) {
            this.error = error;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder totalTokens(Long totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder processingTimeMs(Long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder generatedCount(Integer generatedCount) {
            this.generatedCount = generatedCount;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder configSummary(String configSummary) {
            this.configSummary = configSummary;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder sourceStrategy(String sourceStrategy) {
            this.sourceStrategy = sourceStrategy;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder generatedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }
        
        public AIQuestionGenerationResponseBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public AIQuestionGenerationResponse build() {
            AIQuestionGenerationResponse response = new AIQuestionGenerationResponse();
            response.taskId = this.taskId;
            response.status = this.status;
            response.progress = this.progress;
            response.questions = this.questions;
            response.error = this.error;
            response.totalTokens = this.totalTokens;
            response.processingTimeMs = this.processingTimeMs;
            response.generatedCount = this.generatedCount;
            response.configSummary = this.configSummary;
            response.sourceStrategy = this.sourceStrategy;
            response.generatedAt = this.generatedAt;
            response.warnings = this.warnings;
            response.metadata = this.metadata;
            return response;
        }
    }
    
    /**
     * 生成的题目内嵌类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneratedQuestion {
        /**
         * 题目ID（可选，如果已保存到数据库）
         */
        private Long id;
        
        /**
         * 题目标题
         */
        private String title;
        
        /**
         * 题目内容
         */
        private String content;
        
        /**
         * 题目类型
         */
        private String questionType;
        
        /**
         * 最大分数
         */
        private Integer maxScore;
        
        /**
         * 参考答案
         */
        private String referenceAnswer;
        
        /**
         * 难度级别
         */
        private String difficulty;
        
        /**
         * 来源类型
         */
        private String sourceType;
        
        /**
         * 来源知识点ID
         */
        private Long knowledgePointId;
        
        /**
         * 相关知识点
         */
        private String knowledgePoint;
        
        /**
         * 来源文档
         */
        private String sourceDocument;
        
        /**
         * 选项列表（适用于选择题）
         */
        private List<String> options;
        
        /**
         * 正确答案列表（选择题的正确选项索引）
         */
        private List<String> correctAnswers;
        
        /**
         * 题目解析
         */
        private String explanation;
        
        /**
         * 标签列表
         */
        private List<String> tags;
        
        /**
         * 题目元数据
         */
        private Map<String, Object> metadata;
        
        /**
         * AI生成的置信度 (0.0-1.0)
         */
        private Double confidence;
        
        /**
         * 预估难度系数 (0.0-1.0)
         */
        private Double difficultyCoefficient;
    }
    
    /**
     * 生成状态枚举
     */
    public enum GenerationStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        GenerationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 快速创建成功响应
     */
    public static AIQuestionGenerationResponse success(List<GeneratedQuestion> questions) {
        return AIQuestionGenerationResponse.builder()
                .status(GenerationStatus.COMPLETED.name())
                .progress(100)
                .questions(questions)
                .generatedCount(questions != null ? questions.size() : 0)
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 快速创建失败响应
     */
    public static AIQuestionGenerationResponse failed(String error) {
        return AIQuestionGenerationResponse.builder()
                .status(GenerationStatus.FAILED.name())
                .progress(0)
                .error(error)
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 快速创建处理中响应
     */
    public static AIQuestionGenerationResponse processing(String taskId, Integer progress) {
        return AIQuestionGenerationResponse.builder()
                .taskId(taskId)
                .status(GenerationStatus.PROCESSING.name())
                .progress(progress)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}

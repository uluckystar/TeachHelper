package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 试卷模板题目实体
 * 用于存储模板中的题目信息
 */
@Entity
@Table(name = "exam_paper_template_questions")
@Data
@EqualsAndHashCode(callSuper = false)
public class ExamPaperTemplateQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 所属模板
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ExamPaperTemplate template;
    
    /**
     * 题目序号
     */
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
    
    /**
     * 题目类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;
    
    /**
     * 题目内容
     */
    @Column(name = "question_content", columnDefinition = "TEXT")
    private String questionContent;
    
    /**
     * 题目ID（如果引用现有题目）
     */
    @Column(name = "question_id")
    private Long questionId;
    
    /**
     * 分数
     */
    @Column(name = "score")
    private Double score;
    
    /**
     * 难度等级
     */
    @Column(name = "difficulty_level")
    private String difficultyLevel;
    
    /**
     * 知识点标签（JSON格式）
     */
    @Column(name = "knowledge_tags", columnDefinition = "TEXT")
    private String knowledgeTags;
    
    /**
     * 题目配置（JSON格式，存储额外配置信息）
     */
    @Column(name = "question_config", columnDefinition = "TEXT")
    private String questionConfig;
    
    /**
     * 是否必答
     */
    @Column(name = "is_required")
    private Boolean isRequired = true;
    
    /**
     * 题目状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status = QuestionStatus.DRAFT;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 枚举定义
    public enum QuestionStatus {
        DRAFT,      // 草稿状态
        CONFIGURED, // 已配置
        READY       // 就绪状态
    }
    
    // 生命周期方法
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 业务方法
    /**
     * 检查题目是否已配置
     */
    public boolean isConfigured() {
        return (questionId != null) || 
               (questionContent != null && !questionContent.trim().isEmpty());
    }
    
    /**
     * 检查题目是否就绪
     */
    public boolean isReady() {
        return status == QuestionStatus.READY && isConfigured();
    }
    
    // 显式的getter和setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ExamPaperTemplate getTemplate() { return template; }
    public void setTemplate(ExamPaperTemplate template) { this.template = template; }
    
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    
    public String getQuestionContent() { return questionContent; }
    public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
    
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public String getKnowledgeTags() { return knowledgeTags; }
    public void setKnowledgeTags(String knowledgeTags) { this.knowledgeTags = knowledgeTags; }
    
    public String getQuestionConfig() { return questionConfig; }
    public void setQuestionConfig(String questionConfig) { this.questionConfig = questionConfig; }
    
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    
    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 
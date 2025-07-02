package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 试卷模板实体
 * 用于创建可应用到多个考试的试卷模板
 */
@Entity
@Table(name = "exam_paper_templates")
@Data
@EqualsAndHashCode(callSuper = false)
public class ExamPaperTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模板名称
     */
    @Column(nullable = false, length = 200)
    private String name;
    
    /**
     * 模板描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 科目
     */
    @Column(length = 100)
    private String subject;
    
    /**
     * 年级
     */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;
    
    /**
     * 总分
     */
    @Column(name = "total_score")
    private Integer totalScore = 100;
    
    /**
     * 考试时长（分钟）
     */
    @Column(name = "duration")
    private Integer duration;
    
    /**
     * 模板状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.DRAFT;
    
    /**
     * 模板类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private TemplateType templateType = TemplateType.MANUAL;
    
    /**
     * 题型配置（JSON格式）
     */
    @Column(name = "question_type_config", columnDefinition = "TEXT")
    private String questionTypeConfig;
    
    /**
     * 难度配置（JSON格式）
     */
    @Column(name = "difficulty_config", columnDefinition = "TEXT")
    private String difficultyConfig;
    
    /**
     * 知识库配置（JSON格式）
     */
    @Column(name = "knowledge_base_config", columnDefinition = "TEXT")
    private String knowledgeBaseConfig;
    
    /**
     * 模板标签（JSON格式）
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;
    
    /**
     * 是否公开模板
     */
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    /**
     * 使用次数
     */
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    /**
     * 创建者ID
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
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
    
    /**
     * 最后使用时间
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private User creator;
    
    /**
     * 模板题目列表
     */
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExamPaperTemplateQuestion> templateQuestions = new ArrayList<>();
    
    /**
     * 基于此模板创建的考试
     */
    @OneToMany(mappedBy = "sourcePaperTemplate", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private List<Exam> relatedExams = new ArrayList<>();
    
    // 枚举定义
    public enum TemplateStatus {
        DRAFT,      // 草稿状态
        READY,      // 就绪状态
        PUBLISHED,  // 已发布状态
        ARCHIVED    // 已归档状态
    }
    
    public enum TemplateType {
        MANUAL,         // 手动创建
        AI_GENERATED,   // AI生成
        DOCUMENT_EXTRACTED, // 文档提取
        COPIED          // 复制自其他模板
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
     * 检查模板是否可以使用
     */
    public boolean isUsable() {
        return status == TemplateStatus.READY || status == TemplateStatus.PUBLISHED;
    }
    
    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * 添加模板题目
     */
    public void addTemplateQuestion(ExamPaperTemplateQuestion question) {
        templateQuestions.add(question);
        question.setTemplate(this);
    }
    
    /**
     * 移除模板题目
     */
    public void removeTemplateQuestion(ExamPaperTemplateQuestion question) {
        templateQuestions.remove(question);
        question.setTemplate(null);
    }
    
    /**
     * 获取题目总数
     */
    public int getQuestionCount() {
        return templateQuestions != null ? templateQuestions.size() : 0;
    }
    
    /**
     * 获取已配置的题目数
     */
    public int getConfiguredQuestionCount() {
        if (templateQuestions == null) return 0;
        return (int) templateQuestions.stream()
            .filter(q -> q.getQuestionId() != null || q.getQuestionContent() != null)
            .count();
    }
    
    /**
     * 检查模板是否完整
     */
    public boolean isComplete() {
        return getConfiguredQuestionCount() > 0 && 
               totalScore != null && totalScore > 0 &&
               (templateType == TemplateType.MANUAL || 
                (questionTypeConfig != null && !questionTypeConfig.trim().isEmpty()));
    }
    
    // 显式的getter和setter方法（作为Lombok的备用）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus status) { this.status = status; }
    
    public TemplateType getTemplateType() { return templateType; }
    public void setTemplateType(TemplateType templateType) { this.templateType = templateType; }
    
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
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    
    public List<ExamPaperTemplateQuestion> getTemplateQuestions() { return templateQuestions; }
    public void setTemplateQuestions(List<ExamPaperTemplateQuestion> templateQuestions) { this.templateQuestions = templateQuestions; }
    
    public List<Exam> getRelatedExams() { return relatedExams; }
    public void setRelatedExams(List<Exam> relatedExams) { this.relatedExams = relatedExams; }
} 
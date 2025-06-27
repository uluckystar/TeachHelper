package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exam_templates")
public class ExamTemplate extends BaseEntity {
    
    @Column(name = "template_name", nullable = false)
    private String templateName;          // 模板名称
    
    @Column(name = "subject")
    private String subject;              // 科目
    
    @Column(name = "exam_title")
    private String examTitle;            // 考试标题
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;          // 描述
    
    @Column(name = "total_questions")
    private Integer totalQuestions;       // 总题目数
    
    @Column(name = "matched_questions")
    private Integer matchedQuestions;     // 已匹配题目数
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TemplateStatus status;        // 状态
    
    @Column(name = "source_files", columnDefinition = "TEXT")
    private String sourceFiles;          // 来源文件列表（JSON格式）
    
    @Column(name = "parse_metadata", columnDefinition = "TEXT")
    private String parseMetadata;        // 解析元数据（JSON格式）
    
    @JsonIgnore  // 防止循环引用
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;              // 创建者
    
    @OneToMany(mappedBy = "examTemplate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExamTemplateQuestion> templateQuestions;
    
    // 关联的考试（应用模板后创建的考试）
    @JsonIgnore  // 防止循环引用
    @OneToMany(mappedBy = "sourceTemplate", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private List<Exam> relatedExams;
    
    public ExamTemplate() {}
    
    public ExamTemplate(String templateName, String subject, String examTitle, String description,
                       Integer totalQuestions, Integer matchedQuestions, TemplateStatus status,
                       String sourceFiles, String parseMetadata, User createdBy) {
        this.templateName = templateName;
        this.subject = subject;
        this.examTitle = examTitle;
        this.description = description;
        this.totalQuestions = totalQuestions;
        this.matchedQuestions = matchedQuestions;
        this.status = status;
        this.sourceFiles = sourceFiles;
        this.parseMetadata = parseMetadata;
        this.createdBy = createdBy;
    }
    
    // Getter and Setter methods
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getExamTitle() {
        return examTitle;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public Integer getMatchedQuestions() {
        return matchedQuestions;
    }
    
    public void setMatchedQuestions(Integer matchedQuestions) {
        this.matchedQuestions = matchedQuestions;
    }
    
    public TemplateStatus getStatus() {
        return status;
    }
    
    public void setStatus(TemplateStatus status) {
        this.status = status;
    }
    
    public String getSourceFiles() {
        return sourceFiles;
    }
    
    public void setSourceFiles(String sourceFiles) {
        this.sourceFiles = sourceFiles;
    }
    
    public String getParseMetadata() {
        return parseMetadata;
    }
    
    public void setParseMetadata(String parseMetadata) {
        this.parseMetadata = parseMetadata;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public List<ExamTemplateQuestion> getTemplateQuestions() {
        return templateQuestions;
    }
    
    public void setTemplateQuestions(List<ExamTemplateQuestion> templateQuestions) {
        this.templateQuestions = templateQuestions;
    }
    
    public List<Exam> getRelatedExams() {
        return relatedExams;
    }
    
    public void setRelatedExams(List<Exam> relatedExams) {
        this.relatedExams = relatedExams;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExamTemplate that = (ExamTemplate) o;
        return Objects.equals(templateName, that.templateName) &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(examTitle, that.examTitle) &&
               Objects.equals(description, that.description) &&
               Objects.equals(totalQuestions, that.totalQuestions) &&
               Objects.equals(matchedQuestions, that.matchedQuestions) &&
               status == that.status &&
               Objects.equals(sourceFiles, that.sourceFiles) &&
               Objects.equals(parseMetadata, that.parseMetadata) &&
               Objects.equals(createdBy, that.createdBy);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), templateName, subject, examTitle, description,
                          totalQuestions, matchedQuestions, status, sourceFiles, parseMetadata, createdBy);
    }
    
    @Override
    public String toString() {
        return "ExamTemplate{" +
               "id=" + getId() +
               ", templateName='" + templateName + '\'' +
               ", subject='" + subject + '\'' +
               ", examTitle='" + examTitle + '\'' +
               ", description='" + description + '\'' +
               ", totalQuestions=" + totalQuestions +
               ", matchedQuestions=" + matchedQuestions +
               ", status=" + status +
               ", sourceFiles='" + sourceFiles + '\'' +
               ", parseMetadata='" + parseMetadata + '\'' +
               ", createdBy=" + createdBy +
               '}';
    }
    
    public enum TemplateStatus {
        DRAFT,      // 草稿状态，正在编辑
        READY,      // 就绪状态，可以应用
        APPLIED,    // 已应用状态，已生成考试
        ARCHIVED    // 已归档状态
    }
    
    // 业务方法
    public boolean isReadyForApplication() {
        // 模板就绪的条件：
        // 1. 状态为 READY 或者 DRAFT
        // 2. 总题目数不为 null 且大于 0
        // 3. 已确认的题目数不为 null 且等于总题目数
        return (status == TemplateStatus.READY || status == TemplateStatus.DRAFT) &&
               totalQuestions != null && totalQuestions > 0 &&
               matchedQuestions != null && matchedQuestions.equals(totalQuestions);
    }
    
    public double getMatchingProgress() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0.0;
        }
        return (double) (matchedQuestions != null ? matchedQuestions : 0) / totalQuestions * 100;
    }
} 
package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "exam_template_questions")
public class ExamTemplateQuestion extends BaseEntity {
    
    @JsonIgnore  // 防止循环引用
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ExamTemplate examTemplate;
    
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;       // 题目序号
    
    @Column(name = "question_content", columnDefinition = "TEXT", nullable = false)
    private String questionContent;       // 题目内容
    
    @Column(name = "section_header")
    private String sectionHeader;         // 段落标题（如"一、选择题"）
    
    @Column(name = "question_type")
    private String questionType;          // 题目类型
    
    @Column(name = "score")
    private Double score;                // 分数
    
    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;         // 正确答案
    
    @Column(name = "options", columnDefinition = "TEXT")
    private String options;              // 选项（JSON格式）
    
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;           // 解释说明
    
    @Column(name = "is_required")
    private Boolean isRequired;           // 是否必答
    
    // 匹配相关字段
    @JsonIgnore  // 防止循环引用
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_question_id")
    private Question matchedQuestion;     // 匹配到的题目
    
    @Column(name = "is_matched")
    private Boolean isMatched;           // 是否已匹配
    
    @Column(name = "is_confirmed")
    private Boolean isConfirmed;         // 是否已确认
    
    @Column(name = "matching_strategy")
    private String matchingStrategy;     // 匹配策略
    
    @Column(name = "matching_confidence")
    private Double matchingConfidence;   // 匹配置信度
    
    @Column(name = "matching_reason", columnDefinition = "TEXT")
    private String matchingReason;       // 匹配原因
    
    // 验证相关字段
    @Column(name = "has_issues")
    private Boolean hasIssues;           // 是否有问题
    
    @Column(name = "issues", columnDefinition = "TEXT")
    private String issues;               // 问题列表（JSON格式）
    
    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;          // 建议列表（JSON格式）
    
    @Column(name = "original_index")
    private Integer originalIndex;       // 原始文档中的索引
    
    @Column(name = "source_document")
    private String sourceDocument;       // 来源文档名称
    
    // 构造函数
    public ExamTemplateQuestion() {
    }
    
    public ExamTemplateQuestion(ExamTemplate examTemplate, Integer questionNumber, String questionContent) {
        this.examTemplate = examTemplate;
        this.questionNumber = questionNumber;
        this.questionContent = questionContent;
    }
    
    // Getter和Setter方法
    public ExamTemplate getExamTemplate() {
        return examTemplate;
    }
    
    public void setExamTemplate(ExamTemplate examTemplate) {
        this.examTemplate = examTemplate;
    }
    
    public Integer getQuestionNumber() {
        return questionNumber;
    }
    
    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }
    
    public String getQuestionContent() {
        return questionContent;
    }
    
    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }
    
    public String getSectionHeader() {
        return sectionHeader;
    }
    
    public void setSectionHeader(String sectionHeader) {
        this.sectionHeader = sectionHeader;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    public Question getMatchedQuestion() {
        return matchedQuestion;
    }
    
    public void setMatchedQuestion(Question matchedQuestion) {
        this.matchedQuestion = matchedQuestion;
    }
    
    public Boolean getIsMatched() {
        return isMatched;
    }
    
    public void setIsMatched(Boolean isMatched) {
        this.isMatched = isMatched;
    }
    
    public Boolean getIsConfirmed() {
        return isConfirmed;
    }
    
    public void setIsConfirmed(Boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }
    
    public String getMatchingStrategy() {
        return matchingStrategy;
    }
    
    public void setMatchingStrategy(String matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }
    
    public Double getMatchingConfidence() {
        return matchingConfidence;
    }
    
    public void setMatchingConfidence(Double matchingConfidence) {
        this.matchingConfidence = matchingConfidence;
    }
    
    public String getMatchingReason() {
        return matchingReason;
    }
    
    public void setMatchingReason(String matchingReason) {
        this.matchingReason = matchingReason;
    }
    
    public Boolean getHasIssues() {
        return hasIssues;
    }
    
    public void setHasIssues(Boolean hasIssues) {
        this.hasIssues = hasIssues;
    }
    
    public String getIssues() {
        return issues;
    }
    
    public void setIssues(String issues) {
        this.issues = issues;
    }
    
    public String getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }
    
    public Integer getOriginalIndex() {
        return originalIndex;
    }
    
    public void setOriginalIndex(Integer originalIndex) {
        this.originalIndex = originalIndex;
    }
    
    public String getSourceDocument() {
        return sourceDocument;
    }
    
    public void setSourceDocument(String sourceDocument) {
        this.sourceDocument = sourceDocument;
    }
    
    // equals和hashCode方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExamTemplateQuestion that = (ExamTemplateQuestion) o;
        return Objects.equals(examTemplate, that.examTemplate) &&
               Objects.equals(questionNumber, that.questionNumber) &&
               Objects.equals(questionContent, that.questionContent);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), examTemplate, questionNumber, questionContent);
    }
    
    @Override
    public String toString() {
        return "ExamTemplateQuestion{" +
                "id=" + getId() +
                ", questionNumber=" + questionNumber +
                ", questionContent='" + (questionContent != null ? questionContent.substring(0, Math.min(50, questionContent.length())) + "..." : null) + '\'' +
                ", sectionHeader='" + sectionHeader + '\'' +
                ", isMatched=" + isMatched +
                '}';
    }
    
    // 业务方法
    public boolean isValidForMatching() {
        return questionContent != null && !questionContent.trim().isEmpty() &&
               questionNumber != null && questionNumber > 0;
    }
    
    public boolean hasHighConfidenceMatch() {
        return isMatched != null && isMatched && 
               matchingConfidence != null && matchingConfidence >= 0.8;
    }
    
    public boolean needsManualReview() {
        return (hasIssues != null && hasIssues) || 
               (matchingConfidence != null && matchingConfidence < 0.6) ||
               (isMatched != null && !isMatched);
    }
} 
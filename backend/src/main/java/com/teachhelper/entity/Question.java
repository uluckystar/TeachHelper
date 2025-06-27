package com.teachhelper.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "questions")
public class Question extends BaseEntity {
    
    // 关联到题目库（题目的归属）
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_id", nullable = true)
    private QuestionBank questionBank;
    
    // 原有的直接关联到考试（为了保持向后兼容，将被逐步替换为QuestionReference）
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = true)
    private Exam exam;
    
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;
    
    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType = QuestionType.SHORT_ANSWER;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "max_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal maxScore;
    
    @Column(name = "reference_answer", columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = false; // AI整理题目的确认状态

    @Column(name = "source_knowledge_base_id")
    private Long sourceKnowledgeBaseId;

    @Column(name = "source_knowledge_point_id")
    private Long sourceKnowledgePointId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private DifficultyLevel difficulty;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "ai_generation_prompt", columnDefinition = "TEXT")
    private String aiGenerationPrompt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOption> options = new ArrayList<>();
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RubricCriterion> rubricCriteria = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentAnswer> studentAnswers = new ArrayList<>();
    
    // 题目在考试中的引用关系
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionReference> examReferences = new ArrayList<>();
    
    // 题目与知识点的关联关系
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionKnowledgePoint> knowledgePointAssociations = new ArrayList<>();
    
    // 题目创建者
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    // 题目状态
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // 题目标签
    @Column(name = "tags")
    private String tags;
    
    // Constructors
    public Question() {}
    
    public Question(String title, String content, QuestionType questionType, BigDecimal maxScore) {
        this.title = title;
        this.content = content;
        this.questionType = questionType;
        this.maxScore = maxScore;
    }
    
    public Question(String title, String content, QuestionType questionType, BigDecimal maxScore, String referenceAnswer) {
        this.title = title;
        this.content = content;
        this.questionType = questionType;
        this.maxScore = maxScore;
        this.referenceAnswer = referenceAnswer;
    }
    
    // Helper methods
    public void addOption(QuestionOption option) {
        options.add(option);
        option.setQuestion(this);
    }
    
    public void removeOption(QuestionOption option) {
        options.remove(option);
        option.setQuestion(null);
    }
    
    public void clearOptions() {
        for (QuestionOption option : new ArrayList<>(options)) {
            removeOption(option);
        }
    }
    
    public void addRubricCriterion(RubricCriterion criterion) {
        rubricCriteria.add(criterion);
        criterion.setQuestion(this);
    }
    
    public void removeRubricCriterion(RubricCriterion criterion) {
        rubricCriteria.remove(criterion);
        criterion.setQuestion(null);
    }
    
    public void addStudentAnswer(StudentAnswer answer) {
        studentAnswers.add(answer);
        answer.setQuestion(this);
    }
    
    public void removeStudentAnswer(StudentAnswer answer) {
        studentAnswers.remove(answer);
        answer.setQuestion(null);
    }
    
    // 题目引用管理
    public void addExamReference(QuestionReference reference) {
        examReferences.add(reference);
        reference.setQuestion(this);
    }
    
    public void removeExamReference(QuestionReference reference) {
        examReferences.remove(reference);
        reference.setQuestion(null);
    }
    
    // 知识点关联管理
    public void addKnowledgePointAssociation(QuestionKnowledgePoint association) {
        knowledgePointAssociations.add(association);
        association.setQuestion(this);
    }
    
    public void removeKnowledgePointAssociation(QuestionKnowledgePoint association) {
        knowledgePointAssociations.remove(association);
        association.setQuestion(null);
    }
    
    // 便捷方法：关联知识点
    public void associateWithKnowledgePoint(KnowledgePoint knowledgePoint, Double relevanceScore, Boolean isPrimary) {
        QuestionKnowledgePoint association = new QuestionKnowledgePoint(this, knowledgePoint, relevanceScore, isPrimary);
        addKnowledgePointAssociation(association);
    }
    
    // 便捷方法：取消关联知识点
    public void disassociateFromKnowledgePoint(KnowledgePoint knowledgePoint) {
        knowledgePointAssociations.removeIf(association -> 
            association.getKnowledgePoint().getId().equals(knowledgePoint.getId()));
    }
    
    // Getters and Setters
    public Exam getExam() {
        return exam;
    }
    
    public void setExam(Exam exam) {
        this.exam = exam;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public QuestionType getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
    
    public BigDecimal getMaxScore() {
        return maxScore;
    }
    
    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }
    
    public String getReferenceAnswer() {
        return referenceAnswer;
    }
    
    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }
    
    public List<QuestionOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<QuestionOption> options) {
        this.options = options;
    }
    
    public List<RubricCriterion> getRubricCriteria() {
        return rubricCriteria;
    }
    
    public void setRubricCriteria(List<RubricCriterion> rubricCriteria) {
        this.rubricCriteria = rubricCriteria;
    }
    
    public List<StudentAnswer> getStudentAnswers() {
        return studentAnswers;
    }
    
    public void setStudentAnswers(List<StudentAnswer> studentAnswers) {
        this.studentAnswers = studentAnswers;
    }

    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public Long getSourceKnowledgeBaseId() {
        return sourceKnowledgeBaseId;
    }

    public void setSourceKnowledgeBaseId(Long sourceKnowledgeBaseId) {
        this.sourceKnowledgeBaseId = sourceKnowledgeBaseId;
    }

    public Long getSourceKnowledgePointId() {
        return sourceKnowledgePointId;
    }

    public void setSourceKnowledgePointId(Long sourceKnowledgePointId) {
        this.sourceKnowledgePointId = sourceKnowledgePointId;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getAiGenerationPrompt() {
        return aiGenerationPrompt;
    }

    public void setAiGenerationPrompt(String aiGenerationPrompt) {
        this.aiGenerationPrompt = aiGenerationPrompt;
    }
    
    // 新字段的getter和setter
    public QuestionBank getQuestionBank() {
        return questionBank;
    }
    
    public void setQuestionBank(QuestionBank questionBank) {
        this.questionBank = questionBank;
    }
    
    public List<QuestionReference> getExamReferences() {
        return examReferences;
    }
    
    public void setExamReferences(List<QuestionReference> examReferences) {
        this.examReferences = examReferences;
    }
    
    public List<QuestionKnowledgePoint> getKnowledgePointAssociations() {
        return knowledgePointAssociations;
    }
    
    public void setKnowledgePointAssociations(List<QuestionKnowledgePoint> knowledgePointAssociations) {
        this.knowledgePointAssociations = knowledgePointAssociations;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getIsConfirmed() {
        return isConfirmed;
    }

    public void setIsConfirmed(Boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }
}

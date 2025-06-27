package com.teachhelper.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.teachhelper.dto.EvaluationDetails;
import com.teachhelper.entity.converter.EvaluationDetailsConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Convert;

@Entity
@Table(name = "student_answers")
public class StudentAnswer extends BaseEntity {
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // 直接关联User实体，代替原来的Student
    
    @NotBlank
    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
    private String answerText;
    
    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;
    
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;
    
    @Column(name = "is_evaluated", nullable = false)
    private boolean isEvaluated = false;
    
    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    private User evaluator;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type")
    private EvaluationType evaluationType;
    
    @Column(name = "file_path")
    private String filePath; // For file uploads
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;
    
    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "evaluation_details", columnDefinition = "JSON")
    @Convert(converter = EvaluationDetailsConverter.class)
    private EvaluationDetails evaluationDetails;
    
    @Column(name = "weakness_tags", length = 1024)
    private String weaknessTags;
    
    // Constructors
    public StudentAnswer() {}
    
    public StudentAnswer(Question question, User student, String answerText) {
        this.question = question;
        this.student = student;
        this.answerText = answerText;
    }
    
    // Helper methods
    public void markAsEvaluated(BigDecimal score, String feedback, User evaluator, EvaluationType evaluationType) {
        this.score = score;
        this.feedback = feedback;
        this.evaluator = evaluator;
        this.evaluationType = evaluationType;
        this.isEvaluated = true;
        this.evaluatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public User getStudent() {
        return student;
    }
    
    public void setStudent(User student) {
        this.student = student;
    }
    
    public String getAnswerText() {
        return answerText;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
    
    public BigDecimal getScore() {
        return score;
    }
    
    public void setScore(BigDecimal score) {
        this.score = score;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public boolean isEvaluated() {
        return isEvaluated;
    }
    
    public void setEvaluated(boolean evaluated) {
        isEvaluated = evaluated;
    }
    
    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
    
    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
    
    public User getEvaluator() {
        return evaluator;
    }
    
    public void setEvaluator(User evaluator) {
        this.evaluator = evaluator;
    }
    
    public EvaluationType getEvaluationType() {
        return evaluationType;
    }
    
    public void setEvaluationType(EvaluationType evaluationType) {
        this.evaluationType = evaluationType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Exam getExam() {
        return exam;
    }
    
    public void setExam(Exam exam) {
        this.exam = exam;
    }
    
    public String getAnswerContent() {
        return answerContent;
    }
    
    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public EvaluationDetails getEvaluationDetails() {
        return evaluationDetails;
    }
    
    public void setEvaluationDetails(EvaluationDetails evaluationDetails) {
        this.evaluationDetails = evaluationDetails;
    }
    
    public String getWeaknessTags() {
        return weaknessTags;
    }
    
    public void setWeaknessTags(String weaknessTags) {
        this.weaknessTags = weaknessTags;
    }
}

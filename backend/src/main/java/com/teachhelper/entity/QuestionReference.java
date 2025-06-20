package com.teachhelper.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 题目引用实体 - 题目在考试中的引用关系
 * 同一道题目可以被多个考试引用，每次引用可以有不同的分数权重和顺序
 */
@Entity
@Table(name = "question_references")
public class QuestionReference extends BaseEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "score_weight", precision = 5, scale = 2, nullable = false)
    private BigDecimal scoreWeight;
    
    @Column(name = "question_order")
    private Integer questionOrder;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // 构造器
    public QuestionReference() {}
    
    public QuestionReference(Exam exam, Question question, BigDecimal scoreWeight) {
        this.exam = exam;
        this.question = question;
        this.scoreWeight = scoreWeight;
    }
    
    // Getter和Setter方法（避免Lombok问题）
    public Exam getExam() {
        return exam;
    }
    
    public void setExam(Exam exam) {
        this.exam = exam;
    }
    
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public BigDecimal getScoreWeight() {
        return scoreWeight;
    }
    
    public void setScoreWeight(BigDecimal scoreWeight) {
        this.scoreWeight = scoreWeight;
    }
    
    public Integer getQuestionOrder() {
        return questionOrder;
    }
    
    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

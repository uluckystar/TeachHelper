package com.teachhelper.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "rubric_criteria")
public class RubricCriterion extends BaseEntity {
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @NotBlank
    @Column(name = "criterion_text", columnDefinition = "TEXT", nullable = false)
    private String criterionText;
    
    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "points", precision = 5, scale = 2, nullable = false)
    private BigDecimal points;
    
    @Column(name = "order_index")
    private Integer orderIndex;
    
    // Constructors
    public RubricCriterion() {}
    
    public RubricCriterion(String criterionText, BigDecimal points) {
        this.criterionText = criterionText;
        this.points = points;
    }
    
    // Getters and Setters
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public String getCriterionText() {
        return criterionText;
    }
    
    public void setCriterionText(String criterionText) {
        this.criterionText = criterionText;
    }
    
    public BigDecimal getPoints() {
        return points;
    }
    
    public void setPoints(BigDecimal points) {
        this.points = points;
    }
    
    public Integer getOrderIndex() {
        return orderIndex;
    }
    
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}

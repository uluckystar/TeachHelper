package com.teachhelper.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * 题目知识点关联实体 - 多对多关联表
 * 一道题目可以关联多个知识点，一个知识点可以被多道题目关联
 */
@Entity
@Table(name = "question_knowledge_points")
public class QuestionKnowledgePoint extends BaseEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_point_id", nullable = false)
    private KnowledgePoint knowledgePoint;
    
    @Column(name = "relevance_score")
    private Double relevanceScore; // 关联度评分 0-1
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false; // 是否为主要知识点
    
    @Column(name = "created_by")
    private Long createdBy;
    
    // 构造器
    public QuestionKnowledgePoint() {}
    
    public QuestionKnowledgePoint(Question question, KnowledgePoint knowledgePoint) {
        this.question = question;
        this.knowledgePoint = knowledgePoint;
    }
    
    public QuestionKnowledgePoint(Question question, KnowledgePoint knowledgePoint, Double relevanceScore, Boolean isPrimary) {
        this.question = question;
        this.knowledgePoint = knowledgePoint;
        this.relevanceScore = relevanceScore;
        this.isPrimary = isPrimary;
    }
    
    // Getter和Setter方法（避免Lombok问题）
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public KnowledgePoint getKnowledgePoint() {
        return knowledgePoint;
    }
    
    public void setKnowledgePoint(KnowledgePoint knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }
    
    public Double getRelevanceScore() {
        return relevanceScore;
    }
    
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}

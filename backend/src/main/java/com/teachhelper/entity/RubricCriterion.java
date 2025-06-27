package com.teachhelper.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
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
    
    /**
     * 评分维度的类型（知识理解、分析深度、逻辑论证、表达规范等）
     */
    @Column(name = "dimension_type", length = 50)
    private String dimensionType;
    
    /**
     * 该维度下的得分点列表
     */
    @OneToMany(mappedBy = "rubricCriterion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScoringItem> scoringItems = new ArrayList<>();
    
    /**
     * 该维度下的评分等级列表
     */
    @OneToMany(mappedBy = "rubricCriterion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScoringLevel> scoringLevels = new ArrayList<>();
    
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
    
    public String getDimensionType() {
        return dimensionType;
    }
    
    public void setDimensionType(String dimensionType) {
        this.dimensionType = dimensionType;
    }
    
    public List<ScoringItem> getScoringItems() {
        return scoringItems;
    }
    
    public void setScoringItems(List<ScoringItem> scoringItems) {
        this.scoringItems = scoringItems;
    }
    
    public List<ScoringLevel> getScoringLevels() {
        return scoringLevels;
    }
    
    public void setScoringLevels(List<ScoringLevel> scoringLevels) {
        this.scoringLevels = scoringLevels;
    }
    
    // 辅助方法
    
    /**
     * 添加得分点
     */
    public void addScoringItem(ScoringItem item) {
        scoringItems.add(item);
        item.setRubricCriterion(this);
    }
    
    /**
     * 移除得分点
     */
    public void removeScoringItem(ScoringItem item) {
        scoringItems.remove(item);
        item.setRubricCriterion(null);
    }
    
    /**
     * 添加评分等级
     */
    public void addScoringLevel(ScoringLevel level) {
        scoringLevels.add(level);
        level.setRubricCriterion(this);
    }
    
    /**
     * 移除评分等级
     */
    public void removeScoringLevel(ScoringLevel level) {
        scoringLevels.remove(level);
        level.setRubricCriterion(null);
    }
    
    /**
     * 计算得分点总分值
     */
    public BigDecimal getTotalScoringItemsPoints() {
        return scoringItems.stream()
                .map(ScoringItem::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 检查得分点分值是否与维度总分一致
     */
    public boolean isScoringItemsBalanced() {
        BigDecimal totalItemsPoints = getTotalScoringItemsPoints();
        return totalItemsPoints.compareTo(points) == 0;
    }
    
    /**
     * 获取格式化的维度信息
     */
    public String getFormattedDimensionInfo() {
        return String.format("%s（%s分，%d个得分点）", 
                criterionText, points, scoringItems.size());
    }
}

 package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 得分点实体 - 评分维度下的具体得分点
 * 每个评分维度包含多个得分点，每个得分点有具体的分值和描述
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scoring_items")
public class ScoringItem extends BaseEntity {
    
    /**
     * 所属的评分维度
     */
    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_criterion_id", nullable = false)
    private RubricCriterion rubricCriterion;
    
    /**
     * 得分点名称
     */
    @NotBlank
    @Column(name = "item_name", length = 200, nullable = false)
    private String itemName;
    
    /**
     * 得分点描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 该得分点的分值
     */
    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "points", precision = 5, scale = 2, nullable = false)
    private BigDecimal points;
    
    /**
     * 在该维度中的显示顺序
     */
    @Column(name = "order_index")
    private Integer orderIndex;
    
    /**
     * 是否为必要得分点（影响该维度能否及格）
     */
    @Column(name = "is_required")
    private Boolean isRequired = false;
    
    /**
     * 评分权重（在该维度中的重要性）
     */
    @Column(name = "weight", precision = 3, scale = 2)
    private BigDecimal weight;
    
    /**
     * 得分点类型（知识点、技能点、表达等）
     */
    @Column(name = "item_type", length = 50)
    private String itemType;
    
    // 构造器
    public ScoringItem() {}
    
    public ScoringItem(String itemName, String description, BigDecimal points) {
        this.itemName = itemName;
        this.description = description;
        this.points = points;
    }
    
    public ScoringItem(RubricCriterion rubricCriterion, String itemName, String description, BigDecimal points) {
        this.rubricCriterion = rubricCriterion;
        this.itemName = itemName;
        this.description = description;
        this.points = points;
    }
    
    /**
     * 获取格式化的得分点信息
     */
    public String getFormattedInfo() {
        return String.format("%s（%s分）", itemName, points);
    }
    
    /**
     * 判断是否为高分值得分点
     */
    public boolean isHighValueItem() {
        return rubricCriterion != null && 
               points.compareTo(rubricCriterion.getPoints().multiply(new BigDecimal("0.3"))) >= 0;
    }
}
package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 评分等级实体 - 每个评分维度下的等级划分
 * 定义优秀、良好、及格、不及格等评分等级的具体分数范围和描述
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scoring_levels")
public class ScoringLevel extends BaseEntity {
    
    /**
     * 所属的评分维度
     */
    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_criterion_id", nullable = false)
    private RubricCriterion rubricCriterion;
    
    /**
     * 等级名称（优秀、良好、及格、不及格）
     */
    @NotBlank
    @Column(name = "level_name", length = 50, nullable = false)
    private String levelName;
    
    /**
     * 等级描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 该等级的最低分数（包含）
     */
    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "min_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal minScore;
    
    /**
     * 该等级的最高分数（包含）
     */
    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "max_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal maxScore;
    
    /**
     * 等级类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "level_type", length = 20)
    private LevelType levelType;
    
    /**
     * 该等级的最低分数比例（0-1）
     */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(name = "min_score_rate", precision = 3, scale = 2)
    private BigDecimal minScoreRate;
    
    /**
     * 该等级的最高分数比例（0-1）
     */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(name = "max_score_rate", precision = 3, scale = 2)
    private BigDecimal maxScoreRate;
    
    /**
     * 等级顺序（1=最高等级，数字越大等级越低）
     */
    @Column(name = "level_order")
    private Integer levelOrder;
    
    /**
     * 等级类型枚举
     */
    public enum LevelType {
        EXCELLENT("优秀"),
        GOOD("良好"),
        PASS("及格"),
        FAIL("不及格");
        
        private final String description;
        
        LevelType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造器
    public ScoringLevel() {}
    
    public ScoringLevel(String levelName, BigDecimal minScore, BigDecimal maxScore, LevelType levelType) {
        this.levelName = levelName;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.levelType = levelType;
    }
    
    public ScoringLevel(RubricCriterion rubricCriterion, String levelName, 
                       BigDecimal minScore, BigDecimal maxScore, LevelType levelType) {
        this.rubricCriterion = rubricCriterion;
        this.levelName = levelName;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.levelType = levelType;
    }
    
    /**
     * 检查分数是否在该等级范围内
     */
    public boolean isScoreInRange(BigDecimal score) {
        if (score == null) {
            return false;
        }
        return score.compareTo(minScore) >= 0 && score.compareTo(maxScore) <= 0;
    }
    
    /**
     * 检查分数比例是否在该等级范围内
     */
    public boolean isScoreRateInRange(BigDecimal scoreRate) {
        if (scoreRate == null || minScoreRate == null || maxScoreRate == null) {
            return false;
        }
        return scoreRate.compareTo(minScoreRate) >= 0 && scoreRate.compareTo(maxScoreRate) <= 0;
    }
    
    /**
     * 获取格式化的等级信息
     */
    public String getFormattedInfo() {
        return String.format("%s（%s-%s分）", levelName, minScore, maxScore);
    }
    
    /**
     * 获取分数范围文本
     */
    public String getScoreRangeText() {
        return minScore + "-" + maxScore + "分";
    }
    
    /**
     * 获取比例范围文本
     */
    public String getScoreRateRangeText() {
        if (minScoreRate == null || maxScoreRate == null) {
            return "";
        }
        return String.format("%.1f%%-%.1f%%", 
                minScoreRate.multiply(new BigDecimal("100")), 
                maxScoreRate.multiply(new BigDecimal("100")));
    }
    
    /**
     * 检查等级配置的有效性
     */
    public boolean isValidConfiguration() {
        if (levelName == null || levelName.trim().isEmpty()) {
            return false;
        }
        if (minScore == null || maxScore == null) {
            return false;
        }
        if (minScore.compareTo(maxScore) > 0) {
            return false;
        }
        if (minScoreRate != null && maxScoreRate != null) {
            if (minScoreRate.compareTo(maxScoreRate) > 0) {
                return false;
            }
            if (minScoreRate.compareTo(BigDecimal.ZERO) < 0 || 
                maxScoreRate.compareTo(BigDecimal.ONE) > 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 计算基于总分的实际分数范围
     */
    public void calculateScoresByTotalPoints(BigDecimal totalPoints) {
        if (minScoreRate != null && totalPoints != null) {
            this.minScore = totalPoints.multiply(minScoreRate);
        }
        if (maxScoreRate != null && totalPoints != null) {
            this.maxScore = totalPoints.multiply(maxScoreRate);
        }
    }
    
    /**
     * 是否为及格等级
     */
    public boolean isPassLevel() {
        return levelType == LevelType.PASS || levelType == LevelType.GOOD || levelType == LevelType.EXCELLENT;
    }
    
    /**
     * 是否为不及格等级
     */
    public boolean isFailLevel() {
        return levelType == LevelType.FAIL;
    }
}
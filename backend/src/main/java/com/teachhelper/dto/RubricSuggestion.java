package com.teachhelper.dto;

import java.math.BigDecimal;

/**
 * 评分标准建议DTO
 */
public class RubricSuggestion {
    
    private String description;
    private BigDecimal points;
    private String level;
    private String criteria;
    
    public RubricSuggestion() {}
    
    public RubricSuggestion(String description, BigDecimal points) {
        this.description = description;
        this.points = points;
    }
    
    public RubricSuggestion(String description, BigDecimal points, String level) {
        this.description = description;
        this.points = points;
        this.level = level;
    }
    
    // Getters and Setters
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPoints() {
        return points;
    }
    
    public void setPoints(BigDecimal points) {
        this.points = points;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getCriteria() {
        return criteria;
    }
    
    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }
    
    @Override
    public String toString() {
        return "RubricSuggestion{" +
                "description='" + description + '\'' +
                ", points=" + points +
                ", level='" + level + '\'' +
                '}';
    }
}

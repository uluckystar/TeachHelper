package com.teachhelper.dto.response;

import java.math.BigDecimal;

/**
 * AI生成的评分标准建议响应DTO
 */
public class RubricSuggestionResponse {
    private String criterionText;
    private BigDecimal points;
    
    public RubricSuggestionResponse() {}
    
    public RubricSuggestionResponse(String criterionText, BigDecimal points) {
        this.criterionText = criterionText;
        this.points = points;
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
}

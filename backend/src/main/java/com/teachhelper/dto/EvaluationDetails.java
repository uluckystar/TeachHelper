package com.teachhelper.dto;

import java.math.BigDecimal;
import java.util.List;

public class EvaluationDetails {

    private List<Criterion> criteria;

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<Criterion> criteria) {
        this.criteria = criteria;
    }

    public static class Criterion {
        private String criterionText;
        private BigDecimal maxPoints;
        private BigDecimal earnedPoints;
        private String comment;

        public Criterion(String criterionText, BigDecimal maxPoints, BigDecimal earnedPoints, String comment) {
            this.criterionText = criterionText;
            this.maxPoints = maxPoints;
            this.earnedPoints = earnedPoints;
            this.comment = comment;
        }

        public Criterion() {}

        // getters and setters
        public String getCriterionText() { return criterionText; }
        public void setCriterionText(String criterionText) { this.criterionText = criterionText; }
        public BigDecimal getMaxPoints() { return maxPoints; }
        public void setMaxPoints(BigDecimal maxPoints) { this.maxPoints = maxPoints; }
        public BigDecimal getEarnedPoints() { return earnedPoints; }
        public void setEarnedPoints(BigDecimal earnedPoints) { this.earnedPoints = earnedPoints; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
} 
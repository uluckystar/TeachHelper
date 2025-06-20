package com.teachhelper.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 系统级评分标准创建请求 DTO
 */
public class RubricCreateRequest {
    
    @NotBlank(message = "评分标准名称不能为空")
    @Size(max = 100, message = "评分标准名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    @Size(max = 50, message = "学科长度不能超过50个字符")
    private String subject;
    
    @NotNull(message = "评价标准不能为空")
    @Size(min = 1, message = "至少需要一个评价标准")
    @Valid
    private List<SystemRubricCriterionRequest> criteria;
    
    // 内部类：评价标准请求
    public static class SystemRubricCriterionRequest {
        
        @NotBlank(message = "标准名称不能为空")
        @Size(max = 100, message = "标准名称长度不能超过100个字符")
        private String name;
        
        @Size(max = 500, message = "标准描述长度不能超过500个字符")
        private String description;
        
        @NotNull(message = "权重不能为空")
        private Integer weight;
        
        @NotNull(message = "评分级别不能为空")
        @Size(min = 1, message = "至少需要一个评分级别")
        @Valid
        private List<ScoreLevelRequest> scoreLevels;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Integer getWeight() {
            return weight;
        }
        
        public void setWeight(Integer weight) {
            this.weight = weight;
        }
        
        public List<ScoreLevelRequest> getScoreLevels() {
            return scoreLevels;
        }
        
        public void setScoreLevels(List<ScoreLevelRequest> scoreLevels) {
            this.scoreLevels = scoreLevels;
        }
    }
    
    // 内部类：评分级别请求
    public static class ScoreLevelRequest {
        
        @NotBlank(message = "级别名称不能为空")
        @Size(max = 50, message = "级别名称长度不能超过50个字符")
        private String name;
        
        @NotNull(message = "分数不能为空")
        private BigDecimal score;
        
        @Size(max = 500, message = "级别描述长度不能超过500个字符")
        private String description;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public BigDecimal getScore() {
            return score;
        }
        
        public void setScore(BigDecimal score) {
            this.score = score;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public List<SystemRubricCriterionRequest> getCriteria() {
        return criteria;
    }
    
    public void setCriteria(List<SystemRubricCriterionRequest> criteria) {
        this.criteria = criteria;
    }
}

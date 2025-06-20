package com.teachhelper.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_rubric_criteria")
public class SystemRubricCriterion extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer weight;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private SystemRubric systemRubric;
    
    @OneToMany(mappedBy = "criterion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScoreLevel> scoreLevels;
    
    // Constructors
    public SystemRubricCriterion() {}
    
    public SystemRubricCriterion(String name, String description, Integer weight) {
        this.name = name;
        this.description = description;
        this.weight = weight;
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
    
    public Integer getWeight() {
        return weight;
    }
    
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    
    public SystemRubric getSystemRubric() {
        return systemRubric;
    }
    
    public void setSystemRubric(SystemRubric systemRubric) {
        this.systemRubric = systemRubric;
    }
    
    public List<ScoreLevel> getScoreLevels() {
        return scoreLevels;
    }
    
    public void setScoreLevels(List<ScoreLevel> scoreLevels) {
        this.scoreLevels = scoreLevels;
    }
}

package com.teachhelper.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 年级实体
 * 对应数据库表: grade_levels
 */
@Entity
@Table(name = "grade_levels")
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeLevel extends BaseEntity {
    
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "category", length = 50)
    private String category; // 如：小学、初中、高中、本科、研究生
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    // Constructors
    public GradeLevel() {}
    
    public GradeLevel(String name, String description, String category, Long createdBy) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.createdBy = createdBy;
    }
    
    // Manual getters and setters for fallback
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}

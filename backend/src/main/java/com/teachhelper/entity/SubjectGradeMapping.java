package com.teachhelper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 学科年级关联实体
 * 用于存储学科与年级的关联关系和推荐优先级
 */
@Entity
@Table(name = "subject_grade_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectGradeMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;
    
    @Column(name = "grade_level_id", nullable = false)
    private Long gradeLevelId;
    
    @Column(name = "priority")
    private Integer priority = 0;
    
    @Column(name = "is_recommended")
    private Boolean isRecommended = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_level_id", insertable = false, updatable = false)
    private GradeLevel gradeLevel;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

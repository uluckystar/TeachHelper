package com.teachhelper.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 试卷生成历史记录实体
 */
@Entity
@Table(name = "paper_generation_history")
@Data
@EqualsAndHashCode(callSuper = false)
public class PaperGenerationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 试卷标题
     */
    @Column(nullable = false)
    private String title;
    
    /**
     * 试卷描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 关联的考试ID
     */
    @Column(name = "exam_id")
    private Long examId;
    
    /**
     * 使用的模板ID（可选）
     */
    @Column(name = "template_id")
    private Long templateId;
    
    /**
     * 科目
     */
    @Column(length = 100)
    private String subject;
    
    /**
     * 年级
     */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;
    
    /**
     * 总分
     */
    @Column(name = "total_score")
    private Integer totalScore;
    
    /**
     * 时间限制（分钟）
     */
    @Column(name = "time_limit")
    private Integer timeLimit;
    
    /**
     * 生成的题目数量
     */
    @Column(name = "question_count")
    private Integer questionCount;
    
    /**
     * 生成状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus status;
    
    /**
     * 生成配置（JSON）
     */
    @Column(name = "generation_config", columnDefinition = "TEXT")
    private String generationConfig;
    
    /**
     * 生成警告信息
     */
    @Column(columnDefinition = "TEXT")
    private String warnings;
    
    /**
     * 创建用户ID
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 生成状态枚举
     */
    public enum GenerationStatus {
        SUCCESS,    // 成功
        FAILED,     // 失败
        PARTIAL     // 部分成功
    }
}

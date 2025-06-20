package com.teachhelper.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷生成历史响应DTO
 */
@Data
public class PaperGenerationHistoryResponse {
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 试卷标题
     */
    private String title;
    
    /**
     * 试卷描述
     */
    private String description;
    
    /**
     * 关联的考试ID
     */
    private Long examId;
    
    /**
     * 使用的模板ID
     */
    private Long templateId;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 科目
     */
    private String subject;
    
    /**
     * 年级
     */
    private String gradeLevel;
    
    /**
     * 总分
     */
    private Integer totalScore;
    
    /**
     * 时间限制（分钟）
     */
    private Integer timeLimit;
    
    /**
     * 生成的题目数量
     */
    private Integer questionCount;
    
    /**
     * 生成状态
     */
    private String status;
    
    /**
     * 生成警告信息
     */
    private String warnings;
    
    /**
     * 创建用户ID
     */
    private Long createdBy;
    
    /**
     * 创建用户名
     */
    private String createdByName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 是否可以下载
     */
    private Boolean downloadable;
}

package com.teachhelper.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷生成结果DTO
 */
@Data
public class PaperGenerationResult {
    
    /**
     * 生成任务ID
     */
    private String taskId;
    
    /**
     * 生成状态
     */
    private String status;
    
    /**
     * 试卷配置
     */
    private PaperConfig paperConfig;
    
    /**
     * 生成的题目列表
     */
    private List<GeneratedQuestion> questions;
    
    /**
     * 总题目数
     */
    private Integer totalQuestions;
    
    /**
     * 总分
     */
    private Integer totalScore;
    
    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 生成耗时（毫秒）
     */
    private Long generationTime;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * 生成进度（0-100）
     */
    private Integer progress;
}

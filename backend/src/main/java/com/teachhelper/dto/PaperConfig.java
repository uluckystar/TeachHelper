package com.teachhelper.dto;

import lombok.Data;
import java.util.List;

/**
 * 试卷配置DTO
 */
@Data
public class PaperConfig {
    
    /**
     * 试卷标题
     */
    private String title;
    
    /**
     * 学科
     */
    private String subject;
    
    /**
     * 年级
     */
    private String grade;
    
    /**
     * 难度级别
     */
    private String difficulty;
    
    /**
     * 总分
     */
    private Integer totalScore;
    
    /**
     * 考试时长（分钟）
     */
    private Integer duration;
    
    /**
     * 题型配置列表
     */
    private List<QuestionTypeConfig> questionTypes;
    
    /**
     * 知识点要求
     */
    private List<String> knowledgePoints;
    
    /**
     * 特殊要求
     */
    private String specialRequirements;
}

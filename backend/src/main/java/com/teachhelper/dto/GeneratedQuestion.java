package com.teachhelper.dto;

import lombok.Data;
import java.util.List;

/**
 * 生成的题目DTO
 */
@Data
public class GeneratedQuestion {
    
    /**
     * 题目ID
     */
    private String questionId;
    
    /**
     * 题目类型（选择题、填空题、简答题等）
     */
    private String type;
    
    /**
     * 题目内容
     */
    private String content;
    
    /**
     * 选项（适用于选择题）
     */
    private List<String> options;
    
    /**
     * 正确答案
     */
    private String correctAnswer;
    
    /**
     * 答案解析
     */
    private String explanation;
    
    /**
     * 分值
     */
    private Integer score;
    
    /**
     * 难度级别
     */
    private String difficulty;
    
    /**
     * 知识点
     */
    private List<String> knowledgePoints;
    
    /**
     * 题目来源
     */
    private String source;
    
    /**
     * 生成置信度
     */
    private Double confidence;
}

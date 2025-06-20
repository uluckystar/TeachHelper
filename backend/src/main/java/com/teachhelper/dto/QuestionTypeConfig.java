package com.teachhelper.dto;

import lombok.Data;
import java.util.List;

/**
 * 题型配置DTO
 */
@Data
public class QuestionTypeConfig {
    
    /**
     * 题型类型
     */
    private String type;
    
    /**
     * 题目数量
     */
    private Integer count;
    
    /**
     * 分值
     */
    private Integer score;
    
    /**
     * 难度级别
     */
    private String difficulty;
    
    /**
     * 知识点要求
     */
    private List<String> requiredKnowledgePoints;
    
    /**
     * 题目要求
     */
    private String requirements;
}

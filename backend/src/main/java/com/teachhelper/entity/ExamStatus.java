package com.teachhelper.entity;

/**
 * 考试状态枚举 - 基于AI评估状态
 */
public enum ExamStatus {
    /**
     * 草稿状态
     */
    DRAFT,
    
    /**
     * 已发布，等待学生答题
     */
    PUBLISHED,
    
    /**
     * 进行中 - 学生正在答题或AI正在评估
     */
    IN_PROGRESS,
    
    /**
     * 待评估
     */
    ENDED,
    
    /**
     * 已评估 - AI评估已完成
     */
    EVALUATED
}

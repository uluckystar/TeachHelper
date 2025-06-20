package com.teachhelper.entity;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 待处理
     */
    PENDING,
    
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 已暂停
     */
    PAUSED,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED
}

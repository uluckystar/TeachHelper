package com.teachhelper.entity;

/**
 * 文档处理状态枚举
 */
public enum ProcessingStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    FAILED("处理失败");
    
    private final String displayName;
    
    ProcessingStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

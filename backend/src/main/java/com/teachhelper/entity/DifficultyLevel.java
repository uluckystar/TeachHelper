package com.teachhelper.entity;

/**
 * 难度级别枚举
 */
public enum DifficultyLevel {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难");
    
    private final String displayName;
    
    DifficultyLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

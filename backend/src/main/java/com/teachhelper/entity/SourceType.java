package com.teachhelper.entity;

/**
 * 题目来源类型枚举
 */
public enum SourceType {
    SELF_CREATED("手动创建"),
    INTERNET("网络获取"),
    AI_GENERATED("AI生成"),
    AI_ORGANIZED("AI整理"),
    TEMPLATE_CONFIRM("模板解析"),
    LEARNING_IMPORT("学习通导入"),
    QUESTION_BANK("题库导入");
    
    private final String displayName;
    
    SourceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据字符串获取枚举值
     */
    public static SourceType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (SourceType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        
        // 兼容旧数据，如果没有找到匹配的枚举值，返回默认值
        return SELF_CREATED;
    }
} 
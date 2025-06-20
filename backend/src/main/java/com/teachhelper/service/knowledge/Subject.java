package com.teachhelper.service.knowledge;

/**
 * 学科枚举
 */
public enum Subject {
    MATHEMATICS("数学"),
    CHINESE("语文"),
    ENGLISH("英语"),
    PHYSICS("物理"),
    CHEMISTRY("化学"),
    BIOLOGY("生物"),
    HISTORY("历史"),
    GEOGRAPHY("地理"),
    POLITICS("政治"),
    COMPUTER_SCIENCE("计算机科学"),
    OTHER("其他");
    
    private final String displayName;
    
    Subject(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据显示名称获取枚举值
     */
    public static Subject fromDisplayName(String displayName) {
        for (Subject subject : values()) {
            if (subject.displayName.equals(displayName)) {
                return subject;
            }
        }
        return OTHER;
    }
    
    /**
     * 根据字符串获取枚举值
     */
    public static Subject fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}

package com.teachhelper.enums;

/**
 * 学科枚举
 */
public enum Subject {
    CHINESE("语文"),
    MATH("数学"),
    ENGLISH("英语"),
    PHYSICS("物理"),
    CHEMISTRY("化学"),
    BIOLOGY("生物"),
    HISTORY("历史"),
    GEOGRAPHY("地理"),
    POLITICS("政治"),
    COMPUTER_SCIENCE("计算机科学"),
    ART("美术"),
    MUSIC("音乐"),
    PHYSICAL_EDUCATION("体育"),
    OTHER("其他");

    private final String displayName;

    Subject(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

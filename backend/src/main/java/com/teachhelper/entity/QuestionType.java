package com.teachhelper.entity;

public enum QuestionType {
    SINGLE_CHOICE("单选题"),
    MULTIPLE_CHOICE("多选题"),
    TRUE_FALSE("判断题"),
    FILL_BLANK("填空题"),
    SHORT_ANSWER("简答题"),
    ESSAY("论述题"),
    CODING("编程题"),
    CASE_ANALYSIS("案例分析题"),
    CALCULATION("计算题");
    
    private final String displayName;
    
    QuestionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

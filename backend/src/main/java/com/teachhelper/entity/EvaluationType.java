package com.teachhelper.entity;

public enum EvaluationType {
    MANUAL("手动评估"),
    AI_AUTO("AI自动评估"),
    AI_ASSISTED("AI辅助评估");
    
    private final String displayName;
    
    EvaluationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

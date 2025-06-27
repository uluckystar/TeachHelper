 package com.teachhelper.enums;

/**
 * 提示词名称枚举
 * 定义系统中可用的提示词类型
 */
public enum PromptName {
    
    /**
     * 生成评分标准提示词
     */
    GENERATE_RUBRIC("生成评分标准", "用于生成题目的详细评分标准"),
    
    /**
     * 普通评阅风格
     */
    EVALUATE_ANSWER_NORMAL("普通评阅", "平衡的评阅标准，综合考虑准确性和完整性"),
    
    /**
     * 严格评阅风格
     */
    EVALUATE_ANSWER_STRICT("严格评阅", "严格的评阅标准，高要求高标准"),
    
    /**
     * 宽松评阅风格
     */
    EVALUATE_ANSWER_LENIENT("宽松评阅", "宽松的评阅标准，鼓励性评价"),
    
    /**
     * 生成参考答案提示词
     */
    GENERATE_REFERENCE_ANSWER("生成参考答案", "用于生成题目的标准参考答案");
    
    private final String displayName;
    private final String description;
    
    PromptName(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为评估类型的提示词
     */
    public boolean isEvaluationType() {
        return this == EVALUATE_ANSWER_NORMAL || 
               this == EVALUATE_ANSWER_STRICT || 
               this == EVALUATE_ANSWER_LENIENT;
    }
    
    /**
     * 判断是否为生成类型的提示词
     */
    public boolean isGenerationType() {
        return this == GENERATE_RUBRIC || 
               this == GENERATE_REFERENCE_ANSWER;
    }
}
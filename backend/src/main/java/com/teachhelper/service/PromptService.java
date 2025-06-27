 package com.teachhelper.service;

import com.teachhelper.entity.Prompt;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.entity.SystemRubric;
import com.teachhelper.enums.PromptName;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提示词服务接口
 */
public interface PromptService {
    
    /**
     * 根据名称获取激活的提示词
     */
    Prompt getActivePromptByName(PromptName promptName);
    
    /**
     * 构建题目评估提示词
     */
    String buildPromptForQuestionEvaluation(PromptName promptName, Question question, String studentAnswer, List<RubricCriterion> rubricCriteria);
    
    /**
     * 构建评分标准生成提示词
     */
    String buildPromptForRubricGeneration(Question question);
    
    /**
     * 构建参考答案生成提示词
     */
    String buildPromptForReferenceAnswer(Question question);
    
    /**
     * 构建评估提示词
     */
    String buildEvaluationPrompt(String promptName, String questionContent, String studentAnswer, BigDecimal questionPoints);
    
    /**
     * 构建包含评分标准的评估提示词
     */
    String buildEvaluationPromptWithRubric(String promptName, String questionContent, String studentAnswer, 
                                          BigDecimal questionPoints, SystemRubric rubric);
}
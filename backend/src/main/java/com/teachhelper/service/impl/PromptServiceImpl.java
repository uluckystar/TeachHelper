 package com.teachhelper.service.impl;

import com.teachhelper.entity.*;
import com.teachhelper.enums.PromptName;
import com.teachhelper.repository.PromptRepository;
import com.teachhelper.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromptServiceImpl implements PromptService {

    @Autowired
    private PromptRepository promptRepository;

    @Override
    public Prompt getActivePromptByName(PromptName promptName) {
        return promptRepository.findActivePromptByName(promptName)
                .orElseThrow(() -> new RuntimeException("未找到活跃的提示词: " + promptName));
    }

    @Override
    public String buildPromptForQuestionEvaluation(PromptName promptName, Question question, String studentAnswer, List<RubricCriterion> rubricCriteria) {
        Prompt prompt = getActivePromptByName(promptName);
        
        // 构建变量映射
        Map<String, String> variables = new HashMap<>();
        variables.put("题目内容", question.getContent());
        variables.put("题目满分", question.getMaxScore().toString());
        variables.put("学生答案内容", studentAnswer);
        
        // 构建评分标准列表
        StringBuilder rubricList = new StringBuilder();
        if (rubricCriteria != null && !rubricCriteria.isEmpty()) {
            for (RubricCriterion criterion : rubricCriteria) {
                rubricList.append("- ").append(criterion.getCriterionText())
                         .append("（").append(criterion.getPoints()).append("分）\n");
            }
        } else {
            rubricList.append("- 基础评分标准（").append(question.getMaxScore()).append("分）\n");
        }
        variables.put("评分标准列表", rubricList.toString());
        
        // 添加按比例计算的分数变量
        addProportionalScoreVariables(variables, question.getMaxScore());
        
        // 添加标准分数变量
        addStandardScoreVariables(variables, question.getMaxScore(), rubricCriteria);
        
        return replaceVariables(prompt.getUserPromptTemplate(), variables);
    }

    @Override
    public String buildPromptForRubricGeneration(Question question) {
        Prompt prompt = getActivePromptByName(PromptName.GENERATE_RUBRIC);
        
        Map<String, String> variables = new HashMap<>();
        variables.put("题目内容", question.getContent());
        variables.put("题目类型", question.getQuestionType().toString());
        variables.put("题目满分", question.getMaxScore().toString());
        
        // 计算各维度分值（基于40%, 30%, 20%, 10%的比例）
        BigDecimal maxScore = question.getMaxScore();
        BigDecimal dimension1Score = maxScore.multiply(new BigDecimal("0.4")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal dimension2Score = maxScore.multiply(new BigDecimal("0.3")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal dimension3Score = maxScore.multiply(new BigDecimal("0.2")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal dimension4Score = maxScore.multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.HALF_UP);
        
        variables.put("维度1分值", dimension1Score.toString());
        variables.put("维度2分值", dimension2Score.toString());
        variables.put("维度3分值", dimension3Score.toString());
        variables.put("维度4分值", dimension4Score.toString());
        
        // 计算各维度的等级分数界限
        addDimensionScoreBounds(variables, "1", dimension1Score);
        addDimensionScoreBounds(variables, "2", dimension2Score);
        addDimensionScoreBounds(variables, "3", dimension3Score);
        addDimensionScoreBounds(variables, "4", dimension4Score);
        
        // 获取基础提示词
        String basePrompt = replaceVariables(prompt.getUserPromptTemplate(), variables);
        
        // 如果题目有参考答案，添加到上下文中
        if (question.getReferenceAnswer() != null && !question.getReferenceAnswer().trim().isEmpty()) {
            StringBuilder enhancedPrompt = new StringBuilder(basePrompt);
            enhancedPrompt.append("\n\n=== 参考答案（请参考制定评分标准） ===\n");
            enhancedPrompt.append(question.getReferenceAnswer());
            enhancedPrompt.append("\n\n**请根据上述参考答案的内容结构和知识点分布，制定相应的评分标准。");
            enhancedPrompt.append("确保评分标准的各个维度能够有效评估学生答案与参考答案的符合程度，");
            enhancedPrompt.append("并在各维度的描述中明确指出应重点关注的得分要点和评分细节。**\n");
            enhancedPrompt.append("\n**分值分配建议**：\n");
            enhancedPrompt.append("• 知识理解（").append(dimension1Score).append("分）：参考答案中的核心概念和基础知识掌握情况\n");
            enhancedPrompt.append("• 分析深度（").append(dimension2Score).append("分）：参考答案中的分析层次和逻辑推理能力\n");
            enhancedPrompt.append("• 逻辑论证（").append(dimension3Score).append("分）：参考答案中的论证过程和结论合理性\n");
            enhancedPrompt.append("• 表达规范（").append(dimension4Score).append("分）：参考答案中的语言表达和格式规范性\n");
            return enhancedPrompt.toString();
        }
        
        return basePrompt;
    }

    @Override
    public String buildPromptForReferenceAnswer(Question question) {
        Prompt prompt = getActivePromptByName(PromptName.GENERATE_REFERENCE_ANSWER);
        
        Map<String, String> variables = new HashMap<>();
        variables.put("题目内容", question.getContent());
        variables.put("题目类型", question.getQuestionType().toString());
        variables.put("题目满分", question.getMaxScore().toString());
        
        return replaceVariables(prompt.getUserPromptTemplate(), variables);
    }

    /**
     * 添加按比例计算的分数变量（用于严格和宽松评分风格）
     */
    private void addProportionalScoreVariables(Map<String, String> variables, BigDecimal maxScore) {
        // 严格评分风格的分数界限
        BigDecimal wordCountLimit = maxScore.multiply(new BigDecimal("0.4")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal conceptMissingLimit = maxScore.multiply(new BigDecimal("0.5")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal errorDeduction = maxScore.multiply(new BigDecimal("0.3")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal shallowLimit = maxScore.multiply(new BigDecimal("0.7")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal expressionDeduction = maxScore.multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.HALF_UP);
        
        variables.put("字数不足上限", wordCountLimit.toString());
        variables.put("概念缺失上限", conceptMissingLimit.toString());
        variables.put("错误扣分", errorDeduction.toString());
        variables.put("论述不深上限", shallowLimit.toString());
        variables.put("表述扣分", expressionDeduction.toString());
        
        // 宽松评分风格的分数界限
        BigDecimal baseScoreMin = maxScore.multiply(new BigDecimal("0.6")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal insightBonus = maxScore.multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal expressionMax = maxScore.multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal completenessMax = maxScore.multiply(new BigDecimal("0.2")).setScale(1, RoundingMode.HALF_UP);
        
        variables.put("基础分下限", baseScoreMin.toString());
        variables.put("见解加分", insightBonus.toString());
        variables.put("表述扣分上限", expressionMax.toString());
        variables.put("完整性扣分上限", completenessMax.toString());
    }

    /**
     * 添加标准分数变量（用于分项评分）
     */
    private void addStandardScoreVariables(Map<String, String> variables, BigDecimal maxScore, List<RubricCriterion> rubricCriteria) {
        if (rubricCriteria != null && !rubricCriteria.isEmpty()) {
            for (int i = 0; i < rubricCriteria.size(); i++) {
                RubricCriterion criterion = rubricCriteria.get(i);
                variables.put("标准" + (i + 1) + "满分", criterion.getPoints().toString());
            }
        } else {
            variables.put("标准1满分", maxScore.toString());
        }
        
        variables.put("最终得分", "{最终得分}"); // 占位符，在AI返回时会被替换
    }

    /**
     * 添加维度分数界限
     */
    private void addDimensionScoreBounds(Map<String, String> variables, String dimensionNum, BigDecimal dimensionScore) {
        // 优秀: 90%-100%，良好: 75%-89%，及格: 60%-74%，不及格: 0%-59%
        BigDecimal excellent = dimensionScore.multiply(new BigDecimal("0.9")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal good = dimensionScore.multiply(new BigDecimal("0.75")).setScale(1, RoundingMode.HALF_UP);
        BigDecimal pass = dimensionScore.multiply(new BigDecimal("0.6")).setScale(1, RoundingMode.HALF_UP);
        
        variables.put("优秀" + dimensionNum + "下限", excellent.toString());
        variables.put("优秀" + dimensionNum + "上限", dimensionScore.toString()); // 修正：上限应该是满分
        variables.put("良好" + dimensionNum + "下限", good.toString());
        variables.put("良好" + dimensionNum + "上限", excellent.subtract(new BigDecimal("0.1")).toString());
        variables.put("及格" + dimensionNum + "下限", pass.toString());
        variables.put("及格" + dimensionNum + "上限", good.subtract(new BigDecimal("0.1")).toString());
    }

    /**
     * 替换模板中的变量
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
    
    @Override
    public String buildEvaluationPrompt(String promptName, String questionContent, String studentAnswer, BigDecimal questionPoints) {
        try {
            // 将字符串转换为PromptName枚举
            PromptName promptNameEnum = PromptName.valueOf(promptName);
            Prompt prompt = getActivePromptByName(promptNameEnum);
            
            // 构建变量映射
            Map<String, String> variables = new HashMap<>();
            variables.put("题目内容", questionContent);
            variables.put("题目满分", questionPoints.toString());
            variables.put("学生答案内容", studentAnswer);
            
            // 添加按比例计算的分数变量
            addProportionalScoreVariables(variables, questionPoints);
            
            return replaceVariables(prompt.getUserPromptTemplate(), variables);
            
        } catch (IllegalArgumentException e) {
            // 如果枚举转换失败，记录错误并返回null
            System.err.println("无效的提示词名称: " + promptName);
            return null;
        } catch (Exception e) {
            System.err.println("构建评估提示词失败: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String buildEvaluationPromptWithRubric(String promptName, String questionContent, String studentAnswer, 
                                                 BigDecimal questionPoints, SystemRubric rubric) {
        // 首先获取基础的评估提示词
        String basePrompt = buildEvaluationPrompt(promptName, questionContent, studentAnswer, questionPoints);
        
        if (basePrompt == null) {
            return null;
        }
        
        // 构建评分标准信息
        StringBuilder rubricInfo = new StringBuilder();
        rubricInfo.append("\n\n=== 详细评分标准 ===\n");
        rubricInfo.append("评分标准名称：").append(rubric.getName()).append("\n");
        rubricInfo.append("总分：").append(questionPoints).append("分\n\n");
        
        if (rubric.getCriteria() != null && !rubric.getCriteria().isEmpty()) {
            rubricInfo.append("评分维度及分值分配：\n");
            
            for (SystemRubricCriterion criterion : rubric.getCriteria()) {
                BigDecimal dimensionPoints = BigDecimal.valueOf(criterion.getWeight());
                rubricInfo.append("• ").append(criterion.getName())
                         .append("（").append(dimensionPoints).append("分）：")
                         .append(criterion.getDescription()).append("\n");
                
                // 添加该维度的评分等级
                if (criterion.getScoreLevels() != null && !criterion.getScoreLevels().isEmpty()) {
                    rubricInfo.append("  评分等级：\n");
                    for (ScoreLevel level : criterion.getScoreLevels()) {
                        rubricInfo.append("    - ").append(level.getName())
                                 .append("：").append(level.getDescription()).append("\n");
                    }
                    rubricInfo.append("\n");
                }
            }
        }
        
        rubricInfo.append("=== 评分要求 ===\n");
        rubricInfo.append("请严格按照上述评分标准进行评分，并为每个维度给出具体分数和评价。\n");
        rubricInfo.append("格式要求：\n");
        rubricInfo.append("1. 为每个维度单独评分，格式：[维度名称]：[得分]/[满分] - [评价]\n");
        rubricInfo.append("2. 最后给出总分和综合评价\n");
        rubricInfo.append("3. 总分格式：最终得分：[总分]/[满分]\n\n");
        
        // 将评分标准信息插入到基础提示词中
        return basePrompt + rubricInfo.toString();
    }
}
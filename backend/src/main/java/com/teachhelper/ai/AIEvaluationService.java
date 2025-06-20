package com.teachhelper.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.RubricSuggestion;
import com.teachhelper.dto.response.RubricSuggestionResponse;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.UserAIConfig;
import com.teachhelper.service.UserAIConfigService;
import com.teachhelper.service.ai.AIClient;
import com.teachhelper.service.ai.AIClientFactory;
import com.teachhelper.service.ai.AIResponse;
import com.teachhelper.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * AI评估服务 - 使用LLM大模型生成评分标准
 * 通过提示词让AI大模型生成评分标准，而不是使用if-else判断
 */
@Service
public class AIEvaluationService {
    
    @Autowired
    private UserAIConfigService userAIConfigService;
    
    @Autowired
    private AIClientFactory aiClientFactory;
    
    @Autowired
    private AuthService authService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成评分标准建议（使用AI大模型生成）
     */
    public List<RubricSuggestionResponse> generateRubricSuggestions(com.teachhelper.entity.Question question, String customPrompt) {
        System.out.println("=== 开始AI评分标准生成 ===");
        System.out.println("题目ID: " + question.getId());
        System.out.println("题目标题: " + question.getTitle());
        System.out.println("题目类型: " + question.getQuestionType());
        System.out.println("题目满分: " + question.getMaxScore());
        System.out.println("自定义提示词: " + (customPrompt != null ? customPrompt : "无"));
        
        try {
            // 获取用户的默认AI配置
            Long userId = authService.getCurrentUser().getId();
            System.out.println("当前用户ID: " + userId);
            
            Optional<UserAIConfig> configOpt = userAIConfigService.getUserDefaultAIConfig(userId);
            
            if (!configOpt.isPresent()) {
                System.out.println("⚠️  没有找到默认AI配置，尝试获取用户的第一个可用配置");
                // 如果没有默认配置，直接使用基础评分标准（暂时简化处理）
                System.out.println("❌ 用户没有配置默认AI模型，使用fallback基础评分标准");
                return generateBasicRubricSuggestions(question, customPrompt);
            }
            
            UserAIConfig aiConfig = configOpt.get();
            System.out.println("✅ 找到AI配置:");
            System.out.println("  - 提供商: " + aiConfig.getProvider());
            System.out.println("  - 模型名称: " + aiConfig.getModelName());
            System.out.println("  - API端点: " + aiConfig.getApiEndpoint());
            System.out.println("  - 是否激活: " + aiConfig.getIsActive());
            System.out.println("  - 是否为默认: " + aiConfig.getIsDefault());
            
            AIClient aiClient = aiClientFactory.getClient(aiConfig.getProvider());
            System.out.println("✅ 获取AI客户端成功: " + aiClient.getClass().getSimpleName());
            
            // 构建AI提示词
            String prompt = buildRubricGenerationPrompt(question, customPrompt);
            System.out.println("✅ 构建AI提示词成功，长度: " + prompt.length());
            System.out.println("--- 提示词内容开始 ---");
            System.out.println(prompt);
            System.out.println("--- 提示词内容结束 ---");
            
            // 调用AI生成
            System.out.println("🚀 开始调用AI生成评分标准...");
            long startTime = System.currentTimeMillis();
            AIResponse aiResponse = aiClient.chat(prompt, aiConfig);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("⏱️  AI调用耗时: " + duration + "ms");
            
            if (aiResponse.isSuccess()) {
                System.out.println("✅ AI调用成功!");
                System.out.println("  - 输入Token数: " + aiResponse.getInputTokens());
                System.out.println("  - 输出Token数: " + aiResponse.getOutputTokens());
                System.out.println("  - 总Token数: " + aiResponse.getTotalTokens());
                System.out.println("--- AI响应内容开始 ---");
                System.out.println(aiResponse.getContent());
                System.out.println("--- AI响应内容结束 ---");
                
                // 解析AI响应
                System.out.println("🔍 开始解析AI响应...");
                List<RubricSuggestionResponse> suggestions = parseAIRubricResponse(aiResponse.getContent(), question.getMaxScore());
                if (!suggestions.isEmpty()) {
                    System.out.println("✅ AI解析成功，生成了 " + suggestions.size() + " 个评分标准:");
                    for (int i = 0; i < suggestions.size(); i++) {
                        RubricSuggestionResponse suggestion = suggestions.get(i);
                        System.out.println("  " + (i+1) + ". " + suggestion.getCriterionText() + " (" + suggestion.getPoints() + "分)");
                    }
                    return suggestions;
                } else {
                    System.out.println("❌ AI响应解析失败，返回空列表");
                }
            } else {
                System.out.println("❌ AI调用失败:");
                System.out.println("  - 错误信息: " + aiResponse.getErrorMessage());
                System.out.println("  - 响应内容: " + aiResponse.getContent());
            }
            
            // AI生成失败，返回基础评分标准
            System.err.println("❌ AI评分标准生成失败，使用基础评分标准");
            System.err.println("  - 错误原因: " + (aiResponse != null ? aiResponse.getErrorMessage() : "未知错误"));
            System.err.println("  - 将使用fallback基础评分标准");
            return generateBasicRubricSuggestions(question, customPrompt);
            
        } catch (Exception e) {
            // 出现异常，返回基础评分标准
            System.err.println("❌ AI评分标准生成出现异常，使用基础评分标准");
            System.err.println("  - 异常类型: " + e.getClass().getSimpleName());
            System.err.println("  - 异常信息: " + e.getMessage());
            System.err.println("  - 将使用fallback基础评分标准");
            e.printStackTrace();
            return generateBasicRubricSuggestions(question, customPrompt);
        }
    }
    
    /**
     * 构建AI评分标准生成提示词
     */
    private String buildRubricGenerationPrompt(com.teachhelper.entity.Question question, String customPrompt) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的教育评估专家，请为以下题目生成详细、针对性的评分标准。\n\n");
        
        // 题目基本信息
        prompt.append("=== 题目信息 ===\n");
        prompt.append("题目类型：").append(getQuestionTypeDescription(question.getQuestionType())).append("\n");
        prompt.append("题目内容：").append(question.getContent() != null ? question.getContent() : "").append("\n");
        
        // 选择题选项
        try {
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                prompt.append("题目选项：\n");
                for (int i = 0; i < question.getOptions().size(); i++) {
                    var option = question.getOptions().get(i);
                    char optionLetter = (char)('A' + i);
                    prompt.append("  ").append(optionLetter).append(". ").append(option.getContent());
                    if (option.getIsCorrect() != null && option.getIsCorrect()) {
                        prompt.append(" [正确答案]");
                    }
                    prompt.append("\n");
                }
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // 如果options集合无法加载，跳过选项显示
            System.out.println("⚠️  无法加载题目选项（LazyInitializationException），跳过选项显示");
        }
        
        if (question.getReferenceAnswer() != null && !question.getReferenceAnswer().trim().isEmpty()) {
            prompt.append("参考答案：").append(question.getReferenceAnswer()).append("\n");
        }
        
        prompt.append("总分：").append(question.getMaxScore()).append("分\n");
        
        if (question.getDifficulty() != null) {
            prompt.append("难度等级：").append(question.getDifficulty().name()).append("\n");
        }
        
        // 自定义要求
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            prompt.append("特殊要求：").append(customPrompt).append("\n");
        }
        
        prompt.append("\n=== 重要要求 ===\n");
        prompt.append("⚠️ 请仔细分析上述题目的具体内容，生成针对性的评分标准，而不是通用的评分维度！\n\n");
        prompt.append("生成要求：\n");
        prompt.append("1. 评分标准必须结合题目的具体内容和知识点\n");
        prompt.append("2. 标准名称要体现题目的核心要素，避免使用'内容完整性'等通用词汇\n");
        prompt.append("3. 分值分配合理，总和必须等于").append(question.getMaxScore()).append("分\n");
        prompt.append("4. 按照题型区分，跟据题目内容生成对应数量的评分标准项目\n");
        prompt.append("5. 每个标准都要针对题目的具体考查内容\n\n");
        
        // 根据题目类型给出具体指导
        prompt.append("=== 评分要点参考 ===\n");
        switch (question.getQuestionType()) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
            case TRUE_FALSE:
                prompt.append("选择题重点关注：正确答案、知识点掌握、理解深度\n");
                break;
            case SHORT_ANSWER:
            case ESSAY:
                prompt.append("主观题需要结合具体题目内容，不要使用通用评分维度！\n");
                prompt.append("示例：如果题目问'请分析XX的影响因素'，评分标准应该是'XX因素识别'、'XX分析深度'等\n");
                break;
            case FILL_BLANK:
                prompt.append("填空题重点关注：答案准确性、表达规范性、理解程度\n");
                break;
            case CODING:
                prompt.append("编程题重点关注：功能正确性、代码质量、算法效率、思路清晰度\n");
                break;
            case CALCULATION:
                prompt.append("计算题重点关注：解题步骤、计算准确性、公式应用、思路清晰度\n");
                break;
            case CASE_ANALYSIS:
                prompt.append("案例分析题重点关注：案例理解、分析深度、理论运用、解决方案\n");
                break;
            default:
                prompt.append("根据题目内容特点确定评分重点\n");
                break;
        }
        
        prompt.append("\n=== 输出格式 ===\n");
        prompt.append("请严格按照以下JSON格式返回结果：\n");
        prompt.append("{\n");
        prompt.append("  \"rubrics\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"评分标准名称\",\n");
        prompt.append("      \"points\": 分值(数字)\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("- 所有分值总和必须等于").append(question.getMaxScore()).append("分\n");
        prompt.append("- 分值可以是小数，但建议保留1位小数\n");
        prompt.append("- 评分标准必须针对题目具体内容，避免使用通用词汇如'内容完整性'、'逻辑清晰度'等\n");
        prompt.append("- 分析题目要求什么，评分标准就应该针对这些具体要求\n");
        prompt.append("- 例如：题目问'分析数据结构的优缺点'，评分标准应该是'数据结构优点分析'、'数据结构缺点分析'等\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析AI返回的评分标准
     */
    private List<RubricSuggestionResponse> parseAIRubricResponse(String aiResponse, BigDecimal maxScore) {
        System.out.println("🔍 开始解析AI响应内容");
        System.out.println("  - 响应长度: " + (aiResponse != null ? aiResponse.length() : 0));
        System.out.println("  - 目标满分: " + maxScore);
        
        List<RubricSuggestionResponse> suggestions = new ArrayList<>();
        
        try {
            // 尝试提取JSON部分
            System.out.println("🔍 提取JSON部分...");
            String jsonPart = extractJsonFromResponse(aiResponse);
            if (jsonPart == null) {
                System.err.println("❌ AI响应中未找到有效JSON");
                System.err.println("  - 完整响应: " + aiResponse);
                return suggestions;
            }
            
            System.out.println("✅ 提取到JSON: " + jsonPart);
            
            JsonNode rootNode = objectMapper.readTree(jsonPart);
            JsonNode rubricsNode = rootNode.get("rubrics");
            
            if (rubricsNode != null && rubricsNode.isArray()) {
                System.out.println("✅ 找到rubrics数组，包含 " + rubricsNode.size() + " 个元素");
                BigDecimal totalPoints = BigDecimal.ZERO;
                
                for (JsonNode rubricNode : rubricsNode) {
                    if (!rubricNode.has("name") || !rubricNode.has("points")) {
                        System.out.println("⚠️  跳过无效节点: " + rubricNode.toString());
                        continue;
                    }
                    
                    String name = rubricNode.get("name").asText();
                    double points = rubricNode.get("points").asDouble();
                    
                    System.out.println("🔍 解析评分项: " + name + " = " + points + "分");
                    
                    if (name.trim().isEmpty() || points <= 0) {
                        System.out.println("⚠️  跳过无效评分项: 名称为空或分值<=0");
                        continue;
                    }
                    
                    BigDecimal pointsBd = BigDecimal.valueOf(points);
                    totalPoints = totalPoints.add(pointsBd);
                    
                    RubricSuggestionResponse suggestion = new RubricSuggestionResponse(name, pointsBd);
                    suggestions.add(suggestion);
                }
                
                System.out.println("📊 解析统计:");
                System.out.println("  - 有效评分项数量: " + suggestions.size());
                System.out.println("  - 总分: " + totalPoints);
                System.out.println("  - 目标满分: " + maxScore);
                
                // 检查总分是否匹配，如果不匹配进行调整
                if (!suggestions.isEmpty() && totalPoints.compareTo(maxScore) != 0) {
                    System.out.println("⚠️  总分不匹配，需要调整:");
                    System.out.println("  - 当前总分: " + totalPoints);
                    System.out.println("  - 目标满分: " + maxScore);
                    System.out.println("🔧 开始调整分值...");
                    adjustRubricPoints(suggestions, maxScore);
                    System.out.println("✅ 分值调整完成");
                } else {
                    System.out.println("✅ 总分匹配，无需调整");
                }
            } else {
                System.err.println("❌ 未找到有效的rubrics数组节点");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 解析AI评分标准响应失败:");
            System.err.println("  - 异常类型: " + e.getClass().getSimpleName());
            System.err.println("  - 异常信息: " + e.getMessage());
            System.err.println("  - AI响应内容: " + aiResponse);
            e.printStackTrace();
            return new ArrayList<>();
        }
        
        System.out.println("✅ AI响应解析完成，返回 " + suggestions.size() + " 个评分标准");
        return suggestions;
    }
    
    /**
     * 从AI响应中提取JSON部分
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        // 查找JSON开始和结束标记
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        return null;
    }
    
    /**
     * 调整评分标准分值以匹配总分
     */
    private void adjustRubricPoints(List<RubricSuggestionResponse> suggestions, BigDecimal maxScore) {
        if (suggestions.isEmpty()) {
            return;
        }
        
        // 计算当前总分
        BigDecimal currentTotal = suggestions.stream()
            .map(RubricSuggestionResponse::getPoints)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算调整比例
        double adjustmentRatio = maxScore.doubleValue() / currentTotal.doubleValue();
        
        // 按比例调整每个评分标准的分值
        BigDecimal adjustedTotal = BigDecimal.ZERO;
        for (int i = 0; i < suggestions.size() - 1; i++) {
            RubricSuggestionResponse suggestion = suggestions.get(i);
            BigDecimal adjustedPoints = BigDecimal.valueOf(
                Math.round(suggestion.getPoints().doubleValue() * adjustmentRatio * 10.0) / 10.0
            );
            suggestion.setPoints(adjustedPoints);
            adjustedTotal = adjustedTotal.add(adjustedPoints);
        }
        
        // 最后一个评分标准处理剩余差值
        RubricSuggestionResponse lastSuggestion = suggestions.get(suggestions.size() - 1);
        BigDecimal remainingPoints = maxScore.subtract(adjustedTotal);
        lastSuggestion.setPoints(remainingPoints);
    }
    
    /**
     * 获取题目类型描述
     */
    private String getQuestionTypeDescription(QuestionType questionType) {
        switch (questionType) {
            case SINGLE_CHOICE:
                return "单选题";
            case MULTIPLE_CHOICE:
                return "多选题";
            case TRUE_FALSE:
                return "判断题";
            case SHORT_ANSWER:
                return "简答题";
            case ESSAY:
                return "论述题";
            case FILL_BLANK:
                return "填空题";
            case CODING:
                return "编程题";
            case CALCULATION:
                return "计算题";
            case CASE_ANALYSIS:
                return "案例分析题";
            default:
                return "其他类型";
        }
    }
    
    /**
     * 生成基础评分标准（当AI不可用时使用）
     */
    private List<RubricSuggestionResponse> generateBasicRubricSuggestions(com.teachhelper.entity.Question question, String customPrompt) {
        System.out.println("🔄 使用fallback基础评分标准生成");
        System.out.println("  - 题目类型: " + question.getQuestionType());
        System.out.println("  - 满分: " + question.getMaxScore());
        System.out.println("  - 自定义提示词: " + (customPrompt != null ? "有" : "无"));
        
        List<RubricSuggestionResponse> suggestions = new ArrayList<>();
        BigDecimal maxScore = question.getMaxScore();
        QuestionType questionType = question.getQuestionType();
        
        // 根据题目类型生成基础评分标准
        switch (questionType) {
            case MULTIPLE_CHOICE:
            case SINGLE_CHOICE:
            case TRUE_FALSE:
                suggestions.add(new RubricSuggestionResponse("正确答案", calculatePoints(maxScore, 0.8)));
                suggestions.add(new RubricSuggestionResponse("知识掌握", calculatePoints(maxScore, 0.2)));
                break;
            case SHORT_ANSWER:
            case ESSAY:
                suggestions.add(new RubricSuggestionResponse("要点完整性", calculatePoints(maxScore, 0.4)));
                suggestions.add(new RubricSuggestionResponse("内容准确性", calculatePoints(maxScore, 0.3)));
                suggestions.add(new RubricSuggestionResponse("逻辑条理性", calculatePoints(maxScore, 0.2)));
                suggestions.add(new RubricSuggestionResponse("表达清晰度", calculatePoints(maxScore, 0.1)));
                break;
            case FILL_BLANK:
                suggestions.add(new RubricSuggestionResponse("答案准确性", calculatePoints(maxScore, 0.7)));
                suggestions.add(new RubricSuggestionResponse("表达规范性", calculatePoints(maxScore, 0.3)));
                break;
            case CODING:
                suggestions.add(new RubricSuggestionResponse("功能实现", calculatePoints(maxScore, 0.4)));
                suggestions.add(new RubricSuggestionResponse("代码质量", calculatePoints(maxScore, 0.3)));
                suggestions.add(new RubricSuggestionResponse("算法效率", calculatePoints(maxScore, 0.3)));
                break;
            case CALCULATION:
                suggestions.add(new RubricSuggestionResponse("计算准确性", calculatePoints(maxScore, 0.5)));
                suggestions.add(new RubricSuggestionResponse("解题步骤", calculatePoints(maxScore, 0.3)));
                suggestions.add(new RubricSuggestionResponse("公式应用", calculatePoints(maxScore, 0.2)));
                break;
            case CASE_ANALYSIS:
                suggestions.add(new RubricSuggestionResponse("案例理解", calculatePoints(maxScore, 0.3)));
                suggestions.add(new RubricSuggestionResponse("分析深度", calculatePoints(maxScore, 0.3)));
                suggestions.add(new RubricSuggestionResponse("解决方案", calculatePoints(maxScore, 0.4)));
                break;
            default:
                suggestions.add(new RubricSuggestionResponse("内容准确性", calculatePoints(maxScore, 0.5)));
                suggestions.add(new RubricSuggestionResponse("理解深度", calculatePoints(maxScore, 0.3)));
                suggestions.add(new RubricSuggestionResponse("表达清晰度", calculatePoints(maxScore, 0.2)));
                break;
        }
        
        System.out.println("✅ 基础评分标准生成完成，共 " + suggestions.size() + " 项:");
        for (int i = 0; i < suggestions.size(); i++) {
            RubricSuggestionResponse suggestion = suggestions.get(i);
            System.out.println("  " + (i+1) + ". " + suggestion.getCriterionText() + " (" + suggestion.getPoints() + "分)");
        }
        
        return suggestions;
    }
    
    /**
     * 计算评分标准分值
     */
    private BigDecimal calculatePoints(BigDecimal maxScore, double ratio) {
        double points = maxScore.doubleValue() * ratio;
        double roundedPoints = Math.round(points * 10.0) / 10.0;
        return BigDecimal.valueOf(roundedPoints);
    }
    
    /**
     * 生成评分标准建议（无自定义提示词）
     */
    public List<RubricSuggestionResponse> generateRubricSuggestions(com.teachhelper.entity.Question question) {
        return generateRubricSuggestions(question, null);
    }
    
    /**
     * 保留原有的方法，用于向后兼容
     */
    public RubricSuggestion generateRubric(com.teachhelper.entity.Question question) {
        List<RubricSuggestionResponse> suggestions = generateRubricSuggestions(question);
        
        RubricSuggestion rubricSuggestion = new RubricSuggestion();
        if (suggestions != null && !suggestions.isEmpty()) {
            RubricSuggestionResponse firstSuggestion = suggestions.get(0);
            rubricSuggestion.setDescription(firstSuggestion.getCriterionText());
            rubricSuggestion.setPoints(firstSuggestion.getPoints());
        }
        return rubricSuggestion;
    }
    
    /**
     * 评估学生答案 - 使用AI进行智能评分
     * 
     * @param studentAnswer 学生答案实体
     * @return 评估结果对象，包含分数、反馈等信息
     */
    public EvaluationResult evaluateAnswer(com.teachhelper.entity.StudentAnswer studentAnswer) {
        System.out.println("=== AIEvaluationService.evaluateAnswer() 调试信息 ===");
        try {
            // 检查当前SecurityContext状态
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication auth = context.getAuthentication();
            
            System.out.println("SecurityContext: " + context);
            System.out.println("Authentication: " + auth);
            System.out.println("Authentication 是否为null: " + (auth == null));
            
            if (auth != null) {
                System.out.println("Authentication 类型: " + auth.getClass().getName());
                System.out.println("Authentication getName(): " + auth.getName());
                System.out.println("Authentication getPrincipal(): " + auth.getPrincipal());
                System.out.println("Authentication isAuthenticated(): " + auth.isAuthenticated());
            }
            
            // 尝试获取当前用户ID
            Long userId = authService.getCurrentUser().getId();
            System.out.println("✅ 成功获取用户ID: " + userId);
            return evaluateAnswer(studentAnswer, userId);
        } catch (Exception e) {
            // 如果无法获取当前用户（例如在异步线程中），使用基础评估
            System.err.println("⚠️  无法获取当前用户信息，使用基础评估规则");
            System.err.println("  - 异常类型: " + e.getClass().getName());
            System.err.println("  - 原因: " + e.getMessage());
            e.printStackTrace(); // 打印完整堆栈跟踪
            return createBasicEvaluation(studentAnswer);
        }
    }

    /**
     * 评估学生答案 - 使用AI进行智能评分（指定用户ID版本）
     * 
     * @param studentAnswer 学生答案实体
     * @param userId 评估者用户ID
     * @return 评估结果对象，包含分数、反馈等信息
     */
    public EvaluationResult evaluateAnswer(com.teachhelper.entity.StudentAnswer studentAnswer, Long userId) {
        System.out.println("=== 开始AI答案评估 ===");
        
        if (studentAnswer == null) {
            System.err.println("❌ 学生答案为空");
            return createErrorResult("学生答案不能为空");
        }
        
        com.teachhelper.entity.Question question = studentAnswer.getQuestion();
        if (question == null) {
            System.err.println("❌ 题目信息为空");
            return createErrorResult("题目信息不能为空");
        }
        
        System.out.println("题目ID: " + question.getId());
        System.out.println("题目标题: " + question.getTitle());
        System.out.println("题目类型: " + question.getQuestionType());
        System.out.println("题目满分: " + question.getMaxScore());
        System.out.println("学生答案: " + studentAnswer.getAnswerText());
        System.out.println("学生信息: " + (studentAnswer.getStudent() != null ? 
            studentAnswer.getStudent().getName() + "(" + studentAnswer.getStudent().getStudentId() + ")" : "未知"));
        
        try {
            // 使用传入的用户ID获取AI配置
            System.out.println("当前评估者ID: " + userId);
            
            Optional<UserAIConfig> configOpt = userAIConfigService.getUserDefaultAIConfig(userId);
            
            if (!configOpt.isPresent()) {
                System.out.println("⚠️  没有找到默认AI配置，使用基础评估规则");
                return createBasicEvaluation(studentAnswer);
            }
            
            UserAIConfig aiConfig = configOpt.get();
            System.out.println("✅ 找到AI配置:");
            System.out.println("  - 提供商: " + aiConfig.getProvider());
            System.out.println("  - 模型名称: " + aiConfig.getModelName());
            System.out.println("  - API端点: " + aiConfig.getApiEndpoint());
            
            AIClient aiClient = aiClientFactory.getClient(aiConfig.getProvider());
            System.out.println("✅ 获取AI客户端成功: " + aiClient.getClass().getSimpleName());
            
            // 构建评估提示词
            String prompt = buildEvaluationPrompt(studentAnswer);
            System.out.println("✅ 构建AI评估提示词成功，长度: " + prompt.length());
            System.out.println("--- 评估提示词内容开始 ---");
            System.out.println(prompt);
            System.out.println("--- 评估提示词内容结束 ---");
            
            // 调用AI进行评估
            System.out.println("🚀 开始调用AI进行答案评估...");
            long startTime = System.currentTimeMillis();
            AIResponse aiResponse = aiClient.chat(prompt, aiConfig);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("⏱️  AI评估耗时: " + duration + "ms");
            
            if (aiResponse.isSuccess()) {
                System.out.println("✅ AI评估调用成功!");
                System.out.println("  - 输入Token数: " + aiResponse.getInputTokens());
                System.out.println("  - 输出Token数: " + aiResponse.getOutputTokens());
                System.out.println("  - 总Token数: " + aiResponse.getTotalTokens());
                System.out.println("--- AI评估响应内容开始 ---");
                System.out.println(aiResponse.getContent());
                System.out.println("--- AI评估响应内容结束 ---");
                
                // 解析AI评估结果
                System.out.println("🔍 开始解析AI评估结果...");
                EvaluationResult result = parseEvaluationResponse(aiResponse.getContent(), question.getMaxScore());
                
                if (result.isSuccess()) {
                    System.out.println("✅ AI评估解析成功:");
                    System.out.println("  - 得分: " + result.getScore() + "/" + question.getMaxScore());
                    System.out.println("  - 反馈长度: " + (result.getFeedback() != null ? result.getFeedback().length() : 0) + " 字符");
                    System.out.println("  - 评估详情数: " + (result.getCriteriaEvaluations() != null ? result.getCriteriaEvaluations().size() : 0));
                    return result;
                } else {
                    System.out.println("❌ AI评估结果解析失败");
                }
            } else {
                System.out.println("❌ AI评估调用失败:");
                System.out.println("  - 错误信息: " + aiResponse.getErrorMessage());
                System.out.println("  - 响应内容: " + aiResponse.getContent());
            }
            
            // AI评估失败，使用基础评估
            System.err.println("❌ AI评估失败，使用基础评估规则");
            System.err.println("  - 错误原因: " + (aiResponse != null ? aiResponse.getErrorMessage() : "未知错误"));
            return createBasicEvaluation(studentAnswer);
            
        } catch (Exception e) {
            // 出现异常，使用基础评估
            System.err.println("❌ AI评估出现异常，使用基础评估规则");
            System.err.println("  - 异常类型: " + e.getClass().getSimpleName());
            System.err.println("  - 异常信息: " + e.getMessage());
            e.printStackTrace();
            return createBasicEvaluation(studentAnswer);
        }
    }
    
    /**
     * 评估学生答案 - 使用AI进行智能评分（指定用户名版本）
     * 
     * @param studentAnswer 学生答案实体
     * @param username 评估者用户名
     * @return 评估结果对象，包含分数、反馈等信息
     */
    public EvaluationResult evaluateAnswer(com.teachhelper.entity.StudentAnswer studentAnswer, String username) {
        System.out.println("=== AIEvaluationService.evaluateAnswer(username) 调试信息 ===");
        System.out.println("传入的用户名: " + username);
        
        try {
            if (username != null) {
                // 通过用户名获取用户ID
                System.out.println("尝试通过用户名获取用户ID...");
                Long userId = authService.getUserIdByUsername(username);
                if (userId != null) {
                    System.out.println("✅ 通过用户名 " + username + " 获取到用户ID: " + userId);
                    return evaluateAnswer(studentAnswer, userId);
                } else {
                    System.err.println("⚠️  无法通过用户名 " + username + " 找到用户，使用基础评估规则");
                }
            } else {
                System.err.println("⚠️  用户名为空，使用基础评估规则");
            }
        } catch (Exception e) {
            System.err.println("⚠️  通过用户名获取用户ID失败，使用基础评估规则");
            System.err.println("  - 异常类型: " + e.getClass().getName());
            System.err.println("  - 原因: " + e.getMessage());
            e.printStackTrace(); // 打印完整堆栈跟踪
        }
        
        // 如果无法获取用户ID，回退到基础评估
        System.out.println("回退到基础评估...");
        return createBasicEvaluation(studentAnswer);
    }
    
    /**
     * 构建AI答案评估提示词
     */
    private String buildEvaluationPrompt(com.teachhelper.entity.StudentAnswer studentAnswer) {
        StringBuilder prompt = new StringBuilder();
        com.teachhelper.entity.Question question = studentAnswer.getQuestion();
        
        prompt.append("你是一位专业的教育评估专家，请对以下学生答案进行公正、客观的评分。\n\n");
        
        // 题目信息
        prompt.append("=== 题目信息 ===\n");
        prompt.append("题目类型：").append(getQuestionTypeDescription(question.getQuestionType())).append("\n");
        prompt.append("题目标题：").append(question.getTitle() != null ? question.getTitle() : "").append("\n");
        prompt.append("题目内容：").append(question.getContent() != null ? question.getContent() : "").append("\n");
        
        // 选择题选项
        try {
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                prompt.append("题目选项：\n");
                for (int i = 0; i < question.getOptions().size(); i++) {
                    var option = question.getOptions().get(i);
                    char optionLetter = (char)('A' + i);
                    prompt.append("  ").append(optionLetter).append(". ").append(option.getContent());
                    if (option.getIsCorrect() != null && option.getIsCorrect()) {
                        prompt.append(" [正确答案]");
                    }
                    prompt.append("\n");
                }
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // 如果options集合无法加载，跳过选项显示
            System.out.println("⚠️  无法加载题目选项（LazyInitializationException），跳过选项显示");
        }
        
        // 参考答案
        if (question.getReferenceAnswer() != null && !question.getReferenceAnswer().trim().isEmpty()) {
            prompt.append("参考答案：").append(question.getReferenceAnswer()).append("\n");
        }
        
        prompt.append("题目满分：").append(question.getMaxScore()).append("分\n");
        
        // 评分标准
        if (question.getRubricCriteria() != null && !question.getRubricCriteria().isEmpty()) {
            prompt.append("\n=== 评分标准 ===\n");
            BigDecimal totalCriteriaPoints = BigDecimal.ZERO;
            for (int i = 0; i < question.getRubricCriteria().size(); i++) {
                var criterion = question.getRubricCriteria().get(i);
                prompt.append((i + 1)).append(". ").append(criterion.getCriterionText())
                      .append("（").append(criterion.getPoints()).append("分）\n");
                totalCriteriaPoints = totalCriteriaPoints.add(criterion.getPoints());
            }
            prompt.append("评分标准总分：").append(totalCriteriaPoints).append("分\n");
        }
        
        // 学生答案
        prompt.append("\n=== 学生答案 ===\n");
        prompt.append("学生姓名：").append(studentAnswer.getStudent() != null ? studentAnswer.getStudent().getName() : "未知").append("\n");
        prompt.append("学生学号：").append(studentAnswer.getStudent() != null ? studentAnswer.getStudent().getStudentId() : "未知").append("\n");
        prompt.append("答案内容：").append(studentAnswer.getAnswerText()).append("\n");
        
        // 评估要求
        prompt.append("\n=== 评估要求 ===\n");
        prompt.append("请对该学生答案进行客观、公正的评分，并提供详细的反馈意见。\n\n");
        
        prompt.append("评估重点：\n");
        switch (question.getQuestionType()) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
            case TRUE_FALSE:
                prompt.append("1. 答案是否正确\n");
                prompt.append("2. 是否理解题目考查的知识点\n");
                break;
            case SHORT_ANSWER:
            case ESSAY:
                prompt.append("1. 答案要点的完整性和准确性\n");
                prompt.append("2. 逻辑结构和条理性\n");
                prompt.append("3. 语言表达的清晰度\n");
                prompt.append("4. 对知识点的理解深度\n");
                break;
            case FILL_BLANK:
                prompt.append("1. 填空内容的准确性\n");
                prompt.append("2. 表达的规范性\n");
                prompt.append("3. 知识点掌握程度\n");
                break;
            case CODING:
                prompt.append("1. 代码功能的正确性\n");
                prompt.append("2. 代码质量和编程规范\n");
                prompt.append("3. 算法效率和思路清晰度\n");
                prompt.append("4. 边界条件处理\n");
                break;
            case CALCULATION:
                prompt.append("1. 计算结果的准确性\n");
                prompt.append("2. 解题步骤的完整性\n");
                prompt.append("3. 公式运用的正确性\n");
                prompt.append("4. 单位和格式的规范性\n");
                break;
            case CASE_ANALYSIS:
                prompt.append("1. 案例理解的准确性\n");
                prompt.append("2. 分析思路的逻辑性\n");
                prompt.append("3. 理论知识的运用\n");
                prompt.append("4. 解决方案的可行性\n");
                break;
            default:
                prompt.append("1. 答案的准确性和完整性\n");
                prompt.append("2. 理解深度和表达清晰度\n");
                break;
        }
        
        // 输出格式要求
        prompt.append("\n=== 输出格式 ===\n");
        prompt.append("请严格按照以下JSON格式返回评估结果：\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 分数(数字，保留1位小数),\n");
        prompt.append("  \"feedback\": \"详细的评分反馈\",\n");
        prompt.append("  \"strengths\": \"答案的优点\",\n");
        prompt.append("  \"improvements\": \"改进建议\",\n");
        if (question.getRubricCriteria() != null && !question.getRubricCriteria().isEmpty()) {
            prompt.append("  \"criteriaEvaluations\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"criterionText\": \"评分标准名称\",\n");
            prompt.append("      \"earnedPoints\": 获得分数(数字),\n");
            prompt.append("      \"maxPoints\": 满分(数字),\n");
            prompt.append("      \"comment\": \"针对该标准的评价\"\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
        }
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("- 总分不能超过").append(question.getMaxScore()).append("分\n");
        prompt.append("- 分数要合理，避免过于严格或过于宽松\n");
        prompt.append("- 反馈要具体、建设性，帮助学生改进\n");
        prompt.append("- 评分要客观公正，基于答案质量而非主观偏好\n");
        prompt.append("- 如果有评分标准，每个标准的得分总和应该等于或接近总分\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析AI评估响应
     */
    private EvaluationResult parseEvaluationResponse(String aiResponse, BigDecimal maxScore) {
        System.out.println("🔍 开始解析AI评估响应");
        System.out.println("  - 响应长度: " + (aiResponse != null ? aiResponse.length() : 0));
        System.out.println("  - 题目满分: " + maxScore);
        
        try {
            // 提取JSON部分
            String jsonPart = extractJsonFromResponse(aiResponse);
            if (jsonPart == null) {
                System.err.println("❌ AI评估响应中未找到有效JSON");
                System.err.println("  - 完整响应: " + aiResponse);
                return createErrorResult("AI响应格式错误：未找到有效JSON");
            }
            
            System.out.println("✅ 提取到JSON: " + jsonPart);
            
            JsonNode rootNode = objectMapper.readTree(jsonPart);
            
            // 解析分数
            BigDecimal score = BigDecimal.ZERO;
            if (rootNode.has("score")) {
                double scoreValue = rootNode.get("score").asDouble();
                score = BigDecimal.valueOf(scoreValue);
                System.out.println("✅ 解析得分: " + score);
            } else {
                System.err.println("❌ 响应中缺少score字段");
                return createErrorResult("AI响应格式错误：缺少score字段");
            }
            
            // 验证分数范围
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
                System.out.println("⚠️  分数超出范围，进行调整: " + score + " -> ");
                score = score.max(BigDecimal.ZERO).min(maxScore);
                System.out.println(score);
            }
            
            // 解析反馈
            String feedback = rootNode.has("feedback") ? rootNode.get("feedback").asText() : "";
            String strengths = rootNode.has("strengths") ? rootNode.get("strengths").asText() : "";
            String improvements = rootNode.has("improvements") ? rootNode.get("improvements").asText() : "";
            
            System.out.println("✅ 解析反馈信息:");
            System.out.println("  - 主要反馈长度: " + feedback.length());
            System.out.println("  - 优点描述长度: " + strengths.length());
            System.out.println("  - 改进建议长度: " + improvements.length());
            
            // 解析评分标准详情
            List<CriterionEvaluation> criteriaEvaluations = new ArrayList<>();
            if (rootNode.has("criteriaEvaluations") && rootNode.get("criteriaEvaluations").isArray()) {
                JsonNode criteriaNode = rootNode.get("criteriaEvaluations");
                System.out.println("✅ 解析评分标准详情，共 " + criteriaNode.size() + " 项");
                
                for (JsonNode criterionNode : criteriaNode) {
                    if (criterionNode.has("criterionText") && criterionNode.has("earnedPoints") && criterionNode.has("maxPoints")) {
                        String criterionText = criterionNode.get("criterionText").asText();
                        BigDecimal earnedPoints = BigDecimal.valueOf(criterionNode.get("earnedPoints").asDouble());
                        BigDecimal maxPoints = BigDecimal.valueOf(criterionNode.get("maxPoints").asDouble());
                        String comment = criterionNode.has("comment") ? criterionNode.get("comment").asText() : "";
                        
                        CriterionEvaluation evaluation = new CriterionEvaluation(criterionText, earnedPoints, maxPoints, comment);
                        criteriaEvaluations.add(evaluation);
                        
                        System.out.println("  - " + criterionText + ": " + earnedPoints + "/" + maxPoints + " 分");
                    }
                }
            }
            
            // 构建完整反馈
            StringBuilder fullFeedback = new StringBuilder();
            if (!feedback.trim().isEmpty()) {
                fullFeedback.append("总体评价：").append(feedback).append("\n\n");
            }
            if (!strengths.trim().isEmpty()) {
                fullFeedback.append("答案优点：").append(strengths).append("\n\n");
            }
            if (!improvements.trim().isEmpty()) {
                fullFeedback.append("改进建议：").append(improvements).append("\n\n");
            }
            
            EvaluationResult result = new EvaluationResult(true, score, fullFeedback.toString(), criteriaEvaluations);
            
            System.out.println("✅ AI评估结果解析完成:");
            System.out.println("  - 最终得分: " + result.getScore());
            System.out.println("  - 反馈总长度: " + result.getFeedback().length());
            System.out.println("  - 评分标准详情数: " + result.getCriteriaEvaluations().size());
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 解析AI评估响应失败:");
            System.err.println("  - 异常类型: " + e.getClass().getSimpleName());
            System.err.println("  - 异常信息: " + e.getMessage());
            System.err.println("  - AI响应内容: " + aiResponse);
            e.printStackTrace();
            return createErrorResult("解析AI评估响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建基础评估结果（当AI不可用时）
     */
    private EvaluationResult createBasicEvaluation(com.teachhelper.entity.StudentAnswer studentAnswer) {
        System.out.println("🔄 使用基础评估规则");
        
        com.teachhelper.entity.Question question = studentAnswer.getQuestion();
        String answerText = studentAnswer.getAnswerText();
        
        if (answerText == null || answerText.trim().isEmpty()) {
            return new EvaluationResult(true, BigDecimal.ZERO, "答案为空，无法评分。", new ArrayList<>());
        }
        
        BigDecimal score;
        String feedback;
        List<CriterionEvaluation> criteriaEvaluations = new ArrayList<>();
        
        // 根据题目类型进行基础评估
        switch (question.getQuestionType()) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
            case TRUE_FALSE:
                score = evaluateChoiceQuestion(question, answerText);
                feedback = "这是一道选择题，已根据标准答案进行评分。";
                break;
                
            case FILL_BLANK:
                score = evaluateFillBlankQuestion(question, answerText);
                feedback = "这是一道填空题，已根据参考答案进行评分。";
                break;
                
            default:
                // 主观题基础评估
                score = evaluateSubjectiveQuestion(question, answerText);
                feedback = "这是一道主观题，已进行基础评估。由于AI评估暂时不可用，建议人工复查。";
                break;
        }
        
        // 如果有评分标准，按比例分配分数
        if (question.getRubricCriteria() != null && !question.getRubricCriteria().isEmpty()) {
            BigDecimal totalCriteriaPoints = question.getRubricCriteria().stream()
                .map(com.teachhelper.entity.RubricCriterion::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalCriteriaPoints.compareTo(BigDecimal.ZERO) > 0) {
                for (com.teachhelper.entity.RubricCriterion criterion : question.getRubricCriteria()) {
                    BigDecimal ratio = criterion.getPoints().divide(totalCriteriaPoints, 4, BigDecimal.ROUND_HALF_UP);
                    BigDecimal earnedPoints = score.multiply(ratio).setScale(1, BigDecimal.ROUND_HALF_UP);
                    
                    CriterionEvaluation evaluation = new CriterionEvaluation(
                        criterion.getCriterionText(),
                        earnedPoints,
                        criterion.getPoints(),
                        "基础评估结果"
                    );
                    criteriaEvaluations.add(evaluation);
                }
            }
        }
        
        System.out.println("✅ 基础评估完成，得分: " + score + "/" + question.getMaxScore());
        
        return new EvaluationResult(true, score, feedback, criteriaEvaluations);
    }
    
    /**
     * 评估选择题
     */
    private BigDecimal evaluateChoiceQuestion(com.teachhelper.entity.Question question, String answerText) {
        if (question.getReferenceAnswer() == null || question.getReferenceAnswer().trim().isEmpty()) {
            // 没有参考答案，通过选项判断
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                // 查找正确选项
                StringBuilder correctAnswer = new StringBuilder();
                for (int i = 0; i < question.getOptions().size(); i++) {
                    var option = question.getOptions().get(i);
                    if (option.getIsCorrect() != null && option.getIsCorrect()) {
                        if (correctAnswer.length() > 0) {
                            correctAnswer.append(",");
                        }
                        correctAnswer.append((char)('A' + i));
                    }
                }
                
                if (correctAnswer.length() > 0) {
                    String correct = correctAnswer.toString();
                    String studentAnswer = answerText.trim().toUpperCase();
                    
                    if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                        // 多选题部分分数
                        return calculateMultipleChoiceScore(correct, studentAnswer, question.getMaxScore());
                    } else {
                        // 单选题或判断题
                        return correct.equals(studentAnswer) ? question.getMaxScore() : BigDecimal.ZERO;
                    }
                }
            }
        } else {
            // 有参考答案，直接比较
            String correct = question.getReferenceAnswer().trim().toUpperCase();
            String studentAnswer = answerText.trim().toUpperCase();
            
            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                return calculateMultipleChoiceScore(correct, studentAnswer, question.getMaxScore());
            } else {
                return correct.equals(studentAnswer) ? question.getMaxScore() : BigDecimal.ZERO;
            }
        }
        
        // 无法判断，给50%分数
        return question.getMaxScore().multiply(BigDecimal.valueOf(0.5));
    }
    
    /**
     * 计算多选题分数
     */
    private BigDecimal calculateMultipleChoiceScore(String correctAnswer, String studentAnswer, BigDecimal maxScore) {
        String[] correctOptions = correctAnswer.split(",");
        String[] studentOptions = studentAnswer.split(",");
        
        Set<String> correctSet = new HashSet<>();
        Set<String> studentSet = new HashSet<>();
        
        for (String option : correctOptions) {
            correctSet.add(option.trim());
        }
        for (String option : studentOptions) {
            studentSet.add(option.trim());
        }
        
        // 计算交集和并集
        Set<String> intersection = new HashSet<>(correctSet);
        intersection.retainAll(studentSet);
        
        Set<String> union = new HashSet<>(correctSet);
        union.addAll(studentSet);
        
        // 使用 Jaccard 相似度计算分数
        if (union.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        double similarity = (double) intersection.size() / union.size();
        return maxScore.multiply(BigDecimal.valueOf(similarity)).setScale(1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * 评估填空题
     */
    private BigDecimal evaluateFillBlankQuestion(com.teachhelper.entity.Question question, String answerText) {
        if (question.getReferenceAnswer() == null || question.getReferenceAnswer().trim().isEmpty()) {
            // 没有参考答案，给50%分数
            return question.getMaxScore().multiply(BigDecimal.valueOf(0.5));
        }
        
        String referenceAnswer = question.getReferenceAnswer().trim().toLowerCase();
        String studentAnswer = answerText.trim().toLowerCase();
        
        // 完全匹配
        if (referenceAnswer.equals(studentAnswer)) {
            return question.getMaxScore();
        }
        
        // 包含关键词
        if (referenceAnswer.contains(studentAnswer) || studentAnswer.contains(referenceAnswer)) {
            return question.getMaxScore().multiply(BigDecimal.valueOf(0.8));
        }
        
        // 使用简单的相似度算法
        double similarity = calculateStringSimilarity(referenceAnswer, studentAnswer);
        if (similarity > 0.7) {
            return question.getMaxScore().multiply(BigDecimal.valueOf(similarity));
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 评估主观题
     */
    private BigDecimal evaluateSubjectiveQuestion(com.teachhelper.entity.Question question, String answerText) {
        // 基于答案长度和参考答案相似度的简单评估
        int answerLength = answerText.trim().length();
        
        if (answerLength == 0) {
            return BigDecimal.ZERO;
        }
        
        // 基础分数基于答案长度
        BigDecimal baseScore;
        if (answerLength < 50) {
            baseScore = question.getMaxScore().multiply(BigDecimal.valueOf(0.3));
        } else if (answerLength < 200) {
            baseScore = question.getMaxScore().multiply(BigDecimal.valueOf(0.6));
        } else {
            baseScore = question.getMaxScore().multiply(BigDecimal.valueOf(0.8));
        }
        
        // 如果有参考答案，计算相似度加分
        if (question.getReferenceAnswer() != null && !question.getReferenceAnswer().trim().isEmpty()) {
            double similarity = calculateStringSimilarity(
                question.getReferenceAnswer().toLowerCase(),
                answerText.toLowerCase()
            );
            
            BigDecimal similarityBonus = question.getMaxScore().multiply(BigDecimal.valueOf(similarity * 0.2));
            baseScore = baseScore.add(similarityBonus);
        }
        
        // 确保不超过满分
        return baseScore.min(question.getMaxScore()).setScale(1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * 计算字符串相似度
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int editDistance = calculateEditDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }
    
    /**
     * 计算编辑距离
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 创建错误结果
     */
    private EvaluationResult createErrorResult(String errorMessage) {
        return new EvaluationResult(false, BigDecimal.ZERO, errorMessage, new ArrayList<>());
    }
    
    /**
     * 评估结果类
     */
    public static class EvaluationResult {
        private final boolean success;
        private final BigDecimal score;
        private final String feedback;
        private final List<CriterionEvaluation> criteriaEvaluations;
        
        public EvaluationResult(boolean success, BigDecimal score, String feedback, List<CriterionEvaluation> criteriaEvaluations) {
            this.success = success;
            this.score = score;
            this.feedback = feedback;
            this.criteriaEvaluations = criteriaEvaluations != null ? criteriaEvaluations : new ArrayList<>();
        }
        
        public boolean isSuccess() { return success; }
        public BigDecimal getScore() { return score; }
        public String getFeedback() { return feedback; }
        public List<CriterionEvaluation> getCriteriaEvaluations() { return criteriaEvaluations; }
    }
    
    /**
     * 评分标准评估结果类
     */
    public static class CriterionEvaluation {
        private final String criterionText;
        private final BigDecimal earnedPoints;
        private final BigDecimal maxPoints;
        private final String comment;
        
        public CriterionEvaluation(String criterionText, BigDecimal earnedPoints, BigDecimal maxPoints, String comment) {
            this.criterionText = criterionText;
            this.earnedPoints = earnedPoints;
            this.maxPoints = maxPoints;
            this.comment = comment;
        }
        
        public String getCriterionText() { return criterionText; }
        public BigDecimal getEarnedPoints() { return earnedPoints; }
        public BigDecimal getMaxPoints() { return maxPoints; }
        public String getComment() { return comment; }
    }
}

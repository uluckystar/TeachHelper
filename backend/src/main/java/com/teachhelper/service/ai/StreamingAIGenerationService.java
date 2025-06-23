package com.teachhelper.service.ai;

import com.teachhelper.ai.AIEvaluationService;
import com.teachhelper.dto.request.TaskCreateRequest;
import com.teachhelper.dto.response.AIGenerationStatusResponse;
import com.teachhelper.dto.response.RubricSuggestionResponse;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.repository.RubricCriterionRepository;
import com.teachhelper.service.question.QuestionService;
import com.teachhelper.service.task.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流式AI生成服务 - 基于任务管理系统的实现
 */
@Service
public class StreamingAIGenerationService {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private AIEvaluationService aiEvaluationService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;
    
    /**
     * 创建AI评分标准生成任务
     */
    public String createGenerationTask(Long questionId) {
        // 验证题目是否存在
        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在: " + questionId);
        }
        
        // 创建任务配置
        Map<String, Object> config = new HashMap<>();
        config.put("questionId", questionId);
        config.put("action", "GENERATE_RUBRIC");
        config.put("questionTitle", question.getTitle());
        config.put("questionType", question.getQuestionType().name());
        config.put("maxScore", question.getMaxScore());
        
        // 创建任务请求
        TaskCreateRequest request = new TaskCreateRequest();
        request.setType("AI_GENERATION");
        request.setName("AI评分标准生成 - 题目 " + questionId);
        request.setDescription("为题目《" + question.getTitle() + "》生成AI评分标准");
        request.setConfig(config);
        request.setPriority("MEDIUM");
        request.setAutoStart(true);
        
        // 创建任务
        TaskResponse task = taskService.createTask(request);
        
        // 异步执行生成任务
        executeGenerationTask(task.getTaskId(), questionId);
        
        return task.getTaskId();
    }
    
    /**
     * 创建AI评分标准生成任务（带现有评分标准检测）
     */
    public String createGenerationTaskWithCheck(Long questionId, String mode, Integer targetScore, String customPrompt) {
        // 验证题目是否存在
        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在: " + questionId);
        }
        
        // 创建任务配置
        Map<String, Object> config = new HashMap<>();
        config.put("questionId", questionId);
        config.put("action", "GENERATE_RUBRIC");
        config.put("questionTitle", question.getTitle());
        config.put("questionType", question.getQuestionType().name());
        config.put("maxScore", question.getMaxScore());
        config.put("generationMode", mode); // "OVERWRITE" 或 "COMPLEMENT"
        config.put("targetScore", targetScore); // 补全模式下的目标分数
        config.put("customPrompt", customPrompt); // 自定义提示词
        
        // 创建任务请求
        TaskCreateRequest request = new TaskCreateRequest();
        request.setType("AI_GENERATION");
        String taskName = "AI评分标准生成 - 题目 " + questionId + " (" + (mode.equals("OVERWRITE") ? "覆盖" : "补全") + ")";
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            taskName += " (自定义提示词)";
        }
        request.setName(taskName);
        request.setDescription("为题目《" + question.getTitle() + "》" + (mode.equals("OVERWRITE") ? "重新生成" : "补全") + "AI评分标准");
        request.setConfig(config);
        request.setPriority("MEDIUM");
        request.setAutoStart(true);
        
        // 创建任务
        TaskResponse task = taskService.createTask(request);
        
        // 异步执行生成任务
        executeGenerationTaskWithMode(task.getTaskId(), questionId, mode, targetScore, customPrompt);
        
        return task.getTaskId();
    }
    
    /**
     * 获取任务状态
     */
    public AIGenerationStatusResponse getTaskStatus(String taskId) {
        try {
            TaskResponse task = taskService.getTaskById(taskId);
            
            AIGenerationStatusResponse response = new AIGenerationStatusResponse();
            response.setTaskId(taskId);
            response.setProgress(task.getProgress() != null ? task.getProgress().intValue() : 0);
            response.setMessage(task.getDescription());
            
            // 转换任务状态
            switch (task.getStatus()) {
                case "PENDING":
                    response.setStatus(AIGenerationStatusResponse.GenerationStatus.PENDING);
                    break;
                case "RUNNING":
                    response.setStatus(AIGenerationStatusResponse.GenerationStatus.PROCESSING);
                    break;
                case "COMPLETED":
                    response.setStatus(AIGenerationStatusResponse.GenerationStatus.COMPLETED);
                    // 从任务结果中获取建议
                    Map<String, Object> results = taskService.getTaskResults(taskId, 0, 100);
                    System.out.println("🔍 StreamingAIGenerationService.getTaskStatus - 检查任务结果:");
                    System.out.println("  - 任务ID: " + taskId);
                    System.out.println("  - 结果keys: " + (results != null ? results.keySet() : "null"));
                    
                    if (results != null) {
                        // 检查是否有resultData字段（新的方式）
                        if (results.containsKey("resultData")) {
                            String resultDataStr = (String) results.get("resultData");
                            System.out.println("  - 找到resultData字段，长度: " + 
                                (resultDataStr != null ? resultDataStr.length() : 0));
                            
                            // 尝试解析JSON字符串中的建议
                            if (resultDataStr != null && resultDataStr.contains("suggestions")) {
                                try {
                                    // 简单的字符串解析方法
                                    response.setSuggestions(parseResultDataForSuggestions(resultDataStr));
                                    System.out.println("  - 成功解析到建议数量: " + 
                                        (response.getSuggestions() != null ? response.getSuggestions().size() : 0));
                                } catch (Exception e) {
                                    System.err.println("  - 解析建议失败: " + e.getMessage());
                                }
                            }
                        }
                        // 检查是否直接有suggestions字段（旧的方式）
                        else if (results.containsKey("suggestions")) {
                            @SuppressWarnings("unchecked")
                            List<RubricSuggestionResponse> suggestions = (List<RubricSuggestionResponse>) results.get("suggestions");
                            response.setSuggestions(suggestions);
                            System.out.println("  - 直接找到suggestions字段，数量: " + 
                                (suggestions != null ? suggestions.size() : 0));
                        }
                        
                        // 设置其他统计信息
                        if (results.containsKey("totalTokens")) {
                            response.setTotalTokens((Integer) results.get("totalTokens"));
                        }
                        if (results.containsKey("promptTokens")) {
                            response.setPromptTokens((Integer) results.get("promptTokens"));
                        }
                        if (results.containsKey("completionTokens")) {
                            response.setCompletionTokens((Integer) results.get("completionTokens"));
                        }
                        if (results.containsKey("processingTimeMs")) {
                            Object timeObj = results.get("processingTimeMs");
                            if (timeObj instanceof Long) {
                                response.setProcessingTimeMs((Long) timeObj);
                            } else if (timeObj instanceof Integer) {
                                response.setProcessingTimeMs(((Integer) timeObj).longValue());
                            }
                        }
                    }
                    break;
                case "FAILED":
                    response.setStatus(AIGenerationStatusResponse.GenerationStatus.FAILED);
                    response.setError(task.getErrorMessage());
                    break;
                case "CANCELLED":
                    response.setStatus(AIGenerationStatusResponse.GenerationStatus.CANCELLED);
                    break;
                default:
                    response.setStatus(AIGenerationStatusResponse.GenerationStatus.PENDING);
            }
            
            // 设置题目ID（从配置中获取）
            if (task.getConfig() != null && task.getConfig().containsKey("questionId")) {
                Object questionIdObj = task.getConfig().get("questionId");
                if (questionIdObj instanceof Number) {
                    response.setQuestionId(((Number) questionIdObj).longValue());
                }
            }
            
            return response;
        } catch (Exception e) {
            System.err.println("❌ StreamingAIGenerationService.getTaskStatus - 获取任务状态失败:");
            System.err.println("  - 任务ID: " + taskId);
            System.err.println("  - 错误: " + e.getMessage());
            e.printStackTrace();
            
            AIGenerationStatusResponse response = new AIGenerationStatusResponse();
            response.setTaskId(taskId);
            response.setStatus(AIGenerationStatusResponse.GenerationStatus.FAILED);
            response.setError("获取任务状态失败: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * 从JSON字符串中解析建议列表
     */
    private List<RubricSuggestionResponse> parseResultDataForSuggestions(String resultDataStr) {
        List<RubricSuggestionResponse> suggestions = new ArrayList<>();
        
        try {
            System.out.println("🔍 解析任务结果数据:");
            System.out.println("  - 数据长度: " + (resultDataStr != null ? resultDataStr.length() : 0));
            System.out.println("  - 数据内容: " + resultDataStr);
            
            if (resultDataStr == null || resultDataStr.trim().isEmpty()) {
                System.out.println("⚠️ 结果数据为空");
                return suggestions;
            }
            
            // 简单的字符串解析，查找suggestions数组
            String jsonData = resultDataStr.trim();
            
            // 先查找 "suggestions" 字段（新格式）
            int suggestionsIndex = jsonData.indexOf("\"suggestions\":");
            if (suggestionsIndex != -1) {
                System.out.println("✅ 找到suggestions字段");
                // 找到suggestions数组的开始位置
                int arrayStart = jsonData.indexOf('[', suggestionsIndex);
                if (arrayStart != -1) {
                    // 找到数组的结束位置
                    int arrayEnd = findMatchingBracket(jsonData, arrayStart);
                    if (arrayEnd != -1) {
                        String suggestionsArray = jsonData.substring(arrayStart, arrayEnd + 1);
                        System.out.println("🔍 提取到suggestions数组: " + suggestionsArray);
                        
                        // 解析数组中的每个对象
                        return parseSuggestionsArray(suggestionsArray);
                    }
                }
            }
            
            // 兼容旧格式，查找 "rubrics" 字段
            int rubricsIndex = jsonData.indexOf("\"rubrics\"");
            if (rubricsIndex == -1) {
                System.out.println("⚠️ 未找到rubrics字段");
                return suggestions;
            }
            
            // 查找rubrics数组的开始 [
            int arrayStart = jsonData.indexOf("[", rubricsIndex);
            if (arrayStart == -1) {
                System.out.println("⚠️ 未找到rubrics数组开始");
                return suggestions;
            }
            
            // 查找rubrics数组的结束 ]
            int arrayEnd = jsonData.indexOf("]", arrayStart);
            if (arrayEnd == -1) {
                System.out.println("⚠️ 未找到rubrics数组结束");
                return suggestions;
            }
            
            String rubricsArray = jsonData.substring(arrayStart + 1, arrayEnd);
            System.out.println("🔍 提取到rubrics数组: " + rubricsArray);
            
            // 解析数组中的每个对象
            return parseSuggestionsArray("[" + rubricsArray + "]");
            
        } catch (Exception e) {
            System.err.println("⚠️ 解析结果数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return suggestions;
    }
    
    /**
     * 查找匹配的括号位置
     */
    private int findMatchingBracket(String text, int startPos) {
        int count = 0;
        for (int i = startPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') {
                count++;
            } else if (c == ']') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 解析建议数组
     */
    private List<RubricSuggestionResponse> parseSuggestionsArray(String arrayStr) {
        List<RubricSuggestionResponse> suggestions = new ArrayList<>();
        
        try {
            // 简单的字符串解析，分割为单个评分项
            String content = arrayStr.substring(1, arrayStr.length() - 1); // 移除 [ ]
            String[] items = content.split("\\},\\s*\\{");
            
            for (String item : items) {
                // 清理大括号
                item = item.replaceAll("[{}]", "").trim();
                if (item.isEmpty()) continue;
                
                System.out.println("🔍 处理评分项: " + item);
                
                // 解析 criterionText 和 points
                String name = null;
                BigDecimal points = null;
                
                // 查找 criterionText 字段
                String criterionTextPattern = "\"criterionText\"\\s*:\\s*\"([^\"]+)\"";
                java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile(criterionTextPattern);
                java.util.regex.Matcher nameMatcher = namePattern.matcher(item);
                if (nameMatcher.find()) {
                    name = nameMatcher.group(1);
                } else {
                    // 兼容旧格式的 name 字段
                    String namePattern2 = "\"name\"\\s*:\\s*\"([^\"]+)\"";
                    java.util.regex.Pattern namePattern2Compiled = java.util.regex.Pattern.compile(namePattern2);
                    java.util.regex.Matcher nameMatcher2 = namePattern2Compiled.matcher(item);
                    if (nameMatcher2.find()) {
                        name = nameMatcher2.group(1);
                    }
                }
                
                // 查找 points 字段
                String pointsPattern = "\"points\"\\s*:\\s*([0-9.]+)";
                java.util.regex.Pattern pointsPatternCompiled = java.util.regex.Pattern.compile(pointsPattern);
                java.util.regex.Matcher pointsMatcher = pointsPatternCompiled.matcher(item);
                if (pointsMatcher.find()) {
                    try {
                        points = new BigDecimal(pointsMatcher.group(1));
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ 解析分值失败: " + pointsMatcher.group(1));
                    }
                }
                
                if (name != null && points != null) {
                    RubricSuggestionResponse suggestion = new RubricSuggestionResponse();
                    suggestion.setCriterionText(name);
                    suggestion.setPoints(points);
                    suggestions.add(suggestion);
                    
                    System.out.println("✅ 成功解析评分项: " + name + " = " + points + "分");
                } else {
                    System.out.println("⚠️ 跳过无效评分项: name=" + name + ", points=" + points);
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ 解析建议数组失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return suggestions;
    }
    
    /**
     * 取消任务
     */
    public void cancelTask(String taskId) {
        try {
            taskService.cancelTask(taskId);
        } catch (Exception e) {
            System.err.println("取消任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步执行生成任务
     */
    @Async
    public void executeGenerationTask(String taskId, Long questionId) {
        try {
            // 更新任务状态为运行中
            taskService.updateTaskProgress(taskId, 0, 4, "RUNNING");
            taskService.addTaskLog(taskId, "INFO", "开始生成AI评分标准");
            
            // 模拟生成过程的进度
            Thread.sleep(1000); // 模拟初始化时间
            taskService.updateTaskProgress(taskId, 1, 4, "RUNNING");
            taskService.addTaskLog(taskId, "INFO", "正在分析题目内容...");
            
            Thread.sleep(2000); // 模拟AI分析时间
            taskService.updateTaskProgress(taskId, 2, 4, "RUNNING");
            taskService.addTaskLog(taskId, "INFO", "正在生成评分标准...");
            
            // 实际生成评分标准
            Question question = questionService.getQuestionById(questionId);
            List<RubricSuggestionResponse> suggestions = aiEvaluationService.generateRubricSuggestions(question);
            
            Thread.sleep(1000); // 模拟处理时间
            taskService.updateTaskProgress(taskId, 3, 4, "RUNNING");
            taskService.addTaskLog(taskId, "INFO", "正在完成生成...");
            
            // 保存结果
            Map<String, Object> results = new HashMap<>();
            results.put("suggestions", suggestions);
            results.put("totalTokens", 150); // 模拟token使用
            results.put("promptTokens", 50);
            results.put("completionTokens", 100);
            results.put("processingTimeMs", 4000L);
            results.put("generatedCount", suggestions.size());
            
            // 保存任务结果（这里需要扩展TaskService来支持保存结果）
            saveTaskResults(taskId, results);
            
            // 完成任务
            taskService.updateTaskProgress(taskId, 4, 4, "COMPLETED");
            taskService.addTaskLog(taskId, "INFO", "AI评分标准生成完成，共生成 " + suggestions.size() + " 个建议");
            
        } catch (Exception e) {
            // 任务失败
            taskService.updateTaskProgress(taskId, 0, 4, "FAILED");
            taskService.addTaskLog(taskId, "ERROR", "生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步执行生成任务（带模式支持）
     */
    @Async
    public void executeGenerationTaskWithMode(String taskId, Long questionId, String mode, Integer targetScore, String customPrompt) {
        try {
            // 更新任务状态为运行中
            taskService.updateTaskProgress(taskId, 0, 4, "RUNNING");
            String logMessage = "开始生成AI评分标准 - 模式: " + mode;
            if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                logMessage += " (自定义提示词)";
            }
            taskService.addTaskLog(taskId, "INFO", logMessage);
            
            // 模拟生成过程的进度
            Thread.sleep(1000); // 模拟初始化时间
            taskService.updateTaskProgress(taskId, 1, 4, "RUNNING");
            
            if ("COMPLEMENT".equals(mode)) {
                taskService.addTaskLog(taskId, "INFO", "检测现有评分标准并计算补全需求...");
            } else if ("OVERWRITE".equals(mode)) {
                taskService.addTaskLog(taskId, "INFO", "正在生成新的评分标准以供预览和替换...");
                // OVERWRITE模式：不直接删除，而是生成新标准供用户选择
            } else {
                taskService.addTaskLog(taskId, "INFO", "正在分析题目内容...");
            }
            
            Thread.sleep(2000); // 模拟AI分析时间
            taskService.updateTaskProgress(taskId, 2, 4, "RUNNING");
            taskService.addTaskLog(taskId, "INFO", "正在生成评分标准...");
            
            // 实际生成评分标准
            Question question = questionService.getQuestionById(questionId);
            List<RubricSuggestionResponse> suggestions;
            
            if ("COMPLEMENT".equals(mode) && targetScore != null) {
                // 补全模式：使用AI生成补全评分标准
                taskService.addTaskLog(taskId, "INFO", "使用AI生成补全评分标准，目标分数: " + targetScore);
                suggestions = generateComplementarySuggestionsWithAI(question, targetScore, customPrompt);
            } else {
                // 覆盖模式或普通模式：生成完整的评分标准
                if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                    // 使用自定义提示词生成
                    taskService.addTaskLog(taskId, "INFO", "应用自定义提示词: " + customPrompt.substring(0, Math.min(50, customPrompt.length())) + (customPrompt.length() > 50 ? "..." : ""));
                    suggestions = aiEvaluationService.generateRubricSuggestions(question, customPrompt);
                } else {
                    // 使用默认逻辑生成
                    suggestions = aiEvaluationService.generateRubricSuggestions(question);
                }
            }
            
            Thread.sleep(1000); // 模拟处理时间
            taskService.updateTaskProgress(taskId, 3, 4, "RUNNING");
            taskService.addTaskLog(taskId, "INFO", "正在完成生成...");
            
            // 保存结果
            Map<String, Object> results = new HashMap<>();
            results.put("suggestions", suggestions);
            results.put("totalTokens", 150); // 模拟token使用
            results.put("promptTokens", 50);
            results.put("completionTokens", 100);
            results.put("processingTimeMs", 4000L);
            results.put("generatedCount", suggestions.size());
            results.put("generationMode", mode);
            results.put("targetScore", targetScore);
            
            // 对于覆盖模式，添加现有评分标准信息供前端对比
            if ("OVERWRITE".equals(mode)) {
                List<RubricSuggestionResponse> existingCriteria = getCurrentRubricCriteria(questionId);
                results.put("existingCriteria", existingCriteria);
                results.put("comparisonMode", true);
                taskService.addTaskLog(taskId, "INFO", "已准备新旧评分标准对比数据，等待用户选择");
            }
            
            // 保存任务结果
            saveTaskResults(taskId, results);
            
            // 完成任务
            taskService.updateTaskProgress(taskId, 4, 4, "COMPLETED");
            taskService.addTaskLog(taskId, "INFO", "AI评分标准生成完成，共生成 " + suggestions.size() + " 个建议");
            
        } catch (Exception e) {
            // 任务失败
            taskService.updateTaskProgress(taskId, 0, 4, "FAILED");
            taskService.addTaskLog(taskId, "ERROR", "生成失败: " + e.getMessage());
        }
    }

    /**
     * 删除现有的评分标准（OVERWRITE模式使用）
     */
    private void deleteExistingRubrics(Long questionId) {
        try {
            List<RubricCriterion> existingCriteria = rubricCriterionRepository.findByQuestionId(questionId);
            if (!existingCriteria.isEmpty()) {
                rubricCriterionRepository.deleteAll(existingCriteria);
                System.out.println("已删除题目 " + questionId + " 的 " + existingCriteria.size() + " 个现有评分标准");
            }
        } catch (Exception e) {
            System.err.println("删除现有评分标准失败: " + e.getMessage());
        }
    }

    /**
     * 生成补充性评分标准建议
     */
    /**
     * 调整补充评分标准的分数，确保总和等于剩余分数
     */
    private void adjustComplementaryScores(List<RubricSuggestionResponse> suggestions, BigDecimal targetTotal) {
        if (suggestions.isEmpty()) {
            return;
        }
        
        // 计算当前总分
        BigDecimal currentTotal = suggestions.stream()
            .map(RubricSuggestionResponse::getPoints)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算差值
        BigDecimal difference = targetTotal.subtract(currentTotal);
        
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            // 将差值加到最后一个标准上
            RubricSuggestionResponse lastSuggestion = suggestions.get(suggestions.size() - 1);
            BigDecimal newPoints = lastSuggestion.getPoints().add(difference);
            // 确保分数不为负
            if (newPoints.compareTo(BigDecimal.ZERO) < 0) {
                newPoints = BigDecimal.valueOf(0.5); // 最小分数
            }
            lastSuggestion.setPoints(newPoints);
        }
    }

    /**
     * 获取当前题目的评分标准
     */
    private List<RubricSuggestionResponse> getCurrentRubricCriteria(Long questionId) {
        try {
            List<RubricCriterion> criteria = rubricCriterionRepository.findByQuestionId(questionId);
            List<RubricSuggestionResponse> responses = new ArrayList<>();
            
            for (RubricCriterion criterion : criteria) {
                responses.add(new RubricSuggestionResponse(
                    criterion.getCriterionText(), 
                    criterion.getPoints()
                ));
            }
            
            return responses;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存任务结果
     */
    private void saveTaskResults(String taskId, Map<String, Object> results) {
        try {
            // 保存实际结果数据到TaskService
            taskService.saveTaskResults(taskId, results);
            
            // 同时记录到日志中方便查看
            taskService.addTaskLog(taskId, "RESULT", "AI生成完成");
            taskService.addTaskLog(taskId, "RESULT", "生成的评分标准数量: " + results.get("generatedCount"));
            
            @SuppressWarnings("unchecked")
            List<RubricSuggestionResponse> suggestions = (List<RubricSuggestionResponse>) results.get("suggestions");
            
            // 将建议详细记录到日志中，以便状态查询时使用
            if (suggestions != null) {
                for (int i = 0; i < suggestions.size(); i++) {
                    RubricSuggestionResponse suggestion = suggestions.get(i);
                    taskService.addTaskLog(taskId, "SUGGESTION", 
                        String.format("%d. %s (%.1f分)", i + 1, 
                            suggestion.getCriterionText(), 
                            suggestion.getPoints().doubleValue()));
                }
            }
            
            taskService.addTaskLog(taskId, "RESULT", "Token使用统计: " + results.get("totalTokens"));
            taskService.addTaskLog(taskId, "RESULT", "处理耗时: " + results.get("processingTimeMs") + "ms");
            
        } catch (Exception e) {
            System.err.println("保存任务结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用AI生成补全评分标准
     */
    private List<RubricSuggestionResponse> generateComplementarySuggestionsWithAI(Question question, Integer targetScore, String customPrompt) {
        System.out.println("=== 使用AI生成补全评分标准 ===");
        System.out.println("题目ID: " + question.getId());
        System.out.println("目标分数: " + targetScore);
        System.out.println("自定义提示词: " + (customPrompt != null ? "有" : "无"));
        
        // 获取现有评分标准
        List<RubricSuggestionResponse> existingCriteria = getCurrentRubricCriteria(question.getId());
        System.out.println("现有评分标准数量: " + existingCriteria.size());
        
        // 计算已有总分
        BigDecimal existingTotal = existingCriteria.stream()
            .map(RubricSuggestionResponse::getPoints)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("现有总分: " + existingTotal);
        
        // 计算需要补充的分数
        BigDecimal remainingPoints = BigDecimal.valueOf(targetScore).subtract(existingTotal);
        System.out.println("需要补充的分数: " + remainingPoints);
        
        if (remainingPoints.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("⚠️  不需要补充分数，返回空列表");
            return new ArrayList<>();
        }
        
        // 构建补全专用的AI提示词
        String complementPrompt = buildComplementPrompt(question, existingCriteria, remainingPoints, customPrompt);
        System.out.println("构建的补全提示词长度: " + complementPrompt.length());
        
        // 调用AI生成服务
        try {
            System.out.println("🚀 调用AI生成补全评分标准...");
            List<RubricSuggestionResponse> suggestions = aiEvaluationService.generateRubricSuggestions(question, complementPrompt);
            
            if (suggestions != null && !suggestions.isEmpty()) {
                System.out.println("✅ AI补全生成成功，数量: " + suggestions.size());
                
                // 调整分数确保总和等于剩余分数
                adjustComplementaryScores(suggestions, remainingPoints);
                
                BigDecimal actualTotal = suggestions.stream()
                    .map(RubricSuggestionResponse::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                System.out.println("调整后补全总分: " + actualTotal);
                
                return suggestions;
            } else {
                System.out.println("❌ AI补全生成返回空列表，使用fallback");
                return generateComplementaryFallback(question, remainingPoints);
            }
        } catch (Exception e) {
            System.err.println("❌ AI补全生成异常: " + e.getMessage());
            e.printStackTrace();
            return generateComplementaryFallback(question, remainingPoints);
        }
    }
    
    /**
     * 构建补全模式专用的AI提示词
     */
    private String buildComplementPrompt(Question question, List<RubricSuggestionResponse> existingCriteria, BigDecimal remainingPoints, String customPrompt) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的教育评估专家，请为以下题目生成补充评分标准。\n\n");
        
        prompt.append("=== 题目信息 ===\n");
        prompt.append("题目类型：").append(getQuestionTypeDescription(question.getQuestionType())).append("\n");
        prompt.append("题目内容：").append(question.getContent() != null ? question.getContent() : "").append("\n");
        
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            prompt.append("题目选项：");
            for (int i = 0; i < question.getOptions().size(); i++) {
                var option = question.getOptions().get(i);
                prompt.append((char)('A' + i)).append(". ").append(option.getContent());
                if (i < question.getOptions().size() - 1) {
                    prompt.append("; ");
                }
            }
            prompt.append("\n");
        }
        
        if (question.getReferenceAnswer() != null && !question.getReferenceAnswer().trim().isEmpty()) {
            prompt.append("参考答案：").append(question.getReferenceAnswer()).append("\n");
        }
        
        prompt.append("总分：").append(question.getMaxScore()).append("分\n");
        
        prompt.append("\n=== 现有评分标准 ===\n");
        if (existingCriteria.isEmpty()) {
            prompt.append("暂无现有评分标准\n");
        } else {
            BigDecimal existingTotal = BigDecimal.ZERO;
            for (int i = 0; i < existingCriteria.size(); i++) {
                RubricSuggestionResponse criterion = existingCriteria.get(i);
                prompt.append((i + 1)).append(". ").append(criterion.getCriterionText())
                      .append(" (").append(criterion.getPoints()).append("分)\n");
                existingTotal = existingTotal.add(criterion.getPoints());
            }
            prompt.append("现有总分：").append(existingTotal).append("分\n");
        }
        
        prompt.append("\n=== 补全要求 ===\n");
        prompt.append("需要补充：").append(remainingPoints).append("分\n");
        prompt.append("⚠️ 请生成针对题目具体内容的补充评分标准，避免与现有标准重复！\n\n");
        
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            prompt.append("特殊要求：").append(customPrompt).append("\n\n");
        }
        
        prompt.append("=== 生成要求 ===\n");
        prompt.append("1. 分析题目内容，确定现有评分标准未覆盖的评价维度\n");
        prompt.append("2. 生成的评分标准必须针对题目的具体内容和知识点\n");
        prompt.append("3. 避免使用通用词汇，要体现题目的核心要素\n");
        prompt.append("4. 生成的标准总分必须等于").append(remainingPoints).append("分\n");
        prompt.append("5. 确保与现有评分标准形成完整的评价体系\n");
        prompt.append("6. 补充标准应该关注现有标准未涉及的具体能力或知识点\n\n");
        
        prompt.append("=== 输出格式 ===\n");
        prompt.append("请严格按照以下JSON格式返回结果：\n");
        prompt.append("{\n");
        prompt.append("  \"rubrics\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"补充评分标准名称\",\n");
        prompt.append("      \"points\": 分值(数字)\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("- 所有分值总和必须等于").append(remainingPoints).append("分\n");
        prompt.append("- 分值可以是小数，但建议保留1位小数\n");
        prompt.append("- 补充标准必须针对题目具体内容，避免与现有标准重复\n");
        prompt.append("- 分析题目要求中现有标准未覆盖的部分，针对这些部分生成评分标准\n");
        prompt.append("- 例如：如果现有标准只有'基本概念理解'，补充标准可以是'实际应用能力'、'深入分析能力'等\n");
        
        return prompt.toString();
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
     * fallback补全生成方法（当AI失败时使用）
     */
    private List<RubricSuggestionResponse> generateComplementaryFallback(Question question, BigDecimal remainingPoints) {
        System.out.println("🔄 使用fallback补全评分标准生成");
        
        List<RubricSuggestionResponse> suggestions = new ArrayList<>();
        QuestionType questionType = question.getQuestionType();
        
        // 根据题目类型生成针对性的补充标准
        switch (questionType) {
            case SHORT_ANSWER:
            case ESSAY:
                suggestions.add(new RubricSuggestionResponse("深度思考能力", 
                    remainingPoints.multiply(new BigDecimal("0.6"))));
                suggestions.add(new RubricSuggestionResponse("创新观点", 
                    remainingPoints.multiply(new BigDecimal("0.4"))));
                break;
            case CODING:
                suggestions.add(new RubricSuggestionResponse("代码优化程度", 
                    remainingPoints.multiply(new BigDecimal("0.5"))));
                suggestions.add(new RubricSuggestionResponse("异常处理完整性", 
                    remainingPoints.multiply(new BigDecimal("0.5"))));
                break;
            case CALCULATION:
                suggestions.add(new RubricSuggestionResponse("解题思路多样性", 
                    remainingPoints.multiply(new BigDecimal("0.6"))));
                suggestions.add(new RubricSuggestionResponse("结果验证", 
                    remainingPoints.multiply(new BigDecimal("0.4"))));
                break;
            case CASE_ANALYSIS:
                suggestions.add(new RubricSuggestionResponse("创新解决方案", 
                    remainingPoints.multiply(new BigDecimal("0.5"))));
                suggestions.add(new RubricSuggestionResponse("实际应用可行性", 
                    remainingPoints.multiply(new BigDecimal("0.5"))));
                break;
            default:
                // 通用补充标准
                if (remainingPoints.compareTo(new BigDecimal("3")) >= 0) {
                    suggestions.add(new RubricSuggestionResponse("综合运用能力", 
                        remainingPoints.multiply(new BigDecimal("0.6"))));
                    suggestions.add(new RubricSuggestionResponse("实践应用", 
                        remainingPoints.multiply(new BigDecimal("0.4"))));
                } else {
                    suggestions.add(new RubricSuggestionResponse("补充评价", remainingPoints));
                }
                break;
        }
        
        // 调整分数确保精确度
        adjustComplementaryScores(suggestions, remainingPoints);
        
        System.out.println("✅ fallback补全生成完成，数量: " + suggestions.size());
        return suggestions;
    }
}

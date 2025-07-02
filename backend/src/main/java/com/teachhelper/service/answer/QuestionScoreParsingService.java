package com.teachhelper.service.answer;

import com.teachhelper.ai.AIEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class QuestionScoreParsingService {

    @Autowired
    private AIEvaluationService aiEvaluationService;
    
    @Autowired
    private com.teachhelper.service.UserAIConfigService userAIConfigService;
    
    @Autowired
    private com.teachhelper.service.ai.AIClientFactory aiClientFactory;
    
    @Autowired
    private com.teachhelper.service.auth.AuthService authService;

    // 题目类型分数上下文管理
    private Map<String, BigDecimal> sectionScoreContext = new HashMap<>();
    private String currentSectionHeader = null;

    /**
     * 分数解析上下文信息
     */
    public static class ScoreContext {
        private String sectionHeader;
        private Integer totalQuestions;
        private BigDecimal totalScore;
        private BigDecimal scorePerQuestion;
        
        // getters and setters
        public String getSectionHeader() { return sectionHeader; }
        public void setSectionHeader(String sectionHeader) { this.sectionHeader = sectionHeader; }
        public Integer getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
        public BigDecimal getTotalScore() { return totalScore; }
        public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
        public BigDecimal getScorePerQuestion() { return scorePerQuestion; }
        public void setScorePerQuestion(BigDecimal scorePerQuestion) { this.scorePerQuestion = scorePerQuestion; }
    }

    /**
     * 从题目内容中解析分数 - 优化版本
     */
    public BigDecimal parseQuestionScore(String questionContent, String sectionHeader) {
        log.debug("开始解析题目分数 - 段落标题: {}, 题目内容长度: {}", 
                 sectionHeader, questionContent != null ? questionContent.length() : 0);

        // 1. 首先检查是否切换到新的题目类型段落
        if (sectionHeader != null && !sectionHeader.equals(currentSectionHeader)) {
            log.info("检测到新的题目类型段落: {} -> {}", currentSectionHeader, sectionHeader);
            updateSectionContext(sectionHeader);
            currentSectionHeader = sectionHeader;
        }

        // 2. 优先使用段落标题计算的每题分数
        if (sectionScoreContext.containsKey(currentSectionHeader)) {
            BigDecimal contextScore = sectionScoreContext.get(currentSectionHeader);
            log.debug("使用段落上下文分数: {} (来源: {})", contextScore, currentSectionHeader);
            return contextScore;
        }

        // 3. 从题目内容本身解析分数
        BigDecimal score = parseScoreFromContent(questionContent);
        if (score != null) {
            log.debug("从题目内容解析到分数: {}", score);
            return score;
        }

        // 4. 从当前段落标题解析分数
        if (sectionHeader != null) {
            score = parseScoreFromSectionHeader(sectionHeader);
            if (score != null) {
                log.debug("从段落标题解析到分数: {}", score);
                // 更新上下文
                sectionScoreContext.put(sectionHeader, score);
                return score;
            }
        }

        // 5. 最后才使用AI智能解析
        log.debug("无法从结构化信息解析分数，使用AI智能解析");
        return parseScoreWithAI(questionContent, sectionHeader);
    }

    /**
     * 更新题目类型段落上下文
     */
    private void updateSectionContext(String sectionHeader) {
        if (sectionHeader == null || sectionHeader.trim().isEmpty()) {
            return;
        }

        ScoreContext context = parseSectionHeaderContext(sectionHeader);
        if (context.getScorePerQuestion() != null) {
            sectionScoreContext.put(sectionHeader, context.getScorePerQuestion());
            log.info("更新段落分数上下文: {} -> 每题{}分 (共{}题，总{}分)", 
                    sectionHeader, 
                    context.getScorePerQuestion(),
                    context.getTotalQuestions(),
                    context.getTotalScore());
        }
    }

    /**
     * 解析段落标题上下文信息
     */
    private ScoreContext parseSectionHeaderContext(String sectionHeader) {
        ScoreContext context = new ScoreContext();
        context.setSectionHeader(sectionHeader);

        log.debug("解析段落标题上下文: {}", sectionHeader);

        // 匹配各种段落标题格式
        Pattern[] patterns = {
            // "一 . 单选题 ( 共 25 题 ,25 分）" -> 支持空格格式
            Pattern.compile("\\(\\s*共\\s*(\\d+)\\s*题\\s*[,，]\\s*(\\d+)\\s*分\\s*[）)]"),
            // "一.单选题(共25题,25分）" -> 无空格格式  
            Pattern.compile("\\(共(\\d+)题[,，]\\s*(\\d+)分[）)]"),
            // "二、选择题（每题2分，共10题，20分）" -> 每题2分
            Pattern.compile("每题(\\d+)分.*共(\\d+)题.*?(\\d+)分"),
            // "三、填空题（共5题，每题3分，15分）" -> 每题3分
            Pattern.compile("共(\\d+)题.*每题(\\d+)分.*?(\\d+)分"),
            // "单选题(每题5分)" -> 每题5分
            Pattern.compile("\\(每题(\\d+)分\\)"),
            // "四、简答题（每题10分）" -> 每题10分
            Pattern.compile("每题(\\d+)分"),
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(sectionHeader);
            if (matcher.find()) {
                try {
                    if (pattern.pattern().contains("共.*题.*分") || pattern.pattern().contains("\\(\\s*共\\s*")) {
                        // 格式：共X题，Y分 -> 每题 Y/X 分 (支持带空格和不带空格)
                        if (matcher.groupCount() >= 2) {
                            int questionCount = Integer.parseInt(matcher.group(1));
                            int totalScore = Integer.parseInt(matcher.group(2));
                            
                            context.setTotalQuestions(questionCount);
                            context.setTotalScore(new BigDecimal(totalScore));
                            
                            if (questionCount > 0) {
                                BigDecimal scorePerQuestion = new BigDecimal(totalScore)
                                    .divide(new BigDecimal(questionCount), 2, RoundingMode.HALF_UP);
                                context.setScorePerQuestion(scorePerQuestion);
                                log.info("✅ 段落标题分数解析成功: {} -> 共{}题，总{}分，每题{}分", 
                                        sectionHeader, questionCount, totalScore, scorePerQuestion);
                                break;
                            }
                        }
                    } else if (pattern.pattern().contains("每题.*分.*共.*题")) {
                        // 格式：每题X分，共Y题，Z分
                        if (matcher.groupCount() >= 3) {
                            int scorePerQuestion = Integer.parseInt(matcher.group(1));
                            int questionCount = Integer.parseInt(matcher.group(2));
                            int totalScore = Integer.parseInt(matcher.group(3));
                            
                            context.setScorePerQuestion(new BigDecimal(scorePerQuestion));
                            context.setTotalQuestions(questionCount);
                            context.setTotalScore(new BigDecimal(totalScore));
                            log.debug("解析成功: 每题{}分，共{}题，总{}分", scorePerQuestion, questionCount, totalScore);
                            break;
                        }
                    } else if (pattern.pattern().contains("共.*题.*每题.*分")) {
                        // 格式：共X题，每题Y分，Z分
                        if (matcher.groupCount() >= 3) {
                            int questionCount = Integer.parseInt(matcher.group(1));
                            int scorePerQuestion = Integer.parseInt(matcher.group(2));
                            int totalScore = Integer.parseInt(matcher.group(3));
                            
                            context.setTotalQuestions(questionCount);
                            context.setScorePerQuestion(new BigDecimal(scorePerQuestion));
                            context.setTotalScore(new BigDecimal(totalScore));
                            log.debug("解析成功: 共{}题，每题{}分，总{}分", questionCount, scorePerQuestion, totalScore);
                            break;
                        }
                    } else {
                        // 直接的每题分数
                        int scorePerQuestion = Integer.parseInt(matcher.group(1));
                        context.setScorePerQuestion(new BigDecimal(scorePerQuestion));
                        log.debug("解析成功: 每题{}分", scorePerQuestion);
                        break;
                    }
                } catch (NumberFormatException | ArithmeticException e) {
                    log.warn("解析分数失败: {}", matcher.group());
                }
            }
        }

        return context;
    }

    /**
     * 清理分数上下文（用于新的文档解析）
     */
    public void clearScoreContext() {
        sectionScoreContext.clear();
        currentSectionHeader = null;
        log.debug("清理分数解析上下文");
    }

    /**
     * 获取当前分数上下文状态（用于调试）
     */
    public Map<String, BigDecimal> getCurrentScoreContext() {
        return new HashMap<>(sectionScoreContext);
    }

    /**
     * 从题目内容中解析分数
     */
    private BigDecimal parseScoreFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 匹配各种分数格式
        Pattern[] patterns = {
            Pattern.compile("\\((\\d+)分\\)"),           // (5分)
            Pattern.compile("（(\\d+)分）"),              // （5分）
            Pattern.compile("(\\d+)分题"),               // 5分题
            Pattern.compile("每题(\\d+)分"),             // 每题5分
            Pattern.compile("共(\\d+)分"),               // 共10分
            Pattern.compile("学生得分[：:]?\\s*(\\d+)\\s*分"), // 学生得分：5分（从答案中推断）
            Pattern.compile("得分[：:]?\\s*(\\d+)\\s*分"),     // 得分：5分
            Pattern.compile("满分[：:]?\\s*(\\d+)\\s*分"),     // 满分：10分
            Pattern.compile("总分[：:]?\\s*(\\d+)\\s*分"),     // 总分：15分
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                try {
                    BigDecimal score = new BigDecimal(matcher.group(1));
                    log.debug("从题目内容解析到分数: {} (匹配模式: {})", score, pattern.pattern());
                    return score;
                } catch (NumberFormatException e) {
                    log.warn("解析分数失败: {}", matcher.group(1));
                }
            }
        }

        return null;
    }

    /**
     * 从题目类型标题解析分数
     */
    private BigDecimal parseScoreFromSectionHeader(String sectionHeader) {
        if (sectionHeader == null || sectionHeader.trim().isEmpty()) {
            return null;
        }

        log.debug("解析题目类型标题分数: {}", sectionHeader);

        // 匹配题目类型标题中的分数格式
        Pattern[] patterns = {
            // "一 . 单选题 ( 共 25 题 ,25 分）" -> 支持空格格式
            Pattern.compile("\\(\\s*共\\s*(\\d+)\\s*题\\s*[,，]\\s*(\\d+)\\s*分\\s*[）)]"),
            // "六.论述题(共1题,15分）" -> 总分15，题数1，每题15分
            Pattern.compile("\\(共(\\d+)题[,，]\\s*(\\d+)分[）)]"),
            // "二、选择题（每题2分，共10题，20分）" -> 每题2分
            Pattern.compile("每题(\\d+)分"),
            // "三、填空题（共5题，每题3分，15分）" -> 每题3分
            Pattern.compile("每题(\\d+)分"),
            // "单选题(每题5分)" -> 每题5分
            Pattern.compile("\\(每题(\\d+)分\\)"),
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(sectionHeader);
            if (matcher.find()) {
                try {
                    if (pattern.pattern().contains("共.*题")) {
                        // 格式：共X题，Y分 -> 每题 Y/X 分
                        int questionCount = Integer.parseInt(matcher.group(1));
                        int totalScore = Integer.parseInt(matcher.group(2));
                        if (questionCount > 0) {
                            return new BigDecimal(totalScore).divide(new BigDecimal(questionCount));
                        }
                    } else {
                        // 直接的每题分数
                        return new BigDecimal(matcher.group(1));
                    }
                } catch (NumberFormatException | ArithmeticException e) {
                    log.warn("解析分数失败: {}", matcher.group());
                }
            }
        }

        return null;
    }

    /**
     * 使用AI智能解析分数
     */
    private BigDecimal parseScoreWithAI(String questionContent, String sectionHeader) {
        try {
            log.debug("开始AI智能分数解析");
            
            // 获取用户的默认AI配置
            Long userId = authService.getCurrentUser().getId();
            var configOpt = userAIConfigService.getUserDefaultAIConfig(userId);
            
            if (!configOpt.isPresent()) {
                log.debug("没有找到默认AI配置，使用默认分数");
                return getDefaultScore();
            }
            
            var aiConfig = configOpt.get();
            var aiClient = aiClientFactory.getClient(aiConfig.getProvider());
            
            // 构建AI提示词
            String prompt = buildScoreParsingPrompt(questionContent, sectionHeader);
            log.debug("AI分数解析提示词构建完成，长度: {}", prompt.length());
            
            // 调用AI
            var aiResponse = aiClient.chat(prompt, aiConfig);
            
            if (aiResponse.isSuccess()) {
                log.debug("AI调用成功，响应: {}", aiResponse.getContent());
                return extractScoreFromAIResponse(aiResponse.getContent());
            } else {
                log.warn("AI调用失败: {}", aiResponse.getErrorMessage());
                return getDefaultScore();
            }
            
        } catch (Exception e) {
            log.warn("AI分数解析失败: {}", e.getMessage());
            return getDefaultScore();
        }
    }

    /**
     * 构建AI分数解析提示词
     */
    private String buildScoreParsingPrompt(String questionContent, String sectionHeader) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的教育评估专家，请分析以下题目信息，准确判断该题目的分值。\n\n");
        
        prompt.append("=== 题目信息 ===\n");
        if (sectionHeader != null && !sectionHeader.trim().isEmpty()) {
            prompt.append("题目类型标题：").append(sectionHeader).append("\n");
        }
        prompt.append("题目内容：").append(questionContent).append("\n\n");
        
        prompt.append("=== 分值解析规则 ===\n");
        prompt.append("1. 优先级1：如果题目类型标题包含分数信息，请按以下方式计算：\n");
        prompt.append("   - \"共X题，Y分\" → 每题 Y/X 分\n");
        prompt.append("   - \"每题X分\" → 每题 X 分\n");
        prompt.append("   - \"单题X分\" → 每题 X 分\n");
        prompt.append("   - \"(X分)\" → 每题 X 分\n\n");
        
        prompt.append("2. 优先级2：如果题目内容中包含分数信息：\n");
        prompt.append("   - \"(X分)\"、\"（X分）\" → X 分\n");
        prompt.append("   - \"共X分\"、\"满分X分\" → X 分\n");
        prompt.append("   - \"学生得分：X分\" → 推断题目分值为 X 分\n\n");
        
        prompt.append("3. 优先级3：根据题目类型和复杂度判断：\n");
        prompt.append("   - 选择题（单选/多选）：2-5分\n");
        prompt.append("   - 判断题：1-3分\n");
        prompt.append("   - 填空题：3-5分\n");
        prompt.append("   - 简答题：8-15分\n");
        prompt.append("   - 论述题：15-25分\n");
        prompt.append("   - 计算题：10-20分\n");
        prompt.append("   - 编程题：15-30分\n");
        prompt.append("   - 案例分析：20-30分\n\n");
        
        prompt.append("=== 输出要求 ===\n");
        prompt.append("请严格按照以下JSON格式返回结果：\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 分值(数字),\n");
        prompt.append("  \"reasoning\": \"分析推理过程\"\n");
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("- 分值必须是正数\n");
        prompt.append("- 可以是小数，但建议保留1位小数\n");
        prompt.append("- 如果有明确的分数信息，严格按照信息计算\n");
        prompt.append("- 如果没有明确信息，根据题目复杂度和类型合理推断\n");
        prompt.append("- reasoning字段简要说明分值判断的依据\n");
        
        return prompt.toString();
    }

    /**
     * 从AI响应中提取分数
     */
    private BigDecimal extractScoreFromAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return getDefaultScore();
        }

        try {
            // 首先尝试解析JSON格式响应
            String jsonContent = extractJsonFromResponse(aiResponse);
            if (jsonContent != null) {
                return parseScoreFromJson(jsonContent);
            }
            
            // 如果不是JSON，尝试直接提取数字
            return parseScoreFromText(aiResponse);
            
        } catch (Exception e) {
            log.warn("解析AI响应失败: {}", e.getMessage());
            return getDefaultScore();
        }
    }
    
    /**
     * 从响应中提取JSON内容
     */
    private String extractJsonFromResponse(String response) {
        if (response == null) return null;
        
        // 查找JSON开始和结束位置
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return null;
    }
    
    /**
     * 从JSON中解析分数
     */
    private BigDecimal parseScoreFromJson(String jsonContent) {
        try {
            // 使用简单的JSON解析
            if (jsonContent.contains("\"score\"")) {
                Pattern pattern = Pattern.compile("\"score\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
                Matcher matcher = pattern.matcher(jsonContent);
                if (matcher.find()) {
                    BigDecimal score = new BigDecimal(matcher.group(1));
                    log.debug("从JSON中解析到分数: {}", score);
                    
                    // 提取reasoning信息用于日志
                    Pattern reasoningPattern = Pattern.compile("\"reasoning\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher reasoningMatcher = reasoningPattern.matcher(jsonContent);
                    if (reasoningMatcher.find()) {
                        log.debug("AI分析推理: {}", reasoningMatcher.group(1));
                    }
                    
                    return score;
                }
            }
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 从文本中解析分数
     */
    private BigDecimal parseScoreFromText(String text) {
        // 优先查找完整的分数表达式
        Pattern[] patterns = {
            Pattern.compile("分值[：:]?\\s*(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("分数[：:]?\\s*(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*分"),
            Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*$"), // 纯数字
            Pattern.compile("(\\d+(?:\\.\\d+)?)") // 任意数字
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    BigDecimal score = new BigDecimal(matcher.group(1));
                    log.debug("从文本中解析到分数: {}", score);
                    return score;
                } catch (NumberFormatException e) {
                    log.warn("无法解析数字: {}", matcher.group(1));
                }
            }
        }
        
        return getDefaultScore();
    }

    /**
     * 获取默认分数
     */
    private BigDecimal getDefaultScore() {
        return new BigDecimal("5"); // 默认5分，避免返回0
    }

    /**
     * 基于题目内容和类型获取智能默认分数
     */
    public BigDecimal getIntelligentDefaultScore(String questionContent, String sectionHeader) {
        // 首先根据段落标题判断
        if (sectionHeader != null) {
            String lower = sectionHeader.toLowerCase();
            if (lower.contains("选择题") || lower.contains("单选") || lower.contains("多选")) {
                return new BigDecimal("1");
            } else if (lower.contains("填空题") || lower.contains("填空")) {
                return new BigDecimal("2");
            } else if (lower.contains("判断题") || lower.contains("判断")) {
                return new BigDecimal("0.5");  // 判断题通常分值较低
            } else if (lower.contains("简答题") || lower.contains("简答")) {
                return new BigDecimal("4");
            } else if (lower.contains("论述题") || lower.contains("论述")) {
                return new BigDecimal("15");
            } else if (lower.contains("计算题") || lower.contains("计算")) {
                return new BigDecimal("10");
            } else if (lower.contains("分析题") || lower.contains("分析")) {
                return new BigDecimal("12");  // 分析题通常分值较高
            }
        }
        
        // 根据题目内容复杂度判断
        if (questionContent != null) {
            int contentLength = questionContent.length();
            String lower = questionContent.toLowerCase();
            
            // 选择题特征
            if (lower.contains("( )") || lower.contains("（ ）") || lower.matches(".*[A-D][.、].*")) {
                return new BigDecimal("3");
            }
            
            // 判断题特征
            if (lower.contains("正确") && lower.contains("错误") || 
                lower.contains("√") || lower.contains("×")) {
                return new BigDecimal("2");
            }
            
            // 填空题特征
            if (lower.contains("____") || lower.contains("填空")) {
                return new BigDecimal("4");
            }
            
            // 计算题特征
            if (lower.contains("计算") || lower.contains("求") || lower.contains("公式")) {
                return new BigDecimal("12");
            }
            
            // 根据内容长度判断复杂度
            if (contentLength > 200) {
                return new BigDecimal("15"); // 长题目，论述题
            } else if (contentLength > 100) {
                return new BigDecimal("10"); // 中等题目，简答题
            } else if (contentLength > 50) {
                return new BigDecimal("5");  // 短题目
            }
        }
        
        return new BigDecimal("5"); // 最终默认分数
    }

    /**
     * 判断题目类型 - 优化版本，更准确识别学习通格式
     */
    public String detectQuestionType(String questionContent, String sectionHeader) {
        log.debug("检测题目类型 - 段落标题: {}, 题目内容: {}", sectionHeader, 
                 questionContent != null && questionContent.length() > 50 ? questionContent.substring(0, 50) + "..." : questionContent);
        
        // 优先根据段落标题判断（学习通格式最可靠）
        if (sectionHeader != null && !sectionHeader.trim().isEmpty()) {
            String lower = sectionHeader.toLowerCase();
            log.debug("段落标题小写: {}", lower);
            
            if (lower.contains("选择题") || lower.contains("单选") || lower.contains("多选")) {
                log.debug("根据段落标题判断为选择题");
                return "选择题";
            } else if (lower.contains("判断题") || lower.contains("判断")) {
                log.debug("根据段落标题判断为判断题");
                return "判断题";
            } else if (lower.contains("填空题") || lower.contains("填空")) {
                log.debug("根据段落标题判断为填空题");
                return "填空题";
            } else if (lower.contains("简答题") || lower.contains("简答")) {
                log.debug("根据段落标题判断为简答题");
                return "简答题";
            } else if (lower.contains("论述题") || lower.contains("论述")) {
                log.debug("根据段落标题判断为论述题");
                return "论述题";
            } else if (lower.contains("计算题") || lower.contains("计算")) {
                log.debug("根据段落标题判断为计算题");
                return "计算题";
            } else if (lower.contains("分析题") || lower.contains("分析")) {
                log.debug("根据段落标题判断为分析题");
                return "分析题";
            }
        }

        // 从题目内容判断（需要更严格的规则）
        if (questionContent != null && !questionContent.trim().isEmpty()) {
            String content = questionContent.trim();
            String lower = content.toLowerCase();
            log.debug("题目内容小写片段: {}", lower.length() > 100 ? lower.substring(0, 100) + "..." : lower);
            
            // 过滤明显不是题目的内容
            if (isNotValidQuestion(content)) {
                log.debug("检测到无效题目内容，使用默认类型");
                return "简答题";
            }
            
            // 1. 选择题特征（必须包含问号和选项括号）
            if ((lower.contains("?") || lower.contains("？")) && 
                (lower.contains("( )") || lower.contains("（ ）"))) {
                log.debug("根据题目内容判断为选择题");
                return "选择题";
            }
            
            // 2. 判断题特征（简化逻辑，更准确识别学习通格式）
            if ((lower.contains("( )") || lower.contains("（ ）")) && 
                !lower.contains("?") && !lower.contains("？")) {
                // 学习通判断题通常以"( )"结尾，不包含问号
                log.debug("根据题目内容判断为判断题");
                return "判断题";
            }
            
            // 3. 填空题特征
            if (lower.contains("____") || lower.contains("_____") || 
                lower.contains("填空") || lower.contains("空白")) {
                log.debug("根据题目内容判断为填空题");
                return "填空题";
            }
            
            // 4. 计算题特征
            if (lower.contains("计算") || lower.contains("求") || 
                lower.contains("解：") || lower.contains("公式") ||
                (lower.contains("已知") && lower.contains("求解"))) {
                log.debug("根据题目内容判断为计算题");
                return "计算题";
            }
            
            // 5. 论述题特征
            if (lower.contains("论述") || lower.contains("阐述") || 
                lower.contains("试述") || lower.contains("简述") ||
                lower.contains("分析") || lower.contains("评价") ||
                content.length() > 100) { // 长题目通常是论述题
                log.debug("根据题目内容判断为论述题");
                return "论述题";
            }
        }

        log.debug("无法明确判断题目类型，使用默认类型：简答题");
        return "简答题"; // 默认类型
    }
    
    /**
     * 检查是否不是有效的题目内容
     */
    private boolean isNotValidQuestion(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = content.trim();
        
        // 过滤IP地址
        if (trimmed.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
            return true;
        }
        
        // 过滤纯数字序列
        if (trimmed.matches("^\\d+(\\.\\d+)*$")) {
            return true;
        }
        
        // 过滤选项行（A、B、C、D开头）
        if (trimmed.matches("^[A-Z]、\\s*.*") && trimmed.length() < 100) {
            return true;
        }
        
        // 过滤学生信息相关行
        if (trimmed.contains("学生答案") || trimmed.contains("学生得分") || 
            trimmed.contains("正确答案") || trimmed.contains("批语") ||
            trimmed.contains("答题人") || trimmed.contains("学号") ||
            trimmed.contains("提交时间") || trimmed.contains("ip")) {
            return true;
        }
        
        // 过滤过短的内容
        if (trimmed.length() < 5) {
            return true;
        }
        
        // 过滤特殊格式的非题目内容
        if (trimmed.matches("^\\d+\\s*分$") || // 单纯的分数
            trimmed.matches("^[一二三四五六七八九十]+[.、]$")) { // 单纯的序号
            return true;
        }
        
        return false;
    }
} 
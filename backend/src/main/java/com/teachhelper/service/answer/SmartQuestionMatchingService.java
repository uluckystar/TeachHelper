package com.teachhelper.service.answer;

import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionBank;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmartQuestionMatchingService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionScoreParsingService questionScoreParsingService;
    
    @Autowired
    private com.teachhelper.repository.ExamRepository examRepository;
    
    private static final double SIMILARITY_THRESHOLD = 0.7; // 相似度阈值
    private static final int MAX_SEARCH_RESULTS = 20;

    /**
     * 清理分数解析上下文（用于新文档解析）
     */
    public void clearScoreContext() {
        questionScoreParsingService.clearScoreContext();
        log.debug("清理题目匹配服务的分数解析上下文");
    }

    /**
     * 智能匹配题目，优先匹配现有题目，避免创建重复题目
     * 
     * @param qa 题目答案数据
     * @param examId 目标考试ID（如果指定）
     * @param questionBank 默认题库
     * @return 匹配或创建的题目
     */
    public Question smartMatchQuestion(StudentAnswerImportData.QuestionAnswer qa, Long examId, QuestionBank questionBank) {
        String questionContent = qa.getQuestionContent();
        if (questionContent == null || questionContent.trim().isEmpty()) {
            return null;
        }

        // 1. 提取题目的关键信息
        QuestionInfo questionInfo = extractQuestionInfo(questionContent);
        log.debug("提取的题目信息: 编号={}, 核心内容={}", questionInfo.questionNumber, questionInfo.coreContent);

        // 2. 优先在指定考试中查找匹配题目
        if (examId != null) {
            Question examMatch = findMatchInExam(questionInfo, examId);
            if (examMatch != null) {
                log.info("在考试{}中找到匹配题目: {} (ID: {})", examId, examMatch.getTitle(), examMatch.getId());
                return examMatch;
            }
        }

        // 3. 在题库中查找相似题目
        Question similarQuestion = findSimilarQuestion(questionInfo);
        if (similarQuestion != null) {
            log.info("找到相似题目: {} (ID: {})", similarQuestion.getTitle(), similarQuestion.getId());
            
            // 如果指定了考试ID，将题目关联到考试
            if (examId != null && (similarQuestion.getExam() == null || !Objects.equals(similarQuestion.getExam().getId(), examId))) {
                return cloneQuestionForExam(similarQuestion, examId);
            }
            
            return similarQuestion;
        }

        // 4. 创建新题目
        return createNewQuestion(qa, examId, questionBank);
    }

    /**
     * 提取题目的关键信息
     */
    private QuestionInfo extractQuestionInfo(String content) {
        QuestionInfo info = new QuestionInfo();
        
        // 提取题目编号
        info.questionNumber = extractQuestionNumber(content);
        
        // 提取题目核心内容（去除编号、格式等干扰信息）
        info.coreContent = extractCoreContent(content);
        
        // 提取关键词
        info.keywords = extractKeywords(info.coreContent);
        
        // 计算内容指纹（用于快速比较）
        info.contentHash = calculateContentHash(info.coreContent);
        
        return info;
    }

    /**
     * 提取题目编号
     */
    private String extractQuestionNumber(String content) {
        // 匹配各种题目编号格式
        Pattern[] patterns = {
            Pattern.compile("^(\\d+)[.、]"),          // 1. 2. 3.
            Pattern.compile("^\\((\\d+)\\)"),         // (1) (2) (3)
            Pattern.compile("第(\\d+)题"),            // 第1题 第2题
            Pattern.compile("([一二三四五六七八九十]+)[.、]"), // 一、二、三、
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content.trim());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }

    /**
     * 提取题目核心内容
     */
    private String extractCoreContent(String content) {
        String cleaned = content.trim();
        
        // 移除题目编号前缀（如 "题目 1: "）
        cleaned = cleaned.replaceFirst("^题目\\s*\\d+[：:]\\s*", "");
        
        // 移除题目编号
        cleaned = cleaned.replaceFirst("^\\d+[.、]\\s*", "");
        cleaned = cleaned.replaceFirst("^\\(\\d+\\)\\s*", "");
        cleaned = cleaned.replaceFirst("^[一二三四五六七八九十]+[.、]\\s*", "");
        cleaned = cleaned.replaceFirst("第\\d+题[：:]?\\s*", "");
        
        // 移除学习通特有的标记（更全面的匹配）
        cleaned = cleaned.replaceAll("学生得分[：:]?\\s*\\d+\\s*分", "");
        cleaned = cleaned.replaceAll("得分[：:]?\\s*\\d+\\s*分", "");
        cleaned = cleaned.replaceAll("分数[：:]?\\s*\\d+\\s*分", "");
        cleaned = cleaned.replaceAll("\\s*\\([）]\\s*", "");
        
        // 移除多余的空白字符
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }

    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String content) {
        Set<String> keywords = new HashSet<>();
        
        // 提取中文词汇（简单的基于长度的分词）
        String[] words = content.split("[\\s，。！？；：、]+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 8) {
                keywords.add(word.trim());
            }
        }
        
        return keywords;
    }

    /**
     * 计算内容哈希（简单的字符串哈希）
     */
    private int calculateContentHash(String content) {
        return content.replaceAll("\\s", "").hashCode();
    }

    /**
     * 在指定考试中查找匹配题目
     */
    private Question findMatchInExam(QuestionInfo questionInfo, Long examId) {
        try {
            // 首先尝试精确匹配
            List<Question> examQuestions = questionRepository.findByExamId(examId);
            
            for (Question question : examQuestions) {
                double similarity = calculateSimilarity(questionInfo, question);
                if (similarity >= 0.9) { // 高相似度匹配
                    return question;
                }
            }
            
            // 如果有题目编号，尝试按编号匹配
            if (questionInfo.questionNumber != null) {
                for (Question question : examQuestions) {
                    String existingNumber = extractQuestionNumber(question.getContent());
                    if (questionInfo.questionNumber.equals(existingNumber)) {
                        double similarity = calculateSimilarity(questionInfo, question);
                        if (similarity >= SIMILARITY_THRESHOLD) {
                            return question;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("在考试{}中查找题目时出错: {}", examId, e.getMessage());
        }
        
        return null;
    }

    /**
     * 查找相似题目
     */
    private Question findSimilarQuestion(QuestionInfo questionInfo) {
        try {
            // 使用关键词搜索
            String searchKeyword = questionInfo.coreContent.length() > 30 
                ? questionInfo.coreContent.substring(0, 30) 
                : questionInfo.coreContent;
                
            Page<Question> searchResults = questionRepository.searchQuestionsWithFilters(
                searchKeyword, null, null, null, 
                PageRequest.of(0, MAX_SEARCH_RESULTS));
            
            Question bestMatch = null;
            double bestSimilarity = 0;
            
            for (Question candidate : searchResults.getContent()) {
                double similarity = calculateSimilarity(questionInfo, candidate);
                if (similarity >= SIMILARITY_THRESHOLD && similarity > bestSimilarity) {
                    bestMatch = candidate;
                    bestSimilarity = similarity;
                }
            }
            
            if (bestMatch != null) {
                log.debug("找到最佳匹配题目，相似度: {}", bestSimilarity);
            }
            
            return bestMatch;
            
        } catch (Exception e) {
            log.warn("搜索相似题目时出错: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算题目相似度
     */
    private double calculateSimilarity(QuestionInfo questionInfo, Question question) {
        // 1. 内容哈希完全匹配
        QuestionInfo existingInfo = extractQuestionInfo(question.getContent());
        if (questionInfo.contentHash == existingInfo.contentHash) {
            return 1.0;
        }
        
        // 2. 关键词匹配度
        Set<String> intersection = new HashSet<>(questionInfo.keywords);
        intersection.retainAll(existingInfo.keywords);
        
        Set<String> union = new HashSet<>(questionInfo.keywords);
        union.addAll(existingInfo.keywords);
        
        double keywordSimilarity = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
        
        // 3. 编辑距离（简化版本）
        double contentSimilarity = calculateEditDistanceSimilarity(
            questionInfo.coreContent, existingInfo.coreContent);
        
        // 4. 综合相似度
        return (keywordSimilarity * 0.4 + contentSimilarity * 0.6);
    }

    /**
     * 计算编辑距离相似度
     */
    private double calculateEditDistanceSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int editDistance = calculateEditDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLen;
    }

    /**
     * 计算编辑距离（简化版本）
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        // 为了性能考虑，限制字符串长度
        if (m > 100) s1 = s1.substring(0, 100);
        if (n > 100) s2 = s2.substring(0, 100);
        
        m = s1.length();
        n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        
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
     * 为考试克隆题目
     */
    private Question cloneQuestionForExam(Question originalQuestion, Long examId) {
        Question clonedQuestion = new Question();
        
        clonedQuestion.setTitle(originalQuestion.getTitle());
        clonedQuestion.setContent(originalQuestion.getContent());
        clonedQuestion.setQuestionType(originalQuestion.getQuestionType());
        clonedQuestion.setMaxScore(originalQuestion.getMaxScore());
        clonedQuestion.setCreatedBy(originalQuestion.getCreatedBy());
        clonedQuestion.setSourceType("克隆自题目ID: " + originalQuestion.getId());
        clonedQuestion.setIsConfirmed(originalQuestion.getIsConfirmed());
        clonedQuestion.setQuestionBank(originalQuestion.getQuestionBank());
        
        // 设置考试ID
        if (examId != null) {
            examRepository.findById(examId).ifPresent(clonedQuestion::setExam);
        }
        
        Question savedQuestion = questionRepository.save(clonedQuestion);
        log.info("为考试{}克隆题目: {} (新ID: {}, 原ID: {})", 
            examId, savedQuestion.getTitle(), savedQuestion.getId(), originalQuestion.getId());
        
        return savedQuestion;
    }

    /**
     * 创建新题目
     */
    private Question createNewQuestion(StudentAnswerImportData.QuestionAnswer qa, Long examId, QuestionBank questionBank) {
        try {
            Question newQuestion = new Question();
            
            // 设置题目标题（从内容中提取前50个字符）
            final String title = qa.getQuestionContent().length() > 50 
                ? qa.getQuestionContent().substring(0, 47) + "..." 
                : qa.getQuestionContent();
            newQuestion.setTitle(title);
            
            // 设置题目内容
            newQuestion.setContent(qa.getQuestionContent());
            
            // 智能解析题目分数（传递段落标题信息）
            BigDecimal score = questionScoreParsingService.parseQuestionScore(qa.getQuestionContent(), qa.getSectionHeader());
            
            // 确保分数大于0，否则使用默认分数
            if (score == null || score.compareTo(BigDecimal.ZERO) <= 0) {
                score = questionScoreParsingService.getIntelligentDefaultScore(qa.getQuestionContent(), qa.getSectionHeader());
                log.warn("题目分数解析失败或为0，使用智能默认分数: {}", score);
            }
            
            final BigDecimal finalScore = score;
            newQuestion.setMaxScore(finalScore);
            
            // 智能判断题目类型（使用段落标题信息）
            String questionTypeStr = questionScoreParsingService.detectQuestionType(qa.getQuestionContent(), qa.getSectionHeader());
            final QuestionType questionType = mapStringToQuestionType(questionTypeStr);
            newQuestion.setQuestionType(questionType);
            
            log.debug("题目类型检测: 段落标题={}, 检测结果={}, 映射类型={}", 
                     qa.getSectionHeader(), questionTypeStr, questionType);
            
            // 设置创建者为系统用户（ID为1，通常是admin）
            newQuestion.setCreatedBy(1L);
            
            // 设置题目来源
            newQuestion.setSourceType("学习通智能导入");
            
            // 设置为未确认状态，需要教师确认
            newQuestion.setIsConfirmed(false);
            
            // 设置题库
            newQuestion.setQuestionBank(questionBank);
            
            // 如果指定了考试ID，关联到考试；否则设置为题库题目
            if (examId != null) {
                Optional<com.teachhelper.entity.Exam> examOpt = examRepository.findById(examId);
                if (examOpt.isPresent()) {
                    newQuestion.setExam(examOpt.get());
                    log.info("✅ 创建题目并关联到考试{}: {} (分数: {}, 类型: {})", examId, title, finalScore, questionType);
                } else {
                    log.warn("⚠️ 考试ID {}不存在，题目将创建为题库题目", examId);
                    newQuestion.setExam(null);
                }
            } else {
                newQuestion.setExam(null);
                log.info("创建题库题目: {} (分数: {}, 类型: {})", title, finalScore, questionType);
            }
            
            // 保存题目并刷新，确保获得ID
            Question savedQuestion = questionRepository.save(newQuestion);
            questionRepository.flush(); // 强制刷新到数据库
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("创建题目失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 根据题目类型获取默认分数
     */
    private BigDecimal getDefaultScoreByType(String sectionHeader) {
        if (sectionHeader != null) {
            String lower = sectionHeader.toLowerCase();
            if (lower.contains("选择题") || lower.contains("单选") || lower.contains("多选")) {
                return new BigDecimal("3");
            } else if (lower.contains("填空题") || lower.contains("填空")) {
                return new BigDecimal("4");
            } else if (lower.contains("判断题") || lower.contains("判断")) {
                return new BigDecimal("2");
            } else if (lower.contains("简答题") || lower.contains("简答")) {
                return new BigDecimal("10");
            } else if (lower.contains("论述题") || lower.contains("论述")) {
                return new BigDecimal("15");
            } else if (lower.contains("计算题") || lower.contains("计算")) {
                return new BigDecimal("12");
            }
        }
        
        // 默认分数
        return new BigDecimal("5");
    }

    /**
     * 将字符串类型映射为QuestionType枚举
     */
    private QuestionType mapStringToQuestionType(String typeStr) {
        if (typeStr == null) {
            return QuestionType.SHORT_ANSWER;
        }
        
        switch (typeStr) {
            case "选择题":
                return QuestionType.SINGLE_CHOICE;
            case "填空题":
                return QuestionType.FILL_BLANK;
            case "简答题":
                return QuestionType.SHORT_ANSWER;
            case "论述题":
                return QuestionType.ESSAY;
            case "计算题":
                return QuestionType.SHORT_ANSWER; // 计算题归类为简答题
            case "判断题":
                return QuestionType.TRUE_FALSE;
            default:
                return QuestionType.SHORT_ANSWER;
        }
    }

    /**
     * 题目信息类
     */
    private static class QuestionInfo {
        String questionNumber;      // 题目编号
        String coreContent;         // 核心内容
        Set<String> keywords;       // 关键词
        int contentHash;           // 内容哈希
    }
} 
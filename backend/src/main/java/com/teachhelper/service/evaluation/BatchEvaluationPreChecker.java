package com.teachhelper.service.evaluation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.service.student.StudentAnswerService;

/**
 * 批量评估预检查服务
 * 在创建批量评估任务前检查答案状态，提供用户友好的反馈
 */
@Service
public class BatchEvaluationPreChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchEvaluationPreChecker.class);
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    /**
     * 批量评估预检查结果
     */
    public static class PreCheckResult {
        private boolean canProceed;
        private String message;
        private int totalAnswers;
        private int evaluatedAnswers;
        private int unevaluatedAnswers;
        private String suggestion;
        private String warningLevel; // INFO, WARNING, ERROR
        
        public PreCheckResult(boolean canProceed, String message, int totalAnswers, 
                             int evaluatedAnswers, int unevaluatedAnswers, String suggestion, String warningLevel) {
            this.canProceed = canProceed;
            this.message = message;
            this.totalAnswers = totalAnswers;
            this.evaluatedAnswers = evaluatedAnswers;
            this.unevaluatedAnswers = unevaluatedAnswers;
            this.suggestion = suggestion;
            this.warningLevel = warningLevel;
        }
        
        // Getters
        public boolean isCanProceed() { return canProceed; }
        public String getMessage() { return message; }
        public int getTotalAnswers() { return totalAnswers; }
        public int getEvaluatedAnswers() { return evaluatedAnswers; }
        public int getUnevaluatedAnswers() { return unevaluatedAnswers; }
        public String getSuggestion() { return suggestion; }
        public String getWarningLevel() { return warningLevel; }
    }
    
    /**
     * 检查批量评估任务的可执行性
     */
    public PreCheckResult checkBatchEvaluation(Map<String, Object> config) {
        logger.info("开始预检查批量评估任务，配置: {}", config);
        
        try {
            // 获取答案ID列表
            List<Long> answerIds = extractAnswerIds(config);
            
            if (answerIds.isEmpty()) {
                return new PreCheckResult(
                    false,
                    "未找到任何答案",
                    0, 0, 0,
                    "请检查考试是否存在学生答案，或者选择的考试/题目是否正确",
                    "ERROR"
                );
            }
            
            // 获取答案详情
            List<StudentAnswer> answers = answerIds.stream()
                .map(id -> studentAnswerService.getAnswerById(id))
                .filter(answer -> answer != null)
                .collect(Collectors.toList());
            
            int totalAnswers = answers.size();
            int evaluatedAnswers = (int) answers.stream()
                .filter(answer -> answer.isEvaluated())
                .count();
            int unevaluatedAnswers = totalAnswers - evaluatedAnswers;
            
            logger.info("答案统计 - 总数: {}, 已评估: {}, 未评估: {}", 
                       totalAnswers, evaluatedAnswers, unevaluatedAnswers);
            
            // 根据评估状态返回不同的结果
            if (unevaluatedAnswers == 0) {
                return new PreCheckResult(
                    true,  // 改为 true，允许继续进行重新评估
                    String.format("所有答案都已经被评估过了（共 %d 个答案）", totalAnswers),
                    totalAnswers, evaluatedAnswers, unevaluatedAnswers,
                    "所有答案已完成评估，您可以创建重新评估任务来改进评分结果",
                    "WARNING"
                );
            } else if (evaluatedAnswers > 0) {
                double evaluatedPercentage = (double) evaluatedAnswers / totalAnswers * 100;
                return new PreCheckResult(
                    true,
                    String.format("发现 %d 个答案中有 %d 个已被评估（%.1f%%），%d 个未评估", 
                                 totalAnswers, evaluatedAnswers, evaluatedPercentage, unevaluatedAnswers),
                    totalAnswers, evaluatedAnswers, unevaluatedAnswers,
                    "系统将只评估未评估的答案。如需重新评估所有答案，请使用「重新评估」功能",
                    "INFO"
                );
            } else {
                return new PreCheckResult(
                    true,
                    String.format("找到 %d 个未评估的答案，准备进行批量评估", unevaluatedAnswers),
                    totalAnswers, evaluatedAnswers, unevaluatedAnswers,
                    "所有答案都未被评估，可以正常进行批量评估",
                    "INFO"
                );
            }
            
        } catch (Exception e) {
            logger.error("预检查过程中发生异常: {}", e.getMessage(), e);
            return new PreCheckResult(
                false,
                "预检查失败: " + e.getMessage(),
                0, 0, 0,
                "请检查配置是否正确，或联系系统管理员",
                "ERROR"
            );
        }
    }
    
    /**
     * 从配置中提取答案ID列表（复用BatchEvaluationExecutorService的逻辑）
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractAnswerIds(Map<String, Object> config) {
        logger.info("开始解析配置获取答案ID列表，配置: {}", config);
        
        // 检查是否直接包含answerIds
        if (config.containsKey("answerIds")) {
            logger.info("发现直接的answerIds配置");
            Object answerIdsObj = config.get("answerIds");
            if (answerIdsObj instanceof List) {
                List<?> rawList = (List<?>) answerIdsObj;
                List<Long> answerIds = rawList.stream()
                    .map(id -> {
                        if (id instanceof Number number) {
                            return number.longValue();
                        } else if (id instanceof String string) {
                            return Long.parseLong(string);
                        }
                        return null;
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
                logger.info("从answerIds解析到 {} 个答案ID", answerIds.size());
                return answerIds;
            }
        }
        
        // 检查是否按题目ID获取答案
        if (config.containsKey("questionId")) {
            logger.info("发现questionId配置");
            Long questionId = extractLongValue(config.get("questionId"));
            if (questionId != null) {
                boolean evaluateAll = Boolean.TRUE.equals(config.get("evaluateAll"));
                if (evaluateAll) {
                    List<Long> answerIds = studentAnswerService.getAnswersByQuestionId(questionId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .collect(Collectors.toList());
                    logger.info("从题目ID {} 获取到 {} 个答案", questionId, answerIds.size());
                    return answerIds;
                } else {
                    List<Long> answerIds = studentAnswerService.getUnevaluatedAnswerIdsByQuestionId(questionId);
                    logger.info("从题目ID {} 获取到 {} 个未评估答案", questionId, answerIds.size());
                    return answerIds;
                }
            }
        }
        
        // 检查是否按考试ID获取答案（单个）
        if (config.containsKey("examId")) {
            logger.info("发现examId配置");
            Long examId = extractLongValue(config.get("examId"));
            if (examId != null) {
                boolean evaluateAll = Boolean.TRUE.equals(config.get("evaluateAll"));
                if (evaluateAll) {
                    List<Long> answerIds = studentAnswerService.getAnswersByExamId(examId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .collect(Collectors.toList());
                    logger.info("从考试ID {} 获取到 {} 个答案", examId, answerIds.size());
                    return answerIds;
                } else {
                    List<Long> answerIds = studentAnswerService.getUnevaluatedAnswersByExamId(examId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .collect(Collectors.toList());
                    logger.info("从考试ID {} 获取到 {} 个未评估答案", examId, answerIds.size());
                    return answerIds;
                }
            }
        }
        
        // 检查是否按考试ID列表获取答案（多个）
        if (config.containsKey("examIds")) {
            logger.info("发现examIds配置");
            Object examIdsObj = config.get("examIds");
            if (examIdsObj instanceof List) {
                List<?> rawList = (List<?>) examIdsObj;
                List<Long> examIds = rawList.stream()
                    .map(id -> {
                        if (id instanceof Number number) {
                            return number.longValue();
                        } else if (id instanceof String string) {
                            return Long.parseLong(string);
                        }
                        return null;
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
                
                logger.info("解析到 {} 个考试ID: {}", examIds.size(), examIds);
                
                List<Long> allAnswerIds = new java.util.ArrayList<>();
                boolean evaluateAll = Boolean.TRUE.equals(config.get("evaluateAll"));
                logger.info("是否评估所有答案: {}", evaluateAll);
                
                for (Long examId : examIds) {
                    logger.info("开始处理考试ID: {}", examId);
                    List<Long> answerIds;
                    if (evaluateAll) {
                        // 获取考试的所有答案
                        logger.info("获取考试 {} 的所有答案", examId);
                        answerIds = studentAnswerService.getAnswersByExamId(examId)
                            .stream()
                            .map(StudentAnswer::getId)
                            .collect(Collectors.toList());
                        logger.info("从考试ID {} 获取到 {} 个答案", examId, answerIds.size());
                    } else {
                        // 只获取未评估的答案
                        logger.info("获取考试 {} 的未评估答案", examId);
                        List<StudentAnswer> allAnswersForExam = studentAnswerService.getAnswersByExamId(examId);
                        logger.info("考试 {} 总共有 {} 个答案", examId, allAnswersForExam.size());
                        
                        long evaluatedCount = allAnswersForExam.stream().filter(StudentAnswer::isEvaluated).count();
                        long unevaluatedCount = allAnswersForExam.size() - evaluatedCount;
                        logger.info("其中已评估: {}, 未评估: {}", evaluatedCount, unevaluatedCount);
                        
                        answerIds = studentAnswerService.getUnevaluatedAnswersByExamId(examId)
                            .stream()
                            .map(StudentAnswer::getId)
                            .collect(Collectors.toList());
                        logger.info("从考试ID {} 获取到 {} 个未评估答案", examId, answerIds.size());
                    }
                    allAnswerIds.addAll(answerIds);
                    logger.info("当前累计答案数: {}", allAnswerIds.size());
                }
                
                logger.info("总共获取到 {} 个答案ID", allAnswerIds.size());
                return allAnswerIds;
            }
        }
        
        logger.warn("未找到有效的配置来获取答案ID列表");
        return List.of();
    }
    
    /**
     * 从对象中提取Long值
     */
    private Long extractLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        } else if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

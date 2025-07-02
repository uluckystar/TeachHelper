package com.teachhelper.service.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;

import com.teachhelper.ai.AIEvaluationService;
import com.teachhelper.entity.StudentAnswer;

import com.teachhelper.entity.EvaluationType;
import com.teachhelper.service.student.StudentAnswerService;
import com.teachhelper.service.exam.ExamService;


/**
 * 批量评估执行引擎
 * 负责实际执行批量AI评估任务
 */
@Service
public class BatchEvaluationExecutorService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchEvaluationExecutorService.class);
    
    @Value("${app.evaluation.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;

    private Semaphore evaluationSemaphore;
    
    @Autowired
    private AIEvaluationService aiEvaluationService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    @Qualifier("securityContextTaskExecutor")
    private Executor securityContextTaskExecutor;
    
    @PostConstruct
    public void init() {
        // 限制并发数在1到200之间，防止过高导致系统不稳定
        int concurrency = Math.max(1, Math.min(maxConcurrentTasks, 200));
        this.evaluationSemaphore = new Semaphore(concurrency);
        logger.info("BatchEvaluationExecutorService initialized with concurrency: {}", concurrency);
    }
    
    /**
     * 异步执行批量评估任务
     */
    @Async("securityContextTaskExecutor")
    public CompletableFuture<Void> executeBatchEvaluationTask(String taskId, Map<String, Object> config, TaskProgressCallback callback) {
        logger.info("开始执行批量评估任务: {}", taskId);
        
        try {
            // 更新任务状态为运行中
            callback.updateTaskProgress(taskId, 0, 0, "RUNNING");
            callback.addTaskLog(taskId, "INFO", "开始执行批量评估任务");
            
            // 根据配置获取答案ID列表
            List<Long> answerIds = extractAnswerIds(config);
            
            if (answerIds.isEmpty()) {
                callback.updateTaskProgress(taskId, 0, 0, "FAILED");
                callback.addTaskLog(taskId, "ERROR", "没有找到需要评估的答案");
                return CompletableFuture.completedFuture(null);
            }
            
            logger.info("找到 {} 个答案需要评估", answerIds.size());
            callback.addTaskLog(taskId, "INFO", "找到 " + answerIds.size() + " 个答案需要评估");
            
            // 执行批量评估
            return executeBatchEvaluation(taskId, answerIds, config, callback);
            
        } catch (Exception e) {
            logger.error("批量评估任务执行失败: {}", e.getMessage(), e);
            callback.updateTaskProgress(taskId, 0, 0, "FAILED");
            callback.addTaskLog(taskId, "ERROR", "任务执行失败: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * 从配置中提取答案ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractAnswerIds(Map<String, Object> config) {
        logger.info("开始解析配置获取答案ID列表，配置: {}", config);

        // 优先处理单个答案评估
        if (config.containsKey("answerId")) {
            Long answerId = extractLongValue(config.get("answerId"));
            if (answerId != null) {
                logger.info("从 'answerId' 配置中提取到单个答案ID: {}", answerId);
                return List.of(answerId);
            }
        }
        
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
                    .toList();
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
                boolean revaluateOnly = Boolean.TRUE.equals(config.get("revaluateOnly"));
                
                if (revaluateOnly) {
                    // 只重新评估已评估的答案
                    List<Long> answerIds = studentAnswerService.getAnswersByQuestionId(questionId)
                        .stream()
                        .filter(StudentAnswer::isEvaluated)
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从题目ID {} 获取到 {} 个已评估答案（重新评阅）", questionId, answerIds.size());
                    return answerIds;
                } else if (evaluateAll) {
                    // 获取题目的所有答案
                    List<Long> answerIds = studentAnswerService.getAnswersByQuestionId(questionId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从题目ID {} 获取到 {} 个答案", questionId, answerIds.size());
                    return answerIds;
                } else {
                    // 只获取未评估的答案
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
                boolean revaluateOnly = Boolean.TRUE.equals(config.get("revaluateOnly"));
                
                if (revaluateOnly) {
                    // 只重新评估已评估的答案
                    List<Long> answerIds = studentAnswerService.getAnswersByExamId(examId)
                        .stream()
                        .filter(StudentAnswer::isEvaluated)
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从考试ID {} 获取到 {} 个已评估答案（重新评阅）", examId, answerIds.size());
                    return answerIds;
                } else if (evaluateAll) {
                    // 获取考试的所有答案
                    List<Long> answerIds = studentAnswerService.getAnswersByExamId(examId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从考试ID {} 获取到 {} 个答案", examId, answerIds.size());
                    return answerIds;
                } else {
                    // 只获取未评估的答案
                    List<Long> answerIds = studentAnswerService.getUnevaluatedAnswersByExamId(examId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从考试ID {} 获取到 {} 个未评估答案", examId, answerIds.size());
                    return answerIds;
                }
            }
        }
        
        // 检查是否按考试ID列表获取答案（多个）
        if (config.containsKey("examIds")) {
            logger.info("发现examIds配置");
            @SuppressWarnings("unchecked")
            List<Long> examIds = (List<Long>) config.get("examIds");
            if (examIds != null && !examIds.isEmpty()) {
                List<Long> allAnswerIds = new ArrayList<>();
                boolean evaluateAll = Boolean.TRUE.equals(config.get("evaluateAll"));
                boolean revaluateOnly = Boolean.TRUE.equals(config.get("revaluateOnly"));
                
                for (Long examId : examIds) {
                    if (revaluateOnly) {
                        // 只重新评估已评估的答案
                        List<Long> examAnswerIds = studentAnswerService.getAnswersByExamId(examId)
                            .stream()
                            .filter(StudentAnswer::isEvaluated)
                            .map(StudentAnswer::getId)
                            .toList();
                        allAnswerIds.addAll(examAnswerIds);
                    } else if (evaluateAll) {
                        List<Long> examAnswerIds = studentAnswerService.getAnswersByExamId(examId)
                            .stream()
                            .map(StudentAnswer::getId)
                            .toList();
                        allAnswerIds.addAll(examAnswerIds);
                    } else {
                        List<Long> examAnswerIds = studentAnswerService.getUnevaluatedAnswersByExamId(examId)
                            .stream()
                            .map(StudentAnswer::getId)
                            .toList();
                        allAnswerIds.addAll(examAnswerIds);
                    }
                }
                logger.info("从 {} 个考试获取到 {} 个答案{}", examIds.size(), allAnswerIds.size(), 
                    revaluateOnly ? "（重新评阅）" : (evaluateAll ? "（全部）" : "（未评估）"));
                return allAnswerIds;
            }
        }
        
        // 检查是否按学生ID和考试ID获取答案
        if (config.containsKey("studentId") && config.containsKey("examId")) {
            logger.info("发现studentId和examId配置");
            Long studentId = extractLongValue(config.get("studentId"));
            Long examId = extractLongValue(config.get("examId"));
            if (studentId != null && examId != null) {
                boolean evaluateAll = Boolean.TRUE.equals(config.get("evaluateAll"));
                if (evaluateAll) {
                    // 获取该学生在该考试中的所有答案
                    List<Long> answerIds = studentAnswerService.getStudentAnswersInExam(examId, studentId)
                        .stream()
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从学生ID {} 在考试ID {} 中获取到 {} 个答案", studentId, examId, answerIds.size());
                    return answerIds;
                } else {
                    // 只获取该学生在该考试中的未评估答案
                    List<Long> answerIds = studentAnswerService.getStudentAnswersInExam(examId, studentId)
                        .stream()
                        .filter(answer -> !answer.isEvaluated())
                        .map(StudentAnswer::getId)
                        .toList();
                    logger.info("从学生ID {} 在考试ID {} 中获取到 {} 个未评估答案", studentId, examId, answerIds.size());
                    return answerIds;
                }
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
    
    /**
     * 执行批量评估的核心逻辑
     */
    private CompletableFuture<Void> executeBatchEvaluation(String taskId, List<Long> answerIds, Map<String, Object> config, TaskProgressCallback callback) {
        
        final int totalAnswers = answerIds.size();
        final AtomicInteger processedCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        // 用于收集详细的评估结果
        List<Map<String, Object>> detailedResults = Collections.synchronizedList(new ArrayList<>());
        
        logger.info("开始评估 {} 个答案，并发数: {}", totalAnswers, maxConcurrentTasks);
        callback.addTaskLog(taskId, "INFO", "开始批量评估，总计 " + totalAnswers + " 个答案，并发数: " + maxConcurrentTasks);
        
        // 获取当前SecurityContext以传播到并行线程
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        
        Long currentUserId = null;
        String currentUsername = null;
        
        if (config != null) {
            Object configUserId = config.get("userId");
            Object configUsername = config.get("username");
            
            if (configUserId != null) {
                currentUserId = extractLongValue(configUserId);
            }
            
            if (configUsername != null) {
                currentUsername = configUsername.toString();
            }
        }
        
        if (currentUsername == null && authentication != null && authentication.getName() != null) {
            currentUsername = authentication.getName();
        }
        
        if (currentUsername == null) {
            currentUsername = "system";
        }
        
        final Long evaluatorUserId = currentUserId;
        final String evaluatorUsername = currentUsername;
        
        List<StudentAnswer> answers;
        try {
            answers = studentAnswerService.getAnswersByIdsWithFetch(answerIds);
        } catch (Exception e) {
            logger.error("预加载答案信息失败: {}", e.getMessage(), e);
            callback.updateTaskProgress(taskId, 0, totalAnswers, "FAILED");
            callback.addTaskLog(taskId, "ERROR", "预加载答案信息失败: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
        
        List<CompletableFuture<Void>> evaluationFutures = new ArrayList<>();
        for (StudentAnswer answer : answers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                boolean success = false;
                try {
                    evaluationSemaphore.acquire();
                    try {
                        // 传递config参数给评估方法
                        Map<String, Object> evaluationResult = evaluateSingleAnswerWithFetchedData(taskId, answer, callback, evaluatorUserId, evaluatorUsername, config);
                        detailedResults.add(evaluationResult);
                        String status = (String) evaluationResult.getOrDefault("evaluationStatus", "failed");
                        if ("success".equals(status)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        evaluationSemaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("评估任务被中断", e);
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("评估答案 " + answer.getId() + " 时发生未知异常", e);
                    failureCount.incrementAndGet();
                } finally {
                    int currentProcessed = processedCount.incrementAndGet();
                    callback.updateTaskProgress(taskId, currentProcessed, totalAnswers, "RUNNING");
                }
            }, securityContextTaskExecutor);
            evaluationFutures.add(future);
        }
        
        return CompletableFuture.allOf(evaluationFutures.toArray(new CompletableFuture[0]))
            .whenComplete((res, err) -> {
                String finalStatus = failureCount.get() == 0 ? "COMPLETED" : (successCount.get() > 0 ? "COMPLETED_WITH_ERRORS" : "FAILED");
                callback.updateTaskProgress(taskId, totalAnswers, totalAnswers, finalStatus);
                String completionMsg = String.format(
                    "批量评估任务完成！总计: %d，成功: %d，失败: %d",
                    totalAnswers, successCount.get(), failureCount.get()
                );
                callback.addTaskLog(taskId, "INFO", completionMsg);
                logger.info("任务 {} 完成: {}", taskId, completionMsg);
                
                checkAndUpdateExamStatusAfterEvaluation(answerIds, taskId);
            });
    }
    
    /**
     * 评估单个答案（使用预加载的答案数据，避免LazyInitializationException）
     * 返回详细的评估结果信息
     */
    @Transactional
    private Map<String, Object> evaluateSingleAnswerWithFetchedData(String taskId, StudentAnswer answer, TaskProgressCallback callback, Long evaluatorUserId, String evaluatorUsername, Map<String, Object> config) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                if (answer == null) {
                callback.addTaskLog(taskId, "WARN", "答案对象为空");
                
                // 返回空答案的详细结果
                Map<String, Object> nullResult = new HashMap<>();
                nullResult.put("id", null);
                nullResult.put("studentId", null);
                nullResult.put("studentName", "未知学生");
                nullResult.put("questionId", null);
                nullResult.put("questionTitle", "未知题目");
                nullResult.put("answerText", "");
                nullResult.put("score", 0);
                nullResult.put("maxScore", 100);
                nullResult.put("feedback", "答案对象为空");
                nullResult.put("evaluationStatus", "error");
                nullResult.put("evaluatedAt", LocalDateTime.now().toString());
                nullResult.put("evaluationType", "AI_AUTO");
                nullResult.put("retryCount", 0);
                nullResult.put("errorMessage", "答案对象为空");
                
                return nullResult;
            }
                
                logger.debug("正在评估答案 {} (学生: {}, 题目: {}), 尝试次数: {}", 
                    answer.getId(), 
                    answer.getStudent() != null ? answer.getStudent().getStudentId() : "未知",
                    answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知",
                    retryCount + 1
                );
                
                // 从配置中获取评分模式
                String evaluationStyle = null;
                if (config != null && config.containsKey("evaluationStyle")) {
                    evaluationStyle = (String) config.get("evaluationStyle");
                    logger.debug("从配置中读取到评分模式: {}", evaluationStyle);
                } else {
                    logger.debug("配置中未找到评分模式，使用默认模式 NORMAL");
                    evaluationStyle = "NORMAL";
                }
                
                // 调用AI评估服务（传入用户名以避免SecurityContext问题）
                logger.debug("=== 准备调用AI评估服务 ===");
                logger.debug("评估参数:");
                logger.debug("- taskId: {}", taskId);
                logger.debug("- answer.getId(): {}", answer.getId());
                logger.debug("- evaluatorUserId: {}", evaluatorUserId);
                logger.debug("- evaluatorUsername: {}", evaluatorUsername);
                logger.debug("- evaluationStyle: {}", evaluationStyle);
                
                AIEvaluationService.EvaluationResult result;
                if (evaluatorUsername != null) {
                    logger.debug("🔄 使用用户名 {} 进行AI评估，评分模式: {}", evaluatorUsername, evaluationStyle);
                    result = aiEvaluationService.evaluateAnswer(answer, evaluatorUsername, evaluationStyle);
                } else if (evaluatorUserId != null) {
                    logger.debug("🔄 使用用户ID {} 进行AI评估，评分模式: {}", evaluatorUserId, evaluationStyle);
                    result = aiEvaluationService.evaluateAnswer(answer, evaluatorUserId, evaluationStyle);
                } else {
                    logger.debug("🔄 无用户信息，使用默认AI评估方法");
                    // 如果无法获取用户信息，使用原方法（会自动回退到基础评估）
                    result = aiEvaluationService.evaluateAnswer(answer);
                }
                
                logger.debug("=== AI评估结果 ===");
                logger.debug("- 是否成功: {}", result.isSuccess());
                logger.debug("- 得分: {}", result.getScore());
                logger.debug("- 反馈: {}", result.getFeedback());
                
                if (result.isSuccess()) {
                    // 更新答案评估结果
                    answer.setScore(result.getScore());
                    answer.setFeedback(result.getFeedback());
                    answer.setEvaluated(true);
                    answer.setEvaluatedAt(LocalDateTime.now());
                    answer.setEvaluationType(EvaluationType.AI_AUTO);
                    
                    // 保存更新的答案
                    studentAnswerService.updateAnswer(answer.getId(), answer);
                    
                    logger.debug("答案 {} 评估成功，得分: {}", answer.getId(), result.getScore());
                    
                    // 如果之前有重试，记录成功日志
                    if (retryCount > 0) {
                        callback.addTaskLog(taskId, "INFO", 
                            String.format("答案 %d 在第 %d 次尝试后评估成功", answer.getId(), retryCount + 1));
                    }
                    
                    // 返回详细的评估结果
                    Map<String, Object> detailResult = new HashMap<>();
                    detailResult.put("id", answer.getId());
                    detailResult.put("studentId", answer.getStudent() != null ? answer.getStudent().getId() : null);
                    detailResult.put("studentName", answer.getStudent() != null ? answer.getStudent().getName() : "未知学生");
                    detailResult.put("questionId", answer.getQuestion() != null ? answer.getQuestion().getId() : null);
                    detailResult.put("questionTitle", answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知题目");
                    detailResult.put("answerText", answer.getAnswerText());
                    detailResult.put("score", result.getScore());
                    detailResult.put("maxScore", answer.getQuestion() != null ? answer.getQuestion().getMaxScore() : 100);
                    detailResult.put("feedback", result.getFeedback());
                    detailResult.put("evaluationStatus", "success");
                    detailResult.put("evaluatedAt", answer.getEvaluatedAt().toString());
                    detailResult.put("evaluationType", "AI_AUTO");
                    detailResult.put("retryCount", retryCount);
                    detailResult.put("evaluationStyle", evaluationStyle);
                    
                    return detailResult;
                    
                } else {
                    String errorMsg = "答案 " + answer.getId() + " AI评估失败: " + result.getFeedback();
                    logger.warn(errorMsg);
                    
                    if (retryCount == maxRetries - 1) {
                        callback.addTaskLog(taskId, "ERROR", errorMsg + " (已重试 " + maxRetries + " 次)");
                        
                        // 返回失败的详细结果
                        Map<String, Object> failedResult = new HashMap<>();
                        failedResult.put("id", answer.getId());
                        failedResult.put("studentId", answer.getStudent() != null ? answer.getStudent().getId() : null);
                        failedResult.put("studentName", answer.getStudent() != null ? answer.getStudent().getName() : "未知学生");
                        failedResult.put("questionId", answer.getQuestion() != null ? answer.getQuestion().getId() : null);
                        failedResult.put("questionTitle", answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知题目");
                        failedResult.put("answerText", answer.getAnswerText());
                        failedResult.put("score", 0);
                        failedResult.put("maxScore", answer.getQuestion() != null ? answer.getQuestion().getMaxScore() : 100);
                        failedResult.put("feedback", "AI评估失败: " + result.getFeedback());
                        failedResult.put("evaluationStatus", "failed");
                        failedResult.put("evaluatedAt", LocalDateTime.now().toString());
                        failedResult.put("evaluationType", "AI_AUTO");
                        failedResult.put("retryCount", retryCount);
                        failedResult.put("errorMessage", result.getFeedback());
                        failedResult.put("evaluationStyle", evaluationStyle);
                        
                        return failedResult;
                    }
                }
                
            } catch (Exception e) {
                logger.warn("评估答案 {} 时发生异常 (尝试 {}/{}): {}", 
                           answer.getId(), retryCount + 1, maxRetries, e.getMessage());
                
                if (retryCount == maxRetries - 1) {
                    logger.error("答案 {} 评估最终失败: {}", answer.getId(), e.getMessage(), e);
                    callback.addTaskLog(taskId, "ERROR", 
                        String.format("评估答案 %d 异常 (已重试 %d 次): %s", answer.getId(), maxRetries, e.getMessage()));
                    
                    // 返回异常的详细结果
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("id", answer.getId());
                    errorResult.put("studentId", answer.getStudent() != null ? answer.getStudent().getId() : null);
                    errorResult.put("studentName", answer.getStudent() != null ? answer.getStudent().getName() : "未知学生");
                    errorResult.put("questionId", answer.getQuestion() != null ? answer.getQuestion().getId() : null);
                    errorResult.put("questionTitle", answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知题目");
                    errorResult.put("answerText", answer.getAnswerText());
                    errorResult.put("score", 0);
                    errorResult.put("maxScore", answer.getQuestion() != null ? answer.getQuestion().getMaxScore() : 100);
                    errorResult.put("feedback", "评估异常: " + e.getMessage());
                    errorResult.put("evaluationStatus", "error");
                    errorResult.put("evaluatedAt", LocalDateTime.now().toString());
                    errorResult.put("evaluationType", "AI_AUTO");
                    errorResult.put("retryCount", retryCount);
                    errorResult.put("errorMessage", e.getMessage());
                    
                    return errorResult;
                }
            }
            
            retryCount++;
            
            // 如果不是最后一次尝试，等待一段时间再重试
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(1000 * retryCount); // 递增等待时间：1秒、2秒、3秒
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    callback.addTaskLog(taskId, "ERROR", "评估答案 " + answer.getId() + " 被中断");
                    
                    // 返回中断的详细结果
                    Map<String, Object> interruptedResult = new HashMap<>();
                    interruptedResult.put("id", answer.getId());
                    interruptedResult.put("studentId", answer.getStudent() != null ? answer.getStudent().getId() : null);
                    interruptedResult.put("studentName", answer.getStudent() != null ? answer.getStudent().getName() : "未知学生");
                    interruptedResult.put("questionId", answer.getQuestion() != null ? answer.getQuestion().getId() : null);
                    interruptedResult.put("questionTitle", answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知题目");
                    interruptedResult.put("answerText", answer.getAnswerText());
                    interruptedResult.put("score", 0);
                    interruptedResult.put("maxScore", answer.getQuestion() != null ? answer.getQuestion().getMaxScore() : 100);
                    interruptedResult.put("feedback", "评估过程被中断");
                    interruptedResult.put("evaluationStatus", "interrupted");
                    interruptedResult.put("evaluatedAt", LocalDateTime.now().toString());
                    interruptedResult.put("evaluationType", "AI_AUTO");
                    interruptedResult.put("retryCount", retryCount);
                    interruptedResult.put("errorMessage", "任务被中断");
                    
                    return interruptedResult;
                }
            }
        }
        
        // 如果所有重试都失败了，返回最终失败结果
        Map<String, Object> finalFailedResult = new HashMap<>();
        finalFailedResult.put("id", answer.getId());
        finalFailedResult.put("studentId", answer.getStudent() != null ? answer.getStudent().getId() : null);
        finalFailedResult.put("studentName", answer.getStudent() != null ? answer.getStudent().getName() : "未知学生");
        finalFailedResult.put("questionId", answer.getQuestion() != null ? answer.getQuestion().getId() : null);
        finalFailedResult.put("questionTitle", answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知题目");
        finalFailedResult.put("answerText", answer.getAnswerText());
        finalFailedResult.put("score", 0);
        finalFailedResult.put("maxScore", answer.getQuestion() != null ? answer.getQuestion().getMaxScore() : 100);
        finalFailedResult.put("feedback", "评估失败，已达到最大重试次数");
        finalFailedResult.put("evaluationStatus", "failed");
        finalFailedResult.put("evaluatedAt", LocalDateTime.now().toString());
        finalFailedResult.put("evaluationType", "AI_AUTO");
        finalFailedResult.put("retryCount", maxRetries);
        finalFailedResult.put("errorMessage", "达到最大重试次数");
        
        return finalFailedResult;
    }
    
    /**
     * 批阅任务完成后检查并更新相关考试的状态
     */
    private void checkAndUpdateExamStatusAfterEvaluation(List<Long> answerIds, String taskId) {
        try {
            // 获取所有涉及的考试ID
            Set<Long> examIds = new java.util.HashSet<>();
            
            // 批量获取答案信息以减少数据库查询
            for (Long answerId : answerIds) {
                try {
                    StudentAnswer answer = studentAnswerService.getAnswerById(answerId);
                    if (answer != null && answer.getQuestion() != null && answer.getQuestion().getExam() != null) {
                        examIds.add(answer.getQuestion().getExam().getId());
                    }
                } catch (Exception e) {
                    logger.warn("获取答案 {} 的考试信息失败: {}", answerId, e.getMessage());
                }
            }
            
            if (!examIds.isEmpty()) {
                logger.info("任务 {} 完成，检查 {} 个考试的状态", taskId, examIds.size());
                
                // 为每个考试检查并更新状态
                for (Long examId : examIds) {
                    try {
                        boolean updated = examService.checkAndUpdateExamToEvaluated(examId);
                        if (updated) {
                            logger.info("✅ 考试 {} 状态已更新为 EVALUATED", examId);
                        }
                    } catch (Exception e) {
                        logger.error("❌ 检查考试 {} 状态失败: {}", examId, e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ 批阅完成后检查考试状态失败: {}", e.getMessage(), e);
        }
    }
}

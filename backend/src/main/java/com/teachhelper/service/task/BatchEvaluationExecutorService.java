package com.teachhelper.service.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.util.concurrent.Executor;

import com.teachhelper.ai.AIEvaluationService;
import com.teachhelper.entity.StudentAnswer;

import com.teachhelper.entity.EvaluationType;
import com.teachhelper.service.student.StudentAnswerService;


/**
 * 批量评估执行引擎
 * 负责实际执行批量AI评估任务
 */
@Service
public class BatchEvaluationExecutorService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchEvaluationExecutorService.class);
    
    @Autowired
    private AIEvaluationService aiEvaluationService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    @Qualifier("securityContextTaskExecutor")
    private Executor securityContextTaskExecutor;
    

    
    // 控制并发评估数量的信号量
    private final Semaphore evaluationSemaphore = new Semaphore(3); // 最多3个并发评估
    
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
                if (evaluateAll) {
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
                if (evaluateAll) {
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
                    .toList();
                
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
                            .toList();
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
                            .toList();
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
    
    /**
     * 执行批量评估
     */
    private CompletableFuture<Void> executeBatchEvaluation(String taskId, List<Long> answerIds, Map<String, Object> config, TaskProgressCallback callback) {
        return CompletableFuture.runAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            AtomicInteger processedCount = new AtomicInteger(0);
            
            // 用于收集详细的评估结果
            List<Map<String, Object>> detailedResults = Collections.synchronizedList(new ArrayList<>());
            
            int totalCount = answerIds.size();
            
            logger.info("开始评估 {} 个答案", totalCount);
            callback.addTaskLog(taskId, "INFO", "开始批量评估，总计 " + totalCount + " 个答案");
            
            // 获取当前SecurityContext以传播到并行线程
            SecurityContext securityContext = SecurityContextHolder.getContext();
            Authentication authentication = securityContext.getAuthentication();
            
            logger.info("=== SecurityContext 调试信息 ===");
            logger.info("SecurityContext 对象: {}", securityContext);
            logger.info("Authentication 对象: {}", authentication);
            logger.info("Authentication 是否为null: {}", authentication == null);
            
            if (authentication != null) {
                logger.info("Authentication 类型: {}", authentication.getClass().getName());
                logger.info("Authentication getName(): {}", authentication.getName());
                logger.info("Authentication getPrincipal(): {}", authentication.getPrincipal());
                logger.info("Authentication getAuthorities(): {}", authentication.getAuthorities());
                logger.info("Authentication isAuthenticated(): {}", authentication.isAuthenticated());
            }
            
            // 获取当前用户ID以传递给AI评估服务 - 在主线程中获取，避免并行线程中的SecurityContext问题
            Long currentUserId = null;
            String currentUsername = null;
            
            // 首先尝试从任务配置中获取用户信息
            if (config != null) {
                Object configUserId = config.get("userId");
                Object configUsername = config.get("username");
                
                if (configUserId != null) {
                    currentUserId = extractLongValue(configUserId);
                    logger.info("从任务配置中获取用户ID: {}", currentUserId);
                }
                
                if (configUsername != null) {
                    currentUsername = configUsername.toString();
                    logger.info("从任务配置中获取用户名: {}", currentUsername);
                }
            }
            
            // 如果配置中没有用户信息，尝试从SecurityContext获取
            if (currentUsername == null && authentication != null && authentication.getName() != null) {
                try {
                    currentUsername = authentication.getName();
                    logger.info("✅ 从SecurityContext成功获取用户名: {}", currentUsername);
                    // 这里可以通过其他方式获取用户ID，但为了简化，我们传递用户名
                } catch (Exception e) {
                    logger.error("❌ 从SecurityContext获取用户信息失败: {}", e.getMessage(), e);
                }
            }
            
            // 如果仍然没有用户信息，使用默认值
            if (currentUsername == null) {
                currentUsername = "system";
                logger.warn("⚠️  无法获取用户信息，使用默认用户: {}", currentUsername);
            }
            
            final Long evaluatorUserId = currentUserId; // 保存为final变量用于lambda
            final String evaluatorUsername = currentUsername; // 保存用户名为final变量
            
            logger.info("最终传递给并行线程的用户信息:");
            logger.info("- evaluatorUserId: {}", evaluatorUserId);
            logger.info("- evaluatorUsername: {}", evaluatorUsername);
            
            // 预加载所有答案及其关联实体以避免LazyInitializationException
            List<StudentAnswer> answers;
            try {
                answers = studentAnswerService.getAnswersByIdsWithFetch(answerIds);
                logger.info("成功预加载 {} 个答案的完整信息", answers.size());
                callback.addTaskLog(taskId, "INFO", "已预加载 " + answers.size() + " 个答案的完整信息");
            } catch (Exception e) {
                logger.error("预加载答案信息失败: {}", e.getMessage(), e);
                callback.updateTaskProgress(taskId, 0, totalCount, "FAILED");
                callback.addTaskLog(taskId, "ERROR", "预加载答案信息失败: " + e.getMessage());
                return;
            }
            
            // 使用配置的securityContextTaskExecutor来确保SecurityContext传播
            List<CompletableFuture<Boolean>> futures = answers.stream()
                .map(answer -> {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            // 获取信号量以控制并发
                            evaluationSemaphore.acquire();
                            
                            try {
                                // 更新进度
                                int processed = processedCount.get();
                                if (processed % 5 == 0) { // 减少进度更新频率
                                    callback.updateTaskProgress(taskId, processed, totalCount, "RUNNING");
                                }
                                
                                // 直接使用主线程中获取的用户信息，不再依赖SecurityContext传播
                                logger.debug("=== 并行线程用户信息调试 ===");
                                logger.debug("从主线程传递的用户名: {}", evaluatorUsername);
                                logger.debug("从主线程传递的用户ID: {}", evaluatorUserId);
                                
                                // 检查当前线程的SecurityContext状态（用于调试）
                                try {
                                    SecurityContext currentContext = SecurityContextHolder.getContext();
                                    Authentication currentAuth = currentContext.getAuthentication();
                                    logger.debug("并行线程SecurityContext: {}", currentContext);
                                    logger.debug("并行线程Authentication: {}", currentAuth);
                                    if (currentAuth != null) {
                                        logger.debug("并行线程Authentication用户名: {}", currentAuth.getName());
                                    }
                                } catch (Exception e) {
                                    logger.debug("并行线程SecurityContext检查异常: {}", e.getMessage());
                                }
                                 
                                // 评估单个答案（使用预加载的答案对象）
                                Map<String, Object> evaluationResult = evaluateSingleAnswerWithFetchedData(taskId, answer, callback, evaluatorUserId, evaluatorUsername);
                                
                                // 根据evaluationStatus判断是否成功
                                String status = (String) evaluationResult.getOrDefault("evaluationStatus", "failed");
                                boolean success = "success".equals(status);
                                    
                                if (success) {
                                    successCount.incrementAndGet();
                                    logger.debug("答案 {} 评估成功", answer.getId());
                                } else {
                                    failureCount.incrementAndGet();
                                    logger.warn("答案 {} 评估失败，状态: {}", answer.getId(), status);
                                }
                                
                                // 无论成功失败都添加到详细结果中
                                detailedResults.add(evaluationResult);
                                    
                                return success;
                                
                            } finally {
                                evaluationSemaphore.release();
                                int currentProcessed = processedCount.incrementAndGet();
                                
                                // 每处理20个答案或处理完成时记录一次进度
                                if (currentProcessed % 20 == 0 || currentProcessed == totalCount) {
                                    int successTotal = successCount.get();
                                    int failureTotal = failureCount.get();
                                    
                                    String progressMsg = String.format(
                                        "已处理 %d/%d 个答案，成功: %d，失败: %d (%.1f%%)", 
                                        currentProcessed, totalCount, successTotal, failureTotal,
                                        (currentProcessed * 100.0 / totalCount)
                                    );
                                    
                                    callback.addTaskLog(taskId, "INFO", progressMsg);
                                    logger.info(progressMsg);
                                    
                                    // 更新最终进度
                                    callback.updateTaskProgress(taskId, currentProcessed, totalCount, "RUNNING");
                                }
                            }
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.error("批量评估任务被中断: {}", e.getMessage());
                            callback.addTaskLog(taskId, "ERROR", "任务被中断: " + e.getMessage());
                            return false;
                        } catch (Exception e) {
                            logger.error("处理答案 {} 时发生错误: {}", answer.getId(), e.getMessage(), e);
                            callback.addTaskLog(taskId, "ERROR", "处理答案 " + answer.getId() + " 时发生错误: " + e.getMessage());
                            failureCount.incrementAndGet();
                            processedCount.incrementAndGet();
                            return false;
                        }
                    }, securityContextTaskExecutor);
                })
                .toList();
            
            // 等待所有任务完成
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
            allTasks.join();
            
            // 完成任务
            int finalProcessed = processedCount.get();
            int finalSuccess = successCount.get();
            int finalFailure = failureCount.get();
            
            String status = finalFailure == 0 ? "COMPLETED" : (finalSuccess > 0 ? "COMPLETED_WITH_ERRORS" : "FAILED");
            callback.updateTaskProgress(taskId, finalProcessed, totalCount, status);
            
            String completionMsg = String.format(
                "批量评估任务完成！总计: %d，成功: %d，失败: %d，成功率: %.1f%%", 
                finalProcessed, finalSuccess, finalFailure,
                finalProcessed > 0 ? (finalSuccess * 100.0 / finalProcessed) : 0
            );
            
            callback.addTaskLog(taskId, "INFO", completionMsg);
            logger.info("任务 {} 完成: {}", taskId, completionMsg);
            
            // 保存任务结果（包含详细评估结果）
            Map<String, Object> results = Map.of(
                "totalAnswers", totalCount,
                "successfulEvaluations", finalSuccess,
                "failedEvaluations", finalFailure,
                "completionRate", finalProcessed * 100.0 / totalCount,
                "successRate", finalProcessed > 0 ? (finalSuccess * 100.0 / finalProcessed) : 0.0,
                "completedAt", LocalDateTime.now().toString(),
                "detailedResults", detailedResults
            );
            
            callback.saveTaskResults(taskId, results);
        }, securityContextTaskExecutor);
    }
    
    /**
     * 评估单个答案（使用预加载的答案数据，避免LazyInitializationException）
     * 返回详细的评估结果信息
     */
    @Transactional
    private Map<String, Object> evaluateSingleAnswerWithFetchedData(String taskId, StudentAnswer answer, TaskProgressCallback callback, Long evaluatorUserId, String evaluatorUsername) {
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
                
                // 调用AI评估服务（传入用户名以避免SecurityContext问题）
                logger.debug("=== 准备调用AI评估服务 ===");
                logger.debug("评估参数:");
                logger.debug("- taskId: {}", taskId);
                logger.debug("- answer.getId(): {}", answer.getId());
                logger.debug("- evaluatorUserId: {}", evaluatorUserId);
                logger.debug("- evaluatorUsername: {}", evaluatorUsername);
                
                AIEvaluationService.EvaluationResult result;
                if (evaluatorUsername != null) {
                    logger.debug("🔄 使用用户名 {} 进行AI评估", evaluatorUsername);
                    result = aiEvaluationService.evaluateAnswer(answer, evaluatorUsername);
                } else if (evaluatorUserId != null) {
                    logger.debug("🔄 使用用户ID {} 进行AI评估", evaluatorUserId);
                    result = aiEvaluationService.evaluateAnswer(answer, evaluatorUserId);
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
}

package com.teachhelper.service.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.teachhelper.dto.response.BatchEvaluationResult;

/**
 * 批量评估进度跟踪和结果缓存服务
 */
@Service
public class BatchEvaluationCacheService {
    
    // 内存缓存，实际项目中可能需要使用Redis
    private final Map<String, BatchEvaluationResult> resultCache = new ConcurrentHashMap<>();
    
    /**
     * 保存批量评估结果
     */
    public void saveResult(String taskId, BatchEvaluationResult result) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                throw new IllegalArgumentException("Task ID cannot be null or empty");
            }
            if (result == null) {
                throw new IllegalArgumentException("Result cannot be null");
            }
            
                        result.setTaskId(taskId);
            resultCache.put(taskId, result);
            // 完成任务保存
            System.out.println("Saved evaluation result for task: " + taskId);
        } catch (Exception e) {
            // Log error but don't throw to avoid breaking the evaluation process
            System.err.println("Failed to save result for task " + taskId + ": " + e.getMessage());
        }
    }
    
    /**
     * 获取批量评估结果
     */
    public BatchEvaluationResult getResult(String taskId) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                return null;
            }
            return resultCache.get(taskId);
        } catch (Exception e) {
            System.err.println("Failed to get result for task " + taskId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查结果是否存在
     */
    public boolean hasResult(String taskId) {
        return resultCache.containsKey(taskId);
    }
    
    /**
     * 简单的进度更新方法 - 仅用于内部状态跟踪
     */
    public void updateProgress(String taskId, int totalAnswers, int evaluatedAnswers, String status) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                return;
            }
            // 简单的日志记录，不使用复杂的缓存
            System.out.println("Progress for task " + taskId + ": " + evaluatedAnswers + "/" + totalAnswers + " (" + status + ")");
        } catch (Exception e) {
            System.err.println("Failed to log progress for task " + taskId + ": " + e.getMessage());
        }
    }
    
    /**
     * 获取批量评估进度百分比
     */
    public Double getProgress(String taskId) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                return null;
            }
            BatchEvaluationResult result = resultCache.get(taskId);
            if (result == null) {
                return null;
            }
            
            int totalAnswers = result.getTotalAnswers();
            int processedAnswers = result.getSuccessfulEvaluations() + result.getFailedEvaluations();
            
            if (totalAnswers == 0) {
                return 100.0;
            }
            
            return (double) processedAnswers / totalAnswers * 100.0;
        } catch (Exception e) {
            System.err.println("Failed to get progress for task " + taskId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取最近的评估任务列表
     */
    public List<BatchEvaluationResult> getRecentTasks() {
        try {
            return new ArrayList<>(resultCache.values())
                .stream()
                .sorted((a, b) -> {
                    if (a.getStartTime() == null && b.getStartTime() == null) return 0;
                    if (a.getStartTime() == null) return 1;
                    if (b.getStartTime() == null) return -1;
                    return b.getStartTime().compareTo(a.getStartTime()); // 最新的在前面
                })
                .limit(10) // 限制返回最近10个任务
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to get recent tasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}

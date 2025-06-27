package com.teachhelper.service.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.entity.TaskStatus;

/**
 * 任务执行调度器
 * 负责根据任务类型分发给相应的执行器
 */
@Service
public class TaskExecutionDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionDispatcher.class);
    
    @Autowired
    private BatchEvaluationExecutorService batchEvaluationExecutor;
    
    @Autowired
    private TaskService taskService;
    
    // 跟踪正在执行的任务
    private final Map<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 执行任务
     */
    public void executeTask(TaskResponse task) {
        String taskId = task.getTaskId();
        String taskType = task.getType();
        
        logger.info("开始执行任务: {} (类型: {})", taskId, taskType);
        
        try {
            CompletableFuture<Void> future = null;
            
            switch (taskType) {
                case "BATCH_EVALUATION":
                case "BATCH_EVALUATION_ANSWERS":
                case "BATCH_EVALUATION_QUESTION":
                case "BATCH_EVALUATION_ALL_QUESTION":
                case "BATCH_REVALUATION_QUESTION":
                case "BATCH_EVALUATION_STUDENT":
                    // 批量评估任务 - 传递 TaskService 作为回调
                    future = batchEvaluationExecutor.executeBatchEvaluationTask(taskId, task.getConfig(), (TaskProgressCallback) taskService);
                    break;
                    
                default:
                    logger.warn("未知的任务类型: {}", taskType);
                    taskService.updateTaskProgress(taskId, 0, 0, "FAILED");
                    taskService.addTaskLog(taskId, "ERROR", "不支持的任务类型: " + taskType);
                    return;
            }
            
            if (future != null) {
                // 跟踪任务执行状态
                runningTasks.put(taskId, future);
                
                // 异步处理任务完成后的清理
                future.whenComplete((result, throwable) -> {
                    runningTasks.remove(taskId);
                    
                    if (throwable != null) {
                        logger.error("任务 {} 执行时发生异常: {}", taskId, throwable.getMessage(), throwable);
                        taskService.updateTaskProgress(taskId, 0, 0, "FAILED");
                        taskService.addTaskLog(taskId, "ERROR", "任务执行异常: " + throwable.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            logger.error("启动任务 {} 时发生异常: {}", taskId, e.getMessage(), e);
            taskService.updateTaskProgress(taskId, 0, 0, "FAILED");
            taskService.addTaskLog(taskId, "ERROR", "启动任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消任务执行
     */
    public boolean cancelTask(String taskId) {
        CompletableFuture<Void> future = runningTasks.get(taskId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                runningTasks.remove(taskId);
                taskService.addTaskLog(taskId, "INFO", "任务已被取消");
                logger.info("任务 {} 已被取消", taskId);
            }
            return cancelled;
        }
        return false;
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning(String taskId) {
        CompletableFuture<Void> future = runningTasks.get(taskId);
        return future != null && !future.isDone();
    }
    
    /**
     * 获取正在运行的任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
}

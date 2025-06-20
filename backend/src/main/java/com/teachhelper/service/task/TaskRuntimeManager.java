package com.teachhelper.service.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 任务运行时管理器
 * 只负责管理运行中的任务，不包含任务分发逻辑
 */
@Service
public class TaskRuntimeManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskRuntimeManager.class);
    
    // 跟踪正在执行的任务
    private final Map<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 注册运行中的任务
     */
    public void registerTask(String taskId, CompletableFuture<Void> future) {
        runningTasks.put(taskId, future);
        
        // 自动清理完成的任务
        future.whenComplete((result, throwable) -> {
            runningTasks.remove(taskId);
            if (throwable != null && !future.isCancelled()) {
                logger.warn("任务 {} 执行异常: {}", taskId, throwable.getMessage());
            }
        });
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

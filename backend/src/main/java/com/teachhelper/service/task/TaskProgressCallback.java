package com.teachhelper.service.task;

/**
 * 任务进度回调接口
 * 用于打破循环依赖
 */
public interface TaskProgressCallback {
    
    /**
     * 更新任务进度
     */
    void updateTaskProgress(String taskId, int processedCount, int totalCount, String status);
    
    /**
     * 添加任务日志
     */
    void addTaskLog(String taskId, String level, String message);
    
    /**
     * 保存任务结果
     */
    void saveTaskResults(String taskId, java.util.Map<String, Object> results);
}

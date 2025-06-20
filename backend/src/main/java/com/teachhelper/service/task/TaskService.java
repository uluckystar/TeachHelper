package com.teachhelper.service.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.teachhelper.dto.request.TaskCreateRequest;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.dto.response.TaskStatistics;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 获取任务列表（分页）
     */
    Page<TaskResponse> getTasks(Pageable pageable, String status, String type, 
                               LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取最近的任务
     */
    List<TaskResponse> getRecentTasks(int limit);

    /**
     * 获取任务统计信息
     */
    TaskStatistics getTaskStatistics();

    /**
     * 根据ID获取任务详情
     */
    TaskResponse getTaskById(String taskId);

    /**
     * 获取任务日志
     */
    List<Map<String, Object>> getTaskLogs(String taskId);

    /**
     * 获取任务结果
     */
    Map<String, Object> getTaskResults(String taskId, int page, int size);

    /**
     * 创建任务
     */
    TaskResponse createTask(TaskCreateRequest request);

    /**
     * 暂停任务
     */
    TaskResponse pauseTask(String taskId);

    /**
     * 恢复任务
     */
    TaskResponse resumeTask(String taskId);

    /**
     * 取消任务
     */
    TaskResponse cancelTask(String taskId);

    /**
     * 重试任务
     */
    TaskResponse retryTask(String taskId);

    /**
     * 删除任务
     */
    void deleteTask(String taskId);

    /**
     * 清理已完成的任务
     */
    int clearCompletedTasks();

    /**
     * 暂停所有运行中的任务
     */
    int pauseAllRunningTasks();

    /**
     * 导出任务结果
     */
    byte[] exportTaskResults(String taskId, String format);

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
    void saveTaskResults(String taskId, Map<String, Object> results);
}

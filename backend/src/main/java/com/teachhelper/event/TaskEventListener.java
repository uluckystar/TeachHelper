package com.teachhelper.event;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.service.task.BatchEvaluationExecutorService;
import com.teachhelper.service.task.TaskService;
import com.teachhelper.service.task.TaskProgressCallback;
import com.teachhelper.service.task.TaskRuntimeManager;

/**
 * 任务事件监听器
 * 监听任务创建事件并分发给相应的执行器
 */
@Component
public class TaskEventListener implements ApplicationListener<TaskCreatedEvent> {
    
    @Autowired
    private BatchEvaluationExecutorService batchEvaluationExecutor;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskRuntimeManager taskRuntimeManager;
    
    @Override
    public void onApplicationEvent(TaskCreatedEvent event) {
        TaskResponse task = event.getTask();
        String taskType = task.getType();
        String taskId = task.getTaskId();
        
        try {
            CompletableFuture<Void> future = null;
            
            switch (taskType) {
                case "BATCH_EVALUATION":
                case "BATCH_EVALUATION_ANSWERS":
                case "BATCH_EVALUATION_QUESTION":
                case "BATCH_EVALUATION_ALL_QUESTION":
                case "BATCH_REVALUATION_QUESTION":
                case "BATCH_EVALUATION_STUDENT":
                case "SINGLE_EVALUATION":
                case "SINGLE_REVALUATION":
                    // 批量评估任务 - 传递 TaskService 作为回调
                    future = batchEvaluationExecutor.executeBatchEvaluationTask(
                        taskId, 
                        task.getConfig(), 
                        (TaskProgressCallback) taskService
                    );
                    break;
                    
                default:
                    taskService.updateTaskProgress(taskId, 0, 0, "FAILED");
                    taskService.addTaskLog(taskId, "ERROR", "不支持的任务类型: " + taskType);
                    return;
            }
            
            // 注册到运行时管理器
            if (future != null) {
                taskRuntimeManager.registerTask(taskId, future);
            }
            
        } catch (Exception e) {
            taskService.updateTaskProgress(taskId, 0, 0, "FAILED");
            taskService.addTaskLog(taskId, "ERROR", "启动任务失败: " + e.getMessage());
        }
    }
}

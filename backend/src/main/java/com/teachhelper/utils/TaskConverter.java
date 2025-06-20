package com.teachhelper.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.request.TaskCreateRequest;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.entity.Task;
import com.teachhelper.entity.Task.TaskPriority;
import com.teachhelper.entity.TaskStatus;
import com.teachhelper.entity.TaskLog;
import com.teachhelper.entity.User;

/**
 * 任务实体转换工具类
 */
public class TaskConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将TaskCreateRequest转换为Task实体
     */
    public static Task convertToEntity(TaskCreateRequest request, User createdBy) {
        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setType(request.getType());
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(convertPriority(request.getPriority()));
        task.setCreatedBy(createdBy);
        
        // 序列化配置
        if (request.getConfig() != null) {
            try {
                task.setConfiguration(objectMapper.writeValueAsString(request.getConfig()));
            } catch (JsonProcessingException e) {
                task.setConfiguration("{}");
            }
        }
        
        return task;
    }

    /**
     * 将Task实体转换为TaskResponse
     */
    public static TaskResponse convertToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getTaskId());
        response.setName(task.getName());
        response.setDescription(task.getDescription());
        response.setType(task.getType());
        response.setStatus(task.getStatus().name());
        response.setPriority(task.getPriority().name());
        response.setProgress(task.getProgress());
        response.setProcessedCount(task.getProcessedCount());
        response.setTotalCount(task.getTotalCount());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedAt(task.getCreatedAt());
        response.setStartedAt(task.getStartedAt());
        response.setCompletedAt(task.getCompletedAt());
        
        if (task.getCreatedBy() != null) {
            response.setCreatedBy(task.getCreatedBy().getId());
            response.setCreatedByName(task.getCreatedBy().getUsername());
        }
        
        // 反序列化配置
        if (task.getConfiguration() != null) {
            try {
                response.setConfig(objectMapper.readValue(task.getConfiguration(), Map.class));
            } catch (JsonProcessingException e) {
                response.setConfig(new HashMap<>());
            }
        }
        
        // 反序列化结果
        if (task.getResultData() != null) {
            try {
                response.setResultSummary(objectMapper.readValue(task.getResultData(), Map.class));
            } catch (JsonProcessingException e) {
                response.setResultSummary(new HashMap<>());
            }
        }
        
        return response;
    }

    /**
     * 将TaskLog实体转换为Map
     */
    public static Map<String, Object> convertLogToMap(TaskLog taskLog) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("id", taskLog.getId());
        logMap.put("level", taskLog.getLevel().name());
        logMap.put("message", taskLog.getMessage());
        logMap.put("timestamp", taskLog.getCreatedAt());
        return logMap;
    }

    /**
     * 生成任务ID
     */
    private static String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 转换优先级
     */
    private static TaskPriority convertPriority(String priority) {
        if (priority == null) {
            return TaskPriority.MEDIUM;
        }
        try {
            return TaskPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TaskPriority.MEDIUM;
        }
    }

    /**
     * 转换状态
     */
    public static TaskStatus convertStatus(String status) {
        if (status == null) {
            return TaskStatus.PENDING;
        }
        try {
            return TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TaskStatus.PENDING;
        }
    }

    /**
     * 保存任务结果
     */
    public static void saveTaskResult(Task task, Map<String, Object> result) {
        if (result != null) {
            try {
                task.setResultData(objectMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                task.setResultData("{}");
            }
        }
    }

    /**
     * 更新任务进度
     */
    public static void updateTaskProgress(Task task, int processedCount, int totalCount, String currentStep) {
        task.setProcessedCount(processedCount);
        task.setTotalCount(totalCount);
        task.setCurrentStep(currentStep);
        
        if (totalCount > 0) {
            task.setProgress((processedCount * 100) / totalCount);
        }
        
        task.setUpdatedAt(LocalDateTime.now());
    }
}

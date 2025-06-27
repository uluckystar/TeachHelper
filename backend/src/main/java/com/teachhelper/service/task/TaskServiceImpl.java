package com.teachhelper.service.task;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.teachhelper.dto.request.TaskCreateRequest;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.dto.response.TaskStatistics;
import com.teachhelper.entity.Task;
import com.teachhelper.entity.TaskStatus;
import com.teachhelper.entity.TaskLog;
import com.teachhelper.entity.TaskLog.LogLevel;
import com.teachhelper.entity.User;
import com.teachhelper.event.TaskCreatedEvent;
import com.teachhelper.repository.TaskLogRepository;
import com.teachhelper.repository.TaskRepository;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.utils.TaskConverter;
import com.teachhelper.websocket.TaskWebSocketHandler;

/**
 * 任务服务实现类 - 使用数据库存储
 */
@Service
public class TaskServiceImpl implements TaskService, TaskProgressCallback {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private TaskWebSocketHandler webSocketHandler;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private TaskRuntimeManager taskRuntimeManager;

    @Override
    public Page<TaskResponse> getTasks(Pageable pageable, String status, String type, 
                                     LocalDateTime startDate, LocalDateTime endDate) {
        TaskStatus taskStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                taskStatus = TaskStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效状态
            }
        }
        
        Page<Task> taskPage = taskRepository.findTasksWithFilters(
            taskStatus, type, startDate, endDate, pageable);
        
        return taskPage.map(TaskConverter::convertToResponse);
    }

    @Override
    public List<TaskResponse> getRecentTasks(int limit) {
        List<Task> recentTasks = taskRepository.findTop10ByOrderByCreatedAtDesc();
        return recentTasks.stream()
                .limit(limit)
                .map(TaskConverter::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TaskStatistics getTaskStatistics() {
        List<Object[]> statusCounts = taskRepository.countByStatus();
        
        Map<TaskStatus, Integer> countMap = new HashMap<>();
        for (Object[] result : statusCounts) {
            TaskStatus status = (TaskStatus) result[0];
            Long count = (Long) result[1];
            countMap.put(status, count.intValue());
        }
        
        int total = (int) taskRepository.count();
        int running = countMap.getOrDefault(TaskStatus.RUNNING, 0);
        int pending = countMap.getOrDefault(TaskStatus.PENDING, 0);
        int completed = countMap.getOrDefault(TaskStatus.COMPLETED, 0);
        int failed = countMap.getOrDefault(TaskStatus.FAILED, 0);
        int paused = countMap.getOrDefault(TaskStatus.PAUSED, 0);
        int cancelled = countMap.getOrDefault(TaskStatus.CANCELLED, 0);
        
        return new TaskStatistics(total, running, pending, completed, failed, paused, cancelled);
    }

    @Override
    public TaskResponse getTaskById(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        return TaskConverter.convertToResponse(taskOpt.get());
    }

    @Override
    public List<Map<String, Object>> getTaskLogs(String taskId) {
        List<TaskLog> logs = taskLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return logs.stream()
                .map(TaskConverter::convertLogToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTaskResults(String taskId, int page, int size) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        Task task = taskOpt.get();
        Map<String, Object> results = new HashMap<>();
        results.put("taskId", taskId);
        results.put("status", task.getStatus().name());
        results.put("progress", task.getProgress());
        results.put("processedCount", task.getProcessedCount());
        results.put("totalCount", task.getTotalCount());
        results.put("page", page);
        results.put("size", size);
        
        // 如果任务有结果数据，尝试返回
        if (task.getResultData() != null && !task.getResultData().trim().isEmpty()) {
            System.out.println("✅ TaskService.getTaskResults - 找到任务结果数据:");
            System.out.println("  - 任务ID: " + taskId);
            System.out.println("  - 状态: " + task.getStatus());
            System.out.println("  - 数据长度: " + task.getResultData().length());
            System.out.println("  - 数据内容前100字符: " + 
                (task.getResultData().length() > 100 ? 
                    task.getResultData().substring(0, 100) + "..." : 
                    task.getResultData()));
            
            // 直接将原始JSON数据作为字符串返回，让前端处理
            results.put("resultData", task.getResultData());
        } else {
            System.out.println("⚠️ TaskService.getTaskResults - 任务无结果数据:");
            System.out.println("  - 任务ID: " + taskId);
            System.out.println("  - 状态: " + task.getStatus());
            System.out.println("  - 结果数据: " + task.getResultData());
        }
        
        return results;
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        // 获取当前用户
        User currentUser;
        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception e) {
            // 在没有认证上下文时使用系统用户
            currentUser = new User();
            currentUser.setId(1L);
            currentUser.setUsername("system");
        }
        
        // 将用户信息添加到任务配置中
        if (request.getConfig() == null) {
            request.setConfig(new HashMap<>());
        }
        request.getConfig().put("userId", currentUser.getId());
        request.getConfig().put("username", currentUser.getUsername());
        
        // 创建任务实体
        Task task = TaskConverter.convertToEntity(request, currentUser);
        
        // 保存任务
        task = taskRepository.save(task);
        
        // 不在这里添加创建日志，避免事务嵌套问题
        
        TaskResponse response = TaskConverter.convertToResponse(task);
        
        // 广播任务创建事件
        broadcastTaskUpdate(response);
        
        // 如果设置了自动开始，立即启动任务
        if (Boolean.TRUE.equals(request.getAutoStart())) {
            // 发布任务创建事件来启动任务，避免直接依赖调度器
            eventPublisher.publishEvent(new TaskCreatedEvent(this, response));
        }
        
        return response;
    }

    @Override
    public TaskResponse pauseTask(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        Task task = taskOpt.get();
        if (task.getStatus() != TaskStatus.RUNNING) {
            throw new RuntimeException("只能暂停运行中的任务");
        }
        
        task.setStatus(TaskStatus.PAUSED);
        task.setUpdatedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        
        addTaskLog(taskId, "INFO", "任务已暂停");
        
        TaskResponse response = TaskConverter.convertToResponse(task);
        broadcastTaskUpdate(response);
        
        return response;
    }

    @Override
    public TaskResponse resumeTask(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        Task task = taskOpt.get();
        if (task.getStatus() != TaskStatus.PAUSED) {
            throw new RuntimeException("只能恢复已暂停的任务");
        }
        
        task.setStatus(TaskStatus.RUNNING);
        task.setUpdatedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        
        addTaskLog(taskId, "INFO", "任务已恢复，重新开始执行");
        
        TaskResponse response = TaskConverter.convertToResponse(task);
        broadcastTaskUpdate(response);
        
        // 重新启动任务执行
        try {
            eventPublisher.publishEvent(new TaskCreatedEvent(this, response));
        } catch (Exception e) {
            // 如果重新启动失败，将任务状态设为失败
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("恢复任务执行失败: " + e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            addTaskLog(taskId, "ERROR", "恢复任务执行失败: " + e.getMessage());
            throw new RuntimeException("恢复任务执行失败: " + e.getMessage(), e);
        }
        
        return response;
    }

    @Override
    public TaskResponse cancelTask(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        Task task = taskOpt.get();
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new RuntimeException("无法取消已完成或已取消的任务");
        }
        
        // 尝试取消正在执行的任务
        boolean cancelled = false;
        if (task.getStatus() == TaskStatus.RUNNING) {
            cancelled = taskRuntimeManager.cancelTask(taskId);
        }
        
        task.setStatus(TaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        
        String logMessage = cancelled ? "运行中的任务已被强制取消" : "任务已取消";
        addTaskLog(taskId, "WARN", logMessage);
        
        TaskResponse response = TaskConverter.convertToResponse(task);
        broadcastTaskUpdate(response);
        
        return response;
    }

    @Override
    public TaskResponse retryTask(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        Task task = taskOpt.get();
        if (task.getStatus() != TaskStatus.FAILED && task.getStatus() != TaskStatus.CANCELLED) {
            throw new RuntimeException("只能重试失败或已取消的任务");
        }
        
        task.setStatus(TaskStatus.RUNNING);
        task.setProgress(0);
        task.setProcessedCount(0);
        task.setStartedAt(LocalDateTime.now());
        task.setCompletedAt(null);
        task.setErrorMessage(null);
        task.setUpdatedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        
        addTaskLog(taskId, "INFO", "任务重试已启动，开始重新执行");
        
        TaskResponse response = TaskConverter.convertToResponse(task);
        broadcastTaskUpdate(response);
        
        // 重新启动任务执行
        try {
            eventPublisher.publishEvent(new TaskCreatedEvent(this, response));
        } catch (Exception e) {
            // 如果重新启动失败，将任务状态设为失败
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("重试任务执行失败: " + e.getMessage());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            addTaskLog(taskId, "ERROR", "重试任务执行失败: " + e.getMessage());
            throw new RuntimeException("重试任务执行失败: " + e.getMessage(), e);
        }
        broadcastTaskUpdate(response);
        
        return response;
    }

    @Override
    public void deleteTask(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        Task task = taskOpt.get();
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new RuntimeException("无法删除运行中的任务，请先停止任务");
        }
        
        // 删除相关日志
        taskLogRepository.deleteByTaskId(taskId);
        
        // 删除任务
        taskRepository.deleteByTaskId(taskId);
    }

    @Override
    public int clearCompletedTasks() {
        List<TaskStatus> completedStatuses = Arrays.asList(
            TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED);
        
        List<Task> completedTasks = taskRepository.findByStatusIn(completedStatuses);
        
        for (Task task : completedTasks) {
            taskLogRepository.deleteByTask(task);
        }
        
        int count = completedTasks.size();
        taskRepository.deleteAll(completedTasks);
        
        return count;
    }

    @Override
    public int pauseAllRunningTasks() {
        List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);
        
        for (Task task : runningTasks) {
            task.setStatus(TaskStatus.PAUSED);
            task.setUpdatedAt(LocalDateTime.now());
            addTaskLog(task.getTaskId(), "INFO", "任务被批量暂停");
        }
        
        taskRepository.saveAll(runningTasks);
        
        return runningTasks.size();
    }

    @Override
    public byte[] exportTaskResults(String taskId, String format) {
        Map<String, Object> results = getTaskResults(taskId, 0, 1000);
        
        // 简单的JSON导出实现
        String jsonData = "{\n  \"taskId\": \"" + taskId + "\",\n  \"exportTime\": \"" + 
                         LocalDateTime.now() + "\",\n  \"results\": " + results + "\n}";
        
        return jsonData.getBytes();
    }

    @Override
    @Transactional
    public void updateTaskProgress(String taskId, int processedCount, int totalCount, String status) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            TaskConverter.updateTaskProgress(task, processedCount, totalCount, null);
            
            if (status != null) {
                try {
                    TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
                    task.setStatus(taskStatus);
                    
                    // 根据状态设置相应的时间字段
                    LocalDateTime now = LocalDateTime.now();
                    if (taskStatus == TaskStatus.RUNNING && task.getStartedAt() == null) {
                        task.setStartedAt(now);
                    } else if (taskStatus == TaskStatus.COMPLETED || taskStatus == TaskStatus.FAILED || taskStatus == TaskStatus.CANCELLED) {
                        if (task.getStartedAt() == null) {
                            task.setStartedAt(now.minusHours(1)); // 设置一个默认的开始时间
                        }
                        task.setCompletedAt(now);
                    }
                } catch (IllegalArgumentException e) {
                    // 忽略无效状态
                }
            }
            
            taskRepository.save(task);
            
            // 广播进度更新
            TaskResponse response = TaskConverter.convertToResponse(task);
            broadcastTaskUpdate(response);
        }
    }

    @Override
    @Transactional
    public void addTaskLog(String taskId, String level, String message) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            
            LogLevel logLevel;
            try {
                logLevel = LogLevel.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                logLevel = LogLevel.INFO;
            }
            
            TaskLog taskLog = new TaskLog(task, logLevel, message);
            taskLogRepository.save(taskLog);
        }
    }
    
    @Override
    public void saveTaskResults(String taskId, Map<String, Object> results) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            TaskConverter.saveTaskResult(task, results);
            taskRepository.save(task);
        }
    }

    /**
     * 根据taskId查找任务
     */
    public Task findTaskByTaskId(String taskId) {
        Optional<Task> taskOpt = taskRepository.findByTaskId(taskId);
        return taskOpt.orElse(null);
    }
    
    /**
     * 保存任务
     */
    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    /**
     * 广播任务更新事件
     */
    private void broadcastTaskUpdate(TaskResponse task) {
        try {
            // 立即广播任务状态变化
            webSocketHandler.broadcastTaskUpdate(
                task.getTaskId(), 
                task.getStatus(), 
                task.getProgress(), 
                task.getType(), 
                task.getResultSummary()
            );
            
            // 添加日志以便调试
            System.out.println("📡 广播任务更新: " + task.getTaskId() + " -> " + task.getStatus());
        } catch (Exception e) {
            // 忽略广播失败，不影响主要功能
            System.err.println("❌ 广播任务更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public String createLearningAnswerImportTask(String subject, List<String> classFolders, Long examId, String operator) {
        System.out.println("创建学习通答案导入任务: subject=" + subject + ", examId=" + examId + ", classFolders=" + classFolders);
        
        // 创建任务请求
        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setName("学习通答案导入: " + subject);
        taskRequest.setDescription("导入学习通答案 - 科目: " + subject + ", 班级: " + String.join(", ", classFolders) + 
                                  (examId != null ? ", 考试ID: " + examId : ""));
        taskRequest.setType("LEARNING_ANSWER_IMPORT");
        taskRequest.setPriority("MEDIUM");
        
        // 设置任务参数
        Map<String, Object> params = new HashMap<>();
        params.put("subject", subject);
        params.put("classFolders", classFolders);
        params.put("examId", examId);
        params.put("operator", operator);
        taskRequest.setConfig(params);
        
        // 创建任务
        TaskResponse taskResponse = createTask(taskRequest);
        return taskResponse.getTaskId();
    }
}

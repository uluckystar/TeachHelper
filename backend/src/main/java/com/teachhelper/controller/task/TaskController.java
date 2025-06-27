package com.teachhelper.controller.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.request.TaskCreateRequest;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.dto.response.TaskStatistics;
import com.teachhelper.service.task.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 统一任务管理控制器
 * 负责管理所有类型的后台任务，包括评估任务、生成任务、知识库处理任务等
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "任务管理", description = "统一管理所有后台任务，支持实时监控、进度控制和结果预览")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 获取任务列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取任务列表，支持分页、筛选和排序")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "任务状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "任务类型筛选") @RequestParam(required = false) String type,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TaskResponse> tasks = taskService.getTasks(pageable, status, type, startDate, endDate);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取最近的任务
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近任务", description = "获取最近的任务列表，用于快速查看")
    public ResponseEntity<List<TaskResponse>> getRecentTasks(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        
        List<TaskResponse> tasks = taskService.getRecentTasks(limit);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取任务统计", description = "获取各状态任务的统计信息")
    public ResponseEntity<TaskStatistics> getTaskStats() {
        TaskStatistics stats = taskService.getTaskStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情", description = "根据任务ID获取详细信息")
    public ResponseEntity<TaskResponse> getTaskDetail(@PathVariable String taskId) {
        TaskResponse task = taskService.getTaskById(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/{taskId}/logs")
    @Operation(summary = "获取任务日志", description = "获取任务的执行日志")
    public ResponseEntity<List<Map<String, Object>>> getTaskLogs(@PathVariable String taskId) {
        List<Map<String, Object>> logs = taskService.getTaskLogs(taskId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取任务结果
     */
    @GetMapping("/{taskId}/results")
    @Operation(summary = "获取任务结果", description = "获取任务的执行结果")
    public ResponseEntity<Map<String, Object>> getTaskResults(
            @PathVariable String taskId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> results = taskService.getTaskResults(taskId, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * 创建任务
     */
    @PostMapping
    @Operation(summary = "创建任务", description = "创建新的后台任务")
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskCreateRequest request) {
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity.ok(task);
    }

    /**
     * 暂停任务
     */
    @PostMapping("/{taskId}/pause")
    @Operation(summary = "暂停任务", description = "暂停正在运行的任务")
    public ResponseEntity<TaskResponse> pauseTask(@PathVariable String taskId) {
        TaskResponse task = taskService.pauseTask(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 恢复任务
     */
    @PostMapping("/{taskId}/resume")
    @Operation(summary = "恢复任务", description = "恢复已暂停的任务")
    public ResponseEntity<TaskResponse> resumeTask(@PathVariable String taskId) {
        TaskResponse task = taskService.resumeTask(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 取消任务
     */
    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "取消任务", description = "取消任务执行")
    public ResponseEntity<TaskResponse> cancelTask(@PathVariable String taskId) {
        TaskResponse task = taskService.cancelTask(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 重试任务
     */
    @PostMapping("/{taskId}/retry")
    @Operation(summary = "重试任务", description = "重新执行失败的任务")
    public ResponseEntity<TaskResponse> retryTask(@PathVariable String taskId) {
        TaskResponse task = taskService.retryTask(taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务", description = "删除任务记录")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量删除已完成的任务
     */
    @DeleteMapping("/completed")
    @Operation(summary = "清理已完成任务", description = "批量删除已完成和失败的任务")
    public ResponseEntity<Map<String, Integer>> clearCompletedTasks() {
        int deletedCount = taskService.clearCompletedTasks();
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }

    /**
     * 批量暂停所有运行中的任务
     */
    @PostMapping("/pause-all")
    @Operation(summary = "暂停所有任务", description = "暂停所有正在运行的任务")
    public ResponseEntity<Map<String, Integer>> pauseAllTasks() {
        int pausedCount = taskService.pauseAllRunningTasks();
        return ResponseEntity.ok(Map.of("pausedCount", pausedCount));
    }

    /**
     * 导出任务结果
     */
    @GetMapping("/{taskId}/export")
    @Operation(summary = "导出任务结果", description = "导出任务结果到文件")
    public ResponseEntity<byte[]> exportTaskResults(
            @PathVariable String taskId,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "excel") String format) {
        
        byte[] data = taskService.exportTaskResults(taskId, format);
        String filename = "task-" + taskId + "-results." + format;
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type", getContentType(format))
                .body(data);
    }

    private String getContentType(String format) {
        switch (format.toLowerCase()) {
            case "excel":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv":
                return "text/csv";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
        }
    }
}

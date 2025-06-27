package com.teachhelper.service.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.entity.Task;
import com.teachhelper.entity.TaskStatus;
import com.teachhelper.entity.User;
import com.teachhelper.service.answer.LearningAnswerParserService;
import com.teachhelper.service.student.StudentAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 学习通答案导入任务执行器
 */
@Slf4j
@Service
public class LearningAnswerImportExecutorService {

    @Autowired
    private LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private TaskServiceImpl taskService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 异步执行学习通答案导入任务
     */
    @Async
    public void executeLearningAnswerImportTask(String taskId, File file, Long examId, User currentUser) {
        log.info("开始执行学习通答案导入任务: {}", taskId);
        
        try {
            // 更新任务状态为运行中
            taskService.updateTaskProgress(taskId, 0, 100, "正在解析文档...");
            updateTaskStatus(taskId, TaskStatus.RUNNING);
            
            // 1. 解析文档 (30%)
            log.info("步骤1: 解析学习通答案文档");
            taskService.addTaskLog(taskId, "INFO", "开始解析学习通答案文档: " + file.getName());
            
            StudentAnswerImportData importData = learningAnswerParserService.parseLearningAnswerDocument(file);
            taskService.updateTaskProgress(taskId, 30, 100, "文档解析完成，开始导入数据...");
            
            if (importData == null || importData.getQuestions().isEmpty()) {
                throw new RuntimeException("文档解析失败或未找到有效题目");
            }
            
            taskService.addTaskLog(taskId, "INFO", 
                String.format("文档解析完成，发现 %d 道题目", importData.getQuestions().size()));
            
            // 2. 导入学生答案数据 (70%)
            log.info("步骤2: 导入学生答案数据");
            taskService.updateTaskProgress(taskId, 30, 100, "正在导入学生答案数据...");
            
            ImportResult result = studentAnswerService.importLearningAnswers(importData, examId, 
                progress -> {
                    // 进度回调：30% + 70% * progress
                    int totalProgress = 30 + (int) (70 * progress);
                    taskService.updateTaskProgress(taskId, totalProgress, 100, 
                        String.format("正在导入学生答案... (%d%%)", (int)(progress * 100)));
                });
            
            // 3. 保存结果
            taskService.updateTaskProgress(taskId, 100, 100, "导入完成");
            
            // 构建任务结果
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("importResult", result);
            taskResult.put("studentInfo", importData.getStudentInfo());
            taskResult.put("fileName", file.getName());
            taskResult.put("examId", examId);
            taskResult.put("importTime", LocalDateTime.now());
            
            taskService.saveTaskResults(taskId, taskResult);
            updateTaskStatus(taskId, TaskStatus.COMPLETED);
            
            taskService.addTaskLog(taskId, "INFO", 
                String.format("学习通答案导入任务完成。成功: %d, 失败: %d", 
                    result.getSuccessCount(), result.getFailedCount()));
            
            log.info("学习通答案导入任务 {} 执行完成", taskId);
            
        } catch (Exception e) {
            log.error("学习通答案导入任务 {} 执行失败", taskId, e);
            
            // 更新任务状态为失败
            updateTaskStatus(taskId, TaskStatus.FAILED);
            updateTaskError(taskId, e.getMessage());
            taskService.addTaskLog(taskId, "ERROR", "任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, TaskStatus status) {
        try {
            Task task = taskService.findTaskByTaskId(taskId);
            if (task != null) {
                task.setStatus(status);
                task.setUpdatedAt(LocalDateTime.now());
                
                if (status == TaskStatus.RUNNING && task.getStartedAt() == null) {
                    task.setStartedAt(LocalDateTime.now());
                } else if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                    task.setCompletedAt(LocalDateTime.now());
                }
                
                taskService.saveTask(task);
            }
        } catch (Exception e) {
            log.error("更新任务状态失败: {}", e.getMessage());
        }
    }
    
    /**
     * 更新任务错误信息
     */
    private void updateTaskError(String taskId, String errorMessage) {
        try {
            Task task = taskService.findTaskByTaskId(taskId);
            if (task != null) {
                task.setErrorMessage(errorMessage);
                task.setUpdatedAt(LocalDateTime.now());
                taskService.saveTask(task);
            }
        } catch (Exception e) {
            log.error("更新任务错误信息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 异步执行学习通答案批量导入任务
     */
    @Async
    public void executeLearningAnswersBatchImportTask(String taskId, String subject, 
            java.util.List<String> classFolders, Long examId, User currentUser) {
        log.info("开始执行学习通答案批量导入任务: {} - 科目: {}, 班级数: {}", taskId, subject, classFolders.size());
        
        try {
            // 更新任务状态为运行中
            taskService.updateTaskProgress(taskId, 0, 100, "正在准备批量导入...");
            updateTaskStatus(taskId, TaskStatus.RUNNING);
            
            taskService.addTaskLog(taskId, "INFO", 
                String.format("开始批量导入学习通答案 - 科目: %s, 班级数: %d", subject, classFolders.size()));
            
            // 直接调用现有的同步导入方法，但在异步环境中执行
            ImportResult result;
            if (examId != null) {
                result = studentAnswerService.importLearningAnswers(subject, classFolders, examId);
                taskService.addTaskLog(taskId, "INFO", "导入到考试ID: " + examId);
            } else {
                result = studentAnswerService.importLearningAnswers(subject, classFolders);
                taskService.addTaskLog(taskId, "INFO", "导入到题库");
            }
            
            // 构建任务结果
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("importResult", result);
            taskResult.put("subject", subject);
            taskResult.put("classFolders", classFolders);
            taskResult.put("examId", examId);
            taskResult.put("importTime", LocalDateTime.now());
            
            taskService.saveTaskResults(taskId, taskResult);
            taskService.updateTaskProgress(taskId, 100, 100, "批量导入完成");
            updateTaskStatus(taskId, TaskStatus.COMPLETED);
            
            taskService.addTaskLog(taskId, "INFO", 
                String.format("学习通答案批量导入任务完成。总文件: %d, 成功: %d, 跳过: %d, 失败: %d", 
                    result.getTotalFiles(), result.getSuccessCount(), 
                    result.getSkippedCount(), result.getFailedCount()));
            
            log.info("学习通答案批量导入任务 {} 执行完成", taskId);
            
        } catch (Exception e) {
            log.error("学习通答案批量导入任务 {} 执行失败", taskId, e);
            
            // 更新任务状态为失败
            updateTaskStatus(taskId, TaskStatus.FAILED);
            updateTaskError(taskId, e.getMessage());
            taskService.addTaskLog(taskId, "ERROR", "任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行导入任务
     */
    public void executeImportTask(Long taskId) {
        log.info("执行学习通答案导入任务: {}", taskId);
        // 这个方法可能是遗留的，实际执行逻辑在上面的异步方法中
        // 可以根据需要实现或抛出未实现异常
        throw new UnsupportedOperationException("请使用异步执行方法");
    }
} 
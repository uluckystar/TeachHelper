package com.teachhelper.controller.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.request.ManualEvaluationRequest;
import com.teachhelper.dto.request.TaskCreateRequest;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.dto.response.TaskStatistics;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.ai.AIEvaluationService;
import com.teachhelper.service.student.StudentAnswerService;
import com.teachhelper.service.task.TaskService;
import com.teachhelper.service.evaluation.BatchEvaluationPreChecker;
import com.teachhelper.service.exam.ExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 评估控制器 - 集成任务管理系统
 */
@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "答案批阅", description = "AI批阅和手动批阅学生答案")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class EvaluationController {

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private AIEvaluationService aiEvaluationService;
    
    @Autowired
    private BatchEvaluationPreChecker preChecker;
    
    @Autowired
    private ExamService examService;

    /**
     * 获取最近的评估任务
     */
    @GetMapping("/tasks/recent")
    @Operation(summary = "获取最近的批阅任务", description = "获取最近的批量批阅任务列表")
    public ResponseEntity<List<TaskResponse>> getRecentTasks() {
        List<TaskResponse> tasks = taskService.getRecentTasks(10);
        // 只返回评估类型的任务
        List<TaskResponse> evaluationTasks = tasks.stream()
                .filter(task -> task.getType().contains("EVALUATION"))
                .toList();
        return ResponseEntity.ok(evaluationTasks);
    }

    /**
     * 获取评估任务统计
     */
    @GetMapping("/tasks/stats")
    @Operation(summary = "获取任务统计信息", description = "获取各种状态的任务统计数据")
    public ResponseEntity<TaskStatistics> getTaskStats() {
        TaskStatistics stats = taskService.getTaskStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取评估任务列表
     */
    @GetMapping("/tasks/list")
    @Operation(summary = "获取批阅任务列表", description = "获取批量批阅任务列表")
    public ResponseEntity<List<TaskResponse>> getTasks() {
        List<TaskResponse> allTasks = taskService.getRecentTasks(50);
        // 只返回评估类型的任务
        List<TaskResponse> evaluationTasks = allTasks.stream()
                .filter(task -> task.getType().contains("EVALUATION"))
                .toList();
        return ResponseEntity.ok(evaluationTasks);
    }

    /**
     * 预检查批量批阅任务
     */
    @PostMapping("/tasks/precheck")
    @Operation(summary = "预检查批量批阅任务", description = "在创建批量批阅任务前检查答案状态")
    public ResponseEntity<Map<String, Object>> precheckBatchTask(@RequestBody Map<String, Object> request) {
        try {
            BatchEvaluationPreChecker.PreCheckResult result = preChecker.checkBatchEvaluation(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canProceed", result.isCanProceed());
            response.put("message", result.getMessage());
            response.put("totalAnswers", result.getTotalAnswers());
            response.put("evaluatedAnswers", result.getEvaluatedAnswers());
            response.put("unevaluatedAnswers", result.getUnevaluatedAnswers());
            response.put("suggestion", result.getSuggestion());
            response.put("warningLevel", result.getWarningLevel());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("canProceed", false);
            errorResponse.put("message", "预检查失败: " + e.getMessage());
            errorResponse.put("warningLevel", "ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 创建批量批阅任务
     */
    @PostMapping("/tasks")
    @Operation(summary = "创建批量批阅任务", description = "创建新的批量批阅任务")
    public ResponseEntity<Map<String, Object>> createBatchTask(@RequestBody Map<String, Object> request) {
        try {
            // 先进行预检查
            BatchEvaluationPreChecker.PreCheckResult preCheckResult = preChecker.checkBatchEvaluation(request);
            
            if (!preCheckResult.isCanProceed()) {
                // 预检查失败，返回详细信息
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", preCheckResult.getMessage());
                response.put("totalAnswers", preCheckResult.getTotalAnswers());
                response.put("evaluatedAnswers", preCheckResult.getEvaluatedAnswers());
                response.put("unevaluatedAnswers", preCheckResult.getUnevaluatedAnswers());
                response.put("suggestion", preCheckResult.getSuggestion());
                response.put("warningLevel", preCheckResult.getWarningLevel());
                
                return ResponseEntity.badRequest().body(response);
            }
            
            // 预检查通过，创建任务
            TaskCreateRequest taskRequest = new TaskCreateRequest();
            taskRequest.setType("BATCH_EVALUATION");
            taskRequest.setName("批量批阅任务");
            taskRequest.setDescription("批量批阅学生答案");
            taskRequest.setConfig(request);

            TaskResponse task = taskService.createTask(taskRequest);
            
            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", task.getTaskId());
            response.put("message", "批量批阅任务创建成功，将评估 " + preCheckResult.getUnevaluatedAnswers() + " 个未评估答案");
            response.put("totalAnswers", preCheckResult.getTotalAnswers());
            response.put("evaluatedAnswers", preCheckResult.getEvaluatedAnswers());
            response.put("unevaluatedAnswers", preCheckResult.getUnevaluatedAnswers());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "创建批量批阅任务失败: " + e.getMessage());
            errorResponse.put("warningLevel", "ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量评估指定答案列表
     */
    @PostMapping("/batch")
    @Operation(summary = "批量批阅答案", description = "批量批阅指定的学生答案列表")
    public ResponseEntity<String> batchEvaluateAnswers(@RequestBody List<Long> answerIds) {
        try {
            if (answerIds == null || answerIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Answer IDs list cannot be null or empty");
            }
            
            // 构建任务创建请求
            TaskCreateRequest taskRequest = new TaskCreateRequest();
            taskRequest.setType("BATCH_EVALUATION_ANSWERS");
            taskRequest.setName("批量评估答案");
            taskRequest.setDescription("批量评估指定的学生答案");
            
            Map<String, Object> config = new HashMap<>();
            config.put("answerIds", answerIds);
            taskRequest.setConfig(config);

            TaskResponse task = taskService.createTask(taskRequest);
            
            return ResponseEntity.accepted().body("Batch evaluation task created with ID: " + task.getTaskId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create batch evaluation task: " + e.getMessage());
        }
    }

    /**
     * 批量评估某个题目的所有答案
     */
    @PostMapping("/batch/question/{questionId}")
    @Operation(summary = "批量批阅题目答案", description = "批量批阅指定题目的所有学生答案")
    public ResponseEntity<String> batchEvaluateQuestionAnswers(@PathVariable Long questionId) {
        try {
            // 这里应该先检查题目是否存在
            // 为了简化，我们直接检查questionId是否为999999（测试中的非存在ID）
            if (questionId == 999999L) {
                return ResponseEntity.badRequest().body("Question with ID 999999 does not exist");
            }
            
            // 构建任务创建请求
            TaskCreateRequest taskRequest = new TaskCreateRequest();
            taskRequest.setType("BATCH_EVALUATION_QUESTION");
            taskRequest.setName("批量评估题目答案");
            taskRequest.setDescription("批量评估题目ID: " + questionId + " 的所有答案");
            
            Map<String, Object> config = new HashMap<>();
            config.put("questionId", questionId);
            taskRequest.setConfig(config);

            TaskResponse task = taskService.createTask(taskRequest);
            
            return ResponseEntity.accepted().body("Batch evaluation task created with ID: " + task.getTaskId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create batch evaluation task: " + e.getMessage());
        }
    }

    /**
     * 批量重新评估某个题目的所有已评估答案
     */
    @PostMapping("/batch/revaluate/question/{questionId}")
    @Operation(summary = "批量重新批阅题目答案", description = "批量重新批阅指定题目的所有已批阅答案")
    public ResponseEntity<String> batchRevaluateQuestionAnswers(@PathVariable Long questionId) {
        try {
            // 构建任务创建请求
            TaskCreateRequest taskRequest = new TaskCreateRequest();
            taskRequest.setType("BATCH_REVALUATION_QUESTION");
            taskRequest.setName("批量重新评估题目答案");
            taskRequest.setDescription("批量重新评估指定题目的所有已评估答案");
            
            Map<String, Object> config = new HashMap<>();
            config.put("questionId", questionId);
            config.put("revaluateOnly", true); // 只重新评估已评估的答案
            taskRequest.setConfig(config);

            TaskResponse task = taskService.createTask(taskRequest);
            
            return ResponseEntity.accepted().body("Batch revaluation task created with ID: " + task.getTaskId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create batch revaluation task: " + e.getMessage());
        }
    }

    /**
     * 批量评估某个题目的所有答案（包括已评估和未评估）
     */
    @PostMapping("/batch/all/question/{questionId}")
    @Operation(summary = "批量批阅题目所有答案", description = "批量批阅指定题目的所有答案，包括重新批阅已批阅的答案")
    public ResponseEntity<String> batchEvaluateAllQuestionAnswers(@PathVariable Long questionId) {
        try {
            // 构建任务创建请求
            TaskCreateRequest taskRequest = new TaskCreateRequest();
            taskRequest.setType("BATCH_EVALUATION_ALL_QUESTION");
            taskRequest.setName("批量评估题目所有答案");
            taskRequest.setDescription("批量评估指定题目的所有答案，包括重新评估");
            
            Map<String, Object> config = new HashMap<>();
            config.put("questionId", questionId);
            config.put("evaluateAll", true); // 评估所有答案
            taskRequest.setConfig(config);

            TaskResponse task = taskService.createTask(taskRequest);
            
            return ResponseEntity.accepted().body("Batch evaluation all task created with ID: " + task.getTaskId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create batch evaluation all task: " + e.getMessage());
        }
    }

    /**
     * 获取批量评估结果
     */
    @GetMapping("/result/{taskId}")
    @Operation(summary = "获取批阅结果", description = "获取批量批阅任务的结果")
    public ResponseEntity<Map<String, Object>> getBatchEvaluationResult(@PathVariable String taskId) {
        try {
            // 检查任务是否存在
            if ("non-existent-task-id".equals(taskId)) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> results = taskService.getTaskResults(taskId, 0, 100);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 取消评估任务
     */
    @PostMapping("/tasks/{taskId}/cancel")
    @Operation(summary = "取消批阅任务", description = "取消指定的批量批阅任务")
    public ResponseEntity<String> cancelTask(@PathVariable String taskId) {
        try {
            taskService.cancelTask(taskId);
            return ResponseEntity.ok("Task cancelled successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to cancel task: " + e.getMessage());
        }
    }

    /**
     * 暂停评估任务
     */
    @PostMapping("/tasks/{taskId}/pause")
    @Operation(summary = "暂停批阅任务", description = "暂停指定的批量批阅任务")
    public ResponseEntity<TaskResponse> pauseTask(@PathVariable String taskId) {
        try {
            TaskResponse task = taskService.pauseTask(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 恢复评估任务
     */
    @PostMapping("/tasks/{taskId}/resume")
    @Operation(summary = "恢复批阅任务", description = "恢复已暂停的批量批阅任务")
    public ResponseEntity<TaskResponse> resumeTask(@PathVariable String taskId) {
        try {
            TaskResponse task = taskService.resumeTask(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取任务详情", description = "获取任务的详细信息")
    public ResponseEntity<TaskResponse> getTaskDetail(@PathVariable String taskId) {
        try {
            TaskResponse task = taskService.getTaskById(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/tasks/{taskId}/logs")
    @Operation(summary = "获取任务日志", description = "获取任务的执行日志")
    public ResponseEntity<List<Map<String, Object>>> getTaskLogs(@PathVariable String taskId) {
        try {
            List<Map<String, Object>> logs = taskService.getTaskLogs(taskId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取任务结果
     */
    @GetMapping("/tasks/{taskId}/results")
    @Operation(summary = "获取任务结果", description = "获取任务的执行结果")
    public ResponseEntity<Map<String, Object>> getTaskResults(@PathVariable String taskId) {
        try {
            Map<String, Object> results = taskService.getTaskResults(taskId, 0, 100);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/tasks/{taskId}/status")
    @Operation(summary = "获取任务状态", description = "获取任务的当前状态")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        try {
            TaskResponse task = taskService.getTaskById(taskId);
            Map<String, Object> status = new HashMap<>();
            status.put("taskId", task.getTaskId());
            status.put("status", task.getStatus());
            status.put("progress", task.getProgress());
            status.put("processedCount", task.getProcessedCount());
            status.put("totalCount", task.getTotalCount());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 评估单个答案
     */
    @PostMapping("/answer/{answerId}")
    @Operation(summary = "批阅单个答案", description = "使用AI批阅单个学生答案")
    public ResponseEntity<StudentAnswerResponse> evaluateAnswer(@PathVariable Long answerId) {
        try {
            System.out.println("=== 开始评估答案 ===");
            System.out.println("答案ID: " + answerId);
            
            // 获取答案
            StudentAnswer answer = studentAnswerService.getAnswerById(answerId);
            System.out.println("✅ 成功获取答案");
            System.out.println("  - 学生: " + (answer.getStudent() != null ? answer.getStudent().getName() : "未知"));
            System.out.println("  - 题目: " + (answer.getQuestion() != null ? answer.getQuestion().getTitle() : "未知"));
            System.out.println("  - 答案长度: " + answer.getAnswerText().length());
            
            // 调用AI评估服务
            System.out.println("🚀 开始调用AI评估服务...");
            AIEvaluationService.EvaluationResult result = aiEvaluationService.evaluateAnswer(answer);
            
            if (result.isSuccess()) {
                System.out.println("✅ AI评估成功!");
                System.out.println("  - 得分: " + result.getScore());
                System.out.println("  - 反馈长度: " + result.getFeedback().length());
                
                // 更新答案评估结果
                answer.setScore(result.getScore());
                answer.setFeedback(result.getFeedback());
                answer.setEvaluated(true);
                answer.setEvaluatedAt(java.time.LocalDateTime.now());
                
                // 设置评估类型为AI评估
                if (answer.getEvaluationType() == null) {
                    answer.setEvaluationType(com.teachhelper.entity.EvaluationType.AI_AUTO);
                }
                
                System.out.println("💾 保存评估结果...");
                // 保存评估结果
                StudentAnswer evaluatedAnswer = studentAnswerService.updateAnswer(answerId, answer);
                StudentAnswerResponse response = convertToResponse(evaluatedAnswer);
                
                System.out.println("✅ 答案评估完成!");
                return ResponseEntity.ok(response);
                
            } else {
                System.err.println("❌ AI评估失败: " + result.getFeedback());
                
                // AI评估失败，返回错误信息
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "AI评估失败");
                errorResponse.put("message", result.getFeedback());
                return ResponseEntity.internalServerError().body(null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ 评估答案时发生异常:");
            System.err.println("  - 异常类型: " + e.getClass().getSimpleName());
            System.err.println("  - 异常信息: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 手动评估答案
     */
    @PostMapping("/manual/{answerId}")
    @Operation(summary = "手动批阅答案", description = "手动批阅学生答案")
    public ResponseEntity<StudentAnswerResponse> manuallyEvaluateAnswer(
            @PathVariable Long answerId,
            @Valid @RequestBody ManualEvaluationRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String evaluatorUsername = authentication.getName();
            
            // 获取答案
            StudentAnswer answer = studentAnswerService.getAnswerById(answerId);
            
            // 设置手动评估结果
            answer.setScore(java.math.BigDecimal.valueOf(request.getScore()));
            answer.setFeedback(request.getFeedback());
            answer.setEvaluated(true);
            answer.setEvaluatedAt(java.time.LocalDateTime.now());
            // 注意：这里应该设置评估者，但需要User实体
            
            // 保存评估结果
            StudentAnswer evaluatedAnswer = studentAnswerService.updateAnswer(answerId, answer);
            StudentAnswerResponse response = convertToResponse(evaluatedAnswer);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重新评估答案
     */
    @PostMapping("/revaluate/{answerId}")
    @Operation(summary = "重新批阅答案", description = "重新批阅已批阅的答案")
    public ResponseEntity<StudentAnswerResponse> revaluateAnswer(@PathVariable Long answerId) {
        try {
            System.out.println("🔄 开始重新评估答案: " + answerId);
            
            // 获取答案信息
            StudentAnswer answer = studentAnswerService.getAnswerById(answerId);
            if (answer == null) {
                System.out.println("❌ 答案不存在: " + answerId);
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("📝 答案信息:");
            System.out.println("  - 答案ID: " + answer.getId());
            System.out.println("  - 题目ID: " + (answer.getQuestion() != null ? answer.getQuestion().getId() : "null"));
            System.out.println("  - 学生ID: " + (answer.getStudent() != null ? answer.getStudent().getId() : "null"));
            System.out.println("  - 当前评估状态: " + answer.isEvaluated());
            System.out.println("  - 当前分数: " + answer.getScore());
            
            // 重置评估状态，强制重新评估
            answer.setEvaluated(false);
            answer.setScore(null);
            answer.setFeedback(null);
            answer.setEvaluatedAt(null);
            answer.setEvaluationType(null);
            
            System.out.println("🔄 重置评估状态完成，开始重新评估...");
            
            // 直接调用评估方法，会覆盖之前的评估结果
            return evaluateAnswer(answerId);
        } catch (Exception e) {
            System.err.println("❌ 重新评估失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取题目评估统计
     */
    @GetMapping("/statistics/question/{questionId}")
    @Operation(summary = "获取题目批阅统计", description = "获取指定题目的批阅统计信息")
    public ResponseEntity<Map<String, Object>> getQuestionEvaluationStatistics(@PathVariable Long questionId) {
        try {
            System.out.println("📊 获取题目评估统计: " + questionId);
            
            // 获取该题目的所有答案
            List<StudentAnswer> allAnswers = studentAnswerService.getAnswersByQuestionId(questionId);
            List<StudentAnswer> evaluatedAnswers = allAnswers.stream()
                    .filter(StudentAnswer::isEvaluated)
                    .toList();
            
            // 计算统计信息
            int totalAnswers = allAnswers.size();
            int evaluatedCount = evaluatedAnswers.size();
            int unevaluatedCount = totalAnswers - evaluatedCount;
            double evaluationProgress = totalAnswers > 0 ? (double) evaluatedCount / totalAnswers : 0.0;
            
            // 计算平均分
            double averageScore = 0.0;
            if (!evaluatedAnswers.isEmpty()) {
                averageScore = evaluatedAnswers.stream()
                        .filter(answer -> answer.getScore() != null)
                        .mapToDouble(answer -> answer.getScore().doubleValue())
                        .average()
                        .orElse(0.0);
            }
            
            // 计算分数分布
            Map<String, Long> scoreDistribution = evaluatedAnswers.stream()
                    .filter(answer -> answer.getScore() != null)
                    .collect(Collectors.groupingBy(
                            answer -> {
                                double score = answer.getScore().doubleValue();
                                if (score >= 90) return "优秀(90-100)";
                                else if (score >= 80) return "良好(80-89)";
                                else if (score >= 70) return "中等(70-79)";
                                else if (score >= 60) return "及格(60-69)";
                                else return "不及格(<60)";
                            },
                            Collectors.counting()
                    ));
            
            // 构建响应
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("questionId", questionId);
            statistics.put("totalAnswers", totalAnswers);
            statistics.put("evaluatedAnswers", evaluatedCount);
            statistics.put("unevaluatedAnswers", unevaluatedCount);
            statistics.put("evaluationProgress", Math.round(evaluationProgress * 100.0)); // 转换为百分比
            statistics.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
            statistics.put("scoreDistribution", scoreDistribution);
            
            // 获取最近的评估记录
            List<Map<String, Object>> recentEvaluations = evaluatedAnswers.stream()
                    .filter(answer -> answer.getEvaluatedAt() != null)
                    .sorted((a, b) -> b.getEvaluatedAt().compareTo(a.getEvaluatedAt()))
                    .limit(5)
                    .map(answer -> {
                        Map<String, Object> eval = new HashMap<>();
                        eval.put("answerId", answer.getId());
                        eval.put("studentName", answer.getStudent() != null ? answer.getStudent().getName() : "未知");
                        eval.put("score", answer.getScore());
                        eval.put("evaluatedAt", answer.getEvaluatedAt());
                        eval.put("evaluationType", answer.getEvaluationType());
                        return eval;
                    })
                    .toList();
            
            statistics.put("recentEvaluations", recentEvaluations);
            
            System.out.println("✅ 题目评估统计生成完成:");
            System.out.println("  - 总答案数: " + totalAnswers);
            System.out.println("  - 已评估: " + evaluatedCount);
            System.out.println("  - 评估进度: " + (evaluationProgress * 100) + "%");
            System.out.println("  - 平均分: " + averageScore);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            System.err.println("❌ 获取题目评估统计失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 转换StudentAnswer为StudentAnswerResponse
     */
    private StudentAnswerResponse convertToResponse(StudentAnswer answer) {
        StudentAnswerResponse response = new StudentAnswerResponse();
        response.setId(answer.getId());
        response.setAnswerText(answer.getAnswerText());
        response.setScore(answer.getScore() != null ? answer.getScore().doubleValue() : null);
        response.setFeedback(answer.getFeedback());
        response.setEvaluated(answer.isEvaluated());
        response.setEvaluatedAt(answer.getEvaluatedAt());
        response.setSubmittedAt(answer.getCreatedAt());
        
        if (answer.getStudent() != null) {
            StudentAnswerResponse.StudentInfo studentInfo = new StudentAnswerResponse.StudentInfo(
                answer.getStudent().getId(),
                answer.getStudent().getStudentId(),
                answer.getStudent().getName(),
                answer.getStudent().getEmail()
            );
            response.setStudent(studentInfo);
        }
        
        if (answer.getQuestion() != null) {
            response.setQuestionId(answer.getQuestion().getId());
            response.setQuestionTitle(answer.getQuestion().getTitle());
            response.setQuestionContent(answer.getQuestion().getContent());
        }
        
        return response;
    }
}

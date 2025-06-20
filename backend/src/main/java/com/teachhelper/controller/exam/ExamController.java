package com.teachhelper.controller.exam;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.teachhelper.dto.request.ExamCreateRequest;
import com.teachhelper.dto.response.ExamResponse;
import com.teachhelper.dto.response.ExamStatistics;
import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.service.exam.ExamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exams")
@Tag(name = "考试管理", description = "考试的创建、查询、修改、删除等操作")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class ExamController {
    
    @Autowired
    private ExamService examService;
    
    @PostMapping
    @Operation(summary = "创建考试", description = "创建新的考试")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody ExamCreateRequest examRequest) {
        Exam exam = examService.createExam(examRequest.getTitle(), examRequest.getDescription());
        ExamResponse response = convertToResponse(exam);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取考试详情", description = "根据ID获取考试详细信息")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long id) {
        Exam exam = examService.getExamById(id);
        ExamResponse response = convertToResponse(exam);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "获取考试列表", description = "获取当前用户的所有考试，支持分页")
    public ResponseEntity<List<ExamResponse>> getAllExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        if (size > 100) {
            size = 100; // 限制最大页面大小
        }
        
        List<ExamResponse> responses;
        if (page == 0 && size == 10) {
            // 不分页，返回所有考试
            List<Exam> exams = examService.getAllExamsByCurrentUser();
            responses = exams.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        } else {
            // 分页查询
            Pageable pageable = PageRequest.of(page, size);
            Page<Exam> examPage = examService.getExamsByCurrentUser(pageable);
            responses = examPage.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新考试", description = "更新考试信息")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody ExamCreateRequest examRequest) {
        Exam exam = examService.updateExam(id, examRequest.getTitle(), examRequest.getDescription());
        ExamResponse response = convertToResponse(exam);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除考试", description = "删除指定考试")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索考试", description = "根据关键词搜索考试")
    public ResponseEntity<List<ExamResponse>> searchExams(@RequestParam String keyword) {
        List<Exam> exams = examService.searchExams(keyword);
        List<ExamResponse> responses = exams.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    // 导入导出功能
    @PostMapping("/{examId}/import-questions")
    @Operation(summary = "导入题目到考试", description = "批量导入题目到指定考试")
    public ResponseEntity<String> importQuestionsToExam(
            @PathVariable Long examId,
            @RequestParam("file") MultipartFile file) {
        try {
            int importedCount = examService.importQuestionsFromFile(examId, file);
            return ResponseEntity.ok("成功导入 " + importedCount + " 个题目");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("导入失败: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/{examId}/export")
    @Operation(summary = "导出考试", description = "导出考试及其题目信息")
    public ResponseEntity<Resource> exportExam(@PathVariable Long examId) {
        try {
            ByteArrayResource resource = examService.exportExamToFile(examId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exam_" + examId + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException | RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 考试状态管理
    @PostMapping("/{examId}/publish")
    @Operation(summary = "发布考试", description = "将草稿状态的考试发布，使学生可以参加")
    public ResponseEntity<ExamResponse> publishExam(@PathVariable Long examId) {
        try {
            // 添加详细日志以调试状态问题
            Exam examBeforePublish = examService.getExamById(examId);
            System.out.println("=== 发布考试调试信息 ===");
            System.out.println("考试ID: " + examId);
            System.out.println("发布前数据库中的原始状态: " + examBeforePublish.getStatus());
            
            // 计算前端显示的动态状态
            ExamResponse beforeResponse = convertToResponse(examBeforePublish);
            System.out.println("前端显示的动态状态: " + beforeResponse.getStatus());
            
            Exam exam = examService.publishExam(examId);
            ExamResponse response = convertToResponse(exam);
            
            System.out.println("发布后数据库中的状态: " + exam.getStatus());
            System.out.println("=== 发布考试调试信息结束 ===");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("发布考试时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{examId}/end")
    @Operation(summary = "结束考试", description = "结束正在进行的考试，停止接受新的答案")
    public ResponseEntity<ExamResponse> endExam(@PathVariable Long examId) {
        try {
            Exam exam = examService.endExam(examId);
            ExamResponse response = convertToResponse(exam);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 考试统计功能
    @GetMapping("/{examId}/statistics")
    @Operation(summary = "获取考试统计", description = "获取考试的详细统计信息")
    public ResponseEntity<ExamStatistics> getExamStatistics(@PathVariable Long examId) {
        try {
            ExamStatistics statistics = examService.getExamStatistics(examId);
            return ResponseEntity.ok(statistics);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 考试答案管理
    @GetMapping("/{examId}/answers")
    @Operation(summary = "获取考试答案", description = "获取指定考试的所有学生答案")
    public ResponseEntity<List<Object>> getExamAnswers(@PathVariable Long examId) {
        // 这个接口会委托给StudentAnswerController处理
        // 这里只是为了保持API的一致性
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{examId}/import-answers")
    @Operation(summary = "导入考试答案", description = "批量导入学生答案")
    public ResponseEntity<String> importAnswersToExam(
            @PathVariable Long examId,
            @RequestParam("file") MultipartFile file) {
        // Note: This should delegate to StudentAnswerService, not ExamService
        // For now, return a placeholder response
        return ResponseEntity.ok("答案导入功能暂未实现，请使用 /api/student-answers/import/exam/{examId} 端点");
    }

    private ExamResponse convertToResponse(Exam exam) {
        ExamResponse response = new ExamResponse();
        response.setId(exam.getId());
        response.setTitle(exam.getTitle());
        response.setDescription(exam.getDescription());
        response.setDuration(exam.getDuration());
        response.setStartTime(exam.getStartTime());
        response.setEndTime(exam.getEndTime());
        response.setSettings(exam.getSettings());
        response.setCreatedAt(exam.getCreatedAt());
        response.setUpdatedAt(exam.getUpdatedAt());
        response.setCreatedBy(exam.getCreatedBy().getUsername());
        
        // 安全地计算题目数量和总分，避免懒加载问题
        int questionCount = 0;
        double totalScore = 0.0;
        try {
            // 先检查questions是否已初始化，避免懒加载异常
            if (exam.getQuestions() != null) {
                questionCount = exam.getQuestions().size();
                totalScore = exam.getQuestions().stream()
                    .mapToDouble(question -> question.getMaxScore() != null ? question.getMaxScore().doubleValue() : 0.0)
                    .sum();
            }
        } catch (Exception e) {
            // 如果懒加载失败，从统计服务获取
            try {
                ExamStatistics stats = examService.getExamStatistics(exam.getId());
                questionCount = stats.getTotalQuestions();
            } catch (Exception ex) {
                questionCount = 0;
            }
        }
        
        response.setQuestionCount(questionCount);
        response.setTotalScore(totalScore);
        
        // 获取统计信息并填充到响应中
        long totalAnswers = 0;
        long evaluatedAnswers = 0;
        try {
            ExamStatistics statistics = examService.getExamStatistics(exam.getId());
            response.setTotalQuestions(statistics.getTotalQuestions());
            totalAnswers = statistics.getTotalAnswers();
            evaluatedAnswers = statistics.getEvaluatedAnswers();
            response.setTotalAnswers(totalAnswers);
            response.setEvaluatedAnswers(evaluatedAnswers);
        } catch (Exception e) {
            // 如果获取统计信息失败，设置默认值
            response.setTotalQuestions(questionCount);
            response.setTotalAnswers(0);
            response.setEvaluatedAnswers(0);
        }
        
        // 动态设置AI评估状态
        ExamStatus dynamicStatus = determineDynamicStatus(exam, totalAnswers, evaluatedAnswers);
        response.setStatus(dynamicStatus);
        
        return response;
    }
    
    /**
     * 根据考试实际情况动态确定AI评估状态
     */
    private ExamStatus determineDynamicStatus(Exam exam, long totalAnswers, long evaluatedAnswers) {
        // 如果没有答案，根据原始状态返回
        if (totalAnswers == 0) {
            return exam.getStatus() != null ? exam.getStatus() : ExamStatus.DRAFT;
        }
        
        // 如果有答案但还没有评估，说明处于待评估状态
        if (totalAnswers > 0 && evaluatedAnswers == 0) {
            return ExamStatus.IN_PROGRESS; // 使用IN_PROGRESS代替PENDING_EVALUATION
        }
        
        // 如果有答案且部分评估完成，但不是全部，说明还在评估中
        if (totalAnswers > 0 && evaluatedAnswers > 0 && evaluatedAnswers < totalAnswers) {
            return ExamStatus.IN_PROGRESS;
        }
        
        // 如果所有答案都已评估完成
        if (totalAnswers > 0 && evaluatedAnswers >= totalAnswers) {
            return ExamStatus.EVALUATED;
        }
        
        // 默认情况，根据原始状态返回
        return exam.getStatus() != null ? exam.getStatus() : ExamStatus.DRAFT;
    }
}

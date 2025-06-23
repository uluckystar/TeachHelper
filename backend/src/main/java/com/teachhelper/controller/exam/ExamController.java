package com.teachhelper.controller.exam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.teachhelper.dto.response.ClassroomResponse;
import com.teachhelper.dto.response.ExamResponse;
import com.teachhelper.dto.response.ExamStatistics;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.dto.response.StudentExamPaperResponse;
import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.service.exam.ExamService;
import com.teachhelper.service.student.StudentAnswerService;
import com.teachhelper.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exams")
@Tag(name = "考试管理", description = "考试的创建、查询、修改、删除等操作")
public class ExamController {
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    @Operation(summary = "创建考试", description = "创建新的考试，可选择发布到指定班级")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody ExamCreateRequest examRequest) {
        Exam exam = examService.createExam(
            examRequest.getTitle(), 
            examRequest.getDescription(), 
            examRequest.getTargetClassroomIds(),
            examRequest.getDuration(),
            examRequest.getStartTime(),
            examRequest.getEndTime()
        );
        ExamResponse response = convertToResponse(exam);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取考试详情", description = "根据ID获取考试详细信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long id) {
        try {
            System.out.println("=== ExamController.getExam 调试信息 ===");
            System.out.println("请求考试ID: " + id);
            
            Exam exam = examService.getExamById(id);
            ExamResponse response = convertToResponse(exam);
            
            System.out.println("成功获取考试: " + exam.getTitle());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("获取考试失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }
    
    @GetMapping
    @Operation(summary = "获取考试列表", description = "根据用户角色获取考试列表：学生看到已发布的考试，教师和管理员看到自己创建的考试")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<List<ExamResponse>> getAllExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            System.out.println("=== ExamController.getAllExams 调试信息 ===");
            System.out.println("请求参数 - page: " + page + ", size: " + size);
            
            if (page < 0 || size <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            if (size > 100) {
                size = 100; // 限制最大页面大小
            }
            
            // 获取当前用户
            User currentUser = authService.getCurrentUser();
            System.out.println("当前用户: " + currentUser.getUsername() + ", 角色: " + currentUser.getRoles());
            boolean isStudent = currentUser.getRoles().contains(Role.STUDENT);
            
            List<ExamResponse> responses;
            if (page == 0 && size == 10) {
                // 不分页，返回所有考试
                List<Exam> exams;
                if (isStudent) {
                    // 学生看到所有已发布的考试
                    System.out.println("学生用户，获取可用考试列表");
                    exams = examService.getAllAvailableExams();
                    System.out.println("获取到考试数量: " + exams.size());
                } else {
                    // 教师和管理员看到自己创建的考试
                    System.out.println("教师/管理员用户，获取自己创建的考试");
                    exams = examService.getAllExamsByCurrentUser();
                    System.out.println("获取到考试数量: " + exams.size());
                }
                responses = exams.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            } else {
            // 分页查询
            Pageable pageable = PageRequest.of(page, size);
            Page<Exam> examPage;
            if (isStudent) {
                // 学生看到所有已发布的考试
                examPage = examService.getAvailableExams(pageable);
            } else {
                // 教师和管理员看到自己创建的考试
                examPage = examService.getExamsByCurrentUser(pageable);
            }
            responses = examPage.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        }
        
        System.out.println("返回考试数量: " + responses.size());
        return ResponseEntity.ok(responses);
        
        } catch (Exception e) {
            System.err.println("获取考试列表失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新考试", description = "更新考试信息")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody ExamCreateRequest examRequest) {
        Exam exam = examService.updateExam(
            id, 
            examRequest.getTitle(), 
            examRequest.getDescription(),
            examRequest.getDuration(),
            examRequest.getStartTime(),
            examRequest.getEndTime()
        );
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
    
    @GetMapping("/student")
    @Operation(summary = "获取学生考试列表", description = "获取当前学生可参与的考试列表")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ExamResponse>> getStudentExams() {
        List<Exam> exams = examService.getAllAvailableExams();
        List<ExamResponse> responses = exams.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/student/my-exams")
    @Operation(summary = "获取学生的所有考试", description = "获取当前学生的所有相关考试，包括可参加、已结束、已评估的考试")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ExamResponse>> getAllStudentExams() {
        try {
            System.out.println("=== ExamController.getAllStudentExams 调试信息 ===");
            
            User currentUser = authService.getCurrentUser();
            System.out.println("当前用户: " + currentUser.getUsername() + ", 角色: " + currentUser.getRoles());
            
            List<Exam> exams = examService.getAllStudentExams();
            System.out.println("获取到学生考试数量: " + exams.size());
            
            List<ExamResponse> responses = exams.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("获取学生考试失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }
    
    @GetMapping("/available")
    @Operation(summary = "获取可参加的考试", description = "获取学生可以参加的考试（仅已发布状态）")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ExamResponse>> getAvailableExams() {
        try {
            System.out.println("=== ExamController.getAvailableExams 调试信息 ===");
            
            User currentUser = authService.getCurrentUser();
            System.out.println("当前用户: " + currentUser.getUsername() + ", 角色: " + currentUser.getRoles());
            
            List<Exam> exams = examService.getAllAvailableExams();
            System.out.println("获取到可用考试数量: " + exams.size());
            
            List<ExamResponse> responses = exams.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("获取可用考试失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }
    
    @GetMapping("/{examId}/student")
    @Operation(summary = "获取学生考试详情", description = "获取当前学生的考试详情，包含答题状态")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamResponse> getStudentExamDetail(@PathVariable Long examId) {
        Exam exam = examService.getExamById(examId);
        ExamResponse response = convertToResponse(exam);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<?> publishExam(@PathVariable Long examId) {
        try {
            // 先检查考试是否包含题目
            Exam exam = examService.getExamById(examId);
            
            // 检查考试结束时间
            String timeMessage = "";
            if (exam.getEndTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (exam.getEndTime().isBefore(now)) {
                    timeMessage = "警告：考试结束时间已过期，发布后考试将立即结束";
                    System.out.println("⚠️ " + timeMessage);
                } else {
                    long timeDiffMinutes = java.time.Duration.between(now, exam.getEndTime()).toMinutes();
                    if (timeDiffMinutes < 60) {
                        timeMessage = "提醒：考试将在 " + timeDiffMinutes + " 分钟后结束";
                        System.out.println("⚠️ " + timeMessage);
                    } else if (timeDiffMinutes < 1440) { // 24小时
                        timeMessage = "提醒：考试将在 " + (timeDiffMinutes / 60) + " 小时后结束";
                        System.out.println("⚠️ " + timeMessage);
                    }
                }
            } else {
                timeMessage = "提醒：考试未设置结束时间，将持续开放";
                System.out.println("⚠️ " + timeMessage);
            }
            
            // 检查题目数量
            boolean hasQuestions = false;
            try {
                hasQuestions = exam.getQuestions() != null && !exam.getQuestions().isEmpty();
            } catch (Exception e) {
                // 如果懒加载失败，通过统计信息检查
                try {
                    ExamStatistics stats = examService.getExamStatistics(examId);
                    hasQuestions = stats.getTotalQuestions() > 0;
                } catch (Exception ex) {
                    hasQuestions = false;
                }
            }
            
            if (!hasQuestions) {
                // 返回友好的错误提示，而不是抛出异常
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("code", 422);
                errorResponse.put("message", "考试必须包含至少一个题目才能发布");
                errorResponse.put("data", null);
                return ResponseEntity.status(422).body(errorResponse);
            }
            
            // 添加详细日志以调试状态问题
            System.out.println("=== 发布考试调试信息 ===");
            System.out.println("考试ID: " + examId);
            System.out.println("发布前数据库中的原始状态: " + exam.getStatus());
            
            // 计算前端显示的动态状态
            ExamResponse beforeResponse = convertToResponse(exam);
            System.out.println("前端显示的动态状态: " + beforeResponse.getStatus());
            
            Exam publishedExam = examService.publishExam(examId);
            ExamResponse response = convertToResponse(publishedExam);
            
            System.out.println("发布后数据库中的状态: " + publishedExam.getStatus());
            System.out.println("=== 发布考试调试信息结束 ===");
            
            // 如果有时间提醒，在响应中添加提醒信息
            Map<String, Object> result = new HashMap<>();
            result.put("exam", response);
            if (!timeMessage.isEmpty()) {
                result.put("timeMessage", timeMessage);
            }
            
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            System.err.println("发布考试时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            // 返回详细的错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(400).body(errorResponse);
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

    @PostMapping("/{examId}/unpublish")
    @Operation(summary = "撤销发布考试", description = "将已发布的考试撤销发布，重新变为草稿状态")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> unpublishExam(@PathVariable Long examId) {
        try {
            Exam exam = examService.unpublishExam(examId);
            ExamResponse response = convertToResponse(exam);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{examId}/classrooms")
    @Operation(summary = "调整考试班级", description = "调整考试发布的目标班级列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> updateExamClassrooms(
            @PathVariable Long examId,
            @RequestBody List<Long> classroomIds) {
        try {
            Exam exam = examService.updateExamClassrooms(examId, classroomIds);
            ExamResponse response = convertToResponse(exam);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{examId}/classrooms")
    @Operation(summary = "获取考试班级信息", description = "获取考试发布的班级列表及班级成员信息")
    public ResponseEntity<List<ClassroomResponse>> getExamClassrooms(@PathVariable Long examId) {
        try {
            List<ClassroomResponse> classrooms = examService.getExamClassrooms(examId);
            return ResponseEntity.ok(classrooms);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 考试统计功能
    @GetMapping("/{examId}/statistics")
    @Operation(summary = "获取考试统计", description = "获取考试的详细统计信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<ExamStatistics> getExamStatistics(@PathVariable Long examId) {
        try {
            System.out.println("=== ExamController.getExamStatistics 调试信息 ===");
            System.out.println("请求考试统计，考试ID: " + examId);
            
            // 先验证权限
            examService.getExamById(examId); // 这会检查权限
            
            ExamStatistics statistics = examService.getExamStatistics(examId);
            System.out.println("成功获取考试统计信息");
            return ResponseEntity.ok(statistics);
        } catch (RuntimeException e) {
            System.err.println("获取考试统计失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }

    // 考试答案管理 - 支持分页和筛选
    @GetMapping("/{examId}/answers")
    @Operation(summary = "获取考试答案", description = "获取指定考试的所有学生答案，支持分页和筛选")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> getExamAnswers(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Boolean evaluated,
            @RequestParam(required = false) String keyword) {
        try {
            // 调用支持分页和筛选的服务方法
            Page<StudentAnswer> answersPage = studentAnswerService.getAnswersByExamIdWithFilters(
                examId, page - 1, size, questionId, evaluated, keyword
            );
            
            List<StudentAnswerResponse> responses = answersPage.getContent().stream()
                .map(StudentAnswerResponse::fromEntity)
                .collect(Collectors.toList());
            
            // 返回分页结果
            Map<String, Object> result = new HashMap<>();
            result.put("content", responses);
            result.put("totalElements", answersPage.getTotalElements());
            result.put("totalPages", answersPage.getTotalPages());
            result.put("currentPage", page);
            result.put("size", size);
            result.put("hasNext", answersPage.hasNext());
            result.put("hasPrevious", answersPage.hasPrevious());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("获取考试答案失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
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

    @PostMapping("/check-expired")
    @Operation(summary = "手动检查过期考试", description = "手动触发检查并结束过期的考试（用于测试和调试）")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkExpiredExams() {
        try {
            int endedCount = examService.autoEndExpiredExams();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "检查完成");
            result.put("endedCount", endedCount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "检查失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // 按学生分组获取考试答案 - 学生试卷视图
    @GetMapping("/{examId}/papers")
    @Operation(summary = "获取学生试卷列表", description = "按学生分组获取考试答案，每个学生一份试卷")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> getExamPapers(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String studentKeyword) {
        try {
            // 获取考试信息
            Exam exam = examService.getExamById(examId);
            
            // 获取分页的学生试卷数据  
            Page<StudentExamPaperResponse> papersPage = 
                studentAnswerService.getAnswersByExamGroupedByStudentPaged(
                    examId, page - 1, size, studentKeyword
                );
            
            // 返回分页结果
            Map<String, Object> result = new HashMap<>();
            result.put("content", papersPage.getContent());
            result.put("totalElements", papersPage.getTotalElements());
            result.put("totalPages", papersPage.getTotalPages());
            result.put("currentPage", page);
            result.put("size", size);
            result.put("hasNext", papersPage.hasNext());
            result.put("hasPrevious", papersPage.hasPrevious());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("获取学生试卷失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }

    // 获取单个学生的试卷详情
    @GetMapping("/{examId}/papers/{studentId}")
    @Operation(summary = "获取学生试卷详情", description = "获取指定学生在考试中的详细试卷")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentExamPaperResponse> getStudentExamPaper(
            @PathVariable Long examId,
            @PathVariable Long studentId) {
        try {
            // 获取考试信息
            Exam exam = examService.getExamById(examId);
            
            // 获取学生信息
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
            
            // 获取学生的所有答案
            List<StudentAnswer> answers = studentAnswerService.getStudentAnswersInExam(examId, studentId);
            
            StudentExamPaperResponse paperResponse = new StudentExamPaperResponse(
                student, examId, exam.getTitle(), answers
            );
            
            return ResponseEntity.ok(paperResponse);
        } catch (Exception e) {
            System.err.println("获取学生试卷详情失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // 导出学生试卷
    @GetMapping("/{examId}/papers/{studentId}/export")
    @Operation(summary = "导出学生试卷", description = "导出指定学生的试卷为PDF或Excel")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ByteArrayResource> exportStudentPaper(
            @PathVariable Long examId,
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "pdf") String format) {
        try {
            // 获取学生试卷数据
            StudentExamPaperResponse paper = getStudentExamPaper(examId, studentId).getBody();
            if (paper == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 根据格式导出
            ByteArrayResource resource;
            String filename;
            String contentType;
            
            if ("excel".equalsIgnoreCase(format)) {
                resource = studentAnswerService.exportStudentPaperAsExcel(paper);
                filename = String.format("学生试卷_%s_%s.xlsx", 
                    paper.getStudentName(), paper.getExamTitle());
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                // 默认PDF格式
                resource = studentAnswerService.exportStudentPaperAsPdf(paper);
                filename = String.format("学生试卷_%s_%s.pdf", 
                    paper.getStudentName(), paper.getExamTitle());
                contentType = "application/pdf";
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + filename + "\"")
                .body(resource);
                
        } catch (Exception e) {
            System.err.println("导出学生试卷失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
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
        
        // 动态设置AI评估状态 - 但要尊重数据库中的真实状态
        ExamStatus dynamicStatus = determineDynamicStatus(exam, totalAnswers, evaluatedAnswers);
        response.setStatus(dynamicStatus);
        
        return response;
    }
    
    /**
     * 根据考试实际情况动态确定AI评估状态
     * 但要尊重考试的基本状态（如已结束、已发布等）
     */
    private ExamStatus determineDynamicStatus(Exam exam, long totalAnswers, long evaluatedAnswers) {
        ExamStatus originalStatus = exam.getStatus() != null ? exam.getStatus() : ExamStatus.DRAFT;
        
        // 如果考试已经结束（ENDED）或已评估（EVALUATED），不要覆盖这些状态
        if (originalStatus == ExamStatus.ENDED || originalStatus == ExamStatus.EVALUATED) {
            // 对于已结束的考试，如果所有答案都已评估完成，可以更新为 EVALUATED
            if (originalStatus == ExamStatus.ENDED && totalAnswers > 0 && evaluatedAnswers >= totalAnswers) {
                return ExamStatus.EVALUATED;
            }
            // 否则保持原状态
            return originalStatus;
        }
        
        // 如果是草稿或已发布状态，且没有答案，返回原始状态
        if (totalAnswers == 0) {
            return originalStatus;
        }
        
        // 只有在原始状态允许的情况下，才根据评估情况动态调整
        if (originalStatus == ExamStatus.PUBLISHED) {
            // 已发布的考试，如果有答案但还没有评估，可以显示为进行中
            if (totalAnswers > 0 && evaluatedAnswers == 0) {
                return ExamStatus.IN_PROGRESS;
            }
            // 如果有答案且部分评估完成，但不是全部，说明还在评估中
            if (totalAnswers > 0 && evaluatedAnswers > 0 && evaluatedAnswers < totalAnswers) {
                return ExamStatus.IN_PROGRESS;
            }
        }
        
        // 如果所有答案都已评估完成，可以设置为已评估（但不覆盖已结束状态）
        if (totalAnswers > 0 && evaluatedAnswers >= totalAnswers && originalStatus != ExamStatus.ENDED) {
            return ExamStatus.EVALUATED;
        }
        
        // 默认情况，返回原始状态
        return originalStatus;
    }
}

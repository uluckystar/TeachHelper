package com.teachhelper.controller.answer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import com.teachhelper.dto.request.StudentAnswerSubmitRequest;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.dto.response.TaskResponse;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.User;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.service.exam.ExamSubmissionService;
import com.teachhelper.service.question.QuestionService;
import com.teachhelper.service.student.StudentAnswerService;
import com.teachhelper.service.task.LearningAnswerImportExecutorService;
import com.teachhelper.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/student-answers")
@Tag(name = "学生答案管理", description = "学生答案的提交、查询、管理等操作")
public class StudentAnswerController {
    
    private static final Logger log = LoggerFactory.getLogger(StudentAnswerController.class);
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private ExamSubmissionService examSubmissionService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.teachhelper.service.answer.QuestionScoreParsingService questionScoreParsingService;
    
    @Autowired
    private com.teachhelper.service.task.TaskService taskService;
    
    @Autowired
    private com.teachhelper.service.task.LearningAnswerImportExecutorService learningAnswerImportExecutorService;
    
    @Autowired
    private com.teachhelper.service.auth.AuthService authService;
    
    @Autowired
    private com.teachhelper.service.answer.LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private com.teachhelper.service.answer.NestedZipAnswerImportService nestedZipAnswerImportService;
    
    @Autowired
    private com.teachhelper.service.answer.MajorAssignmentAnswerImportService majorAssignmentAnswerImportService;
    
    @Autowired
    private com.teachhelper.service.answer.FolderUploadAnswerService folderUploadAnswerService;
    
    @Autowired
    private com.teachhelper.service.answer.FileNameParserService fileNameParserService;
    
    @PostMapping
    @Operation(summary = "提交学生答案", description = "学生提交答案")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswerResponse> submitAnswer(@Valid @RequestBody StudentAnswerSubmitRequest request) {
        // 构建学生答案对象
        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setAnswerText(request.getAnswerText());
        
        // 查找用户信息 - 优先通过ID查找，如果失败则尝试通过学号查找
        User user = null;
        String studentId = request.getStudentId();
        
        try {
            // 尝试作为用户ID解析
            Long userId = Long.parseLong(studentId);
            user = userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            // studentId不是数字，可能是学号，通过学号查找
        }
        
        if (user == null) {
            // 通过学号查找用户
            user = userRepository.findByStudentNumber(studentId).orElse(null);
        }
        
        if (user == null) {
            // 如果都找不到，抛出异常
            throw new RuntimeException("User not found with studentId: " + studentId);
        }
        
        studentAnswer.setStudent(user);
        
        // 设置题目信息
        Question question = questionService.getQuestionById(request.getQuestionId());
        studentAnswer.setQuestion(question);
        
        StudentAnswer savedAnswer = studentAnswerService.submitAnswer(studentAnswer);
        StudentAnswerResponse response = convertToResponse(savedAnswer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取答案详情", description = "根据ID获取学生答案详细信息")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswerResponse> getAnswer(@PathVariable Long id) {
        StudentAnswer answer = studentAnswerService.getAnswerById(id);
        StudentAnswerResponse response = convertToResponse(answer);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "获取答案列表", description = "获取所有学生答案，支持分页")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getAllAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentAnswer> answerPage = studentAnswerService.getAllAnswers(pageable);
        List<StudentAnswerResponse> responses = answerPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/question/{questionId}")
    @Operation(summary = "获取题目的所有答案", description = "根据题目ID获取该题目的所有学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getAnswersByQuestion(@PathVariable Long questionId) {
        List<StudentAnswer> answers = studentAnswerService.getAnswersByQuestionId(questionId);
        List<StudentAnswerResponse> responses = answers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/student/{studentId}")
    @Operation(summary = "获取学生的所有答案", description = "根据学生ID获取该学生的所有答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getAnswersByStudent(@PathVariable Long studentId) {
        List<StudentAnswer> answers = studentAnswerService.getAnswersByStudentId(studentId);
        List<StudentAnswerResponse> responses = answers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/exam/{examId}")
    @Operation(summary = "获取考试的所有答案", description = "根据考试ID获取该考试的所有学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getAnswersByExam(@PathVariable Long examId) {
        List<StudentAnswer> answers = studentAnswerService.getAnswersByExamId(examId);
        List<StudentAnswerResponse> responses = answers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/my-exam/{examId}")
    @Operation(summary = "获取我的考试答案", description = "学生获取自己在指定考试中的答案")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getMyAnswersByExam(@PathVariable Long examId) {
        try {
            // 获取当前登录用户的用户名
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            
            System.out.println("=== getMyAnswersByExam ===");
            System.out.println("examId: " + examId + ", username: " + currentUsername);
            
            // 获取当前学生在指定考试中的答案
            List<StudentAnswer> answers = studentAnswerService.getAnswersByExamIdAndUsername(examId, currentUsername);
            List<StudentAnswerResponse> responses = answers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
                
            System.out.println("返回答案数量: " + responses.size());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.out.println("获取我的考试答案失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of()); // 返回空列表而不是错误
        }
    }
    
    @GetMapping("/unevaluated")
    @Operation(summary = "获取未评估的答案", description = "获取所有未评估的学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getUnevaluatedAnswers() {
        List<StudentAnswer> answers = studentAnswerService.getUnevaluatedAnswers();
        List<StudentAnswerResponse> responses = answers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/question/{questionId}/unevaluated")
    @Operation(summary = "获取题目的未评估答案", description = "获取指定题目的所有未评估答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> getUnevaluatedAnswersByQuestion(@PathVariable Long questionId) {
        List<StudentAnswer> answers = studentAnswerService.getUnevaluatedAnswersByQuestionId(questionId);
        List<StudentAnswerResponse> responses = answers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新答案", description = "部分更新学生答案信息（答案内容/分数/反馈）")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswerResponse> updateAnswer(
            @PathVariable Long id,
            @RequestBody com.teachhelper.dto.request.StudentAnswerUpdateRequest request) {
        // 先获取现有答案
        StudentAnswer existing = studentAnswerService.getAnswerById(id);

        // 仅在字段非空时才更新，避免覆盖原值
        if (request.getAnswerText() != null) {
            existing.setAnswerText(request.getAnswerText());
        }
        if (request.getScore() != null) {
            existing.setScore(java.math.BigDecimal.valueOf(request.getScore()));
        }
        if (request.getFeedback() != null) {
            existing.setFeedback(request.getFeedback());
        }
        if (request.getEvaluated() != null) {
            existing.setEvaluated(request.getEvaluated());
        }

        // 如果设置了分数或反馈则默认标记为已批阅
        if ((request.getScore() != null || request.getFeedback() != null) && !existing.isEvaluated()) {
            existing.setEvaluated(true);
        }

        StudentAnswer saved = studentAnswerService.save(existing);
        return ResponseEntity.ok(convertToResponse(saved));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除答案", description = "删除指定学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        studentAnswerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/batch")
    @Operation(summary = "批量提交答案", description = "批量提交学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAnswerResponse>> submitAnswersInBatch(
            @RequestBody List<StudentAnswerSubmitRequest> requests) {
        
        List<StudentAnswer> answers = requests.stream().map(request -> {
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setAnswerText(request.getAnswerText());
            
            // 查找用户信息 - 优先通过ID查找，如果失败则尝试通过学号查找
            User user = null;
            String studentId = request.getStudentId();
            
            try {
                // 尝试作为用户ID解析
                Long userId = Long.parseLong(studentId);
                user = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException e) {
                // studentId不是数字，可能是学号，通过学号查找
            }
            
            if (user == null) {
                // 通过学号查找用户
                user = userRepository.findByStudentNumber(studentId).orElse(null);
            }
            
            if (user == null) {
                // 如果都找不到，抛出异常
                throw new RuntimeException("User not found with studentId: " + studentId);
            }
            
            studentAnswer.setStudent(user);
            
            Question question = questionService.getQuestionById(request.getQuestionId());
            studentAnswer.setQuestion(question);
            
            return studentAnswer;
        }).collect(Collectors.toList());
        
        List<StudentAnswer> savedAnswers = studentAnswerService.submitAnswersInBatch(answers);
        List<StudentAnswerResponse> responses = savedAnswers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
    
    @PostMapping("/import")
    @Operation(summary = "导入答案文件", description = "从Excel/CSV文件导入学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<String> importAnswers(@RequestParam("file") MultipartFile file) {
        try {
            int importedCount = studentAnswerService.importAnswersFromFile(file);
            return ResponseEntity.ok("成功导入 " + importedCount + " 条学生答案");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("文件读取失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("文件格式错误: " + e.getMessage());
        }
    }

    @PostMapping("/import/exam/{examId}")
    @Operation(summary = "批量导入学生答案到考试", description = "从文件批量导入学生答案到指定考试")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<String> importAnswersToExam(
            @PathVariable Long examId,
            @RequestParam("file") MultipartFile file) {
        try {
            int importedCount = studentAnswerService.importAnswersToExam(examId, file);
            return ResponseEntity.ok("成功导入 " + importedCount + " 条学生答案到考试");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("文件读取失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("文件格式错误: " + e.getMessage());
        }
    }

    @PostMapping("/import/learning")
    @Operation(summary = "导入学习通答案", description = "从学习通答案文件夹批量导入学生答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importLearningAnswers(
            @RequestParam("subject") String subject,
            @RequestParam("classFolders") List<String> classFolders,
            @RequestParam(value = "examId", required = false) Long examId) {
        try {
            ImportResult result;
            if (examId != null) {
                log.info("导入学习通答案到考试 {}", examId);
                result = studentAnswerService.importLearningAnswers(subject, classFolders, examId);
            } else {
                log.info("导入学习通答案到题库");
                result = studentAnswerService.importLearningAnswers(subject, classFolders);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("导入完成！成功: %d, 跳过: %d, 失败: %d", 
                result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount()));
            response.put("result", result);
            response.put("examId", examId);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "参数错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/import/learning/exam/{examId}")
    @Operation(summary = "导入学习通答案到指定考试", description = "从学习通答案文件夹批量导入学生答案到指定考试")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importLearningAnswersToExam(
            @PathVariable Long examId,
            @RequestParam("subject") String subject,
            @RequestParam("classFolders") List<String> classFolders) {
        try {
            ImportResult result = studentAnswerService.importLearningAnswers(subject, classFolders, examId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("导入到考试%d完成！成功: %d, 跳过: %d, 失败: %d", 
                examId, result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount()));
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "参数错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/import/learning-file")
    @Operation(summary = "异步导入学习通答案文件", description = "异步导入单个学习通答案文档到题库，返回任务ID")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importLearningAnswerFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "examId", required = false) Long examId) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "请选择文件"));
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".doc") && !fileName.toLowerCase().endsWith(".docx"))) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "只支持.doc和.docx格式的文件"));
            }
            
            // 获取当前用户
            User currentUser = authService.getCurrentUser();
            
            // 保存上传的文件到临时目录
            String uploadDirPath = System.getProperty("java.io.tmpdir");
            File uploadDir = new File(uploadDirPath, "learning-imports");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            File tempFile = new File(uploadDir, System.currentTimeMillis() + "_" + fileName);
            file.transferTo(tempFile);
            
            // 创建任务
            var taskRequest = new com.teachhelper.dto.request.TaskCreateRequest();
            taskRequest.setName("学习通答案导入: " + fileName);
            taskRequest.setDescription("导入学习通答案文档: " + fileName + (examId != null ? " 到考试ID: " + examId : " 到题库"));
            taskRequest.setType("LEARNING_ANSWER_IMPORT");
            taskRequest.setPriority("MEDIUM");
            
            var task = taskService.createTask(taskRequest);
            
            // 异步执行导入任务
            learningAnswerImportExecutorService.executeLearningAnswerImportTask(
                task.getTaskId(), tempFile, examId, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "学习通答案导入任务已启动，请在任务中心查看进度");
            response.put("taskId", task.getTaskId());
            response.put("fileName", fileName);
            
            log.info("学习通答案导入任务已创建: {} (文件: {})", task.getTaskId(), fileName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建学习通答案导入任务失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建导入任务失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/import/learning-async")
    @Operation(summary = "异步导入学习通答案文件夹", description = "异步导入学习通答案文件夹，返回任务ID")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importLearningAnswersAsync(
            @RequestParam("subject") String subject,
            @RequestParam("classFolders") String classFoldersStr,
            @RequestParam(value = "examId", required = false) Long examId) {
        
        try {
            // 获取当前用户信息
            User currentUser = authService.getCurrentUser();
            List<String> classFolders = Arrays.asList(classFoldersStr.split(","));
            
            // 创建异步任务
            String taskId = taskService.createLearningAnswerImportTask(
                    subject, classFolders, examId, currentUser.getUsername());
            
            // 启动异步导入执行
            learningAnswerImportExecutorService.executeLearningAnswersBatchImportTask(
                    taskId, subject, classFolders, examId, currentUser);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "导入任务已启动");
            result.put("taskId", taskId);
            result.put("estimatedTime", "预计需要几分钟时间");
            
            log.info("学习通答案批量导入任务已启动: {} (科目: {}, 班级数: {})", taskId, subject, classFolders.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("启动异步导入任务失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "启动失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @PostMapping("/import/learning-with-template")
    @Operation(summary = "基于模板导入学习通学生答案", description = "使用已就绪的试卷模板导入学生答案，只需匹配题号")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importLearningAnswersWithTemplate(
            @RequestParam("subject") String subject,
            @RequestParam("classFolders") List<String> classFolders,
            @RequestParam("templateId") Long templateId,
            @RequestParam(value = "examId", required = false) Long examId) {
        
        try {
            log.info("开始基于模板导入学生答案，模板ID: {}, 考试ID: {}, 班级数: {}", 
                    templateId, examId, classFolders.size());
            
            // 获取当前用户
            User currentUser = authService.getCurrentUser();
            
            if (examId == null) {
                // 如果没有指定考试ID，返回错误
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "必须指定考试ID才能导入学生答案");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 逐个班级导入
            List<ImportResult> allResults = new ArrayList<>();
            int totalSuccess = 0;
            int totalFailure = 0;
            List<String> allSuccessStudents = new ArrayList<>();
            List<String> allFailedStudents = new ArrayList<>();
            List<String> allErrorMessages = new ArrayList<>();
            
            for (String classFolder : classFolders) {
                try {
                    log.info("开始处理班级: {}", classFolder);
                    
                    // 实现基于模板的导入逻辑
                    ImportResult result = studentAnswerService.importLearningAnswersWithTemplate(
                        subject, classFolder, templateId, examId
                    );
                    
                    allResults.add(result);
                    totalSuccess += result.getSuccessCount();
                    totalFailure += result.getFailedCount();
                    
                    if (result.getSuccessfulStudents() != null) {
                        allSuccessStudents.addAll(result.getSuccessfulStudents());
                    }
                    if (result.getFailedStudents() != null) {
                        allFailedStudents.addAll(result.getFailedStudents());
                    }
                    if (result.getErrorMessages() != null) {
                        allErrorMessages.addAll(result.getErrorMessages());
                    }
                    
                    log.info("班级 {} 导入完成: 成功{}, 失败{}", classFolder, 
                            result.getSuccessCount(), result.getFailedCount());
                    
                } catch (Exception e) {
                    log.error("班级 {} 导入失败", classFolder, e);
                    totalFailure++;
                    allFailedStudents.add("班级" + classFolder + "整体导入失败");
                    allErrorMessages.add("班级" + classFolder + ": " + e.getMessage());
                }
            }
            
            // 汇总结果
            Map<String, Object> response = new HashMap<>();
            response.put("success", totalSuccess > 0); // 只要有成功的就算成功
            response.put("templateId", templateId);
            response.put("examId", examId);
            response.put("totalProcessed", totalSuccess + totalFailure);
            response.put("successCount", totalSuccess);
            response.put("failureCount", totalFailure);
            response.put("successfulStudents", allSuccessStudents);
            response.put("failedStudents", allFailedStudents);
            response.put("errorMessages", allErrorMessages);
            response.put("classResults", allResults);
            
            if (totalSuccess > 0) {
                response.put("message", String.format("基于模板导入完成：成功 %d 人，失败 %d 人", 
                        totalSuccess, totalFailure));
                log.info("基于模板导入总计完成：模板ID={}, 考试ID={}, 成功={}, 失败={}", 
                        templateId, examId, totalSuccess, totalFailure);
            } else {
                response.put("message", "导入失败：没有成功导入任何学生答案");
                log.warn("基于模板导入失败：模板ID={}, 考试ID={}, 没有成功导入任何学生", templateId, examId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("基于模板导入学生答案失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            response.put("templateId", templateId);
            response.put("examId", examId);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/learning/subjects")
    @Operation(summary = "获取可用科目列表", description = "获取学习通答案文件夹下的科目列表")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAvailableSubjects() {
        List<String> subjects = studentAnswerService.getAvailableSubjects();
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/learning/subjects/{subject}/classes")
    @Operation(summary = "获取科目下的班级列表", description = "获取指定科目下的班级文件夹列表")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getSubjectClasses(@PathVariable String subject) {
        List<String> classes = studentAnswerService.getSubjectClasses(subject);
        return ResponseEntity.ok(classes);
    }
    
    @PostMapping("/parse-template/test")
    @Operation(summary = "测试学习通模板解析", description = "从学习通文档中提取考试模板信息")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testTemplateExtraction(
            @RequestParam("file") MultipartFile file) {
        
        log.info("📚 开始测试学习通模板解析功能");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 保存上传的文件到临时目录
            String uploadsDir = System.getProperty("java.io.tmpdir");
            Path tempDir = Paths.get(uploadsDir);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            String fileName = file.getOriginalFilename();
            Path tempFile = tempDir.resolve(fileName);
            Files.write(tempFile, file.getBytes());
            
            log.info("📁 临时文件保存成功: {}", tempFile.toString());
            
            // 2. 使用新的模板解析方法
            com.teachhelper.service.answer.LearningAnswerParserService.ExamTemplateData templateData = 
                learningAnswerParserService.parseExamTemplate(tempFile.toFile());
            
            if (templateData == null) {
                response.put("success", false);
                response.put("message", "无法解析该文档，请检查文档格式");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 3. 构建返回结果
            response.put("success", true);
            response.put("message", "模板解析成功");
            response.put("data", buildTemplateResponse(templateData));
            
            // 4. 清理临时文件
            Files.deleteIfExists(tempFile);
            
            log.info("✅ 模板解析测试完成: {}", templateData.getExamTitle());
            
        } catch (Exception e) {
            log.error("❌ 模板解析测试失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "解析失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 构建模板响应数据
     */
    private Map<String, Object> buildTemplateResponse(com.teachhelper.service.answer.LearningAnswerParserService.ExamTemplateData templateData) {
        Map<String, Object> data = new HashMap<>();
        data.put("examTitle", templateData.getExamTitle());
        data.put("subject", templateData.getSubject());
        data.put("studentName", templateData.getStudentName());
        data.put("className", templateData.getClassName());
        data.put("totalQuestions", templateData.getTotalQuestions());
        data.put("totalScore", templateData.getTotalScore());
        
        List<Map<String, Object>> sections = new ArrayList<>();
        if (templateData.getSections() != null) {
            for (var section : templateData.getSections()) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("sectionNumber", section.getSectionNumber());
                sectionMap.put("sectionTitle", section.getSectionTitle());
                sectionMap.put("questionType", section.getQuestionType());
                sectionMap.put("questionCount", section.getQuestionCount());
                sectionMap.put("totalScore", section.getTotalScore());
                sectionMap.put("scorePerQuestion", section.getScorePerQuestion());
                
                List<Map<String, Object>> questions = new ArrayList<>();
                if (section.getQuestions() != null) {
                    for (var question : section.getQuestions()) {
                        Map<String, Object> questionMap = new HashMap<>();
                        questionMap.put("questionNumber", question.getQuestionNumber());
                        questionMap.put("questionContent", question.getQuestionContent());
                        questionMap.put("correctAnswer", question.getCorrectAnswer());
                        questionMap.put("options", question.getOptions());
                        questionMap.put("score", question.getScore());
                        questions.add(questionMap);
                    }
                }
                sectionMap.put("questions", questions);
                sections.add(sectionMap);
            }
        }
        data.put("sections", sections);
        
        return data;
    }

    @PostMapping("/parse-score/test")
    @Operation(summary = "测试AI智能分数解析", description = "测试AI分数解析功能")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testScoreParsing(
            @RequestBody Map<String, String> request) {
        
        String questionContent = request.get("questionContent");
        String sectionHeader = request.get("sectionHeader");
        
        if (questionContent == null || questionContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "题目内容不能为空"));
        }
        
        try {
            log.info("开始测试AI分数解析...");
            log.info("题目内容: {}", questionContent);
            log.info("段落标题: {}", sectionHeader);
            
            BigDecimal score = questionScoreParsingService.parseQuestionScore(questionContent, sectionHeader);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "questionContent", questionContent,
                "sectionHeader", sectionHeader != null ? sectionHeader : "",
                "parsedScore", score != null ? score.doubleValue() : null,
                "message", "AI分数解析成功"
            );
            
            log.info("AI分数解析结果: {}", score);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("AI分数解析测试失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "AI分数解析失败"
                ));
        }
    }
    
    @GetMapping("/export")
    @Operation(summary = "导出答案", description = "导出学生答案到Excel/CSV文件")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> exportAnswers(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Boolean evaluated) {
        try {
            ByteArrayResource resource = studentAnswerService.exportAnswersToFile(examId, questionId, evaluated);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_answers.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/student/{studentId}")
    @Operation(summary = "按学生导出答案", description = "导出指定学生的所有答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> exportAnswersByStudent(@PathVariable Long studentId) {
        try {
            ByteArrayResource resource = studentAnswerService.exportAnswersByStudent(studentId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_" + studentId + "_answers.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/exam/{examId}/student/{studentId}/submitted")
    @Operation(summary = "检查学生是否已提交考试", description = "检查指定学生是否已提交指定考试")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> hasStudentSubmittedExam(@PathVariable Long examId, @PathVariable Long studentId) {
        boolean hasSubmitted = studentAnswerService.hasStudentSubmittedExam(examId, studentId);
        return ResponseEntity.ok(hasSubmitted);
    }
    
    @GetMapping("/exam/{examId}/my-submission-status")
    @Operation(summary = "检查当前学生是否已提交考试", description = "学生检查自己是否已提交指定考试")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Boolean> hasCurrentStudentSubmittedExam(@PathVariable Long examId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String studentId = authentication.getName();
            
            System.out.println("=== hasCurrentStudentSubmittedExam ===");
            System.out.println("examId: " + examId + ", studentId: " + studentId);
            
            boolean hasSubmitted = examSubmissionService.hasStudentSubmittedExam(examId, studentId);
            System.out.println("hasSubmitted: " + hasSubmitted);
            
            return ResponseEntity.ok(hasSubmitted);
        } catch (Exception e) {
            System.out.println("检查提交状态时出错: " + e.getMessage());
            e.printStackTrace();
            // 如果获取用户信息失败，返回false（保守处理）
            return ResponseEntity.ok(false);
        }
    }
    
    @GetMapping("/exam/{examId}/my-submission-detail")
    @Operation(summary = "获取当前学生的提交详情", description = "学生查看自己的考试提交详情")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> getCurrentStudentSubmissionDetail(@PathVariable Long examId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            Map<String, Object> submissionDetail = examSubmissionService.getStudentSubmissionDetail(examId, username);
            return ResponseEntity.ok(submissionDetail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取提交详情失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/exam/{examId}/submit")
    @Operation(summary = "正式提交整个考试", description = "学生正式提交整个考试，标记为已完成")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> submitExam(@PathVariable Long examId) {
        try {
            // 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth.getName();
            
            // 获取用户信息来得到用户ID
            // 由于我们需要用户ID来查找学生记录，我们需要另一种方式
            // 暂时使用用户名，但需要在ExamSubmissionService中处理
            System.out.println("submitExam called with examId: " + examId + ", username: " + currentUsername);
            
            // 通过ExamSubmissionService提交考试
            examSubmissionService.submitExam(examId, currentUsername, false);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println("submitExam error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/student/{studentId}/exam/{examId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteStudentExamAnswers(
            @PathVariable Long studentId,
            @PathVariable Long examId) {
        try {
            studentAnswerService.deleteStudentExamAnswers(studentId, examId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "学生试卷答案删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除学生试卷答案失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 嵌套压缩包答案导入
     * 支持班级压缩包中包含学生压缩包的结构
     */
    @PostMapping("/import-nested-zip")
    @Operation(summary = "导入嵌套压缩包答案", description = "从指定路径导入单个题目的嵌套压缩包答案")
    public ResponseEntity<ImportResult> importNestedZipAnswers(
            @RequestParam String answerPath,
            @RequestParam Long questionId) {
        try {
            ImportResult result = nestedZipAnswerImportService.importNestedZipAnswersForQuestion(answerPath, questionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("嵌套压缩包导入失败", e);
            ImportResult result = new ImportResult();
            result.setSuccessCount(0);
            result.setFailureCount(1);
            result.setErrorMessages(Arrays.asList("导入失败"));
            result.setErrors(Arrays.asList(e.getMessage()));
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取嵌套压缩包答案的科目列表
     */
    @GetMapping("/nested-zip-subjects")
    public ResponseEntity<List<String>> getNestedZipSubjects() {
        try {
            List<String> subjects = nestedZipAnswerImportService.getAvailableSubjects();
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("获取科目列表失败", e);
            return ResponseEntity.ok(Arrays.asList());
        }
    }
    
    /**
     * 获取指定科目下的作业/实验列表
     */
    @GetMapping("/nested-zip-assignments")
    public ResponseEntity<List<String>> getNestedZipAssignments(@RequestParam String subject) {
        try {
            List<String> assignments = nestedZipAnswerImportService.getAvailableAssignments(subject);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            log.error("获取作业列表失败: subject=" + subject, e);
            return ResponseEntity.ok(Arrays.asList());
        }
    }
    
    /**
     * 基于科目和作业的嵌套压缩包导入
     */
    @PostMapping("/import-nested-zip-by-subject")
    public ResponseEntity<ImportResult> importNestedZipAnswersBySubject(
            @RequestParam String subject,
            @RequestParam String assignment,
            @RequestParam Long questionId) {
        try {
            ImportResult result = nestedZipAnswerImportService.importNestedZipAnswersBySubjectAndAssignment(
                    subject, assignment, questionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("基于科目的嵌套压缩包导入失败", e);
            ImportResult result = new ImportResult();
            result.setSuccessCount(0);
            result.setFailureCount(1);
            result.setErrorMessages(Arrays.asList("导入失败"));
            result.setErrors(Arrays.asList(e.getMessage()));
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 大作业答案导入（班级压缩包内直接是学生答案文档，LLM解析文件名）
     */
    @PostMapping("/import-major-assignment")
    @Operation(summary = "导入大作业答案", description = "从班级压缩包导入大作业答案，LLM解析文件名，无学号用No_student_number")
    public ResponseEntity<ImportResult> importMajorAssignmentAnswers(
            @RequestParam String subject,
            @RequestParam String assignment,
            @RequestParam Long questionId) {
        try {
            // 获取当前用户ID（假设有authService）
            Long userId = authService.getCurrentUser().getId();
            ImportResult result = majorAssignmentAnswerImportService.importMajorAssignmentAnswers(subject, assignment, questionId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("大作业导入失败", e);
            ImportResult result = new ImportResult();
            result.setSuccessCount(0);
            result.setFailedCount(1);
            result.setErrorMessages(Arrays.asList("导入失败: " + e.getMessage()));
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取大作业导入的作业/实验列表
     */
    @GetMapping("/major-assignment-assignments")
    public ResponseEntity<List<String>> getMajorAssignmentAssignments(@RequestParam String subject) {
        try {
            List<String> assignments = majorAssignmentAnswerImportService.getAvailableAssignments(subject);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            log.error("获取大作业作业列表失败: subject=" + subject, e);
            return ResponseEntity.ok(Arrays.asList());
        }
    }

    @PostMapping("/import-folder-upload")
    @Operation(summary = "文件夹批量上传答案", description = "上传文件夹中的多个文档，自动解析文件名提取学生信息，将整个文档内容作为答案")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ImportResult> importFolderUploadAnswers(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("questionId") Long questionId) {
        
        try {
            log.info("接收到文件夹批量上传请求，文件数量: {}, 题目ID: {}", files.length, questionId);
            
            // 过滤支持的文件类型
            List<MultipartFile> supportedFiles = folderUploadAnswerService.filterSupportedFiles(files);
            
            if (supportedFiles.isEmpty()) {
                ImportResult result = new ImportResult();
                result.setSuccess(false);
                result.setErrorMessage("没有找到支持的文件类型，支持：doc, docx, pdf, txt, jpg, png 等");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 处理文件
            com.teachhelper.service.answer.FolderUploadAnswerService.ProcessResult processResult = 
                folderUploadAnswerService.processUploadedFiles(supportedFiles.toArray(new MultipartFile[0]), questionId);
            
            // 转换为ImportResult
            ImportResult importResult = folderUploadAnswerService.convertToImportResult(processResult);
            
            log.info("文件夹批量上传完成: 成功{}, 失败{}", importResult.getSuccessCount(), importResult.getFailedCount());
            return ResponseEntity.ok(importResult);
            
        } catch (Exception e) {
            log.error("文件夹批量上传失败", e);
            ImportResult result = new ImportResult();
            result.setSuccess(false);
            result.setErrorMessage("上传处理失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    @PostMapping("/test-filename-parse")
    @Operation(summary = "测试文件名解析", description = "测试LLM文件名解析功能，返回解析结果")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testFileNameParse(@RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "文件名不能为空"
                ));
            }
            
            var parseResult = fileNameParserService.parseFileName(fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", parseResult.isSuccess());
            response.put("fileName", fileName);
            
            if (parseResult.isSuccess()) {
                response.put("studentName", parseResult.getStudentName());
                response.put("studentNumber", parseResult.getStudentNumber());
                response.put("parseMethod", parseResult.getParseMethod());
                response.put("message", "解析成功");
            } else {
                response.put("errorMessage", parseResult.getErrorMessage());
                response.put("message", "解析失败");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("测试文件名解析失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "解析失败: " + e.getMessage()
            ));
        }
    }

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
            // 使用真正的学号而不是 student_id (用户ID)
            String displayStudentId = answer.getStudent().getStudentNumber() != null 
                ? answer.getStudent().getStudentNumber() 
                : String.valueOf(answer.getStudent().getId()); // 向后兼容
                
            StudentAnswerResponse.StudentInfo studentInfo = new StudentAnswerResponse.StudentInfo(
                answer.getStudent().getId(),
                displayStudentId, // 使用学号作为显示的学生ID
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

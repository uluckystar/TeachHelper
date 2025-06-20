package com.teachhelper.controller.answer;

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

import com.teachhelper.dto.request.StudentAnswerSubmitRequest;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.Student;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.service.question.QuestionService;
import com.teachhelper.service.student.StudentAnswerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student-answers")
@Tag(name = "学生答案管理", description = "学生答案的提交、查询、管理等操作")
public class StudentAnswerController {
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private QuestionService questionService;
    
    @PostMapping
    @Operation(summary = "提交学生答案", description = "学生提交答案")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswerResponse> submitAnswer(@Valid @RequestBody StudentAnswerSubmitRequest request) {
        // 构建学生答案对象
        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setAnswerText(request.getAnswerText());
        
        // 设置学生信息
        Student student = new Student();
        student.setStudentId(request.getStudentId());
        student.setName(request.getStudentName());
        student.setEmail(request.getStudentEmail());
        studentAnswer.setStudent(student);
        
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
        // TODO: 在实际实现中，应该根据当前登录用户过滤答案
        // 目前暂时返回考试的所有答案，但应该只返回当前学生的答案
        List<StudentAnswer> answers = studentAnswerService.getAnswersByExamId(examId);
        List<StudentAnswerResponse> responses = answers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
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
    @Operation(summary = "更新答案", description = "更新学生答案信息")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswerResponse> updateAnswer(
            @PathVariable Long id,
            @Valid @RequestBody StudentAnswerSubmitRequest request) {
        
        StudentAnswer answerDetails = new StudentAnswer();
        answerDetails.setAnswerText(request.getAnswerText());
        
        StudentAnswer updatedAnswer = studentAnswerService.updateAnswer(id, answerDetails);
        StudentAnswerResponse response = convertToResponse(updatedAnswer);
        return ResponseEntity.ok(response);
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
            
            Student student = new Student();
            student.setStudentId(request.getStudentId());
            student.setName(request.getStudentName());
            student.setEmail(request.getStudentEmail());
            studentAnswer.setStudent(student);
            
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

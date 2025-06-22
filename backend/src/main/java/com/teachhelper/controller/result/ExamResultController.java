package com.teachhelper.controller.result;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.response.ExamResultResponse;
import com.teachhelper.dto.response.ExamStatistics;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.service.exam.ExamService;
import com.teachhelper.service.student.StudentAnswerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/exam-results")
@Tag(name = "考试结果管理", description = "基于角色的考试结果查看和管理")
public class ExamResultController {
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private AuthService authService; 

    /**
     * 学生查看自己的考试结果
     */
    @GetMapping("/student/exam/{examId}")
    @Operation(summary = "学生查看考试结果", description = "学生查看自己在指定考试中的结果")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamResultResponse> getStudentExamResult(@PathVariable Long examId) {
        try {
            System.out.println("=== 学生考试结果API调用开始 ===");
            System.out.println("examId: " + examId);
            
            User currentUser = authService.getCurrentUser(); 
            System.out.println("当前用户: " + currentUser.getUsername());
            System.out.println("用户角色: " + currentUser.getRoles());
            
            // 通过用户名查找学生记录
            List<StudentAnswer> answers = studentAnswerService.getAnswersByExamIdAndUsername(examId, currentUser.getUsername());
            System.out.println("找到的答案数量: " + answers.size());
            
            if (answers.isEmpty()) {
                System.out.println("学生尚未答题，返回空结果而非404");
                
                // 创建基本的响应对象
                ExamResultResponse response = new ExamResultResponse();
                response.setExamId(examId);
                response.setStudentId(currentUser.getId());
                response.setStudentName(currentUser.getUsername());
                response.setAnswers(List.of());
                response.setTotalScore(0.0);
                response.setAnsweredQuestions(0); // 设置答题数量为0
                response.setStatus("NOT_STARTED"); // 添加状态标识
                
                // 添加考试信息
                try {
                    var exam = examService.getExamById(examId);
                    response.setExamTitle(exam.getTitle());
                    response.setExamDescription(exam.getDescription());
                    System.out.println("考试信息: " + exam.getTitle());
                } catch (Exception e) {
                    System.out.println("获取考试信息失败: " + e.getMessage());
                }
                
                return ResponseEntity.ok(response);
            }

            List<StudentAnswerResponse> answerResponses = answers.stream()
                .map(StudentAnswerResponse::fromEntity)
                .collect(Collectors.toList());

            double totalScore = answerResponses.stream()
                .filter(ar -> ar.getScore() != null) // 确保分数不为null
                .mapToDouble(StudentAnswerResponse::getScore)
                .sum();
            
            // 计算总分（满分）
            double totalPossibleScore = answerResponses.stream()
                .mapToDouble(ar -> ar.getMaxScore() != null ? ar.getMaxScore() : 0.0)
                .sum();
            
            // 计算得分率
            double scorePercentage = totalPossibleScore > 0 ? (totalScore / totalPossibleScore) * 100.0 : 0.0;
            
            // 计算成绩等级
            String grade = calculateGrade(scorePercentage);
            
            System.out.println("总分: " + totalScore + "/" + totalPossibleScore + " (" + String.format("%.1f", scorePercentage) + "%) - " + grade);
            
            ExamResultResponse response = new ExamResultResponse();
            response.setExamId(examId);
            response.setStudentId(currentUser.getId()); // 修复：使用 setStudentId 而不是 setUserId
            response.setStudentName(currentUser.getUsername());
            response.setAnswers(answerResponses);
            response.setTotalScore(totalScore);
            response.setTotalPossibleScore(totalPossibleScore);
            response.setScorePercentage(scorePercentage);
            response.setGrade(grade);
            response.setAnsweredQuestions(answerResponses.size()); // 设置答题数量
            
            // 判断评分状态
            boolean allEvaluated = answerResponses.stream().allMatch(ar -> ar.isEvaluated());
            response.setStatus(allEvaluated ? "EVALUATED" : "SUBMITTED");
            System.out.println("考试状态: " + response.getStatus() + " (已评分答案数: " + 
                answerResponses.stream().mapToInt(ar -> ar.isEvaluated() ? 1 : 0).sum() + "/" + answerResponses.size() + ")");
            
            // 添加考试信息
            try {
                var exam = examService.getExamById(examId);
                response.setExamTitle(exam.getTitle());
                response.setExamDescription(exam.getDescription());
                System.out.println("考试标题: " + exam.getTitle());
            } catch (Exception e) {
                System.out.println("获取考试信息失败: " + e.getMessage());
                // 忽略获取考试信息的错误
            }

            System.out.println("=== 学生考试结果API调用成功 ===");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("=== 学生考试结果API调用失败 ===");
            System.out.println("错误: " + e.getMessage());
            e.printStackTrace();
            // Log exception e.g. logger.error("Error getting student exam result", e);
            return ResponseEntity.status(500).body(null); 
        }
    }

    /**
     * 教师查看其创建的考试的统计数据
     */
    @GetMapping("/teacher/exam/{examId}/statistics")
    @Operation(summary = "教师查看考试统计", description = "教师查看其创建的指定考试的统计数据")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamStatistics> getExamStatisticsForTeacher(@PathVariable Long examId) {
        try {
            User currentUser = authService.getCurrentUser(); 
            if (!examService.isExamCreatedBy(examId, currentUser.getId())) {
                return ResponseEntity.status(403).build(); 
            }
            ExamStatistics stats = examService.getExamStatistics(examId); 
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // Log exception
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 教师查看指定考试的所有学生成绩概览
     */
    @GetMapping("/teacher/exam/{examId}/results-overview")
    @Operation(summary = "教师查看学生成绩概览", description = "教师查看指定考试的所有学生成绩概览")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<ExamResultResponse>> getExamResultsOverviewForTeacher(@PathVariable Long examId) {
         try {
            User currentUser = authService.getCurrentUser(); 
            if (!examService.isExamCreatedBy(examId, currentUser.getId())) {
                return ResponseEntity.status(403).build(); 
            }
            // teacherId 传递当前用户ID，用于权限验证（虽然isExamCreatedBy已做过一次）和可能的业务逻辑
            List<ExamResultResponse> overview = examService.getAllStudentResultsForExam(examId, currentUser.getId());  
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            // Log exception
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 教师查看特定学生在特定考试中的详细结果
     */
    @GetMapping("/teacher/exam/{examId}/student/{studentId}")
    @Operation(summary = "教师查看特定学生考试结果", description = "教师查看特定学生在特定考试中的详细结果")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResultResponse> getStudentExamResultForTeacher(@PathVariable Long examId, @PathVariable Long studentId) {
        try {
            User currentUser = authService.getCurrentUser(); 
            if (!examService.isExamCreatedBy(examId, currentUser.getId())) {
                return ResponseEntity.status(403).build(); 
            }

            User student = authService.getUserById(studentId); 
            if (student == null) {
                 return ResponseEntity.notFound().build();
            }

            List<StudentAnswer> answers = studentAnswerService.getAnswersByExamIdAndStudentId(examId, studentId);
            if (answers.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<StudentAnswerResponse> answerResponses = answers.stream()
                .map(StudentAnswerResponse::fromEntity)
                .collect(Collectors.toList());
            
            double totalScore = answerResponses.stream()
                .filter(ar -> ar.getScore() != null) // 确保分数不为null
                .mapToDouble(StudentAnswerResponse::getScore)
                .sum();

            ExamResultResponse response = new ExamResultResponse();
            response.setExamId(examId);
            response.setUserId(studentId); // 使用 setUserId
            response.setStudentName(student.getUsername()); // 假设User实体有getUsername
            // response.setExamTitle(examService.getExamById(examId).getTitle()); // 视情况添加
            response.setAnswers(answerResponses);
            response.setTotalScore(totalScore);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log exception
            return ResponseEntity.status(500).build();
        }
    }


    /**
     * 管理员查看任何考试的统计数据
     */
    @GetMapping("/admin/exam/{examId}/statistics")
    @Operation(summary = "管理员查看考试统计", description = "管理员查看任何考试的统计数据")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExamStatistics> getExamStatisticsForAdmin(@PathVariable Long examId) {
        try {
            ExamStatistics stats = examService.getExamStatistics(examId); 
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // Log exception
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 管理员查看任何考试的所有学生成绩概览
     */
    @GetMapping("/admin/exam/{examId}/results-overview")
    @Operation(summary = "管理员查看学生成绩概览", description = "管理员查看任何考试的所有学生成绩概览")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExamResultResponse>> getExamResultsOverviewForAdmin(@PathVariable Long examId) {
        try {
            // 对于管理员，teacherId 参数传 null，表示不根据特定教师过滤
            List<ExamResultResponse> overview = examService.getAllStudentResultsForExam(examId, null); 
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            // Log exception
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 管理员查看特定学生在特定考试中的详细结果
     */
    @GetMapping("/admin/exam/{examId}/student/{studentId}")
    @Operation(summary = "管理员查看特定学生考试结果", description = "管理员查看特定学生在特定考试中的详细结果")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExamResultResponse> getStudentExamResultForAdmin(@PathVariable Long examId, @PathVariable Long studentId) {
        try {
            User student = authService.getUserById(studentId); 
             if (student == null) {
                 return ResponseEntity.notFound().build();
            }

            List<StudentAnswer> answers = studentAnswerService.getAnswersByExamIdAndStudentId(examId, studentId);
             if (answers.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            List<StudentAnswerResponse> answerResponses = answers.stream()
                .map(StudentAnswerResponse::fromEntity)
                .collect(Collectors.toList());

            double totalScore = answerResponses.stream()
                .filter(ar -> ar.getScore() != null) // 确保分数不为null
                .mapToDouble(StudentAnswerResponse::getScore)
                .sum();
            
            ExamResultResponse response = new ExamResultResponse();
            response.setExamId(examId);
            response.setUserId(studentId); // 使用 setUserId
            response.setStudentName(student.getUsername()); // 假设User实体有getUsername
            // response.setExamTitle(examService.getExamById(examId).getTitle()); // 视情况添加
            response.setAnswers(answerResponses);
            response.setTotalScore(totalScore);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log exception
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 根据得分率计算成绩等级
     */
    private String calculateGrade(double scorePercentage) {
        if (scorePercentage >= 90) {
            return "优秀";
        } else if (scorePercentage >= 80) {
            return "良好";
        } else if (scorePercentage >= 70) {
            return "中等";
        } else if (scorePercentage >= 60) {
            return "及格";
        } else {
            return "不及格";
        }
    }
}

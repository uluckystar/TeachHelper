package com.teachhelper.service.exam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.teachhelper.dto.response.ExamStatistics;
import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.entity.User;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.service.question.QuestionService;
import com.teachhelper.service.student.StudentAnswerService;

@Service
@Transactional
public class ExamService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    public Exam createExam(String title, String description) {
        User currentUser = authService.getCurrentUser();
        Exam exam = new Exam(title, description, currentUser);
        return examRepository.save(exam);
    }
    
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("考试不存在，ID: " + id));
    }
    
    public List<Exam> getAllExamsByCurrentUser() {
        User currentUser = authService.getCurrentUser();
        return examRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId());
    }
    
    public Page<Exam> getExamsByCurrentUser(Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        return examRepository.findByCreatedById(currentUser.getId(), pageable);
    }
    
    public Exam updateExam(Long id, String title, String description) {
        Exam exam = getExamById(id);
        
        // Check if current user is the owner
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限修改此考试");
        }
        
        exam.setTitle(title);
        exam.setDescription(description);
        return examRepository.save(exam);
    }
    
    public void deleteExam(Long id) {
        Exam exam = getExamById(id);
        
        // Check if current user is the owner
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限删除此考试");
        }
        
        examRepository.delete(exam);
    }
    
    public List<Exam> searchExams(String keyword) {
        User currentUser = authService.getCurrentUser();
        return examRepository.findByCreatedByIdAndTitleContaining(currentUser.getId(), keyword);
    }

    // 考试状态管理方法
    public Exam publishExam(Long examId) {
        Exam exam = getExamById(examId);
        
        // 验证权限
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限发布此考试");
        }
        
        // 添加详细调试信息
        System.out.println("=== ExamService.publishExam 调试信息 ===");
        System.out.println("考试ID: " + examId);
        System.out.println("数据库中的实际状态: " + exam.getStatus());
        System.out.println("状态检查: exam.getStatus() != ExamStatus.DRAFT -> " + (exam.getStatus() != ExamStatus.DRAFT));
        System.out.println("ExamStatus.DRAFT 枚举值: " + ExamStatus.DRAFT);
        System.out.println("=== ExamService.publishExam 调试信息结束 ===");
        
        // 验证考试状态
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("只能发布草稿状态的考试");
        }
        
        // 验证考试是否有题目（避免懒加载问题）
        boolean hasQuestions = false;
        try {
            hasQuestions = exam.getQuestions() != null && !exam.getQuestions().isEmpty();
        } catch (Exception e) {
            // 如果懒加载失败，通过统计信息检查
            try {
                ExamStatistics stats = getExamStatistics(examId);
                hasQuestions = stats.getTotalQuestions() > 0;
            } catch (Exception ex) {
                hasQuestions = false;
            }
        }
        
        if (!hasQuestions) {
            throw new RuntimeException("考试必须包含至少一个题目才能发布");
        }
        
        // 更新状态为已发布
        exam.setStatus(ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }
    
    public Exam endExam(Long examId) {
        Exam exam = getExamById(examId);
        
        // 验证权限
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限结束此考试");
        }
        
        // 验证考试状态
        if (exam.getStatus() != ExamStatus.PUBLISHED && exam.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new RuntimeException("只能结束已发布或进行中的考试");
        }
        
        // 结束考试
        exam.setStatus(ExamStatus.ENDED);
        return examRepository.save(exam);
    }

    // 导入导出功能 - 简化实现
    public int importQuestionsFromFile(Long examId, MultipartFile file) throws IOException {
        Exam exam = getExamById(examId);
        
        // 验证权限
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限修改此考试");
        }

        // TODO: 实现文件解析逻辑
        return 0;
    }

    public ByteArrayResource exportExamToFile(Long examId) throws IOException {
        Exam exam = getExamById(examId);
        
        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet examSheet = workbook.createSheet("考试信息");
            
            // 填充考试信息
            Row examHeaderRow = examSheet.createRow(0);
            examHeaderRow.createCell(0).setCellValue("考试标题");
            examHeaderRow.createCell(1).setCellValue("考试描述");
            examHeaderRow.createCell(2).setCellValue("创建时间");
            
            Row examDataRow = examSheet.createRow(1);
            examDataRow.createCell(0).setCellValue(exam.getTitle());
            examDataRow.createCell(1).setCellValue(exam.getDescription() != null ? exam.getDescription() : "");
            examDataRow.createCell(2).setCellValue(exam.getCreatedAt().toString());
            
            // 转换为字节数组
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    public boolean isExamCreatedBy(Long examId, Long userId) {
        Exam exam = getExamById(examId);
        return exam.getCreatedBy().getId().equals(userId);
    }

    public ExamStatistics getExamStatistics(Long examId) {
        Exam exam = getExamById(examId);
        ExamStatistics stats = new ExamStatistics();
        stats.setExamId(examId);
        stats.setExamTitle(exam.getTitle());
        // 实际应查询数据库得到以下数据
        // 例如: stats.setTotalStudents(studentAnswerService.countDistinctStudentsByExamId(examId));
        stats.setTotalStudents(0); // 使用 setTotalStudents 替代 setTotalParticipants
        stats.setAverageScore(0.0); 
        stats.setMaxScore(0.0); 
        stats.setMinScore(0.0); 
        stats.setTotalQuestions(exam.getQuestions() != null ? exam.getQuestions().size() : 0);
        return stats;
    }

    public List<com.teachhelper.dto.response.ExamResultResponse> getAllStudentResultsForExam(Long examId, Long teacherId) {
        if (teacherId != null && !isExamCreatedBy(examId, teacherId)) {
            // 对于教师，检查他们是否创建了该考试
            throw new SecurityException("教师无权访问此考试的结果。");
        }
        // 对于管理员 (teacherId == null)，他们可以访问任何考试的结果
        
        // 此处应调用 StudentAnswerService 或 UserRepository 来获取所有参与此考试的学生ID
        // 然后对于每个学生，构建 ExamResultResponse
        // 以下为简化占位符实现
        return new java.util.ArrayList<>(); 
    }
}

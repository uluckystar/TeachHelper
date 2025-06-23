package com.teachhelper.service.exam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.entity.ExamSubmission;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.repository.ExamSubmissionRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.service.student.StudentAnswerService;

@Service
@Transactional
public class ExamSubmissionService {
    
    @Autowired
    private ExamSubmissionRepository examSubmissionRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    /**
     * 学生提交考试
     */
    public ExamSubmission submitExam(Long examId, String studentId, boolean autoSubmitted) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("考试不存在"));
            
        System.out.println("=== ExamSubmissionService.submitExam ===");
        System.out.println("查找学生，传入的标识符: " + studentId);
        
        // 查找用户（现在统一使用User表）
        Optional<User> userOpt = Optional.empty();
        
        // 首先尝试通过用户ID查找（数字ID）
        if (studentId.matches("\\d+")) {
            System.out.println("传入的是数字ID，通过user_id查找");
            userOpt = userRepository.findById(Long.parseLong(studentId));
        } else {
            // 传入的是用户名
            System.out.println("传入的是用户名，通过username查找");
            userOpt = userRepository.findByUsername(studentId);
        }
        
        if (userOpt.isEmpty()) {
            System.out.println("未找到用户记录，studentId: " + studentId);
            throw new ResourceNotFoundException("用户不存在，studentId: " + studentId);
        }
        
        User user = userOpt.get();
        System.out.println("找到用户记录: " + user.getUsername() + ", ID: " + user.getId());
        
        // 检查是否已提交
        if (hasStudentSubmittedExam(examId, user.getId())) {
            throw new IllegalStateException("学生已经提交过该考试");
        }
        
        // 统计答题情况
        List<StudentAnswer> answers = studentAnswerService.getAnswersByExamIdAndStudentId(examId, user.getId());
        int totalQuestions = exam.getQuestions() != null ? exam.getQuestions().size() : 0;
        int answeredQuestions = answers.size();
        
        // 创建提交记录
        ExamSubmission submission = new ExamSubmission();
        submission.setExam(exam);
        submission.setStudent(user);  // 现在关联User
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setAutoSubmitted(autoSubmitted);
        submission.setTotalQuestions(totalQuestions);
        submission.setAnsweredQuestions(answeredQuestions);
        
        if (autoSubmitted) {
            submission.setSubmissionNote("考试时间到期自动提交");
        } else {
            submission.setSubmissionNote("学生主动提交");
        }
        
        try {
            ExamSubmission savedSubmission = examSubmissionRepository.save(submission);
            
            // 提交成功后，检查是否需要更新考试状态
            updateExamStatusIfNeeded(exam, autoSubmitted);
            
            return savedSubmission;
        } catch (Exception e) {
            // 处理唯一约束冲突等数据库异常
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                System.out.println("检测到重复提交异常: " + e.getMessage());
                throw new IllegalStateException("学生已经提交过该考试，请勿重复提交");
            }
            // 其他异常继续抛出
            throw e;
        }
    }
    
    /**
     * 检查学生是否已提交考试
     */
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedExam(Long examId, String studentId) {
        System.out.println("=== ExamSubmissionService.hasStudentSubmittedExam ===");
        System.out.println("examId: " + examId + ", studentId: " + studentId);
        
        // 查找用户（现在统一使用User表）
        Optional<User> userOpt = Optional.empty();
        
        // 首先尝试通过用户ID查找（数字ID）
        if (studentId.matches("\\d+")) {
            System.out.println("传入的是数字ID，通过user_id查找");
            userOpt = userRepository.findById(Long.parseLong(studentId));
        } else {
            // 传入的是用户名
            System.out.println("传入的是用户名，通过username查找");
            userOpt = userRepository.findByUsername(studentId);
        }
        
        if (userOpt.isEmpty()) {
            System.out.println("未找到用户记录，返回false");
            return false;
        }
        
        User user = userOpt.get();
        System.out.println("找到用户记录: " + user.getUsername() + ", ID: " + user.getId());
        
        // 使用用户ID检查
        boolean exists = examSubmissionRepository.existsByExamIdAndStudentId(examId, user.getId());
        System.out.println("exists: " + exists);
        
        // 调试：查看所有提交记录
        List<ExamSubmission> allSubmissions = examSubmissionRepository.findByExamId(examId);
        System.out.println("该考试的所有提交记录数量: " + allSubmissions.size());
        for (ExamSubmission submission : allSubmissions) {
            System.out.println("  学生ID: " + submission.getStudent().getId() + 
                             ", 学生姓名: " + submission.getStudent().getUsername() + 
                             ", 提交时间: " + submission.getSubmittedAt());
        }
        
        return exists;
    }
    
    /**
     * 检查学生是否已提交考试（通过学生ID）
     */
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedExam(Long examId, Long studentId) {
        return examSubmissionRepository.existsByExamIdAndStudentId(examId, studentId);
    }
    
    /**
     * 获取学生的提交记录
     */
    @Transactional(readOnly = true)
    public Optional<ExamSubmission> getSubmission(Long examId, String studentId) {
        return examSubmissionRepository.findByExamIdAndStudentNumber(examId, studentId);
    }
    
    /**
     * 获取考试的所有提交记录
     */
    @Transactional(readOnly = true)
    public List<ExamSubmission> getExamSubmissions(Long examId) {
        return examSubmissionRepository.findByExamId(examId);
    }
    
    /**
     * 统计考试提交人数
     */
    @Transactional(readOnly = true)
    public long getSubmissionCount(Long examId) {
        return examSubmissionRepository.countByExamId(examId);
    }
    
    /**
     * 统计自动提交人数
     */
    @Transactional(readOnly = true)
    public long getAutoSubmissionCount(Long examId) {
        return examSubmissionRepository.countAutoSubmittedByExamId(examId);
    }
    
    /**
     * 批量自动提交超时考试
     * 当考试时间结束时调用
     */
    public int autoSubmitExpiredExams(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("考试不存在"));
            
        // 检查考试是否已结束
        LocalDateTime now = LocalDateTime.now();
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            // 找到所有有答案但没有提交记录的学生
            List<StudentAnswer> answers = studentAnswerService.getAnswersByExamId(examId);
            int autoSubmittedCount = 0;
            
            for (StudentAnswer answer : answers) {
                String studentId = answer.getStudent().getStudentId();
                if (!hasStudentSubmittedExam(examId, studentId)) {
                    try {
                        submitExam(examId, studentId, true);
                        autoSubmittedCount++;
                    } catch (Exception e) {
                        // 记录错误但继续处理其他学生
                        System.err.println("自动提交学生 " + studentId + " 的考试失败: " + e.getMessage());
                    }
                }
            }
            
            return autoSubmittedCount;
        }
        
        return 0;
    }
    
    /**
     * 获取学生提交详情
     */
    public Map<String, Object> getStudentSubmissionDetail(Long examId, String studentId) {
        // 现在通过User查找学生
        Optional<User> userOpt = userRepository.findByUsername(studentId);
        if (userOpt.isEmpty()) {
            throw new ResourceNotFoundException("用户不存在");
        }
        
        User user = userOpt.get();
        
        // 查找提交记录
        Optional<ExamSubmission> submissionOpt = examSubmissionRepository
            .findByExamIdAndStudentId(examId, user.getId());
            
        if (submissionOpt.isEmpty()) {
            throw new ResourceNotFoundException("未找到提交记录");
        }
        
        ExamSubmission submission = submissionOpt.get();
        
        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("submittedAt", submission.getSubmittedAt());
        result.put("answeredQuestions", submission.getAnsweredQuestions());
        result.put("totalQuestions", submission.getTotalQuestions());
        result.put("score", null); // TODO: 添加评分逻辑
        result.put("autoSubmitted", submission.isAutoSubmitted());
        result.put("submissionNote", submission.getSubmissionNote());
        
        return result;
    }
    
    /**
     * 更新考试状态（如果需要）
     */
    private void updateExamStatusIfNeeded(Exam exam, boolean autoSubmitted) {
        try {
            if (autoSubmitted) {
                // 自动提交说明考试时间已到，更新考试状态为已结束
                System.out.println("考试 " + exam.getId() + " 时间到期自动提交，检查是否需要更新状态为ENDED");
                if (exam.getStatus() == ExamStatus.PUBLISHED || exam.getStatus() == ExamStatus.IN_PROGRESS) {
                    exam.setStatus(ExamStatus.ENDED);
                    examRepository.save(exam);
                    System.out.println("考试 " + exam.getId() + " 状态已更新为ENDED");
                }
            } else {
                // 学生主动提交，如果是第一个学生提交，将状态更新为进行中
                if (exam.getStatus() == ExamStatus.PUBLISHED) {
                    exam.setStatus(ExamStatus.IN_PROGRESS);
                    examRepository.save(exam);
                    System.out.println("考试 " + exam.getId() + " 有学生开始提交，状态已更新为IN_PROGRESS");
                }
                
                // 检查是否所有学生都已提交，如果是则可以考虑更新为ENDED
                // 这里可以添加更复杂的逻辑，比如检查参与学生数量等
            }
        } catch (Exception e) {
            System.err.println("更新考试状态时出错: " + e.getMessage());
            // 不抛出异常，避免影响主要的提交流程
        }
    }
}

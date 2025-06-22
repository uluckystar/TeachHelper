package com.teachhelper.service.exam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.teachhelper.dto.response.ClassroomResponse;
import com.teachhelper.dto.response.ExamStatistics;
import com.teachhelper.entity.Classroom;
import com.teachhelper.entity.Exam;
import com.teachhelper.entity.ExamStatus;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.User;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ClassroomRepository;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.service.exam.ExamSubmissionService;
import com.teachhelper.service.question.QuestionService;
import com.teachhelper.service.student.StudentAnswerService;

@Service
@Transactional
public class ExamService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ClassroomRepository classroomRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private ExamSubmissionService examSubmissionService;
    
    public Exam createExam(String title, String description) {
        return createExam(title, description, null);
    }
    
    public Exam createExam(String title, String description, List<Long> targetClassroomIds) {
        User currentUser = authService.getCurrentUser();
        Exam exam = new Exam(title, description, currentUser);
        
        // 如果指定了目标班级，设置目标班级
        if (targetClassroomIds != null && !targetClassroomIds.isEmpty()) {
            List<Classroom> classrooms = classroomRepository.findAllById(targetClassroomIds);
            if (classrooms.size() != targetClassroomIds.size()) {
                throw new RuntimeException("部分班级不存在");
            }
            exam.setTargetClassrooms(Set.copyOf(classrooms));
        }
        
        return examRepository.save(exam);
    }
    
    public Exam createExam(String title, String description, List<Long> targetClassroomIds, 
                          Integer duration, LocalDateTime startTime, LocalDateTime endTime) {
        User currentUser = authService.getCurrentUser();
        Exam exam = new Exam(title, description, currentUser);
        
        // 设置时间字段
        if (duration != null) {
            exam.setDuration(duration);
        }
        if (startTime != null) {
            exam.setStartTime(startTime);
        }
        if (endTime != null) {
            exam.setEndTime(endTime);
        }
        
        // 如果指定了目标班级，设置目标班级
        if (targetClassroomIds != null && !targetClassroomIds.isEmpty()) {
            List<Classroom> classrooms = classroomRepository.findAllById(targetClassroomIds);
            if (classrooms.size() != targetClassroomIds.size()) {
                throw new RuntimeException("部分班级不存在");
            }
            exam.setTargetClassrooms(Set.copyOf(classrooms));
        }
        
        return examRepository.save(exam);
    }
    
    public Exam getExamById(Long id) {
        // 使用包含关联数据的查询方法，避免懒加载问题
        Exam exam = examRepository.findByIdWithClassroomsAndStudents(id)
            .orElseThrow(() -> new ResourceNotFoundException("考试不存在，ID: " + id));
        
        // 检查用户权限
        User currentUser = authService.getCurrentUser();
        System.out.println("=== 考试权限检查 ===");
        System.out.println("考试ID: " + id + ", 标题: " + exam.getTitle() + ", 状态: " + exam.getStatus());
        System.out.println("当前用户ID: " + currentUser.getId() + ", 角色: " + currentUser.getRoles());
        
        // 管理员可以查看所有考试
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            System.out.println("管理员权限，允许访问");
            return exam;
        }
        
        // 教师可以查看自己创建的考试
        if (currentUser.getRoles().contains(Role.TEACHER)) {
            if (exam.getCreatedBy().getId().equals(currentUser.getId())) {
                System.out.println("教师权限，是自己创建的考试，允许访问");
                return exam;
            }
        }
        
        // 学生只能查看已发布或已结束的且自己所在班级的考试
        if (currentUser.getRoles().contains(Role.STUDENT)) {
            // 检查考试是否已发布或已结束（学生可以查看已结束的考试查看批改结果）
            if (exam.getStatus() != ExamStatus.PUBLISHED && exam.getStatus() != ExamStatus.ENDED) {
                System.out.println("考试权限检查失败：考试状态为 " + exam.getStatus() + "，学生ID: " + currentUser.getId());
                throw new RuntimeException("考试尚未发布");
            }
            
            // 强制加载目标班级和学生信息以避免懒加载问题
            Set<Classroom> targetClassrooms = exam.getTargetClassrooms();
            System.out.println("考试 " + exam.getId() + " 的目标班级数量: " + targetClassrooms.size());
            
            // 检查学生是否在考试的目标班级中
            boolean hasAccess = false;
            for (Classroom classroom : targetClassrooms) {
                Set<User> students = classroom.getStudents();
                System.out.println("班级 " + classroom.getId() + " (" + classroom.getName() + ") 的学生数量: " + students.size());
                
                for (User student : students) {
                    System.out.println("检查学生ID: " + student.getId() + " vs 当前用户ID: " + currentUser.getId());
                    if (student.getId().equals(currentUser.getId())) {
                        hasAccess = true;
                        System.out.println("找到匹配的学生，允许访问");
                        break;
                    }
                }
                
                if (hasAccess) break;
            }
            
            if (!hasAccess) {
                System.out.println("学生权限检查失败：学生ID " + currentUser.getId() + " 不在考试 " + exam.getId() + " 的任何目标班级中");
                throw new RuntimeException("无权限访问此考试");
            }
            
            System.out.println("学生权限检查通过：学生ID " + currentUser.getId() + " 可以访问考试 " + exam.getId());
            return exam;
        }
        
        throw new RuntimeException("无权限访问此考试");
    }
    
    public List<Exam> getAllExamsByCurrentUser() {
        User currentUser = authService.getCurrentUser();
        return examRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId());
    }
    
    public Page<Exam> getExamsByCurrentUser(Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        return examRepository.findByCreatedById(currentUser.getId(), pageable);
    }
    
    // 获取学生可以参与的考试（学生所在班级的已发布考试）
    public List<Exam> getAllAvailableExams() {
        User currentUser = authService.getCurrentUser();
        System.out.println("=== ExamService.getAllAvailableExams 调试信息 ===");
        System.out.println("当前用户: " + currentUser.getUsername() + ", ID: " + currentUser.getId() + ", 角色: " + currentUser.getRoles());
        
        if (currentUser.getRoles().contains(Role.STUDENT)) {
            // 学生只能看到自己所在班级的已发布考试
            System.out.println("学生用户，查询学生所在班级的已发布考试");
            List<Exam> exams = examRepository.findAvailableExamsForStudent(currentUser.getId());
            System.out.println("查询到考试数量: " + exams.size());
            return exams;
        } else {
            // 管理员可以看到所有已发布的考试
            System.out.println("管理员用户，查询所有已发布的考试");
            List<Exam> exams = examRepository.findAllPublishedExams();
            System.out.println("查询到考试数量: " + exams.size());
            return exams;
        }
    }
    
    // 获取学生的所有相关考试（包括可参加、已结束、已评估的考试）
    public List<Exam> getAllStudentExams() {
        User currentUser = authService.getCurrentUser();
        System.out.println("=== ExamService.getAllStudentExams 调试信息 ===");
        System.out.println("当前用户: " + currentUser.getUsername() + ", ID: " + currentUser.getId() + ", 角色: " + currentUser.getRoles());
        
        if (!currentUser.getRoles().contains(Role.STUDENT)) {
            throw new RuntimeException("只有学生可以访问此方法");
        }
        
        List<Exam> exams = examRepository.findAllAccessibleExamsForStudent(currentUser.getId());
        System.out.println("查询到学生相关考试数量: " + exams.size());
        return exams;
    }
    
    // 分页获取学生可以参与的考试
    public Page<Exam> getAvailableExams(Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser.getRoles().contains(Role.STUDENT)) {
            // 学生只能看到自己所在班级的已发布考试
            return examRepository.findAvailableExamsForStudent(currentUser.getId(), pageable);
        } else {
            // 管理员可以看到所有已发布的考试
            return examRepository.findAllPublishedExams(pageable);
        }
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
    
    public Exam updateExam(Long id, String title, String description, Integer duration, 
                          LocalDateTime startTime, LocalDateTime endTime) {
        Exam exam = getExamById(id);
        
        // Check if current user is the owner
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限修改此考试");
        }
        
        exam.setTitle(title);
        exam.setDescription(description);
        
        // 更新时间字段
        if (duration != null) {
            exam.setDuration(duration);
        }
        if (startTime != null) {
            exam.setStartTime(startTime);
        }
        if (endTime != null) {
            exam.setEndTime(endTime);
        }
        
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
        
        // 注释掉异常抛出，让控制器层处理验证
        // if (!hasQuestions) {
        //     throw new RuntimeException("考试必须包含至少一个题目才能发布");
        // }
        
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
        
        try {
            // 获取考试的题目数量
            int totalQuestions = exam.getQuestions() != null ? exam.getQuestions().size() : 0;
            stats.setTotalQuestions(totalQuestions);
            
            // 获取考试的所有答案统计
            long totalAnswers = studentAnswerService.getAnswerCountByExamId(examId);
            long evaluatedAnswers = studentAnswerService.getEvaluatedAnswerCountByExamId(examId);
            
            stats.setTotalAnswers(totalAnswers);
            stats.setEvaluatedAnswers(evaluatedAnswers);
            stats.setUnevaluatedAnswers(totalAnswers - evaluatedAnswers);
            
            // 计算评估进度
            if (totalAnswers > 0) {
                double progress = (double) evaluatedAnswers / totalAnswers * 100.0;
                stats.setEvaluationProgress(progress);
            } else {
                stats.setEvaluationProgress(0.0);
            }
            
            // 获取参与考试的学生数量
            long totalStudents = studentAnswerService.getDistinctStudentCountByExamId(examId);
            stats.setTotalStudents((int) totalStudents);
            
            // 获取平均分（仅评估过的答案）
            if (evaluatedAnswers > 0) {
                Double averageScore = studentAnswerService.getAverageScoreByExamId(examId);
                stats.setAverageScore(averageScore != null ? averageScore : 0.0);
                
                // 获取最高分和最低分
                Double maxScore = studentAnswerService.getMaxScoreByExamId(examId);
                Double minScore = studentAnswerService.getMinScoreByExamId(examId);
                stats.setMaxScore(maxScore != null ? maxScore : 0.0);
                stats.setMinScore(minScore != null ? minScore : 0.0);
            } else {
                stats.setAverageScore(0.0);
                stats.setMaxScore(0.0);
                stats.setMinScore(0.0);
            }
            
        } catch (Exception e) {
            System.err.println("❌ 获取考试统计信息失败: " + e.getMessage());
            e.printStackTrace();
            
            // 设置默认值
            stats.setTotalAnswers(0);
            stats.setEvaluatedAnswers(0);
            stats.setUnevaluatedAnswers(0);
            stats.setEvaluationProgress(0.0);
            stats.setTotalStudents(0);
            stats.setAverageScore(0.0);
            stats.setMaxScore(0.0);
            stats.setMinScore(0.0);
            stats.setTotalQuestions(exam.getQuestions() != null ? exam.getQuestions().size() : 0);
        }
        
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

    public Exam unpublishExam(Long examId) {
        Exam exam = getExamById(examId);
        
        // 验证权限
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限撤销发布此考试");
        }
        
        // 验证考试状态 - 只有已发布的考试才能撤销发布
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("只能撤销发布已发布状态的考试");
        }
        
        // 检查是否有学生已经开始答题
        // 这里可以添加更严格的检查，比如如果有学生已经提交答案就不能撤销
        
        // 撤销发布，恢复为草稿状态
        exam.setStatus(ExamStatus.DRAFT);
        return examRepository.save(exam);
    }
    
    public Exam updateExamClassrooms(Long examId, List<Long> classroomIds) {
        Exam exam = getExamById(examId);
        
        // 验证权限
        User currentUser = authService.getCurrentUser();
        if (!exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限调整此考试的班级");
        }
        
        // 如果班级ID列表为空，则清空目标班级
        if (classroomIds == null || classroomIds.isEmpty()) {
            exam.setTargetClassrooms(new HashSet<>());
        } else {
            // 查找所有班级
            List<Classroom> classrooms = classroomRepository.findAllById(classroomIds);
            if (classrooms.size() != classroomIds.size()) {
                throw new RuntimeException("部分班级不存在");
            }
            exam.setTargetClassrooms(new HashSet<>(classrooms));
        }
        
        return examRepository.save(exam);
    }
    
    public List<ClassroomResponse> getExamClassrooms(Long examId) {
        Exam exam = getExamById(examId);
        
        // 验证权限 - 教师只能查看自己创建的考试，管理员可以查看所有考试
        User currentUser = authService.getCurrentUser();
        boolean isAdmin = currentUser.getRoles().contains(Role.ADMIN);
        if (!isAdmin && !exam.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("无权限查看此考试的班级信息");
        }
        
        Set<Classroom> classrooms = exam.getTargetClassrooms();
        if (classrooms == null) {
            return List.of();
        }
        
        return classrooms.stream()
            .map(this::convertToClassroomResponse)
            .toList();
    }
    
    private ClassroomResponse convertToClassroomResponse(Classroom classroom) {
        ClassroomResponse response = new ClassroomResponse();
        response.setId(classroom.getId());
        response.setName(classroom.getName());
        response.setDescription(classroom.getDescription());
        response.setClassCode(classroom.getClassCode());
        response.setCreatedBy(classroom.getCreatedBy().getUsername());
        response.setCreatedAt(classroom.getCreatedAt());
        response.setUpdatedAt(classroom.getUpdatedAt());
        
        // 设置学生相关信息
        if (classroom.getStudents() != null) {
            response.setStudentCount(classroom.getStudents().size());
            response.setStudents(classroom.getStudents().stream()
                .map(student -> {
                    ClassroomResponse.StudentResponse studentResponse = new ClassroomResponse.StudentResponse();
                    studentResponse.setId(student.getId());
                    studentResponse.setUsername(student.getUsername());
                    studentResponse.setEmail(student.getEmail());
                    // joinedAt 需要从关联表获取，这里先用占位符
                    studentResponse.setJoinedAt(classroom.getCreatedAt());
                    return studentResponse;
                })
                .toList());
        } else {
            response.setStudentCount(0);
            response.setStudents(List.of());
        }
        
        // 设置考试数量（暂时设为0，实际应查询数据库）
        response.setExamCount(0);
        
        return response;
    }

    /**
     * 自动结束超时的考试
     * 检查所有已发布的考试，如果结束时间已过则自动结束并提交学生答案
     */
    public int autoEndExpiredExams() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Exam> publishedExams = examRepository.findByStatus(ExamStatus.PUBLISHED);
        
        System.out.println("=== 自动结束考试检查 ===");
        System.out.println("当前时间: " + now);
        System.out.println("已发布考试数量: " + publishedExams.size());
        
        int endedCount = 0;
        for (Exam exam : publishedExams) {
            System.out.println("检查考试: " + exam.getTitle() + " (ID: " + exam.getId() + ")");
            System.out.println("  结束时间: " + exam.getEndTime());
            System.out.println("  当前时间: " + now);
            
            if (exam.getEndTime() != null) {
                boolean isExpired = now.isAfter(exam.getEndTime());
                System.out.println("  是否过期: " + isExpired);
                
                if (isExpired) {
                    try {
                        // 自动提交所有有答案但未提交的学生
                        int autoSubmittedCount = examSubmissionService.autoSubmitExpiredExams(exam.getId());
                        System.out.println("  自动提交学生数: " + autoSubmittedCount);
                        
                        // 更新考试状态为已结束
                        exam.setStatus(ExamStatus.ENDED);
                        examRepository.save(exam);
                        endedCount++;
                        
                        System.out.println("  考试 " + exam.getTitle() + " 已自动结束");
                    } catch (Exception e) {
                        System.err.println("  自动结束考试失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("  考试未设置结束时间，跳过");
            }
        }
        
        System.out.println("自动结束考试检查完成，结束了 " + endedCount + " 个考试");
        return endedCount;
    }
    
    /**
     * 检查考试是否应该自动结束
     */
    public boolean shouldAutoEndExam(Long examId) {
        Exam exam = getExamById(examId);
        if (exam.getEndTime() == null) {
            return false;
        }
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return now.isAfter(exam.getEndTime()) && exam.getStatus() == ExamStatus.PUBLISHED;
    }

    /**
     * 检查并更新考试状态为已评估
     * 当考试的所有答案都已批阅完成时，自动将考试状态更新为EVALUATED
     */
    public boolean checkAndUpdateExamToEvaluated(Long examId) {
        try {
            Exam exam = getExamById(examId);
            
            // 只有ENDED或IN_PROGRESS状态的考试才需要检查是否可以更新为EVALUATED
            if (exam.getStatus() != ExamStatus.ENDED && exam.getStatus() != ExamStatus.IN_PROGRESS) {
                return false;
            }
            
            // 获取考试统计信息
            ExamStatistics stats = getExamStatistics(examId);
            
            // 如果有答案且所有答案都已评估完成，则更新状态为EVALUATED
            if (stats.getTotalAnswers() > 0 && stats.getEvaluatedAnswers() >= stats.getTotalAnswers()) {
                exam.setStatus(ExamStatus.EVALUATED);
                exam = examRepository.save(exam);
                
                System.out.println("✅ 考试 " + examId + " 状态已更新为 EVALUATED (共 " + stats.getTotalAnswers() + " 个答案已全部批阅完成)");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("❌ 检查并更新考试状态失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 批量检查并更新多个考试的状态
     */
    public void checkAndUpdateExamsToEvaluated(List<Long> examIds) {
        for (Long examId : examIds) {
            try {
                checkAndUpdateExamToEvaluated(examId);
            } catch (Exception e) {
                System.err.println("❌ 检查考试 " + examId + " 状态失败: " + e.getMessage());
            }
        }
    }
}

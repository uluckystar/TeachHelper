package com.teachhelper.service.student;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

import com.teachhelper.entity.Question;
import com.teachhelper.entity.Student;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ExamSubmissionRepository;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.StudentAnswerRepository;
import com.teachhelper.repository.StudentRepository;

@Service
@Transactional
public class StudentAnswerService {
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamSubmissionRepository examSubmissionRepository;
    
    public StudentAnswer submitAnswer(StudentAnswer studentAnswer) {
        // Validate question exists
        Question question = questionRepository.findById(studentAnswer.getQuestion().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        
        // Set question reference
        studentAnswer.setQuestion(question);
        
        // Create or find student
        Student student = findOrCreateStudent(studentAnswer.getStudent());
        studentAnswer.setStudent(student);
        
        // 检查是否已存在该学生对该题目的答案
        StudentAnswer existingAnswer = studentAnswerRepository.findByStudentStudentIdAndQuestionId(
            student.getStudentId(), question.getId());
        
        if (existingAnswer != null) {
            // 更新现有答案
            existingAnswer.setAnswerText(studentAnswer.getAnswerText());
            existingAnswer.setCreatedAt(LocalDateTime.now()); // 更新提交时间
            return studentAnswerRepository.save(existingAnswer);
        } else {
            // 创建新答案
            studentAnswer.setCreatedAt(LocalDateTime.now());
            return studentAnswerRepository.save(studentAnswer);
        }
    }
    
    public StudentAnswer updateAnswer(Long id, StudentAnswer answerDetails) {
        StudentAnswer answer = studentAnswerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student answer not found with id: " + id));
        
        answer.setAnswerText(answerDetails.getAnswerText());
        answer.setScore(answerDetails.getScore());
        answer.setFeedback(answerDetails.getFeedback());
        answer.setEvaluated(answerDetails.isEvaluated());
        
        if (answerDetails.isEvaluated() && answer.getEvaluatedAt() == null) {
            answer.setEvaluatedAt(LocalDateTime.now());
        }
        
        return studentAnswerRepository.save(answer);
    }
    
    public void deleteAnswer(Long id) {
        StudentAnswer answer = studentAnswerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student answer not found with id: " + id));
        studentAnswerRepository.delete(answer);
    }
    
    @Transactional(readOnly = true)
    public StudentAnswer getAnswerById(Long id) {
        return studentAnswerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student answer not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public Page<StudentAnswer> getAllAnswers(Pageable pageable) {
        return studentAnswerRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByQuestionId(Long questionId) {
        return studentAnswerRepository.findByQuestionId(questionId);
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByStudentId(Long studentId) {
        return studentAnswerRepository.findByStudentId(studentId);
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByExamId(Long examId) {
        return studentAnswerRepository.findByQuestionExamId(examId);
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getUnevaluatedAnswers() {
        return studentAnswerRepository.findByIsEvaluatedFalse();
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getUnevaluatedAnswersByQuestionId(Long questionId) {
        return studentAnswerRepository.findByQuestionIdAndIsEvaluatedFalse(questionId);
    }
    
    @Transactional(readOnly = true)
    public long getAnswerCountByQuestionId(Long questionId) {
        return studentAnswerRepository.countByQuestionId(questionId);
    }
    
    @Transactional(readOnly = true)
    public long getEvaluatedAnswerCountByQuestionId(Long questionId) {
        return studentAnswerRepository.countByQuestionIdAndIsEvaluatedTrue(questionId);
    }
    
    @Transactional(readOnly = true)
    public double getAverageScoreByQuestionId(Long questionId) {
        return studentAnswerRepository.findAverageScoreByQuestionId(questionId);
    }
    
    // Exam-level statistics methods
    @Transactional(readOnly = true)
    public List<StudentAnswer> getUnevaluatedAnswersByExamId(Long examId) {
        return studentAnswerRepository.findByQuestionExamIdAndIsEvaluated(examId, false);
    }
    
    @Transactional(readOnly = true)
    public List<Long> getUnevaluatedAnswerIdsByQuestionId(Long questionId) {
        return studentAnswerRepository.findByQuestionIdAndIsEvaluatedFalse(questionId)
            .stream()
            .map(StudentAnswer::getId)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public long getAnswerCountByExamId(Long examId) {
        return studentAnswerRepository.countByQuestionExamId(examId);
    }
    
    @Transactional(readOnly = true)
    public long getEvaluatedAnswerCountByExamId(Long examId) {
        return studentAnswerRepository.countByQuestionExamIdAndIsEvaluatedTrue(examId);
    }
    
    @Transactional(readOnly = true)
    public double getAverageScoreByExamId(Long examId) {
        return studentAnswerRepository.findAverageScoreByExamId(examId);
    }

    /**
     * 获取指定考试的最高分（仅已评估的答案）
     */
    public Double getMaxScoreByExamId(Long examId) {
        return studentAnswerRepository.findMaxScoreByExamId(examId);
    }

    /**
     * 获取指定考试的最低分（仅已评估的答案）
     */
    public Double getMinScoreByExamId(Long examId) {
        return studentAnswerRepository.findMinScoreByExamId(examId);
    }

    // Batch operations
    public List<StudentAnswer> submitAnswersInBatch(List<StudentAnswer> answers) {
        // Process each answer to ensure proper student and question references
        for (StudentAnswer answer : answers) {
            // Validate question exists
            Question question = questionRepository.findById(answer.getQuestion().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
            answer.setQuestion(question);
            
            // Create or find student
            Student student = findOrCreateStudent(answer.getStudent());
            answer.setStudent(student);
            
            // Set submission timestamp
            answer.setCreatedAt(LocalDateTime.now());
        }
        
        return studentAnswerRepository.saveAll(answers);
    }
    
    // Student management helper methods
    private Student findOrCreateStudent(Student student) {
        if (student.getId() != null) {
            return studentRepository.findById(student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        }
        
        // 检查studentId是否为数字（用户ID）
        String studentId = student.getStudentId();
        
        // 如果 studentId 为空或无效，需要处理
        if (studentId == null || studentId.trim().isEmpty()) {
            System.out.println("警告：学生记录缺少学号，尝试从邮箱查找现有记录");
            if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
                Optional<Student> existingByEmail = studentRepository.findByEmail(student.getEmail());
                if (existingByEmail.isPresent()) {
                    Student existing = existingByEmail.get();
                    // 如果现有记录也没有学号，生成一个
                    if (existing.getStudentId() == null || existing.getStudentId().trim().isEmpty()) {
                        String generatedStudentId = generateStudentId(existing);
                        existing.setStudentId(generatedStudentId);
                        existing = studentRepository.save(existing);
                        System.out.println("为现有学生生成学号: " + generatedStudentId);
                    }
                    return existing;
                }
            }
            
            // 生成新的学号
            studentId = generateStudentId(student);
            student.setStudentId(studentId);
            System.out.println("为新学生生成学号: " + studentId);
        }
        
        if (studentId != null && studentId.matches("\\d+")) {
            // studentId是数字，这是users表的id，对应students表的student_id字段
            try {
                // 直接通过student_id字段查找（student_id = users.id）
                Optional<Student> existingByUserId = studentRepository.findByStudentId(studentId);
                if (existingByUserId.isPresent()) {
                    return existingByUserId.get();
                }
            } catch (NumberFormatException e) {
                // 如果解析失败，继续下面的逻辑
            }
        } else {
            // studentId不是数字，按传统字符串查找
            Optional<Student> existingStudent = studentRepository.findByStudentId(studentId);
            if (existingStudent.isPresent()) {
                return existingStudent.get();
            }
        }
        
        // Try to find by email as fallback
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            Optional<Student> existingByEmail = studentRepository.findByEmail(student.getEmail());
            if (existingByEmail.isPresent()) {
                Student existing = existingByEmail.get();
                // 如果现有记录没有学号，更新它
                if (existing.getStudentId() == null || existing.getStudentId().trim().isEmpty()) {
                    existing.setStudentId(studentId);
                    existing = studentRepository.save(existing);
                    System.out.println("更新现有学生的学号: " + studentId);
                }
                return existing;
            }
        }
        
        // Create new student only if not found
        try {
            return studentRepository.save(student);
        } catch (Exception e) {
            // If creation fails due to unique constraint, try to find by email again
            if (student.getEmail() != null) {
                Optional<Student> existingByEmail = studentRepository.findByEmail(student.getEmail());
                if (existingByEmail.isPresent()) {
                    return existingByEmail.get();
                }
            }
            // Re-throw if it's not a unique constraint issue
            throw e;
        }
    }
    
    /**
     * 为学生生成学号
     */
    private String generateStudentId(Student student) {
        // 如果有邮箱，可以从邮箱中提取用户名部分作为学号基础
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            String emailPrefix = student.getEmail().split("@")[0];
            // 如果邮箱前缀是数字，直接使用
            if (emailPrefix.matches("\\d+")) {
                return emailPrefix;
            }
            // 否则添加时间戳确保唯一性
            return emailPrefix + "_" + System.currentTimeMillis() % 100000;
        }
        
        // 如果没有邮箱，使用姓名 + 时间戳
        String name = student.getName() != null ? student.getName().replaceAll("\\s+", "") : "student";
        return name + "_" + System.currentTimeMillis() % 100000;
    }
    
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedAnswer(String studentId, Long questionId) {
        return studentAnswerRepository.existsByStudentStudentIdAndQuestionId(studentId, questionId);
    }
    
    /**
     * 检查学生是否已提交指定考试（基于 ExamSubmission 表）
     */
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedExam(Long examId, Long studentId) {
        return examSubmissionRepository.existsByExamIdAndStudentId(examId, studentId);
    }
    
    /**
     * 检查学生是否已提交指定考试（通过 studentId 字符串，基于 ExamSubmission 表）
     */
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedExam(Long examId, String studentId) {
        return examSubmissionRepository.existsByExamIdAndStudentStudentId(examId, studentId);
    }

    // Export functionality
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersForExport(Long examId, Long questionId, Boolean evaluated) {
        if (examId != null && questionId != null) {
            if (evaluated != null) {
                return studentAnswerRepository.findByQuestionExamIdAndQuestionIdAndIsEvaluated(examId, questionId, evaluated);
            } else {
                return studentAnswerRepository.findByQuestionExamIdAndQuestionId(examId, questionId);
            }
        } else if (examId != null) {
            if (evaluated != null) {
                return studentAnswerRepository.findByQuestionExamIdAndIsEvaluated(examId, evaluated);
            } else {
                return studentAnswerRepository.findByQuestionExamId(examId);
            }
        } else if (questionId != null) {
            if (evaluated != null) {
                return studentAnswerRepository.findByQuestionIdAndIsEvaluated(questionId, evaluated);
            } else {
                return studentAnswerRepository.findByQuestionId(questionId);
            }
        } else {
            if (evaluated != null) {
                return studentAnswerRepository.findByIsEvaluated(evaluated);
            } else {
                return studentAnswerRepository.findAll();
            }
        }
    }

    public ByteArrayResource exportAnswersToFile(Long examId, Long questionId, Boolean evaluated) throws IOException {
        List<StudentAnswer> answers = getAnswersForExport(examId, questionId, evaluated);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生答案");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("答案ID");
            headerRow.createCell(1).setCellValue("学生学号");
            headerRow.createCell(2).setCellValue("学生姓名");
            headerRow.createCell(3).setCellValue("学生邮箱");
            headerRow.createCell(4).setCellValue("题目标题");
            headerRow.createCell(5).setCellValue("答案内容");
            headerRow.createCell(6).setCellValue("分数");
            headerRow.createCell(7).setCellValue("反馈");
            headerRow.createCell(8).setCellValue("是否已评估");
            headerRow.createCell(9).setCellValue("提交时间");
            headerRow.createCell(10).setCellValue("评估时间");
            
            // 填充数据
            for (int i = 0; i < answers.size(); i++) {
                StudentAnswer answer = answers.get(i);
                Row dataRow = sheet.createRow(i + 1);
                
                dataRow.createCell(0).setCellValue(answer.getId());
                dataRow.createCell(1).setCellValue(answer.getStudent().getStudentId());
                dataRow.createCell(2).setCellValue(answer.getStudent().getName());
                dataRow.createCell(3).setCellValue(answer.getStudent().getEmail());
                dataRow.createCell(4).setCellValue(answer.getQuestion().getTitle());
                dataRow.createCell(5).setCellValue(answer.getAnswerText());
                dataRow.createCell(6).setCellValue(answer.getScore() != null ? answer.getScore().doubleValue() : 0.0);
                dataRow.createCell(7).setCellValue(answer.getFeedback() != null ? answer.getFeedback() : "");
                dataRow.createCell(8).setCellValue(answer.isEvaluated() ? "是" : "否");
                dataRow.createCell(9).setCellValue(answer.getCreatedAt() != null ? answer.getCreatedAt().toString() : "");
                dataRow.createCell(10).setCellValue(answer.getEvaluatedAt() != null ? answer.getEvaluatedAt().toString() : "");
            }
            
            // 写入字节数组
            workbook.write(outputStream);
        }
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    public ByteArrayResource exportAnswersByStudent(Long studentId) throws IOException {
        List<StudentAnswer> answers = getAnswersByStudentId(studentId);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生答案");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("答案ID");
            headerRow.createCell(1).setCellValue("题目标题");
            headerRow.createCell(2).setCellValue("考试名称");
            headerRow.createCell(3).setCellValue("答案内容");
            headerRow.createCell(4).setCellValue("分数");
            headerRow.createCell(5).setCellValue("反馈");
            headerRow.createCell(6).setCellValue("是否已评估");
            headerRow.createCell(7).setCellValue("提交时间");
            headerRow.createCell(8).setCellValue("评估时间");
            
            // 填充数据
            for (int i = 0; i < answers.size(); i++) {
                StudentAnswer answer = answers.get(i);
                Row dataRow = sheet.createRow(i + 1);
                
                dataRow.createCell(0).setCellValue(answer.getId());
                dataRow.createCell(1).setCellValue(answer.getQuestion().getTitle());
                dataRow.createCell(2).setCellValue(answer.getQuestion().getExam().getTitle());
                dataRow.createCell(3).setCellValue(answer.getAnswerText());
                dataRow.createCell(4).setCellValue(answer.getScore() != null ? answer.getScore().doubleValue() : 0.0);
                dataRow.createCell(5).setCellValue(answer.getFeedback() != null ? answer.getFeedback() : "");
                dataRow.createCell(6).setCellValue(answer.isEvaluated() ? "是" : "否");
                dataRow.createCell(7).setCellValue(answer.getCreatedAt() != null ? answer.getCreatedAt().toString() : "");
                dataRow.createCell(8).setCellValue(answer.getEvaluatedAt() != null ? answer.getEvaluatedAt().toString() : "");
            }
            
            // 写入字节数组
            workbook.write(outputStream);
        }
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    public int importAnswersFromFile(MultipartFile file) throws IOException {
        // TODO: 实现从文件导入答案的逻辑
        // 解析Excel/CSV文件并创建StudentAnswer对象
        return 0;
    }

    public int importAnswersToExam(Long examId, MultipartFile file) throws IOException {
        // TODO: 实现从文件导入答案到指定考试的逻辑
        // 解析文件并验证题目属于指定考试
        return 0;
    }

    // 获取考试中不重复的学生数量
    @Transactional(readOnly = true)
    public long getDistinctStudentCountByExamId(Long examId) {
        return studentAnswerRepository.countDistinctStudentByQuestionExamId(examId);
    }
    
    /**
     * 根据考试ID和学生ID获取答案
     */
    public List<StudentAnswer> getAnswersByExamIdAndStudentId(Long examId, Long studentId) {
        return studentAnswerRepository.findByQuestionExamIdAndStudentId(examId, studentId);
    }
    
    /**
     * 根据考试ID和用户名获取答案（通过Student表的关联）
     */
    public List<StudentAnswer> getAnswersByExamIdAndUsername(Long examId, String username) {
        System.out.println("=== StudentAnswerService.getAnswersByExamIdAndUsername ===");
        System.out.println("examId: " + examId + ", username: " + username);
        
        // 通过用户名查询学生答案
        // 查询逻辑: users.username -> users.id -> students.student_id -> students.id -> student_answers.student_id
        List<StudentAnswer> answers = studentAnswerRepository.findByQuestionExamIdAndUsername(examId, username);
        System.out.println("查询到的答案数量: " + answers.size());
        
        return answers;
    }
    
    /**
     * 获取答案详情（预加载关联实体，用于批量处理）
     */
    @Transactional(readOnly = true)
    public StudentAnswer getAnswerByIdWithFetch(Long answerId) {
        return studentAnswerRepository.findByIdWithFetch(answerId);
    }
    
    /**
     * 批量获取答案详情（预加载关联实体，用于批量处理）
     */
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByIdsWithFetch(List<Long> answerIds) {
        if (answerIds == null || answerIds.isEmpty()) {
            return List.of();
        }
        return studentAnswerRepository.findByIdInWithFetch(answerIds);
    }
    
    /**
     * 批量修复没有学号的学生记录
     */
    @Transactional
    public int fixStudentsWithoutStudentId() {
        System.out.println("=== 开始修复没有学号的学生记录 ===");
        
        // 查找所有没有学号或学号为空的学生
        List<Student> studentsWithoutId = studentRepository.findAll().stream()
            .filter(s -> s.getStudentId() == null || s.getStudentId().trim().isEmpty())
            .collect(java.util.stream.Collectors.toList());
        
        System.out.println("找到 " + studentsWithoutId.size() + " 个没有学号的学生记录");
        
        int fixedCount = 0;
        for (Student student : studentsWithoutId) {
            try {
                String generatedStudentId = generateStudentId(student);
                student.setStudentId(generatedStudentId);
                studentRepository.save(student);
                System.out.println("为学生 " + student.getName() + " 生成学号: " + generatedStudentId);
                fixedCount++;
            } catch (Exception e) {
                System.err.println("修复学生 " + student.getName() + " 的学号时出错: " + e.getMessage());
            }
        }
        
        System.out.println("=== 学号修复完成，共修复 " + fixedCount + " 个学生记录 ===");
        return fixedCount;
    }
}

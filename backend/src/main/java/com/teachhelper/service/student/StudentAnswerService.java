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
    
    public StudentAnswer submitAnswer(StudentAnswer studentAnswer) {
        // Validate question exists
        Question question = questionRepository.findById(studentAnswer.getQuestion().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        
        // Set question reference
        studentAnswer.setQuestion(question);
        
        // Create or find student
        Student student = findOrCreateStudent(studentAnswer.getStudent());
        studentAnswer.setStudent(student);
        
        // Set submission timestamp
        studentAnswer.setCreatedAt(LocalDateTime.now());
        
        return studentAnswerRepository.save(studentAnswer);
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
        
        // Try to find by student ID
        Optional<Student> existingStudent = studentRepository.findByStudentId(student.getStudentId());
        if (existingStudent.isPresent()) {
            return existingStudent.get();
        }
        
        // Create new student
        return studentRepository.save(student);
    }
    
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedAnswer(String studentId, Long questionId) {
        return studentAnswerRepository.existsByStudentStudentIdAndQuestionId(studentId, questionId);
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
}

package com.teachhelper.service.student;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.teachhelper.entity.Question;
import com.teachhelper.entity.User;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.dto.response.StudentExamPaperResponse;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ExamSubmissionRepository;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.StudentAnswerRepository;
import com.teachhelper.repository.UserRepository;

@Service
@Transactional
public class StudentAnswerService {
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
        
        // Validate user exists and is a student
        User user = studentAnswer.getStudent();
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("Student user not found");
        }
        
        User existingUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify the user is a student
        if (!existingUser.isStudent()) {
            throw new IllegalArgumentException("User is not a student");
        }
        
        studentAnswer.setStudent(existingUser);
        
        // Check if answer already exists for this student and question
        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByStudentId(existingUser.getId());
        StudentAnswer existingAnswer = studentAnswers.stream()
            .filter(sa -> sa.getQuestion().getId().equals(question.getId()))
            .findFirst()
            .orElse(null);
        
        if (existingAnswer != null) {
            // Update existing answer
            existingAnswer.setAnswerText(studentAnswer.getAnswerText());
            existingAnswer.setCreatedAt(LocalDateTime.now()); // Update submission time
            return studentAnswerRepository.save(existingAnswer);
        } else {
            // Create new answer
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
    
    // 新增：支持分页和筛选的方法
    @Transactional(readOnly = true)
    public Page<StudentAnswer> getAnswersByExamIdWithFilters(Long examId, int page, int size, 
            Long questionId, Boolean evaluated, String keyword) {
        return studentAnswerRepository.findByExamIdWithFilters(examId, questionId, evaluated, keyword,
                PageRequest.of(page, size));
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

    @Transactional(readOnly = true)
    public Double getMaxScoreByExamId(Long examId) {
        return studentAnswerRepository.findMaxScoreByExamId(examId);
    }

    @Transactional(readOnly = true)
    public Double getMinScoreByExamId(Long examId) {
        return studentAnswerRepository.findMinScoreByExamId(examId);
    }
    
    @Transactional(readOnly = true)
    public long getDistinctStudentCountByExamId(Long examId) {
        return studentAnswerRepository.countDistinctStudentByQuestionExamId(examId);
    }
    
    // Helper methods for User-based operations
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByExamIdAndStudentId(Long examId, Long studentId) {
        return studentAnswerRepository.findByQuestionExamIdAndStudentId(examId, studentId);
    }
    
    @Transactional(readOnly = true)
    public boolean hasSubmittedAnswers(Long studentId, Long examId) {
        List<StudentAnswer> answers = studentAnswerRepository.findByStudentId(studentId);
        return answers.stream()
            .anyMatch(sa -> sa.getQuestion().getExam().getId().equals(examId));
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByUsernameAndExamId(String username, Long examId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        if (!user.isStudent()) {
            throw new IllegalArgumentException("User is not a student");
        }
        
        return getAnswersByExamIdAndStudentId(examId, user.getId());
    }
    
    // Alias method for backward compatibility
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByExamIdAndUsername(Long examId, String username) {
        return getAnswersByUsernameAndExamId(username, examId);
    }
    
    // Batch operations
    public List<StudentAnswer> submitAnswersInBatch(List<StudentAnswer> answers) {
        // Process each answer to ensure proper user and question references
        for (StudentAnswer answer : answers) {
            // Validate question exists
            Question question = questionRepository.findById(answer.getQuestion().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
            answer.setQuestion(question);
            
            // Validate user exists and is a student
            User user = answer.getStudent();
            if (user == null || user.getId() == null) {
                throw new ResourceNotFoundException("Student user not found");
            }
            
            User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // Verify the user is a student
            if (!existingUser.isStudent()) {
                throw new IllegalArgumentException("User is not a student");
            }
            
            answer.setStudent(existingUser);
            
            // Set submission timestamp
            answer.setCreatedAt(LocalDateTime.now());
        }
        
        return studentAnswerRepository.saveAll(answers);
    }
    
    // Helper method for backward compatibility
    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedExam(Long examId, Long studentId) {
        return hasSubmittedAnswers(studentId, examId);
    }
    
    // Batch fetch operations
    @Transactional(readOnly = true)
    public StudentAnswer getAnswerByIdWithFetch(Long answerId) {
        return studentAnswerRepository.findByIdWithFetch(answerId);
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByIdsWithFetch(List<Long> answerIds) {
        if (answerIds == null || answerIds.isEmpty()) {
            return List.of();
        }
        return studentAnswerRepository.findByIdInWithFetch(answerIds);
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
                
                User student = answer.getStudent();
                
                dataRow.createCell(0).setCellValue(answer.getId());
                dataRow.createCell(1).setCellValue(student.getStudentNumber() != null ? student.getStudentNumber() : String.valueOf(student.getId()));
                dataRow.createCell(2).setCellValue(student.getRealName() != null ? student.getRealName() : student.getUsername());
                dataRow.createCell(3).setCellValue(student.getEmail());
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

    // Future implementation for import functionality
    public int importAnswersFromFile(MultipartFile file) throws IOException {
        // TODO: Implement import from file logic using User entities
        return 0;
    }

    public int importAnswersToExam(Long examId, MultipartFile file) throws IOException {
        // TODO: Implement import to specific exam logic using User entities
        return 0;
    }

    /**
     * 按学生分组获取考试答案
     * @param examId 考试ID
     * @return 按学生分组的答案Map，key为学生信息，value为该学生的所有答案
     */
    @Transactional(readOnly = true)
    public Map<User, List<StudentAnswer>> getAnswersByExamGroupedByStudent(Long examId) {
        List<StudentAnswer> allAnswers = studentAnswerRepository.findByQuestionExamIdOrderByStudentIdAscQuestionIdAsc(examId);
        
        return allAnswers.stream()
            .collect(Collectors.groupingBy(
                StudentAnswer::getStudent,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
    
    /**
     * 导出学生试卷为Excel格式
     */
    public ByteArrayResource exportStudentPaperAsExcel(StudentExamPaperResponse paper) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("学生试卷");
        
        int rowIndex = 0;
        
        // 标题行
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.createCell(0).setCellValue("学生试卷详情");
        
        // 空行
        rowIndex++;
        
        // 学生信息
        Row studentInfoRow = sheet.createRow(rowIndex++);
        studentInfoRow.createCell(0).setCellValue("学生姓名:");
        studentInfoRow.createCell(1).setCellValue(paper.getStudentName());
        studentInfoRow.createCell(3).setCellValue("学号:");
        studentInfoRow.createCell(4).setCellValue(paper.getStudentNumber());
        
        Row examInfoRow = sheet.createRow(rowIndex++);
        examInfoRow.createCell(0).setCellValue("考试名称:");
        examInfoRow.createCell(1).setCellValue(paper.getExamTitle());
        examInfoRow.createCell(3).setCellValue("总分:");
        examInfoRow.createCell(4).setCellValue(paper.getTotalScore() + "/" + paper.getMaxPossibleScore());
        
        // 空行
        rowIndex++;
        
        // 表头
        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.createCell(0).setCellValue("题目序号");
        headerRow.createCell(1).setCellValue("题目内容");
        headerRow.createCell(2).setCellValue("学生答案");
        headerRow.createCell(3).setCellValue("得分");
        headerRow.createCell(4).setCellValue("满分");
        headerRow.createCell(5).setCellValue("评估反馈");
        
        // 答案数据
        for (StudentAnswerResponse answer : paper.getAnswers()) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(answer.getQuestionId());
            dataRow.createCell(1).setCellValue(answer.getQuestionTitle());
            dataRow.createCell(2).setCellValue(answer.getAnswerText() != null ? answer.getAnswerText() : "");
            dataRow.createCell(3).setCellValue(answer.getScore() != null ? answer.getScore().toString() : "未评估");
            dataRow.createCell(4).setCellValue(answer.getMaxScore() != null ? answer.getMaxScore().toString() : "");
            dataRow.createCell(5).setCellValue(answer.getFeedback() != null ? answer.getFeedback() : "");
        }
        
        // 自动调整列宽
        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return new ByteArrayResource(outputStream.toByteArray());
    }
    
    /**
     * 导出学生试卷为PDF格式（简化版本，使用文本格式）
     */
    public ByteArrayResource exportStudentPaperAsPdf(StudentExamPaperResponse paper) throws IOException {
        // 这里应该使用PDF库如iText，为了简化，暂时返回文本格式
        StringBuilder content = new StringBuilder();
        content.append("学生试卷\n");
        content.append("======================\n\n");
        content.append("学生姓名: ").append(paper.getStudentName()).append("\n");
        content.append("学号: ").append(paper.getStudentNumber()).append("\n");
        content.append("考试名称: ").append(paper.getExamTitle()).append("\n");
        content.append("总分: ").append(paper.getTotalScore()).append("/").append(paper.getMaxPossibleScore()).append("\n\n");
        
        content.append("答题详情:\n");
        content.append("----------------------\n");
        
        for (int i = 0; i < paper.getAnswers().size(); i++) {
            StudentAnswerResponse answer = paper.getAnswers().get(i);
            content.append("题目 ").append(i + 1).append(": ").append(answer.getQuestionTitle()).append("\n");
            content.append("答案: ").append(answer.getAnswerText() != null ? answer.getAnswerText() : "未作答").append("\n");
            content.append("得分: ").append(answer.getScore() != null ? answer.getScore() : "未评估")
                   .append("/").append(answer.getMaxScore()).append("\n");
            if (answer.getFeedback() != null && !answer.getFeedback().trim().isEmpty()) {
                content.append("反馈: ").append(answer.getFeedback()).append("\n");
            }
            content.append("\n");
        }
        
        return new ByteArrayResource(content.toString().getBytes("UTF-8"));
    }

    // 获取指定考试的学生答案，按学生分组，分页查询
    @Transactional(readOnly = true)
    public Page<StudentExamPaperResponse> getAnswersByExamGroupedByStudentPaged(
            Long examId, int page, int size, String studentKeyword) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> result = studentAnswerRepository.findStudentPapersPagedByExamId(examId, studentKeyword, pageable);
        
        List<StudentExamPaperResponse> papers = result.getContent().stream()
            .map(row -> {
                Long studentId = (Long) row[0];
                String studentName = (String) row[1];
                String studentNumber = (String) row[2];
                String studentEmail = (String) row[3];
                Long totalQuestions = (Long) row[4];
                Long answeredQuestions = (Long) row[5];
                Long evaluatedAnswers = (Long) row[6];
                
                // 获取该学生的所有答案
                List<StudentAnswer> answers = studentAnswerRepository.findByQuestionExamIdAndStudentId(examId, studentId);
                
                StudentExamPaperResponse paper = new StudentExamPaperResponse();
                paper.setStudentId(studentId);
                paper.setStudentName(studentName);
                paper.setStudentNumber(studentNumber);
                paper.setStudentEmail(studentEmail);
                paper.setExamId(examId);
                paper.setAnswers(answers.stream()
                    .map(answer -> {
                        StudentAnswerResponse response = new StudentAnswerResponse();
                        response.setId(answer.getId());
                        response.setQuestionId(answer.getQuestion().getId());
                        response.setQuestionTitle(answer.getQuestion().getTitle());
                        response.setQuestionContent(answer.getQuestion().getContent());
                        response.setAnswerText(answer.getAnswerText());
                        response.setScore(answer.getScore() != null ? answer.getScore().doubleValue() : null);
                        response.setFeedback(answer.getFeedback());
                        response.setEvaluated(answer.isEvaluated());
                        response.setMaxScore(100.0); // 默认最大分数，实际应该从Question获取
                        response.setSubmittedAt(answer.getCreatedAt());
                        response.setEvaluatedAt(answer.getEvaluatedAt());
                        return response;
                    })
                    .collect(Collectors.toList()));
                paper.setTotalQuestions(totalQuestions.intValue());
                paper.setAnsweredQuestions(answeredQuestions.intValue());
                paper.setEvaluatedAnswers(evaluatedAnswers.intValue());
                paper.setFullyEvaluated(evaluatedAnswers.equals(answeredQuestions));
                
                // 计算总分和最大可能分数
                Double totalScore = answers.stream()
                    .filter(answer -> answer.getScore() != null)
                    .mapToDouble(answer -> answer.getScore().doubleValue())
                    .sum();
                
                Double maxPossibleScore = answers.stream()
                    .filter(answer -> answer.getQuestion() != null && answer.getQuestion().getMaxScore() != null)
                    .mapToDouble(answer -> answer.getQuestion().getMaxScore().doubleValue())
                    .sum();
                
                paper.setTotalScore(totalScore);
                paper.setMaxPossibleScore(maxPossibleScore);
                paper.setScorePercentage(maxPossibleScore > 0 ? (totalScore / maxPossibleScore) * 100 : 0.0);
                
                // 设置时间信息
                paper.setSubmittedAt(answers.stream()
                    .filter(answer -> answer.getCreatedAt() != null)
                    .map(StudentAnswer::getCreatedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(null));
                
                paper.setLastUpdated(answers.stream()
                    .filter(answer -> answer.getUpdatedAt() != null)
                    .map(StudentAnswer::getUpdatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null));
                
                return paper;
            })
            .collect(Collectors.toList());
        
        return new PageImpl<>(papers, pageable, result.getTotalElements());
    }
    
    // 获取指定学生在指定考试中的所有答案
    @Transactional(readOnly = true)
    public List<StudentAnswer> getStudentAnswersInExam(Long examId, Long studentId) {
        return studentAnswerRepository.findByQuestionExamIdAndStudentId(examId, studentId);
    }
}

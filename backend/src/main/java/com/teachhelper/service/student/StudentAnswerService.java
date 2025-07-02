package com.teachhelper.service.student;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.dto.response.StudentAnswerResponse;
import com.teachhelper.dto.response.StudentExamPaperResponse;
import com.teachhelper.entity.*;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.*;
import com.teachhelper.service.answer.LearningAnswerParserService;
import com.teachhelper.service.answer.SmartQuestionMatchingService;
import com.teachhelper.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.repository.QuestionOptionRepository;
import com.teachhelper.entity.QuestionOption;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentAnswerService {

    private static final Logger log = LoggerFactory.getLogger(StudentAnswerService.class);

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamSubmissionRepository examSubmissionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Autowired
    private LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private SmartQuestionMatchingService smartQuestionMatchingService;
    
    @Autowired
    private com.teachhelper.service.template.ExamPaperTemplateService examPaperTemplateService;
    
    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Transactional
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
    
    @Transactional
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
    
    @Transactional
    public void deleteAnswer(Long id) {
        StudentAnswer answer = studentAnswerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student answer not found with id: " + id));
        studentAnswerRepository.delete(answer);
    }
    
    @Transactional
    public int batchDeleteAnswers(List<Long> answerIds, Long examId) {
        int deletedCount = 0;
        for (Long answerId : answerIds) {
            try {
                StudentAnswer answer = studentAnswerRepository.findById(answerId).orElse(null);
                if (answer != null) {
                    // 验证答案属于指定的考试
                    if (examId == null || (answer.getQuestion() != null 
                            && answer.getQuestion().getExam() != null 
                            && examId.equals(answer.getQuestion().getExam().getId()))) {
                        studentAnswerRepository.deleteById(answerId);
                        deletedCount++;
                        log.info("成功删除答案 ID: {}", answerId);
                    } else {
                        log.warn("答案 {} 不属于考试 {}, 跳过删除", answerId, examId);
                    }
                } else {
                    log.warn("答案 {} 不存在, 跳过删除", answerId);
                }
            } catch (Exception e) {
                log.error("删除答案 {} 失败: {}", answerId, e.getMessage());
            }
        }
        log.info("批量删除答案完成，删除数量: {}/{}", deletedCount, answerIds.size());
        return deletedCount;
    }
    
    @Transactional(readOnly = true)
    public StudentAnswer getAnswerById(Long id) {
        return studentAnswerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student answer not found with id: " + id));
    }
    
    /**
     * 通用保存方法，供控制层在进行部分字段更新后直接调用
     */
    @Transactional
    public StudentAnswer save(StudentAnswer answer) {
        return studentAnswerRepository.save(answer);
    }
    
    @Transactional(readOnly = true)
    public Page<StudentAnswer> getAllAnswers(Pageable pageable) {
        return studentAnswerRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByQuestionId(Long questionId) {
        List<StudentAnswer> answers = studentAnswerRepository.findByQuestionId(questionId);
        // 获取答案ID列表并使用FETCH JOIN查询来加载关联数据
        if (!answers.isEmpty()) {
            List<Long> answerIds = answers.stream().map(StudentAnswer::getId).toList();
            return studentAnswerRepository.findByIdInWithFetch(answerIds);
        }
        return answers;
    }
    
    @Transactional(readOnly = true)
    public List<StudentAnswer> getAnswersByStudentId(Long studentId) {
        return studentAnswerRepository.findByStudentId(studentId);
    }
    
    @Transactional(readOnly = true)
    public StudentAnswer findByStudentIdAndQuestionId(Long studentId, Long questionId) {
        return studentAnswerRepository.findByStudentIdAndQuestionId(studentId, questionId);
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
    @Transactional
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

    @Transactional(readOnly = true)
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
            headerRow.createCell(5).setCellValue("题目类型");
            headerRow.createCell(6).setCellValue("题目内容");
            headerRow.createCell(7).setCellValue("选项A");
            headerRow.createCell(8).setCellValue("选项B");
            headerRow.createCell(9).setCellValue("选项C");
            headerRow.createCell(10).setCellValue("选项D");
            headerRow.createCell(11).setCellValue("选项E");
            headerRow.createCell(12).setCellValue("选项F");
            headerRow.createCell(13).setCellValue("正确答案");
            headerRow.createCell(14).setCellValue("学生答案");
            headerRow.createCell(15).setCellValue("分数");
            headerRow.createCell(16).setCellValue("反馈");
            headerRow.createCell(17).setCellValue("是否已评估");
            headerRow.createCell(18).setCellValue("提交时间");
            headerRow.createCell(19).setCellValue("评估时间");
            
            // 填充数据
            for (int i = 0; i < answers.size(); i++) {
                StudentAnswer answer = answers.get(i);
                Row dataRow = sheet.createRow(i + 1);
                
                User student = answer.getStudent();
                Question question = answer.getQuestion();
                
                dataRow.createCell(0).setCellValue(answer.getId());
                dataRow.createCell(1).setCellValue(student.getStudentNumber() != null ? student.getStudentNumber() : String.valueOf(student.getId()));
                dataRow.createCell(2).setCellValue(student.getRealName() != null ? student.getRealName() : student.getUsername());
                dataRow.createCell(3).setCellValue(student.getEmail());
                dataRow.createCell(4).setCellValue(question.getTitle());
                dataRow.createCell(5).setCellValue(getQuestionTypeDisplayName(question.getQuestionType()));
                dataRow.createCell(6).setCellValue(question.getContent());
                
                // 获取题目的选项（如果是客观题）
                List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOptionOrder(question.getId());
                
                // 填充选项内容（最多6个选项A-F）
                for (int j = 0; j < 6; j++) {
                    if (j < options.size()) {
                        dataRow.createCell(7 + j).setCellValue(options.get(j).getContent());
                    } else {
                        dataRow.createCell(7 + j).setCellValue("");
                    }
                }
                
                // 获取正确答案
                String correctAnswer = "";
                if (isObjectiveQuestion(question.getQuestionType()) && !options.isEmpty()) {
                    List<String> correctOptions = new ArrayList<>();
                    for (int j = 0; j < options.size(); j++) {
                        if (options.get(j).getIsCorrect()) {
                            correctOptions.add(String.valueOf((char)('A' + j)));
                        }
                    }
                    correctAnswer = String.join(",", correctOptions);
                } else if (question.getReferenceAnswer() != null) {
                    correctAnswer = question.getReferenceAnswer();
                }
                
                dataRow.createCell(13).setCellValue(correctAnswer);
                dataRow.createCell(14).setCellValue(answer.getAnswerText());
                dataRow.createCell(15).setCellValue(answer.getScore() != null ? answer.getScore().doubleValue() : 0.0);
                dataRow.createCell(16).setCellValue(answer.getFeedback() != null ? answer.getFeedback() : "");
                dataRow.createCell(17).setCellValue(answer.isEvaluated() ? "是" : "否");
                dataRow.createCell(18).setCellValue(answer.getCreatedAt() != null ? answer.getCreatedAt().toString() : "");
                dataRow.createCell(19).setCellValue(answer.getEvaluatedAt() != null ? answer.getEvaluatedAt().toString() : "");
            }
            
            // 写入字节数组
            workbook.write(outputStream);
        }
        
        return new ByteArrayResource(outputStream.toByteArray());
    }
    
    /**
     * 判断是否为客观题
     */
    private boolean isObjectiveQuestion(QuestionType questionType) {
        return questionType == QuestionType.SINGLE_CHOICE || 
               questionType == QuestionType.MULTIPLE_CHOICE || 
               questionType == QuestionType.TRUE_FALSE;
    }
    
    /**
     * 获取题目类型的显示名称
     */
    private String getQuestionTypeDisplayName(QuestionType questionType) {
        switch (questionType) {
            case SINGLE_CHOICE:
                return "单选题";
            case MULTIPLE_CHOICE:
                return "多选题";
            case TRUE_FALSE:
                return "判断题";
            case SHORT_ANSWER:
                return "简答题";
            case ESSAY:
                return "论述题";
            case CODING:
                return "编程题";
            case CASE_ANALYSIS:
                return "案例分析题";
            case CALCULATION:
                return "计算题";
            default:
                return "其他";
        }
    }
    
    /**
     * 获取题目的选项信息和正确答案（格式化后的字符串）
     */
    private String getFormattedQuestionOptionsAndAnswer(Long questionId) {
        try {
            // 获取题目信息
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null || !isObjectiveQuestion(question.getQuestionType())) {
                return null;
            }
            
            // 获取选项列表
            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOptionOrder(questionId);
            if (options.isEmpty()) {
                return null;
            }
            
            StringBuilder result = new StringBuilder();
            List<String> correctOptions = new ArrayList<>();
            
            // 格式化选项
            for (int i = 0; i < options.size(); i++) {
                QuestionOption option = options.get(i);
                char optionLetter = (char) ('A' + i);
                result.append(optionLetter).append(". ").append(option.getContent()).append("\n");
                
                if (option.getIsCorrect()) {
                    correctOptions.add(String.valueOf(optionLetter));
                }
            }
            
            // 添加正确答案
            if (!correctOptions.isEmpty()) {
                result.append("正确答案: ").append(String.join(",", correctOptions));
            }
            
            return result.toString().trim();
        } catch (Exception e) {
            log.debug("获取题目选项信息失败: {}", e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
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

    // 导入普通Excel/CSV格式的答案文件
    @Transactional
    public int importAnswersFromFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名为空");
        }
        
        int importedCount = 0;
        
        try {
            if (filename.toLowerCase().endsWith(".csv")) {
                importedCount = importCSVAnswers(file);
            } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
                importedCount = importExcelAnswers(file);
            } else {
                throw new IllegalArgumentException("不支持的文件格式，仅支持 CSV 和 Excel 文件");
            }
        } catch (Exception e) {
            log.error("导入答案文件失败: {}", e.getMessage(), e);
            throw new IOException("导入失败: " + e.getMessage(), e);
        }
        
        return importedCount;
    }

    // 导入答案到指定考试
    @Transactional
    public int importAnswersToExam(Long examId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        
        // 验证考试是否存在
        Optional<Exam> examOpt = examRepository.findById(examId);
        if (examOpt.isEmpty()) {
            throw new IllegalArgumentException("考试不存在: " + examId);
        }
        
        return importAnswersFromFile(file);
    }

    // 批量导入学习通答案（不指定考试ID）
    public ImportResult importLearningAnswers(String subject, List<String> classFolders) throws IOException {
        return importLearningAnswers(subject, classFolders, null);
    }

    // 批量导入学习通答案（指定考试ID）
    public ImportResult importLearningAnswers(String subject, List<String> classFolders, Long examId) throws IOException {
        log.info("🚀 开始批量导入学习通答案 - 科目: {}, 班级数: {}, 考试ID: {}", subject, classFolders.size(), examId);
        
        ImportResult result = new ImportResult();
        int totalFiles = 0;
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        
        for (String classFolder : classFolders) {
            log.info("📁 开始处理班级: {}", classFolder);
            
            try {
                List<File> documents = learningAnswerParserService.getAnswerDocuments(uploadDir, subject, classFolder);
                totalFiles += documents.size();
                
                log.info("📄 班级 {} 共有 {} 个文档", classFolder, documents.size());
                
                int classSuccessCount = 0;
                int classFailedCount = 0;
                int classSkippedCount = 0;
                
                for (File document : documents) {
                    try {
                        log.info("📖 开始处理文档: {}", document.getName());
                        
                        // 解析文档
                        StudentAnswerImportData importData = learningAnswerParserService.parseLearningAnswerDocument(document);
                        
                        if (importData == null) {
                            log.warn("⚠️ 跳过无法解析的文档: {}", document.getName());
                            classSkippedCount++;
                            continue;
                        }
                        
                        if (importData.getQuestions() == null || importData.getQuestions().isEmpty()) {
                            log.warn("⚠️ 跳过没有题目的文档: {}", document.getName());
                            classSkippedCount++;
                            continue;
                        }
                        
                        // 记录文档基本信息
                        log.info("📋 文档信息 - 学生: [{}], 学号: [{}], 题目数: {}", 
                                importData.getStudentName(), importData.getStudentNumber(), 
                                importData.getQuestions().size());
                        
                        // 处理单个学生的答案 - 使用容错机制
                        int importedAnswers = processSingleStudentAnswersWithRetry(importData, examId, document.getName());
                        
                        if (importedAnswers > 0) {
                            classSuccessCount++;
                            log.info("✅ 文档处理成功: {} - 导入 {} 个答案", document.getName(), importedAnswers);
                        } else {
                            classFailedCount++;
                            log.warn("❌ 文档处理失败: {} - 未导入任何答案", document.getName());
                        }
                        
                    } catch (Exception e) {
                        classFailedCount++;
                        log.error("❌ 处理文档异常: {} - {}", document.getName(), e.getMessage());
                        
                        // 记录详细错误信息但不中断处理
                        if (log.isDebugEnabled()) {
                            log.debug("文档处理详细错误:", e);
                        }
                    }
                }
                
                successCount += classSuccessCount;
                failedCount += classFailedCount;
                skippedCount += classSkippedCount;
                
                log.info("📊 班级 {} 处理完成 - 成功: {}, 失败: {}, 跳过: {}", 
                        classFolder, classSuccessCount, classFailedCount, classSkippedCount);
                
            } catch (Exception e) {
                log.error("❌ 处理班级异常: {} - {}", classFolder, e.getMessage(), e);
                // 班级级别的异常，继续处理下一个班级
            }
        }
        
        result.setTotalFiles(totalFiles);
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setSkippedCount(skippedCount);
        
        log.info("🎯 批量导入完成 - 总文件: {}, 成功: {}, 失败: {}, 跳过: {}", 
                totalFiles, successCount, failedCount, skippedCount);
        
        return result;
    }
    
    /**
     * 带重试机制的单个学生答案处理
     */
    private int processSingleStudentAnswersWithRetry(StudentAnswerImportData importData, Long examId, String fileName) {
        int maxRetries = 2;
        int retryCount = 0;
        
        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    log.info("🔄 第 {} 次重试处理学生: {} (文档: {})", retryCount, importData.getStudentName(), fileName);
                    
                    // 重试前稍微等待，避免数据库锁定问题
                    Thread.sleep(100 * retryCount);
                }
                
                return processSingleStudentAnswers(importData, examId);
                
            } catch (Exception e) {
                retryCount++;
                
                if (retryCount <= maxRetries) {
                    log.warn("⚠️ 处理学生失败，准备重试 ({}/{}) - 学生: {}, 错误: {}", 
                            retryCount, maxRetries, importData.getStudentName(), e.getMessage());
                } else {
                    log.error("❌ 处理学生最终失败 - 学生: {}, 文档: {}, 错误: {}", 
                             importData.getStudentName(), fileName, e.getMessage());
                    
                    // 记录失败的学生信息，便于后续排查
                    log.error("🔍 失败学生详细信息 - 姓名: [{}], 学号: [{}], 班级: [{}], 学院: [{}]",
                             importData.getStudentName(), importData.getStudentNumber(), 
                             importData.getClassName(), importData.getCollege());
                    
                    return 0; // 最终失败，返回0
                }
            }
        }
        
        return 0;
    }

    // 处理单个学生的答案数据
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected int processSingleStudentAnswers(StudentAnswerImportData importData) {
        return processSingleStudentAnswers(importData, null);
    }

    // 处理单个学生的答案数据（支持指定考试ID）
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected int processSingleStudentAnswers(StudentAnswerImportData importData, Long examId) {
        try {
            // 查找或创建学生用户
            User student;
            try {
                student = findOrCreateStudent(importData);
                log.debug("✅ 找到/创建学生用户: {} (ID: {})", student.getRealName(), student.getId());
            } catch (Exception e) {
                log.error("❌ 创建/查找学生用户异常: {} - {}", importData.getStudentName(), e.getMessage());
                return 0; // 学生用户失败，直接返回，不影响其他学生
            }
            
            int importedCount = 0;
            List<StudentAnswerImportData.QuestionAnswer> questions = importData.getQuestions();
            
            // 🔥 关键修复：建立题目编号映射，确保编号不错位
            Map<Integer, Question> questionMapping = new HashMap<>();
            Map<Integer, String> skippedReasons = new HashMap<>();
            QuestionBank defaultBank = findOrCreateDefaultQuestionBank(importData);
            
            // 第一遍：建立完整的题目映射，保持编号连续性
            log.info("🔍 开始建立题目映射，总题目数: {}", questions.size());
            for (int i = 0; i < questions.size(); i++) {
                StudentAnswerImportData.QuestionAnswer qa = questions.get(i);
                int questionNumber = i + 1; // 基于位置的编号（1开始）
                
                try {
                    // 验证题目数据
                    if (qa == null || qa.getQuestionContent() == null || qa.getQuestionContent().trim().isEmpty()) {
                        log.warn("📍 题目 {} 数据无效，保留位置但标记为跳过", questionNumber);
                        questionMapping.put(questionNumber, null);
                        skippedReasons.put(questionNumber, "题目数据为空或无效");
                        continue;
                    }
                    
                    // 使用智能匹配服务根据题目内容匹配或创建题目
                    Question question = smartQuestionMatchingService.smartMatchQuestion(qa, examId, defaultBank);
                    
                    // 严格验证题目对象
                    if (question == null) {
                        log.warn("📍 题目 {} 智能匹配失败，尝试创建占位符题目: {}", 
                                questionNumber, qa.getQuestionContent().substring(0, Math.min(50, qa.getQuestionContent().length())));
                        // 创建占位符题目而不是直接跳过
                        question = createPlaceholderQuestion(qa, examId, defaultBank);
                    }
                    
                    if (question != null && question.getId() == null) {
                        log.error("📍 题目 {} ID为null，尝试重新保存: {}", questionNumber, question.getTitle());
                        try {
                            question = questionRepository.save(question);
                            if (question.getId() == null) {
                                log.error("📍 题目 {} 重新保存失败，标记为跳过: {}", questionNumber, question.getTitle());
                                questionMapping.put(questionNumber, null);
                                skippedReasons.put(questionNumber, "题目保存失败，ID为null");
                                continue;
                            }
                        } catch (Exception e) {
                            log.error("📍 题目 {} 重新保存异常: {}", questionNumber, e.getMessage());
                            questionMapping.put(questionNumber, null);
                            skippedReasons.put(questionNumber, "题目保存异常: " + e.getMessage());
                            continue;
                        }
                    }
                    
                    // 如果智能匹配返回的题目需要设置考试关联
                    if (examId != null && question != null && 
                        (question.getExam() == null || !Objects.equals(question.getExam().getId(), examId))) {
                        Optional<Exam> exam = examRepository.findById(examId);
                        if (exam.isPresent()) {
                            question.setExam(exam.get());
                            question = questionRepository.save(question);
                            log.debug("📍 题目 {} 更新考试关联: {} -> 考试{}", questionNumber, question.getTitle(), examId);
                        }
                    }
                    
                    questionMapping.put(questionNumber, question);
                    log.debug("✅ 题目 {} 映射成功: {} (ID: {})", questionNumber, 
                             question != null ? question.getTitle() : "null", 
                             question != null ? question.getId() : "null");
                    
                } catch (Exception e) {
                    log.error("📍 题目 {} 处理异常: {}", questionNumber, e.getMessage());
                    questionMapping.put(questionNumber, null);
                    skippedReasons.put(questionNumber, "处理异常: " + e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("详细错误信息:", e);
                    }
                }
            }
            
            // 记录映射结果统计
            long validMappings = questionMapping.values().stream().filter(Objects::nonNull).count();
            long skippedMappings = questionMapping.size() - validMappings;
            log.info("📊 题目映射完成 - 总数: {}, 有效: {}, 跳过: {}", 
                    questionMapping.size(), validMappings, skippedMappings);
            
            // 第二遍：基于映射创建答案记录，保持编号对应关系
            log.info("📝 开始创建答案记录");
            for (int i = 0; i < questions.size(); i++) {
                StudentAnswerImportData.QuestionAnswer qa = questions.get(i);
                int questionNumber = i + 1;
                Question question = questionMapping.get(questionNumber);
                
                try {
                    if (question == null) {
                        log.debug("📍 题目 {} 已跳过，原因: {}", questionNumber, 
                                skippedReasons.getOrDefault(questionNumber, "未知原因"));
                        continue;
                    }
                    
                    if (question.getId() == null) {
                        log.error("📍 题目 {} ID仍为null，跳过答案创建", questionNumber);
                        continue;
                    }
                    
                    // 检查是否已存在该学生对该题目的答案
                    StudentAnswer existingAnswer = studentAnswerRepository
                        .findByStudentIdAndQuestionId(student.getId(), question.getId());
                    
                    // 处理答案内容 - 允许空答案但记录日志
                    String answerContent = qa.getAnswerContent();
                    if (answerContent == null || answerContent.trim().isEmpty()) {
                        answerContent = ""; // 设置为空字符串而不是null
                        log.debug("📍 题目 {} 的答案为空，设置为空字符串", questionNumber);
                    }
                    
                    if (existingAnswer != null) {
                        // 如果已存在，更新答案
                        existingAnswer.setAnswerText(answerContent);
                        
                        // 严格验证更新前的实体状态
                        if (existingAnswer.getId() == null) {
                            log.error("📍 题目 {} 现有答案ID为null，跳过更新: 学生{}, 题目{}", 
                                    questionNumber, student.getRealName(), question.getTitle());
                            continue;
                        }
                        
                        studentAnswerRepository.save(existingAnswer);
                        log.debug("✅ 题目 {} 更新学生 {} 的答案", questionNumber, student.getRealName());
                    } else {
                        // 创建新的答案记录 - 加强验证
                        if (student.getId() == null || question.getId() == null) {
                            log.error("📍 题目 {} 关联实体ID为null，无法创建答案: 学生ID={}, 题目ID={}", 
                                     questionNumber, student.getId(), question.getId());
                            continue;
                        }
                        
                        StudentAnswer answer = new StudentAnswer();
                        answer.setStudent(student);
                        answer.setQuestion(question);
                        answer.setAnswerText(answerContent);
                        answer.setEvaluated(false);
                        
                        // 验证实体状态
                        log.debug("📍 题目 {} 创建答案实体: 学生ID={}, 题目ID={}, 答案长度={}", 
                                 questionNumber, student.getId(), question.getId(), answerContent.length());
                        
                        try {
                            StudentAnswer savedAnswer = studentAnswerRepository.save(answer);
                            
                            // 验证保存结果
                            if (savedAnswer.getId() != null) {
                                log.debug("✅ 题目 {} 创建学生 {} 的答案 (ID: {})", 
                                         questionNumber, student.getRealName(), savedAnswer.getId());
                            } else {
                                log.error("❌ 题目 {} 答案保存后ID仍为null: 学生{}, 题目{}", 
                                         questionNumber, student.getRealName(), question.getTitle());
                                continue;
                            }
                        } catch (Exception e) {
                            log.error("❌ 题目 {} 保存答案失败: 学生{}, 题目{}, 错误: {}", 
                                     questionNumber, student.getRealName(), question.getTitle(), e.getMessage());
                            continue;
                        }
                    }
                    
                    importedCount++;
                    
                } catch (Exception e) {
                    log.error("📍 题目 {} 保存学生 {} 的答案失败: {}", questionNumber, student.getRealName(), e.getMessage());
                    // 继续处理下一题，不要中断整个导入过程
                    if (log.isDebugEnabled()) {
                        log.debug("详细错误信息:", e);
                    }
                }
            }
            
            // 输出最终统计信息
            log.info("✅ 学生 {} 答案处理完成 - 总题目: {}, 成功导入: {}, 跳过: {}", 
                    student.getRealName(), questions.size(), importedCount, questions.size() - importedCount);
            
            // 输出跳过题目的详细信息（用于调试）
            if (!skippedReasons.isEmpty()) {
                log.info("📋 跳过题目详情:");
                skippedReasons.forEach((num, reason) -> 
                    log.info("  题目 {}: {}", num, reason));
            }
            
            return importedCount;
            
        } catch (Exception e) {
            log.error("❌ 处理学生 {} 的答案时发生异常: {}", importData.getStudentName(), e.getMessage(), e);
            // 不再重新抛出异常，避免影响其他学生的处理
            return 0;
        }
    }
    
    /**
     * 创建占位符题目，用于处理无法匹配的题目，避免编号错位
     */
    private Question createPlaceholderQuestion(StudentAnswerImportData.QuestionAnswer qa, Long examId, QuestionBank questionBank) {
        try {
            Question placeholderQuestion = new Question();
            
            // 设置题目标题，标明是占位符
            String originalContent = qa.getQuestionContent();
            String title = "[占位符] " + (originalContent.length() > 40 
                ? originalContent.substring(0, 37) + "..." 
                : originalContent);
            placeholderQuestion.setTitle(title);
            
            // 设置题目内容
            placeholderQuestion.setContent(originalContent);
            
            // 设置题目类型为简答题
            placeholderQuestion.setQuestionType(QuestionType.SHORT_ANSWER);
            
            // 设置默认分值
            placeholderQuestion.setMaxScore(BigDecimal.valueOf(5));
            
            // 设置创建者为系统用户（ID为1）
            placeholderQuestion.setCreatedBy(1L);
            
            // 设置题目来源，标明是占位符
            placeholderQuestion.setSourceType(SourceType.LEARNING_IMPORT);
            
            // 设置为未确认状态，需要教师确认
            placeholderQuestion.setIsConfirmed(false);
            
            // 设置题库
            if (questionBank != null) {
                placeholderQuestion.setQuestionBank(questionBank);
            }
            
            // 如果指定了考试ID，设置考试关联
            if (examId != null) {
                Optional<Exam> exam = examRepository.findById(examId);
                if (exam.isPresent()) {
                    placeholderQuestion.setExam(exam.get());
                }
            }
            
            // 保存占位符题目
            Question savedQuestion = questionRepository.save(placeholderQuestion);
            log.info("✅ 创建占位符题目: {} (ID: {})", savedQuestion.getTitle(), savedQuestion.getId());
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("❌ 创建占位符题目失败: {}", e.getMessage());
            return null;
        }
    }
    
    // 查找或创建题目
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Question findOrCreateQuestion(StudentAnswerImportData.QuestionAnswer qa, StudentAnswerImportData importData) {
        String questionContent = qa.getQuestionContent();
        if (questionContent == null || questionContent.trim().isEmpty()) {
            return null;
        }
        
        // 清理题目内容
        questionContent = questionContent.trim();
        
        // 尝试通过标题模糊匹配现有题目
        String searchKeyword = questionContent.length() > 30 
            ? questionContent.substring(0, 30) 
            : questionContent;
            
        try {
            // 使用现有的搜索方法
            Page<Question> existingQuestions = questionRepository.searchQuestionsWithFilters(
                searchKeyword, null, null, null, null, null, 
                PageRequest.of(0, 10));
            
            for (Question existingQuestion : existingQuestions.getContent()) {
                // 计算相似度（简单的包含关系检查）
                if (existingQuestion.getContent().contains(searchKeyword) ||
                    questionContent.contains(existingQuestion.getContent().substring(0, Math.min(30, existingQuestion.getContent().length())))) {
                    log.debug("找到匹配的现有题目: {} (ID: {})", existingQuestion.getTitle(), existingQuestion.getId());
                    return existingQuestion;
                }
            }
        } catch (Exception e) {
            log.warn("搜索现有题目时出错: {}", e.getMessage());
        }
        
        // 如果没有找到匹配的题目，创建新题目
        return createNewQuestion(qa, importData);
    }
    
    // 创建新题目
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Question createNewQuestion(StudentAnswerImportData.QuestionAnswer qa, StudentAnswerImportData importData) {
        try {
            Question newQuestion = new Question();
            
            // 设置题目标题（从内容中提取前50个字符）
            String title = qa.getQuestionContent().length() > 50 
                ? qa.getQuestionContent().substring(0, 47) + "..." 
                : qa.getQuestionContent();
            newQuestion.setTitle(title);
            
            // 设置题目内容
            newQuestion.setContent(qa.getQuestionContent());
            
            // 设置题目类型为简答题
            newQuestion.setQuestionType(QuestionType.SHORT_ANSWER);
            
            // 设置默认分值
            newQuestion.setMaxScore(BigDecimal.valueOf(10));
            
            // 设置创建者为系统用户（ID为1，通常是admin）
            newQuestion.setCreatedBy(1L);
            
            // 设置题目来源
            newQuestion.setSourceType(SourceType.LEARNING_IMPORT);
            
            // 设置为未确认状态，需要教师确认
            newQuestion.setIsConfirmed(false);
            
            // 不关联特定考试，设置为题库题目
            newQuestion.setExam(null);
            
            // 为导入的题目设置默认题库
            QuestionBank defaultBank = findOrCreateDefaultQuestionBank(importData);
            newQuestion.setQuestionBank(defaultBank);
            
            // 保存题目并刷新，确保获得ID
            Question savedQuestion = questionRepository.save(newQuestion);
            questionRepository.flush(); // 强制刷新到数据库
            
            // 验证保存是否成功
            if (savedQuestion.getId() == null) {
                log.error("题目保存失败，未获得ID: {}", savedQuestion.getTitle());
                return null;
            }
            
            log.info("创建新题目: {} (ID: {}, 来源: {}, 题库: {})", 
                savedQuestion.getTitle(), savedQuestion.getId(), 
                importData.getExamTitle(), defaultBank.getName());
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("创建题目失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 查找或创建默认题库
     */
    @Transactional
    protected QuestionBank findOrCreateDefaultQuestionBank(StudentAnswerImportData importData) {
        try {
            // 根据科目查找或创建题库
            String subject = importData.getSubject() != null ? importData.getSubject() : "通用";
            String bankName = "学习通导入-" + subject;
            
            // 查找现有题库
            Optional<QuestionBank> existingBank = questionBankRepository.findByNameAndCreatedBy(bankName, 1L);
            if (existingBank.isPresent()) {
                return existingBank.get();
            }
            
            // 创建新题库
            QuestionBank newBank = new QuestionBank();
            newBank.setName(bankName);
            newBank.setDescription("从学习通导入的" + subject + "科目题目");
            newBank.setSubject(subject);
            newBank.setCreatedBy(1L);
            newBank.setIsPublic(false); // 默认私有
            newBank.setIsActive(true);
            
            QuestionBank savedBank = questionBankRepository.save(newBank);
            log.info("创建新题库: {} (ID: {})", savedBank.getName(), savedBank.getId());
            
            return savedBank;
            
        } catch (Exception e) {
            log.error("创建默认题库失败: {}", e.getMessage());
            // 如果创建失败，返回一个最简单的题库
            return createFallbackQuestionBank();
        }
    }
    
    /**
     * 创建备用题库（如果默认题库创建失败）
     */
    @Transactional
    protected QuestionBank createFallbackQuestionBank() {
        QuestionBank fallbackBank = new QuestionBank();
        fallbackBank.setName("导入题库");
        fallbackBank.setDescription("题目导入时的默认题库");
        fallbackBank.setCreatedBy(1L);
        fallbackBank.setIsPublic(false);
        fallbackBank.setIsActive(true);
        
        return questionBankRepository.save(fallbackBank);
    }
    
    // 查找或创建学生用户
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected User findOrCreateStudent(StudentAnswerImportData importData) {
        // 参数验证
        if (importData == null || importData.getStudentName() == null || importData.getStudentName().trim().isEmpty()) {
            throw new IllegalArgumentException("学生信息不完整：姓名不能为空");
        }
        
        // 记录学生数据特征，帮助定位问题
        String studentName = importData.getStudentName().trim();
        String studentNumber = importData.getStudentNumber();
        String className = importData.getClassName();
        
        log.info("🔍 处理学生数据 - 姓名: [{}], 学号: [{}], 班级: [{}], 学院: [{}]", 
                studentName, studentNumber, className, importData.getCollege());
        
        // 检查数据特征
        if (studentName.length() > 50) {
            log.warn("⚠️ 学生姓名过长: {} 字符", studentName.length());
        }
        if (studentNumber != null && studentNumber.length() > 50) {
            log.warn("⚠️ 学号过长: {} 字符", studentNumber.length());
        }
        if (studentName.matches(".*[\\p{Cntrl}].*")) {
            log.warn("⚠️ 学生姓名包含控制字符");
        }
        if (studentNumber != null && studentNumber.matches(".*[\\p{Cntrl}].*")) {
            log.warn("⚠️ 学号包含控制字符");
        }
        
        try {
            // 首先尝试通过学号查找
            if (studentNumber != null && !studentNumber.trim().isEmpty()) {
                String cleanStudentNumber = studentNumber.trim();
                log.debug("🔎 通过学号查找用户: [{}]", cleanStudentNumber);
                
                Optional<User> existingUser = userRepository.findByStudentNumber(cleanStudentNumber);
                if (existingUser.isPresent()) {
                    User user = existingUser.get();
                    log.info("✅ 通过学号找到现有用户: {} (ID: {}) - 学号: [{}]", 
                            user.getRealName(), user.getId(), user.getStudentNumber());
                    return user;
                }
                log.debug("❌ 学号未找到现有用户: [{}]", cleanStudentNumber);
            }
            
            // 如果通过学号找不到，尝试通过姓名模糊匹配
            log.debug("🔎 通过姓名模糊查找用户: [{}]", studentName);
            List<User> usersByName = userRepository.findByRealNameContaining(studentName);
            log.debug("📋 姓名模糊查找结果: {} 个用户", usersByName.size());
            
            if (!usersByName.isEmpty()) {
                // 记录所有匹配的用户
                for (User u : usersByName) {
                    log.debug("  - 匹配用户: {} (ID: {}, 学号: {}, 角色: {})", 
                            u.getRealName(), u.getId(), u.getStudentNumber(), u.getRoles());
                }
                
                // 优先选择学生角色的用户
                Optional<User> studentUser = usersByName.stream()
                    .filter(User::isStudent)
                    .filter(u -> u.getRealName().equals(studentName)) // 精确匹配姓名
                    .findFirst();
                if (studentUser.isPresent()) {
                    User user = studentUser.get();
                    log.info("✅ 通过姓名找到现有学生用户: {} (ID: {}) - 学号: [{}]", 
                            user.getRealName(), user.getId(), user.getStudentNumber());
                    return user;
                }
            }
            
            // 如果找不到现有用户，创建新用户
            log.info("🆕 未找到现有用户，准备创建新学生: [{}]", studentName);
            return createNewStudent(importData);
            
        } catch (Exception e) {
            log.error("❌ 查找或创建学生用户时发生异常: {}", e.getMessage(), e);
            
            // 记录详细的异常信息和学生数据
            log.error("🔍 异常发生时的学生数据 - 姓名: [{}], 学号: [{}], 班级: [{}]", 
                     studentName, studentNumber, className);
            
            // 如果是Session异常，直接抛出，不尝试回退
            if (e.getMessage() != null && e.getMessage().contains("null id in")) {
                log.error("💥 检测到Hibernate Session异常，直接失败");
                throw new RuntimeException(String.format(
                    "Hibernate Session异常。学生姓名: [%s], 学号: [%s], 班级: [%s]", 
                    studentName, studentNumber, className), e);
            }
            
            // 如果是创建用户时的异常，尝试使用更安全的回退方案
            if (e.getMessage() != null && e.getMessage().contains("无法创建学生用户")) {
                log.warn("🔄 创建用户失败，尝试回退方案查找现有用户");
                try {
                    // 使用只读事务进行回退查询，避免Session异常
                    User fallbackUser = findExistingUserSafely(importData);
                    if (fallbackUser != null) {
                        log.info("✅ 回退方案成功找到用户: {} (ID: {})", 
                                fallbackUser.getRealName(), fallbackUser.getId());
                        return fallbackUser;
                    }
                } catch (Exception fallbackException) {
                    log.debug("❌ 安全回退查找也失败: {}", fallbackException.getMessage());
                }
            }
            
            // 完全失败时，抛出包含详细信息的异常
            throw new RuntimeException(String.format(
                "无法查找或创建学生用户。学生姓名: [%s], 学号: [%s], 班级: [%s], 错误: %s", 
                studentName, studentNumber, className, e.getMessage()), e);
        }
    }

    // 创建新的学生用户
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected User createNewStudent(StudentAnswerImportData importData) {
        String studentName = importData.getStudentName();
        String studentNumber = importData.getStudentNumber();
        String className = importData.getClassName();
        
        log.info("🆕 开始创建新学生用户 - 姓名: [{}], 学号: [{}], 班级: [{}]", 
                studentName, studentNumber, className);
        
        try {
            User newStudent = new User();
            
            // 基本信息
            newStudent.setRealName(studentName);
            newStudent.setStudentNumber(studentNumber);
            
            // 生成唯一用户名（使用学号或姓名）
            String baseUsername = studentNumber != null ? studentNumber : studentName;
            log.debug("🔧 生成用户名基础: [{}]", baseUsername);
            
            String username = generateUniqueUsername(baseUsername);
            newStudent.setUsername(username);
            log.debug("✅ 生成唯一用户名: [{}]", username);
            
            // 生成唯一邮箱
            String email = generateUniqueEmail(baseUsername);
            newStudent.setEmail(email);
            log.debug("✅ 生成唯一邮箱: [{}]", email);
            
            // 设置默认密码（应该使用加密的默认密码）
            newStudent.setPassword("$2a$10$defaultPasswordHash");
            
            // 设置为未激活状态
            newStudent.setEnabled(false);
            
            // 设置学生角色
            newStudent.setRoles(Set.of(Role.STUDENT));
            
            // 设置扩展信息
            if (importData.getCollege() != null) {
                newStudent.setDepartment(importData.getCollege());
                log.debug("✅ 设置学院: [{}]", importData.getCollege());
            }
            if (importData.getMajor() != null) {
                newStudent.setMajor(importData.getMajor());
                log.debug("✅ 设置专业: [{}]", importData.getMajor());
            }
            if (className != null) {
                newStudent.setClassName(className);
                log.debug("✅ 设置班级: [{}]", className);
            }
            
            log.info("💾 准备保存用户到数据库...");
            
            // 保存用户 - 移除可能导致Session异常的flush操作
            User savedUser = userRepository.save(newStudent);
            
            // 验证保存结果 - 不使用flush，避免Session异常
            if (savedUser == null || savedUser.getId() == null) {
                log.error("❌ 用户保存失败，返回对象为null或ID为null");
                throw new RuntimeException("用户保存失败，ID为null");
            }
            
            log.info("✅ 成功创建新学生用户: [{}] ({}) - ID: {}, 用户名: [{}], 邮箱: [{}]", 
                    savedUser.getRealName(), savedUser.getStudentNumber(), savedUser.getId(),
                    savedUser.getUsername(), savedUser.getEmail());
            
            return savedUser;
            
        } catch (Exception e) {
            log.error("❌ 创建学生用户失败 - 姓名: [{}], 学号: [{}], 班级: [{}], 错误: {}", 
                     studentName, studentNumber, className, e.getMessage(), e);
            
            // 检查是否是数据库约束违反
            if (e.getMessage() != null) {
                if (e.getMessage().contains("username") || e.getMessage().contains("用户名")) {
                    log.error("🔍 可能的用户名冲突");
                }
                if (e.getMessage().contains("email") || e.getMessage().contains("邮箱")) {
                    log.error("🔍 可能的邮箱冲突");
                }
                if (e.getMessage().contains("student_number") || e.getMessage().contains("学号")) {
                    log.error("🔍 可能的学号冲突");
                }
            }
            
            // 在新事务中创建用户失败，直接抛出异常，不尝试回退
            throw new RuntimeException("无法创建学生用户: " + studentName + 
                                     ", 原因: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成唯一用户名
     */
    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 0;
        
        try {
            while (userRepository.existsByUsername(username)) {
                counter++;
                username = baseUsername + "_" + counter;
                
                // 防止无限循环
                if (counter > 1000) {
                    username = baseUsername + "_" + System.currentTimeMillis();
                    break;
                }
            }
        } catch (Exception e) {
            // 如果查询失败，使用时间戳确保唯一性
            log.warn("查询用户名唯一性失败，使用时间戳: {}", e.getMessage());
            username = baseUsername + "_" + System.currentTimeMillis();
        }
        
        return username;
    }
    
    /**
     * 生成唯一邮箱
     */
    private String generateUniqueEmail(String baseUsername) {
        String email = baseUsername + "@student.temp";
        int counter = 0;
        
        try {
            while (userRepository.existsByEmail(email)) {
                counter++;
                email = baseUsername + "_" + counter + "@student.temp";
                
                // 防止无限循环
                if (counter > 1000) {
                    email = baseUsername + "_" + System.currentTimeMillis() + "@student.temp";
                    break;
                }
            }
        } catch (Exception e) {
            // 如果查询失败，使用时间戳确保唯一性
            log.warn("查询邮箱唯一性失败，使用时间戳: {}", e.getMessage());
            email = baseUsername + "_" + System.currentTimeMillis() + "@student.temp";
        }
        
        return email;
    }
    
    /**
     * 安全的查找现有用户方法（只读事务）
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    protected User findExistingUserSafely(StudentAnswerImportData importData) {
        try {
            // 1. 尝试通过真实姓名查找
            if (importData.getStudentName() != null) {
                List<User> usersByName = userRepository.findByRealNameContaining(importData.getStudentName());
                if (!usersByName.isEmpty()) {
                    // 优先选择学生角色的用户
                    Optional<User> studentUser = usersByName.stream()
                        .filter(User::isStudent)
                        .findFirst();
                    if (studentUser.isPresent()) {
                        log.info("🔄 安全回退找到学生用户: {} (ID: {})", 
                                studentUser.get().getRealName(), studentUser.get().getId());
                        return studentUser.get();
                    }
                    log.info("🔄 安全回退找到普通用户: {} (ID: {})", 
                            usersByName.get(0).getRealName(), usersByName.get(0).getId());
                    return usersByName.get(0);
                }
            }
            
            // 2. 尝试通过模糊匹配学号查找
            if (importData.getStudentNumber() != null) {
                List<User> usersByStudentNumber = userRepository.findByStudentNumberContaining(importData.getStudentNumber());
                if (!usersByStudentNumber.isEmpty()) {
                    log.info("🔄 安全回退通过学号找到用户: {} (ID: {})", 
                            usersByStudentNumber.get(0).getRealName(), usersByStudentNumber.get(0).getId());
                    return usersByStudentNumber.get(0);
                }
            }
            
        } catch (Exception e) {
            log.debug("安全回退查找用户失败: {}", e.getMessage());
        }
        
        return null;
    }

    // 导入CSV格式答案
    private int importCSVAnswers(MultipartFile file) throws IOException {
        // TODO: 实现CSV导入逻辑
        log.warn("CSV导入功能尚未实现");
        return 0;
    }

    // 导入Excel格式答案
    private int importExcelAnswers(MultipartFile file) throws IOException {
        // TODO: 实现Excel导入逻辑
        log.warn("Excel导入功能尚未实现");
        return 0;
    }

    // 获取可用科目列表
    @Transactional(readOnly = true)
    public List<String> getAvailableSubjects() {
        return learningAnswerParserService.getAvailableSubjects(uploadDir);
    }

    // 获取科目下的班级列表
    @Transactional(readOnly = true)
    public List<String> getSubjectClasses(String subject) {
        return learningAnswerParserService.getClassFolders(uploadDir, subject);
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
    @Transactional(readOnly = true)
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
        headerRow.createCell(1).setCellValue("题目标题");
        headerRow.createCell(2).setCellValue("题目内容");
        headerRow.createCell(3).setCellValue("选项和答案");
        headerRow.createCell(4).setCellValue("学生答案");
        headerRow.createCell(5).setCellValue("得分");
        headerRow.createCell(6).setCellValue("满分");
        headerRow.createCell(7).setCellValue("评估反馈");
        
        // 答案数据
        for (StudentAnswerResponse answer : paper.getAnswers()) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(answer.getQuestionId());
            dataRow.createCell(1).setCellValue(answer.getQuestionTitle());
            dataRow.createCell(2).setCellValue(answer.getQuestionContent() != null ? answer.getQuestionContent() : "");
            
            // 获取选项信息（仅客观题）
            String optionsInfo = getFormattedQuestionOptionsAndAnswer(answer.getQuestionId());
            dataRow.createCell(3).setCellValue(optionsInfo != null ? optionsInfo : "");
            
            dataRow.createCell(4).setCellValue(answer.getAnswerText() != null ? answer.getAnswerText() : "");
            dataRow.createCell(5).setCellValue(answer.getScore() != null ? answer.getScore().toString() : "未评估");
            dataRow.createCell(6).setCellValue(answer.getMaxScore() != null ? answer.getMaxScore().toString() : "");
            dataRow.createCell(7).setCellValue(answer.getFeedback() != null ? answer.getFeedback() : "");
        }
        
        // 自动调整列宽
        for (int i = 0; i <= 7; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return new ByteArrayResource(out.toByteArray());
    }

    /**
     * 使用 Apache POI 将学生试卷导出为 Word (.docx) 文件
     */
    @Transactional(readOnly = true)
    public ByteArrayResource exportStudentPaperAsWord(StudentExamPaperResponse paper) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 1. 设置主标题
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText(paper.getExamTitle() + " - 学生试卷");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.addBreak();

            // 2. 写入学生信息
            XWPFParagraph studentInfo = document.createParagraph();
            studentInfo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun studentInfoRun = studentInfo.createRun();
            studentInfoRun.setText("学生: " + paper.getStudentName() + " (学号: " + paper.getStudentNumber() + ")");
            studentInfoRun.setFontSize(12);

            // 添加总分信息
            XWPFParagraph scoreInfo = document.createParagraph();
            scoreInfo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun scoreInfoRun = scoreInfo.createRun();
            scoreInfoRun.setText("总分: " + paper.getTotalScore() + " / " + paper.getMaxPossibleScore());
            scoreInfoRun.setFontSize(12);
            scoreInfoRun.setBold(true);
            scoreInfoRun.addBreak();
            scoreInfoRun.addBreak();

            // 3. 遍历并写入每个题目的详细信息
            int questionNumber = 1;
            for (StudentAnswerResponse answer : paper.getAnswers()) {
                // 题目标题
                XWPFParagraph qTitle = document.createParagraph();
                XWPFRun qTitleRun = qTitle.createRun();
                qTitleRun.setBold(true);
                qTitleRun.setFontSize(14);
                qTitleRun.setText(questionNumber + ". " + answer.getQuestionTitle() + " (" + answer.getMaxScore() + "分)");

                // 题目内容
                XWPFParagraph qContent = document.createParagraph();
                XWPFRun qContentRun = qContent.createRun();
                qContentRun.setText(answer.getQuestionContent());
                qContentRun.addBreak();
                
                // 选项信息（仅客观题）
                String optionsInfo = getFormattedQuestionOptionsAndAnswer(answer.getQuestionId());
                if (optionsInfo != null && !optionsInfo.isEmpty()) {
                    XWPFParagraph optionsHeader = document.createParagraph();
                    XWPFRun optionsHeaderRun = optionsHeader.createRun();
                    optionsHeaderRun.setBold(true);
                    optionsHeaderRun.setText("选项:");
                    
                    XWPFParagraph optionsParagraph = document.createParagraph();
                    XWPFRun optionsRun = optionsParagraph.createRun();
                    optionsRun.setText(optionsInfo);
                    optionsRun.setColor("666666"); // 灰色字体
                    optionsRun.addBreak();
                }
                
                // 学生答案
                XWPFParagraph userAnswerH = document.createParagraph();
                XWPFRun userAnswerHRun = userAnswerH.createRun();
                userAnswerHRun.setBold(true);
                userAnswerHRun.setText("学生答案: ");
                
                XWPFParagraph userAnswer = document.createParagraph();
                XWPFRun userAnswerRun = userAnswer.createRun();
                userAnswerRun.setText(answer.getAnswerText());
                userAnswerRun.setColor("0000FF"); // 蓝色字体
                userAnswerRun.addBreak();

                // 得分
                XWPFParagraph score = document.createParagraph();
                XWPFRun scoreRun = score.createRun();
                scoreRun.setBold(true);
                scoreRun.setText("得分: " + answer.getScore() + " / " + answer.getMaxScore());
                scoreRun.addBreak();

                // 批阅
                if (answer.getFeedback() != null && !answer.getFeedback().isEmpty()) {
                     XWPFParagraph feedbackH = document.createParagraph();
                     XWPFRun feedbackHRun = feedbackH.createRun();
                     feedbackHRun.setBold(true);
                     feedbackHRun.setText("批阅: ");

                     // 处理换行和加粗
                     String[] lines = answer.getFeedback().split("\n");
                     for (String line : lines) {
                         XWPFParagraph feedbackParagraph = document.createParagraph();
                         String[] parts = line.split("(?=\\【)|(?<=\\】)");
                         for (String part : parts) {
                             XWPFRun partRun = feedbackParagraph.createRun();
                             if (part.startsWith("【") && part.endsWith("】")) {
                                 partRun.setBold(true);
                                 partRun.setText(part.substring(1, part.length() - 1));
                             } else {
                                 partRun.setText(part);
                             }
                         }
                     }
                }
                
                document.createParagraph().createRun().addBreak(); // 添加间距
                questionNumber++;
            }

            document.write(out);
            return new ByteArrayResource(out.toByteArray());
        }
    }

    /**
     * 使用 iText7 将学生试卷导出为 PDF 文件
     */
    @Transactional(readOnly = true)
    public ByteArrayResource exportStudentPaperAsPdf(StudentExamPaperResponse paper) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        // 设置中文字体
        PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        document.setFont(font).setFontSize(11);
        // 加载Noto字体（仅用于特殊字符）
        PdfFont notoFont = PdfFontFactory.createFont("src/main/resources/fonts/NotoSansCJKsc-Regular.otf", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

        // 1. 设置主标题
        Paragraph title = new Paragraph(paper.getExamTitle() + " - 学生试卷")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // 2. 写入学生信息
        Paragraph studentInfo = new Paragraph("学生: " + paper.getStudentName() + " (学号: " + paper.getStudentNumber() + ")")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(studentInfo);
        document.add(new Paragraph("\n")); // 间距

        // 添加总分信息
        Paragraph scoreInfo = new Paragraph("总分: " + paper.getTotalScore() + " / " + paper.getMaxPossibleScore())
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(scoreInfo);

        document.add(new Paragraph("\n")); // 间距

        // 3. 遍历并写入每个题目的详细信息
        int questionNumber = 1;
        for (StudentAnswerResponse answer : paper.getAnswers()) {
            // 题目标题
            Paragraph questionTitle = new Paragraph("题目 " + questionNumber + ": " + answer.getQuestionTitle())
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(new DeviceRgb(0, 0, 0));
            document.add(questionTitle);

            // 题目内容
            if (answer.getQuestionContent() != null && !answer.getQuestionContent().trim().isEmpty()) {
                Paragraph questionContent = new Paragraph("题目内容: " + answer.getQuestionContent())
                        .setFontSize(11);
                document.add(questionContent);
            }
            
            // 选项信息（仅客观题）
            String optionsInfo = getFormattedQuestionOptionsAndAnswer(answer.getQuestionId());
            if (optionsInfo != null && !optionsInfo.isEmpty()) {
                Paragraph optionsTitle = new Paragraph("选项:")
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(new DeviceRgb(80, 80, 80)); // 深灰色
                document.add(optionsTitle);
                
                Paragraph options = new Paragraph(optionsInfo)
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(100, 100, 100)); // 灰色
                document.add(options);
            }

            // 学生答案（智能字体切换）
            String answerText = answer.getAnswerText() != null ? answer.getAnswerText() : "未回答";
            Paragraph studentAnswer = new Paragraph();
            studentAnswer.add(new Text("学生答案: ").setFont(font).setFontColor(new DeviceRgb(0, 100, 0)));
            for (int i = 0; i < answerText.length(); i++) {
                char c = answerText.charAt(i);
                String s = String.valueOf(c);
                if (font.containsGlyph(c)) {
                    studentAnswer.add(new Text(s).setFont(font).setFontColor(new DeviceRgb(0, 100, 0)));
                } else if (notoFont.containsGlyph(c)) {
                    studentAnswer.add(new Text(s).setFont(notoFont).setFontColor(new DeviceRgb(0, 100, 0)));
                } else {
                    studentAnswer.add(new Text("□").setFont(notoFont).setFontColor(new DeviceRgb(255, 0, 0)));
                }
            }
            studentAnswer.setFontSize(11);
            document.add(studentAnswer);

            // 分数和反馈
            if (answer.getScore() != null) {
                Paragraph score = new Paragraph("得分: " + answer.getScore() + " / " + (answer.getMaxScore() != null ? answer.getMaxScore() : "N/A"))
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(new DeviceRgb(200, 0, 0)); // 红色
                document.add(score);
            }

            if (answer.getFeedback() != null && !answer.getFeedback().trim().isEmpty()) {
                Paragraph feedback = new Paragraph("评价: " + answer.getFeedback())
                        .setFontSize(11)
                        .setFontColor(new DeviceRgb(0, 0, 200)); // 蓝色
                document.add(feedback);
            }

            // 每个题目之间添加间距
            document.add(new Paragraph("\n"));
            questionNumber++;
        }

        document.close();
        return new ByteArrayResource(out.toByteArray());
    }

    /**
     * 一键导出考试中所有学生的试卷为ZIP文件
     */
    @Transactional(readOnly = true)
    public ByteArrayResource exportAllStudentPapersAsZip(Long examId, String format) throws IOException {
        log.info("开始导出考试 {} 的所有学生试卷，格式: {}", examId, format);
        
        // 获取考试信息
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("考试不存在: " + examId));
        
        // 获取所有学生的试卷数据
        List<StudentExamPaperResponse> allPapers = getAllStudentPapers(examId);
        
        if (allPapers.isEmpty()) {
            log.warn("考试 {} 中没有找到任何学生试卷", examId);
            throw new RuntimeException("该考试中没有学生提交答案");
        }
        
        log.info("找到 {} 份学生试卷", allPapers.size());
        
        // 创建ZIP文件
        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
        
        try (java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(zipOut)) {
            
            String examTitle = exam.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileExtension = ("word".equalsIgnoreCase(format) || "docx".equalsIgnoreCase(format)) ? ".docx" : ".pdf";
            
            for (StudentExamPaperResponse paper : allPapers) {
                try {
                    log.debug("正在导出学生 {} 的试卷", paper.getStudentName());
                    
                    // 生成文件内容
                    ByteArrayResource paperResource;
                    
                    if ("word".equalsIgnoreCase(format) || "docx".equalsIgnoreCase(format)) {
                        paperResource = exportStudentPaperAsWord(paper);
                    } else {
                        // 默认PDF格式
                        paperResource = exportStudentPaperAsPdf(paper);
                    }
                    
                    // 生成文件名
                    String studentName = paper.getStudentName().replaceAll("[\\\\/:*?\"<>|]", "_");
                    String studentNumber = paper.getStudentNumber() != null ? 
                        paper.getStudentNumber().replaceAll("[\\\\/:*?\"<>|]", "_") : "无学号";
                    String fileName = String.format("%s_%s_试卷%s", studentNumber, studentName, fileExtension);
                    
                    // 添加到ZIP文件
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(fileName);
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.write(paperResource.getByteArray());
                    zipOutputStream.closeEntry();
                    
                    log.debug("成功添加学生 {} 的试卷到ZIP文件", paper.getStudentName());
                    
                } catch (Exception e) {
                    log.error("导出学生 {} 的试卷失败: {}", paper.getStudentName(), e.getMessage(), e);
                    // 继续处理其他学生的试卷，不因单个学生失败而停止整个流程
                }
            }
            
            // 添加一个说明文件
            String readmeContent = String.format(
                "考试名称: %s\n" +
                "导出时间: %s\n" +
                "导出格式: %s\n" +
                "学生数量: %d\n" +
                "\n" +
                "文件命名规则: 学号_姓名_试卷.%s\n" +
                "\n" +
                "注意事项:\n" +
                "1. 如果学生没有学号，将显示为'无学号'\n" +
                "2. 文件名中的特殊字符已被替换为下划线\n" +
                "3. 导出失败的学生试卷将不会包含在此ZIP文件中\n",
                exam.getTitle(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                format.toUpperCase(),
                allPapers.size(),
                fileExtension.substring(1)
            );
            
            java.util.zip.ZipEntry readmeEntry = new java.util.zip.ZipEntry("README.txt");
            zipOutputStream.putNextEntry(readmeEntry);
            zipOutputStream.write(readmeContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            
            zipOutputStream.finish();
        }
        
        log.info("成功导出考试 {} 的所有学生试卷为ZIP文件，大小: {} bytes", examId, zipOut.size());
        
        return new ByteArrayResource(zipOut.toByteArray());
    }
    
    /**
     * 获取考试中所有学生的试卷数据（不分页）
     */
    @Transactional(readOnly = true)
    public List<StudentExamPaperResponse> getAllStudentPapers(Long examId) {
        log.debug("获取考试 {} 的所有学生试卷", examId);
        
        // 查询所有提交了答案的学生
        List<Object[]> studentData = studentAnswerRepository.findAllStudentsByExamId(examId);
        
        List<StudentExamPaperResponse> papers = new ArrayList<>();
        
        for (Object[] row : studentData) {
            try {
                Long studentId = (Long) row[0];
                String studentName = (String) row[1];
                String studentNumber = (String) row[2];
                String studentEmail = (String) row[3];
                // row[4] 是 s.real_name，我们已经在 row[1] 中使用了 COALESCE
                
                // 获取该学生的所有答案
                List<StudentAnswer> answers = studentAnswerRepository.findByQuestionExamIdAndStudentId(examId, studentId);
                
                if (!answers.isEmpty()) {
                    // 获取考试信息用于构建试卷响应
                    Exam exam = examRepository.findById(examId).orElse(null);
                    String examTitle = exam != null ? exam.getTitle() : "未知考试";
                    // 获取所有题目
                    List<com.teachhelper.entity.Question> allQuestions = exam != null ? exam.getQuestions() : new java.util.ArrayList<>();
                    // 构建学生用户对象
                    User student = new User();
                    student.setId(studentId);
                    student.setRealName(studentName);
                    student.setStudentNumber(studentNumber);
                    student.setEmail(studentEmail);
                    StudentExamPaperResponse paper = new StudentExamPaperResponse(student, examId, examTitle, allQuestions, answers);
                    papers.add(paper);
                    
                    log.debug("添加学生 {} 的试卷，包含 {} 个答案", studentName, answers.size());
                }
            } catch (Exception e) {
                log.error("处理学生试卷数据失败: {}", e.getMessage(), e);
            }
        }
        
        log.info("获取到 {} 份学生试卷", papers.size());
        return papers;
    }
    
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
    
    /**
     * 删除学生某套试卷的所有答案
     */
    @Transactional
    public void deleteStudentExamAnswers(Long studentId, Long examId) {
        try {
            log.info("开始删除学生{}在考试{}中的答案", studentId, examId);
            
            // 1. 查找该学生在该考试中的所有答案
            List<StudentAnswer> answers = studentAnswerRepository.findByQuestionExamIdAndStudentId(examId, studentId);
            
            if (answers.isEmpty()) {
                log.warn("未找到学生{}在考试{}中的答案", studentId, examId);
                return;
            }
            
            log.info("找到{}条答案记录", answers.size());
            
            // 2. 删除相关的评分记录（如果有）
            for (StudentAnswer answer : answers) {
                // 删除评分记录
                if (answer.getScore() != null || answer.getFeedback() != null) {
                    // 这里可以添加删除评分相关记录的逻辑
                    log.debug("清理答案{}的评分记录", answer.getId());
                }
            }
            
            // 3. 批量删除答案记录
            List<Long> answerIds = answers.stream()
                .map(StudentAnswer::getId)
                .collect(Collectors.toList());
            
            for (Long answerId : answerIds) {
                studentAnswerRepository.deleteById(answerId);
            }
            
            // 4. 检查并删除考试提交记录（如果有）
            try {
                // 查找并删除考试提交记录
                var submissionOpt = examSubmissionRepository.findByExamIdAndStudentId(examId, studentId);
                if (submissionOpt.isPresent()) {
                    examSubmissionRepository.delete(submissionOpt.get());
                    log.debug("删除考试提交记录");
                }
            } catch (Exception e) {
                log.warn("删除考试提交记录失败: {}", e.getMessage());
            }
            
            log.info("成功删除学生{}在考试{}中的{}条答案", studentId, examId, answers.size());
            
        } catch (Exception e) {
            log.error("删除学生{}在考试{}中的答案失败: {}", studentId, examId, e.getMessage(), e);
            throw new RuntimeException("删除学生试卷答案失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导入学习通答案（支持进度回调）
     */
    @Transactional
    public ImportResult importLearningAnswers(StudentAnswerImportData importData, Long examId, 
                                              java.util.function.Consumer<Double> progressCallback) {
        log.info("开始导入学习通答案，学生: {}, 题目数: {}, 考试ID: {}", 
                 importData.getStudentInfo().getStudentName(), 
                 importData.getQuestions().size(),
                 examId);
        
        ImportResult result = new ImportResult();
        result.setSuccessCount(0);
        result.setFailedCount(0);
        result.setErrors(new ArrayList<>());
        
        try {
            // 清理分数解析上下文，开始处理新的学生答案文档
            smartQuestionMatchingService.clearScoreContext();
            log.debug("开始处理学生{}的答案，已清理分数解析上下文", importData.getStudentInfo().getStudentName());
            
            // 查找或创建学生用户
            User student = findOrCreateStudent(importData);
            
            List<StudentAnswerImportData.QuestionAnswer> questions = importData.getQuestions();
            int totalQuestions = questions.size();
            
            for (int i = 0; i < totalQuestions; i++) {
                StudentAnswerImportData.QuestionAnswer qa = questions.get(i);
                
                try {
                    // 使用智能匹配服务根据题目内容匹配或创建题目
                    QuestionBank defaultBank = findOrCreateDefaultQuestionBank(importData);
                    Question question = smartQuestionMatchingService.smartMatchQuestion(qa, examId, defaultBank);
                    
                    if (question != null && question.getId() != null) {
                        // 检查是否已存在该学生对该题目的答案
                        StudentAnswer existingAnswer = studentAnswerRepository
                            .findByStudentIdAndQuestionId(student.getId(), question.getId());
                        
                        if (existingAnswer != null) {
                            // 如果已存在，更新答案
                            existingAnswer.setAnswerText(qa.getAnswerContent());
                            studentAnswerRepository.save(existingAnswer);
                            log.debug("更新学生 {} 对题目 {} 的答案", student.getRealName(), question.getTitle());
                        } else {
                            // 创建新的答案记录
                            StudentAnswer answer = new StudentAnswer();
                            answer.setStudent(student);
                            answer.setQuestion(question);
                            answer.setAnswerText(qa.getAnswerContent());
                            answer.setEvaluated(false);
                            
                            studentAnswerRepository.save(answer);
                            log.debug("创建学生 {} 对题目 {} 的答案", student.getRealName(), question.getTitle());
                        }
                        
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    } else {
                        log.warn("无法匹配或创建题目: {}", qa.getQuestionContent());
                        result.setFailedCount(result.getFailedCount() + 1);
                        result.getErrors().add("题目 " + qa.getQuestionNumber() + " 无法匹配或创建");
                    }
                    
                    // 更新进度
                    if (progressCallback != null) {
                        double progress = (double) (i + 1) / totalQuestions;
                        progressCallback.accept(progress);
                    }
                    
                } catch (Exception e) {
                    log.error("处理题目 {} 时出错: {}", qa.getQuestionNumber(), e.getMessage());
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.getErrors().add("题目 " + qa.getQuestionNumber() + " 处理失败: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("处理学生 {} 的答案时发生异常: {}", importData.getStudentInfo().getStudentName(), e.getMessage(), e);
            throw e; // 重新抛出异常，让上层处理
        }
        
        log.info("学习通答案导入完成，成功: {}, 失败: {}", 
                 result.getSuccessCount(), result.getFailedCount());
        
        return result;
    }
    
    /**
     * 基于模板导入学习通答案
     * @param subject 科目
     * @param classFolder 班级文件夹
     * @param templateId 模板ID
     * @param examId 考试ID
     * @return 导入结果
     */
    public ImportResult importLearningAnswersWithTemplate(String subject, String classFolder, Long templateId, Long examId) throws IOException {
        log.info("🚀 开始基于模板导入学习通答案 - 科目: {}, 班级: {}, 模板ID: {}, 考试ID: {}", 
                 subject, classFolder, templateId, examId);
        
        ImportResult result = new ImportResult();
        result.setSuccessfulStudents(new ArrayList<>());
        result.setFailedStudents(new ArrayList<>());
        result.setErrorMessages(new ArrayList<>());
        
        try {
            // 1. 获取模板信息
            com.teachhelper.dto.response.ExamPaperTemplateResponse template = examPaperTemplateService.getTemplate(templateId);
            if (template == null) {
                throw new RuntimeException("模板不存在: " + templateId);
            }
            
            log.info("📋 使用模板: {} (ID: {}), 题目数: {}", template.getName(), templateId, template.getQuestionCount());
            
            // 2. 获取模板题目列表，建立题目顺序映射
            List<com.teachhelper.dto.response.ExamPaperTemplateQuestionResponse> templateQuestions = template.getQuestions();
            if (templateQuestions == null || templateQuestions.isEmpty()) {
                throw new RuntimeException("模板没有配置题目: " + templateId);
            }
            
            // 按照题目顺序排序
            templateQuestions.sort((q1, q2) -> Integer.compare(q1.getQuestionOrder(), q2.getQuestionOrder()));
            
            // 3. 获取班级下的所有学习通文档
            List<File> documents = learningAnswerParserService.getAnswerDocuments(uploadDir, subject, classFolder);
            log.info("📄 班级 {} 共有 {} 个学习通文档", classFolder, documents.size());
            
            if (documents.isEmpty()) {
                log.warn("⚠️ 班级 {} 没有找到学习通文档", classFolder);
                return result;
            }
            
            int successCount = 0;
            int failedCount = 0;
            
            // 4. 逐个处理学生文档
            for (File document : documents) {
                try {
                    log.info("📖 开始处理文档: {}", document.getName());
                    
                    // 使用基于模板的解析方法，只解析学生答案和分数，题目内容来自模板
                    StudentAnswerImportData importData = learningAnswerParserService.parseStudentAnswersOnlyForTemplate(
                        document, templateQuestions.size());
                    
                    if (importData == null) {
                        log.warn("⚠️ 跳过无法解析的文档: {}", document.getName());
                        failedCount++;
                        result.getFailedStudents().add(document.getName());
                        result.getErrorMessages().add("文档解析失败: " + document.getName());
                        continue;
                    }
                    
                    // 检查学生答案数据
                    List<StudentAnswerImportData.QuestionAnswer> studentAnswers = importData.getAnswers();
                    if (studentAnswers == null || studentAnswers.isEmpty()) {
                        log.warn("⚠️ 跳过没有学生答案的文档: {}", document.getName());
                        failedCount++;
                        result.getFailedStudents().add(document.getName());
                        result.getErrorMessages().add("文档没有学生答案: " + document.getName());
                        continue;
                    }
                    
                    log.info("📝 文档 {} 基于模板解析到 {} 个学生答案", document.getName(), studentAnswers.size());
                    
                    // 基于模板匹配和导入学生答案
                    boolean success = importStudentAnswersWithTemplate(importData, templateQuestions, examId);
                    
                    if (success) {
                        successCount++;
                        result.getSuccessfulStudents().add(importData.getStudentName());
                        log.info("✅ 文档处理成功: {} - 学生: {}", document.getName(), importData.getStudentName());
                    } else {
                        failedCount++;
                        result.getFailedStudents().add(importData.getStudentName());
                        result.getErrorMessages().add("答案导入失败: " + importData.getStudentName());
                        log.warn("❌ 文档处理失败: {} - 学生: {}", document.getName(), importData.getStudentName());
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    result.getFailedStudents().add(document.getName());
                    result.getErrorMessages().add("处理文档异常: " + document.getName() + " - " + e.getMessage());
                    log.error("❌ 处理文档异常: {} - {}", document.getName(), e.getMessage());
                }
            }
            
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalFiles(documents.size());
            
            log.info("🎯 基于模板导入完成 - 总文件: {}, 成功: {}, 失败: {}", 
                     documents.size(), successCount, failedCount);
            
        } catch (Exception e) {
            log.error("❌ 基于模板导入失败: {}", e.getMessage(), e);
            throw new IOException("基于模板导入失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 基于模板匹配和导入单个学生的答案
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean importStudentAnswersWithTemplate(
            StudentAnswerImportData importData, 
            List<com.teachhelper.dto.response.ExamPaperTemplateQuestionResponse> templateQuestions,
            Long examId) {
        
        try {
            // 1. 查找或创建学生用户
            User student = findOrCreateStudent(importData);
            
            List<StudentAnswerImportData.QuestionAnswer> studentAnswers = importData.getAnswers();
            int importedCount = 0;
            
            log.info("📋 开始匹配学生答案与模板题目 - 学生: {}, 学生答案数: {}, 模板题目数: {}", 
                     importData.getStudentName(), studentAnswers.size(), templateQuestions.size());
            
            // 调试：打印前几个学生答案的分数信息（基于模板导入时题目内容来自模板）
            for (int debugIndex = 0; debugIndex < Math.min(3, studentAnswers.size()); debugIndex++) {
                StudentAnswerImportData.QuestionAnswer debugAnswer = studentAnswers.get(debugIndex);
                log.info("🔍 调试 - 题目{}: 题目内容来自模板, 学生答案={}, 分数={}", 
                         debugIndex + 1,
                         debugAnswer.getAnswerContent() != null && debugAnswer.getAnswerContent().length() > 30 
                             ? debugAnswer.getAnswerContent().substring(0, 30) + "..."
                             : debugAnswer.getAnswerContent(),
                         debugAnswer.getScore());
            }
            
            // 2. 基于题目顺序进行匹配
            int maxQuestions = Math.min(studentAnswers.size(), templateQuestions.size());
            
            for (int i = 0; i < maxQuestions; i++) {
                try {
                    StudentAnswerImportData.QuestionAnswer studentAnswer = studentAnswers.get(i);
                    com.teachhelper.dto.response.ExamPaperTemplateQuestionResponse templateQuestion = templateQuestions.get(i);
                    
                    // 3. 获取模板题目对应的实际题目
                    Question question = getOrCreateQuestionFromTemplate(templateQuestion, examId, importData);
                    
                    if (question == null) {
                        log.warn("⚠️ 无法获取题目 {} 对应的Question对象", templateQuestion.getQuestionOrder());
                        continue;
                    }
                    
                    // 4. 检查是否已存在该学生对该题目的答案
                    StudentAnswer existingAnswer = studentAnswerRepository
                        .findByStudentIdAndQuestionId(student.getId(), question.getId());
                    
                    if (existingAnswer != null) {
                        // 更新现有答案
                        String answerContentToSet = studentAnswer.getAnswerContent();
                        log.info("🔍 题目{} 更新答案: 原答案=\"{}\", 新答案=\"{}\"", 
                                i + 1, 
                                existingAnswer.getAnswerText(),
                                answerContentToSet != null ? (answerContentToSet.length() > 50 ? answerContentToSet.substring(0, 50) + "..." : answerContentToSet) : "null");
                        
                        existingAnswer.setAnswerText(answerContentToSet);
                        
                        // 设置学生得分（如果解析到了分数）
                        if (studentAnswer.getScore() != null) {
                            existingAnswer.setScore(new java.math.BigDecimal(studentAnswer.getScore()));
                            // 只有客观题（选择题、判断题）才自动设置为已评估
                            boolean isObjectiveQuestion = isObjectiveQuestionType(question.getQuestionType());
                            existingAnswer.setEvaluated(isObjectiveQuestion);
                            log.info("🔄 更新学生 {} 第{}题的答案，答案内容: \"{}\", 得分: {}，题型: {}，自动评估: {}", 
                                     student.getRealName(), i + 1, 
                                     answerContentToSet != null ? (answerContentToSet.length() > 30 ? answerContentToSet.substring(0, 30) + "..." : answerContentToSet) : "null",
                                     studentAnswer.getScore(), question.getQuestionType(), isObjectiveQuestion);
                        } else {
                            existingAnswer.setEvaluated(false);
                            log.info("🔄 更新学生 {} 第{}题的答案，答案内容: \"{}\", 无分数信息", 
                                    student.getRealName(), i + 1,
                                    answerContentToSet != null ? (answerContentToSet.length() > 30 ? answerContentToSet.substring(0, 30) + "..." : answerContentToSet) : "null");
                        }
                        
                        studentAnswerRepository.save(existingAnswer);
                    } else {
                        // 创建新的答案记录
                        StudentAnswer answer = new StudentAnswer();
                        answer.setStudent(student);
                        answer.setQuestion(question);
                        
                        String answerContentToSet = studentAnswer.getAnswerContent();
                        log.info("🔍 题目{} 创建新答案: \"{}\"", 
                                i + 1, 
                                answerContentToSet != null ? (answerContentToSet.length() > 50 ? answerContentToSet.substring(0, 50) + "..." : answerContentToSet) : "null");
                        
                        answer.setAnswerText(answerContentToSet);
                        
                        // 设置学生得分（如果解析到了分数）
                        if (studentAnswer.getScore() != null) {
                            answer.setScore(new java.math.BigDecimal(studentAnswer.getScore()));
                            // 只有客观题（选择题、判断题）才自动设置为已评估
                            boolean isObjectiveQuestion = isObjectiveQuestionType(question.getQuestionType());
                            answer.setEvaluated(isObjectiveQuestion);
                            log.info("✅ 创建学生 {} 第{}题的答案，答案内容: \"{}\", 得分: {}，题型: {}，自动评估: {}", 
                                     student.getRealName(), i + 1, 
                                     answerContentToSet != null ? (answerContentToSet.length() > 30 ? answerContentToSet.substring(0, 30) + "..." : answerContentToSet) : "null",
                                     studentAnswer.getScore(), question.getQuestionType(), isObjectiveQuestion);
                        } else {
                            answer.setEvaluated(false);
                            log.info("✅ 创建学生 {} 第{}题的答案，答案内容: \"{}\", 无分数信息", 
                                    student.getRealName(), i + 1,
                                    answerContentToSet != null ? (answerContentToSet.length() > 30 ? answerContentToSet.substring(0, 30) + "..." : answerContentToSet) : "null");
                        }
                        
                        studentAnswerRepository.save(answer);
                    }
                    
                    importedCount++;
                    
                } catch (Exception e) {
                    log.error("❌ 处理第{}题时出错: {}", i + 1, e.getMessage());
                }
            }
            
            log.info("✅ 学生 {} 答案导入完成，共导入 {} 道题", importData.getStudentName(), importedCount);
            return importedCount > 0;
            
        } catch (Exception e) {
            log.error("❌ 导入学生 {} 答案失败: {}", importData.getStudentName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 根据模板题目获取实际的Question对象
     * 基于模板导入：正确处理模板配置中的题目内容和选项信息
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Question getOrCreateQuestionFromTemplate(
            com.teachhelper.dto.response.ExamPaperTemplateQuestionResponse templateQuestion,
            Long examId,
            StudentAnswerImportData importData) {
        
        try {
            // 检查考试是否存在
            Optional<Exam> examOpt = examRepository.findById(examId);
            if (examOpt.isEmpty()) {
                log.error("❌ 考试不存在: {}", examId);
                return null;
            }
            Exam exam = examOpt.get();
            
            // 首先尝试在考试中查找已有的对应顺序的题目
            List<Question> examQuestions = questionRepository.findByExamIdOrderByCreatedAt(examId);
            
            // 如果考试中已有对应顺序的题目，直接返回
            if (templateQuestion.getQuestionOrder() != null && 
                templateQuestion.getQuestionOrder() > 0 && 
                templateQuestion.getQuestionOrder() <= examQuestions.size()) {
                
                Question existingQuestion = examQuestions.get(templateQuestion.getQuestionOrder() - 1);
                log.debug("✅ 使用考试中已有的题目: ID={}, 顺序={}", 
                         existingQuestion.getId(), templateQuestion.getQuestionOrder());
                return existingQuestion;
            }
            
            // 如果考试中没有对应的题目，需要为考试创建新题目
            Question newQuestion = new Question();
            
            // 设置题目基本信息
            String questionTitle = "题目" + templateQuestion.getQuestionOrder();
            newQuestion.setTitle(questionTitle);
            newQuestion.setQuestionType(mapStringToQuestionType(templateQuestion.getQuestionType()));
            newQuestion.setMaxScore(templateQuestion.getScore() != null ? 
                templateQuestion.getScore() : BigDecimal.valueOf(5.0));
            newQuestion.setExam(exam);
            newQuestion.setCreatedBy(SecurityUtils.getCurrentUserId());
            newQuestion.setSourceType(SourceType.LEARNING_IMPORT);
            newQuestion.setIsConfirmed(true);
            
            // 处理题目内容和选项
            String questionContent = templateQuestion.getQuestionContent();
            String correctAnswer = null;
            List<String> optionsList = null;
            
            // 从模板配置中解析选项和正确答案
            if (templateQuestion.getQuestionConfig() != null && !templateQuestion.getQuestionConfig().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode configNode = mapper.readTree(templateQuestion.getQuestionConfig());
                    
                    if (configNode.has("correctAnswer")) {
                        correctAnswer = configNode.get("correctAnswer").asText();
                    }
                    
                    if (configNode.has("options")) {
                        JsonNode optionsNode = configNode.get("options");
                        if (optionsNode.isArray()) {
                            optionsList = new ArrayList<>();
                            for (JsonNode optionNode : optionsNode) {
                                optionsList.add(optionNode.asText());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析模板题目配置失败: {}", e.getMessage());
                }
            }
            
            // 构建完整的题目内容（题目干 + 选项）
            StringBuilder fullContent = new StringBuilder();
            fullContent.append(questionContent);
            
            if (optionsList != null && !optionsList.isEmpty()) {
                // 移除题目内容中的( )占位符，因为选项中已经包含了完整信息
                String cleanedContent = questionContent.replaceAll("\\s*\\(\\s*\\)\\s*$", "");
                fullContent = new StringBuilder(cleanedContent);
                
                // 添加选项到题目内容中
                fullContent.append("\n");
                for (String option : optionsList) {
                    fullContent.append(option).append("\n");
                }
            }
            
            newQuestion.setContent(fullContent.toString().trim());
            
            // 设置参考答案
            if (correctAnswer != null) {
                newQuestion.setReferenceAnswer(correctAnswer);
            }
            
            // 保存题目到数据库
            Question savedQuestion = questionRepository.save(newQuestion);
            
            // 为选择题创建选项实体
            if (isChoiceQuestionType(savedQuestion.getQuestionType()) && optionsList != null && !optionsList.isEmpty()) {
                createQuestionOptionsFromTemplate(savedQuestion, optionsList, correctAnswer);
                log.debug("✅ 为题目 {} 创建了 {} 个选项", savedQuestion.getId(), optionsList.size());
            }
            
            log.info("✅ 为考试 {} 创建新题目: ID={}, 顺序={}, 类型={}, 选项数={}", 
                     examId, savedQuestion.getId(), templateQuestion.getQuestionOrder(), 
                     templateQuestion.getQuestionType(), optionsList != null ? optionsList.size() : 0);
            
            return savedQuestion;
            
        } catch (Exception e) {
            log.error("❌ 获取或创建模板题目失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 判断是否为选择题类型
     */
    private boolean isChoiceQuestionType(QuestionType questionType) {
        return questionType == QuestionType.SINGLE_CHOICE || 
               questionType == QuestionType.MULTIPLE_CHOICE || 
               questionType == QuestionType.TRUE_FALSE;
    }
    
    /**
     * 从模板创建题目选项
     */
    private void createQuestionOptionsFromTemplate(Question question, List<String> optionsList, String correctAnswer) {
        if (optionsList == null || optionsList.isEmpty()) {
            return;
        }
        
        log.debug("为题目 {} 创建选项，正确答案: {}", question.getId(), correctAnswer);
        
        int optionOrder = 1;
        for (String optionText : optionsList) {
            if (optionText == null || optionText.trim().isEmpty()) {
                continue;
            }
            
            // 提取选项标识符和内容 (如: "A、选项内容" -> 标识符="A", 内容="选项内容")
            String optionIdentifier = extractOptionIdentifier(optionText);
            String optionContent = extractOptionContent(optionText);
            
            // 判断是否为正确答案
            boolean isCorrect = isCorrectOption(optionIdentifier, optionContent, correctAnswer);
            
            // 创建选项实体
            QuestionOption option = new QuestionOption();
            option.setContent(optionContent);
            option.setIsCorrect(isCorrect);
            option.setOptionOrder(optionOrder);
            option.setQuestion(question);
            
            // 保存选项到数据库
            QuestionOption savedOption = questionOptionRepository.save(option);
            
            log.debug("✅ 创建选项: {} - {} (正确: {}, ID: {})", 
                    optionIdentifier, 
                    optionContent.length() > 20 ? optionContent.substring(0, 20) + "..." : optionContent, 
                    isCorrect, 
                    savedOption.getId());
            
            optionOrder++;
        }
    }
    
    /**
     * 提取选项标识符 (A、B、C、D等)
     */
    private String extractOptionIdentifier(String optionText) {
        if (optionText == null) return "";
        
        // 匹配格式：A、 或 A. 或 (A) 等
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^([A-Z])\\s*[、.]");
        java.util.regex.Matcher matcher = pattern.matcher(optionText.trim());
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果没有找到标识符，返回空字符串
        return "";
    }
    
    /**
     * 提取选项内容（去除选项标识符）
     */
    private String extractOptionContent(String optionText) {
        if (optionText == null) return "";
        
        // 移除开头的选项标识符 (A、 或 A. 等)
        String content = optionText.replaceAll("^[A-Z]\\s*[、.]\\s*", "").trim();
        return content.isEmpty() ? optionText.trim() : content;
    }
    
    /**
     * 判断选项是否为正确答案
     */
    private boolean isCorrectOption(String optionIdentifier, String optionContent, String correctAnswer) {
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return false;
        }
        
        // 1. 直接匹配选项标识符 (A、B、C、D)
        if (optionIdentifier.equals(correctAnswer.trim())) {
            return true;
        }
        
        // 2. 匹配选项内容
        if (optionContent.equals(correctAnswer.trim())) {
            return true;
        }
        
        // 3. 模糊匹配（去除空格后比较）
        if (optionContent.replaceAll("\\s+", "").equals(correctAnswer.replaceAll("\\s+", ""))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断题目类型是否为客观题（可以自动评估的题型）
     */
    private boolean isObjectiveQuestionType(QuestionType questionType) {
        if (questionType == null) {
            return false;
        }
        
        switch (questionType) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
            case TRUE_FALSE:
                return true; // 选择题和判断题是客观题，有答案就可以自动评估
            case FILL_BLANK:
            case SHORT_ANSWER:
            case ESSAY:
            case CODING:
            case CASE_ANALYSIS:
            case CALCULATION:
                return false; // 主观题需要人工评阅，即使有分数也不自动设置为已评估
            default:
                return false;
        }
    }
    
    /**
     * 映射字符串到QuestionType枚举
     */
    private QuestionType mapStringToQuestionType(String questionTypeStr) {
        if (questionTypeStr == null) {
            return QuestionType.SHORT_ANSWER;
        }
        
        try {
            return QuestionType.valueOf(questionTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 处理一些常见的映射
            switch (questionTypeStr.toLowerCase()) {
                case "单选题":
                case "single_choice":
                    return QuestionType.SINGLE_CHOICE;
                case "多选题":
                case "multiple_choice":
                    return QuestionType.MULTIPLE_CHOICE;
                case "判断题":
                case "true_false":
                    return QuestionType.TRUE_FALSE;
                case "填空题":
                case "fill_blank":
                    return QuestionType.FILL_BLANK;
                case "简答题":
                case "short_answer":
                    return QuestionType.SHORT_ANSWER;
                case "论述题":
                case "essay":
                    return QuestionType.ESSAY;
                case "计算题":
                case "calculation":
                    return QuestionType.CALCULATION;
                case "案例分析题":
                case "case_analysis":
                    return QuestionType.CASE_ANALYSIS;
                default:
                    log.warn("未知题目类型: {}, 使用默认类型 SHORT_ANSWER", questionTypeStr);
                    return QuestionType.SHORT_ANSWER;
            }
        }
    }
}

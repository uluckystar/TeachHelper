package com.teachhelper.service.template;

import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.entity.*;
import com.teachhelper.repository.*;
import com.teachhelper.service.answer.LearningAnswerParserService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateBasedAnswerImportService {
    
    private static final Logger log = LoggerFactory.getLogger(TemplateBasedAnswerImportService.class);

    @Autowired
    private ExamTemplateRepository examTemplateRepository;
    
    @Autowired
    private ExamTemplateQuestionRepository examTemplateQuestionRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private LearningAnswerParserService learningAnswerParserService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    @Lazy
    private TemplateBasedAnswerImportService self;
    
    @Value("${app.upload.dir:/uploads}")
    private String uploadDir;

    /**
     * 基于模板导入学生答案
     */
    @Transactional
    public ImportResult importAnswersWithTemplate(Long examId, Long templateId, String subject, String classFolder, User importBy) {
        log.info("开始基于模板导入答案: examId={}, templateId={}, subject={}, classFolder={}",
                examId, templateId, subject, classFolder == null ? "所有班级" : classFolder);
        
        ImportResult result = new ImportResult();
        result.setStartTime(LocalDateTime.now());
        
        try {
            // 验证考试和模板
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));
            
            ExamTemplate template = examTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
            
            // 检查模板状态
            if (!template.isReadyForApplication()) {
                throw new RuntimeException("模板还未就绪，无法用于导入答案");
            }
            
            // 获取模板题目映射
            List<ExamTemplateQuestion> templateQuestions = examTemplateQuestionRepository
                    .findByExamTemplateOrderByQuestionNumber(template);
            
            Map<Integer, ExamTemplateQuestion> questionMap = new HashMap<>();
            for (ExamTemplateQuestion tq : templateQuestions) {
                // 使用确认状态而不是匹配状态
                if (tq.getIsConfirmed() && tq.getMatchedQuestion() != null) {
                    questionMap.put(tq.getQuestionNumber(), tq);
                }
            }
            
            if (questionMap.isEmpty()) {
                throw new RuntimeException("模板中没有已确认的题目，请先在模板详情页面确认题目");
            }
            
            // 获取班级的所有学习通文档
            List<File> documents;
            if (classFolder != null && !classFolder.trim().isEmpty()) {
                 documents = learningAnswerParserService.getAnswerDocuments(uploadDir, subject, classFolder);
            } else {
                // 如果没有指定班级，则获取该科目下所有班级的文档
                documents = learningAnswerParserService.getAllAnswerDocumentsForSubject(uploadDir, subject);
            }
            
            if (documents.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("未找到学习通答案文档");
                return result;
            }
            
            // 逐个解析文档并导入答案
            List<String> successStudents = new ArrayList<>();
            List<String> failedStudents = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();
            
            for (File document : documents) {
                try {
                    self.importSingleStudentAnswers(exam, document, questionMap, successStudents, failedStudents, errorMessages);
                } catch (RuntimeException e) {
                    log.error("导入单个学生答案时发生意外运行时异常: {}", document.getName(), e);
                    failedStudents.add(document.getName());
                    errorMessages.add(document.getName() + ": " + e.getMessage());
                }
            }
                    
            // 设置结果
            result.setSuccess(true);
            result.setTotalProcessed(documents.size());
            result.setSuccessCount(successStudents.size());
            result.setFailureCount(failedStudents.size());
            result.setSuccessfulStudents(successStudents);
            result.setFailedStudents(failedStudents);
            result.setErrorMessages(errorMessages);
            result.setEndTime(LocalDateTime.now());
            
            log.info("基于模板导入完成: 总计{}, 成功{}, 失败{}", 
                    documents.size(), successStudents.size(), failedStudents.size());
            
        } catch (Exception e) {
            log.error("基于模板导入答案失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 预览模板匹配情况
     */
    public Map<String, Object> previewTemplateMatching(Long templateId, String subject, String classFolder) {
        Map<String, Object> preview = new HashMap<>();
        
        try {
            // 获取模板
            ExamTemplate template = examTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
            
            // 获取模板题目
            List<ExamTemplateQuestion> templateQuestions = examTemplateQuestionRepository
                    .findByExamTemplateOrderByQuestionNumber(template);
            
            Map<Integer, ExamTemplateQuestion> questionMap = new HashMap<>();
            for (ExamTemplateQuestion tq : templateQuestions) {
                questionMap.put(tq.getQuestionNumber(), tq);
            }
            
            // 获取文档
            List<File> documents = learningAnswerParserService.getAnswerDocuments(uploadDir, subject, classFolder);
            
            // 分析第一个文档的结构
            if (!documents.isEmpty()) {
                File firstDoc = documents.get(0);
                StudentAnswerImportData importData = learningAnswerParserService.parseLearningAnswerDocument(firstDoc);
                
                if (importData != null && importData.getAnswers() != null) {
                    Map<Integer, String> documentQuestions = new HashMap<>();
                    List<Integer> unmatchedQuestions = new ArrayList<>();
                    
                    for (StudentAnswerImportData.QuestionAnswer qa : importData.getAnswers()) {
                        Integer qNum = qa.getQuestionNumber();
                        if (qNum != null) {
                            documentQuestions.put(qNum, qa.getQuestionContent());
                            if (!questionMap.containsKey(qNum)) {
                                unmatchedQuestions.add(qNum);
                            }
                        }
                    }
                    
                    preview.put("documentQuestions", documentQuestions);
                    preview.put("unmatchedQuestions", unmatchedQuestions);
                    preview.put("matchedCount", documentQuestions.size() - unmatchedQuestions.size());
                    preview.put("totalDocumentQuestions", documentQuestions.size());
                }
            }
            
            preview.put("templateQuestions", templateQuestions.size());
            preview.put("templateMatchedQuestions", template.getMatchedQuestions());
            preview.put("documentsFound", documents.size());
            preview.put("templateStatus", template.getStatus().name());
            preview.put("canImport", template.isReadyForApplication());
            
        } catch (Exception e) {
            log.error("预览模板匹配失败", e);
            preview.put("error", e.getMessage());
        }
        
        return preview;
    }

    // 私有辅助方法
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importSingleStudentAnswers(Exam exam, File document, Map<Integer, ExamTemplateQuestion> questionMap,
                                          List<String> successStudents, List<String> failedStudents, List<String> errorMessages) {
        String studentName = null;
        String displayName = null;
        try {
            // 解析文档
            StudentAnswerImportData importData = learningAnswerParserService.parseLearningAnswerDocument(document);
            if (importData == null || importData.getAnswers() == null || importData.getAnswers().isEmpty()) {
                log.warn("文档 '{}' 解析为空或不包含答案数据，已跳过。", document.getName());
                return;
            }
            
            // 智能解析学生信息
            StudentInfo studentInfo = parseStudentInfoFromImportData(importData, document.getName());
            studentName = studentInfo.getUsername();
            displayName = studentInfo.getDisplayName();
            
            if (studentName == null || studentName.trim().isEmpty()) {
                log.error("无法解析学生用户名，文档: {}", document.getName());
                throw new RuntimeException("无法解析学生用户名");
            }
            
            User student = findOrCreateStudent(studentInfo);
            studentAnswerRepository.deleteByExamAndStudent(exam, student);
            int savedCount = 0;
            for (StudentAnswerImportData.QuestionAnswer qa : importData.getAnswers()) {
                Integer questionNumber = qa.getQuestionNumber();
                if (questionNumber != null && questionMap.containsKey(questionNumber)) {
                    ExamTemplateQuestion templateQuestion = questionMap.get(questionNumber);
                    Question question = templateQuestion.getMatchedQuestion();
                    String answerContent = qa.getAnswerContent();
                    if (question != null && answerContent != null && !answerContent.trim().isEmpty()) {
                        StudentAnswer answer = new StudentAnswer();
                        answer.setExam(exam);
                        answer.setQuestion(question);
                        answer.setStudent(student);
                        answer.setAnswerText(answerContent); // 关键：设置到 answerText
                        
                        // 设置解析到的分数（如果有）
                        if (qa.getScore() != null) {
                            answer.setScore(BigDecimal.valueOf(qa.getScore()));
                            log.debug("保存题目 {} 得分: {}", qa.getQuestionNumber(), qa.getScore());
                        }
                        
                        answer.setSubmittedAt(LocalDateTime.now());
                        answer.setCreatedAt(LocalDateTime.now());
                        answer.setUpdatedAt(LocalDateTime.now());
                        studentAnswerRepository.save(answer);
                        savedCount++;
                    }
                }
            }
            if (savedCount > 0) {
                successStudents.add(displayName);
                log.info("✅ 成功导入学生答案: {} ({}道题)", displayName, savedCount);
            } else {
                log.warn("⚠️ 学生 {} 的答案文档已处理，但没有匹配到任何题目或所有答案均为空。", displayName);
            }
        } catch (Exception e) {
            String name = displayName != null ? displayName : (studentName != null ? studentName : document.getName());
            failedStudents.add(name);
            errorMessages.add(name + ": " + e.getMessage());
            log.error("❌ 导入学生 '{}' 的答案时发生严重错误。", name, e);
            throw new RuntimeException("Failed to process answers for " + name, e);
        }
    }

    /**
     * 学生信息结构体
     */
    private static class StudentInfo {
        private String username;        // 用作登录用户名，符合字段限制
        private String displayName;     // 用于显示的名称
        private String studentNumber;   // 学号
        private String realName;        // 真实姓名
        private String className;       // 班级
        private String major;           // 专业
        private String college;         // 学院
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getStudentNumber() { return studentNumber; }
        public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getMajor() { return major; }
        public void setMajor(String major) { this.major = major; }
        public String getCollege() { return college; }
        public void setCollege(String college) { this.college = college; }
    }
    
    /**
     * 智能解析学生信息
     */
    private StudentInfo parseStudentInfoFromImportData(StudentAnswerImportData importData, String fileName) {
        StudentInfo info = new StudentInfo();
        
        // 从文档内容获取学生信息
        String studentName = importData.getStudentName();
        String studentNumber = importData.getStudentNumber();
        String college = importData.getCollege();
        String major = importData.getMajor();
        
        // 如果解析器没有提取到信息，从文件名再次尝试解析
        if ((studentName == null || studentName.trim().isEmpty()) && 
            (studentNumber == null || studentNumber.trim().isEmpty())) {
            parseStudentInfoFromFileName(fileName, info);
        } else {
            info.setRealName(studentName);
            info.setStudentNumber(studentNumber);
            info.setCollege(college);
            info.setMajor(major);
        }
        
        // 生成合适的用户名和显示名称
        generateUsernameAndDisplayName(info, fileName);
        
        return info;
    }
    
    /**
     * 从文件名解析学生信息
     */
    private void parseStudentInfoFromFileName(String fileName, StudentInfo info) {
        // 移除文件扩展名
        String nameWithoutExtension = fileName.replaceAll("\\.(doc|docx)$", "");
        
        log.debug("开始解析文件名: {}", nameWithoutExtension);
        
        // 模式1: miniprogram数字-姓名-考试信息
        Pattern pattern1 = Pattern.compile("(miniprogram\\d+)-([^-]+)-(.+)");
        Matcher matcher1 = pattern1.matcher(nameWithoutExtension);
        if (matcher1.find()) {
            info.setStudentNumber(matcher1.group(1));
            info.setRealName(matcher1.group(2));
            log.debug("解析文件名模式1: 学号={}, 姓名={}", info.getStudentNumber(), info.getRealName());
            return;
        }

        // 模式2: 学院-专业-学号-姓名-考试信息
        Pattern pattern2 = Pattern.compile("([^-]+)-([^-]+)-([^-]*)(\\d{10,})-([^-]+)-(.+)");
        Matcher matcher2 = pattern2.matcher(nameWithoutExtension);
        if (matcher2.find()) {
            info.setCollege(matcher2.group(1));
            info.setMajor(matcher2.group(2));
            info.setStudentNumber(matcher2.group(4));
            info.setRealName(matcher2.group(5));
            log.debug("解析文件名模式2: 学院={}, 专业={}, 学号={}, 姓名={}", 
                     info.getCollege(), info.getMajor(), info.getStudentNumber(), info.getRealName());
            return;
        }

        // 模式3: 专业年级-学号-姓名
        Pattern pattern3 = Pattern.compile("([^-]+)(\\d{4})-(\\d+)-([^-]+)");
        Matcher matcher3 = pattern3.matcher(nameWithoutExtension);
        if (matcher3.find()) {
            info.setMajor(matcher3.group(1) + matcher3.group(2));
            info.setStudentNumber(matcher3.group(3));
            info.setRealName(matcher3.group(4));
            log.debug("解析文件名模式3: 专业={}, 学号={}, 姓名={}", 
                     info.getMajor(), info.getStudentNumber(), info.getRealName());
            return;
        }
        
        // 模式4: 简单的 姓名-其他信息 格式
        Pattern pattern4 = Pattern.compile("([^-\\d]+)-(.+)");
        Matcher matcher4 = pattern4.matcher(nameWithoutExtension);
        if (matcher4.find()) {
            info.setRealName(matcher4.group(1));
            log.debug("解析文件名模式4: 姓名={}", info.getRealName());
            return;
        }
        
        // 如果都无法匹配，使用文件名作为姓名
        String fallbackName = nameWithoutExtension.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        if (fallbackName.length() > 0) {
            info.setRealName(fallbackName.length() > 10 ? fallbackName.substring(0, 10) : fallbackName);
            log.debug("解析文件名备用方案: 姓名={}", info.getRealName());
        }
    }
    
    /**
     * 生成合适的用户名和显示名称
     */
    private void generateUsernameAndDisplayName(StudentInfo info, String fileName) {
        String realName = info.getRealName();
        String studentNumber = info.getStudentNumber();
        
        // 构建显示名称（用于日志和错误信息）
        StringBuilder displayName = new StringBuilder();
        if (realName != null && !realName.trim().isEmpty()) {
            displayName.append(realName.trim());
        }
        if (studentNumber != null && !studentNumber.trim().isEmpty()) {
            if (displayName.length() > 0) {
                displayName.append("(").append(studentNumber.trim()).append(")");
            } else {
                displayName.append(studentNumber.trim());
            }
        }
        if (displayName.length() == 0) {
            displayName.append(fileName.replaceFirst("[.][^.]+$", ""));
        }
        info.setDisplayName(displayName.toString());
        
        // 生成符合字段限制的用户名（2-50字符）
        String username = null;
        
        // 优先使用学号作为用户名
        if (studentNumber != null && !studentNumber.trim().isEmpty()) {
            String cleanedNumber = studentNumber.trim();
            if (cleanedNumber.length() >= 2 && cleanedNumber.length() <= 50) {
                username = cleanedNumber;
            } else if (cleanedNumber.length() > 50) {
                // 如果学号太长，截取后缀
                username = cleanedNumber.substring(cleanedNumber.length() - 50);
            } else if (cleanedNumber.length() < 2) {
                // 如果学号太短，添加前缀
                username = "stu_" + cleanedNumber;
            }
        }
        
        // 如果学号不可用，使用姓名
        if (username == null && realName != null && !realName.trim().isEmpty()) {
            String cleanedName = realName.trim().replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_]", "");
            if (cleanedName.length() >= 2 && cleanedName.length() <= 50) {
                username = cleanedName;
            } else if (cleanedName.length() > 50) {
                username = cleanedName.substring(0, 50);
            } else if (cleanedName.length() < 2) {
                username = "user_" + cleanedName + "_" + System.currentTimeMillis() % 10000;
            }
        }
        
        // 最后的备用方案：基于文件名生成
        if (username == null) {
            String baseName = fileName.replaceFirst("[.][^.]+$", "")
                                    .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_]", "");
            if (baseName.length() > 50) {
                baseName = baseName.substring(0, 50);
            }
            if (baseName.length() < 2) {
                baseName = "imported_user_" + System.currentTimeMillis() % 10000;
            }
            username = baseName;
        }
        
        // 确保用户名符合要求
        if (username.length() < 2) {
            username = "usr_" + username + "_" + System.currentTimeMillis() % 1000;
        }
        if (username.length() > 50) {
            username = username.substring(0, 50);
        }
        
        info.setUsername(username);
        
        log.debug("生成用户信息: username={}, displayName={}, studentNumber={}, realName={}", 
                 info.getUsername(), info.getDisplayName(), info.getStudentNumber(), info.getRealName());
    }

    /**
     * 查找或创建学生用户
     */
    private User findOrCreateStudent(StudentInfo studentInfo) {
        String username = studentInfo.getUsername();
        String studentNumber = studentInfo.getStudentNumber();
        
        // 首先尝试通过用户名查找
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            log.debug("找到现有用户: {}", username);
            return existingUser.get();
        }
        
        // 如果有学号，尝试通过学号查找
        if (studentNumber != null && !studentNumber.trim().isEmpty()) {
            Optional<User> userByStudentNumber = userRepository.findByStudentNumber(studentNumber.trim());
            if (userByStudentNumber.isPresent()) {
                User user = userByStudentNumber.get();
                log.debug("通过学号找到现有用户: studentNumber={}, username={}", studentNumber, user.getUsername());
                return user;
            }
        }
        
        // 创建新用户
        log.info("创建新学生用户: username={}, studentNumber={}, realName={}", 
                username, studentNumber, studentInfo.getRealName());
        
        User newStudent = new User();
        newStudent.setUsername(username);
        newStudent.setNickname(studentInfo.getDisplayName());
        
        // 设置真实姓名
        if (studentInfo.getRealName() != null && !studentInfo.getRealName().trim().isEmpty()) {
            newStudent.setRealName(studentInfo.getRealName().trim());
        }
        
        // 设置学号
        if (studentNumber != null && !studentNumber.trim().isEmpty()) {
            newStudent.setStudentNumber(studentNumber.trim());
        }
        
        // 设置班级和专业信息
        if (studentInfo.getClassName() != null && !studentInfo.getClassName().trim().isEmpty()) {
            newStudent.setClassName(studentInfo.getClassName().trim());
        }
        if (studentInfo.getMajor() != null && !studentInfo.getMajor().trim().isEmpty()) {
            newStudent.setMajor(studentInfo.getMajor().trim());
        }
        
        // 设置必填字段
        newStudent.setEmail(username + "@auto-imported.com");
        newStudent.setPassword(passwordEncoder.encode("password123")); // 设置默认密码
        newStudent.setRoles(Set.of(Role.STUDENT));
        newStudent.setCreatedAt(LocalDateTime.now());
        newStudent.setUpdatedAt(LocalDateTime.now());
        newStudent.setEnabled(true);
        
        try {
            User savedUser = userRepository.save(newStudent);
            log.info("✅ 成功创建学生用户: username={}, id={}", savedUser.getUsername(), savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            log.error("❌ 创建学生用户失败: username={}, error={}", username, e.getMessage());
            throw new RuntimeException("创建学生用户失败: " + e.getMessage(), e);
        }
    }
}
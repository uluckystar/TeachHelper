package com.teachhelper.service.answer;

import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.Role;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.StudentAnswerRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.service.student.StudentAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 嵌套压缩包答案导入服务
 * 处理班级压缩包中包含学生压缩包的嵌套结构答案导入
 */
@Slf4j
@Service
public class NestedZipAnswerImportService {

    @Autowired
    private LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private StudentAnswerRepository studentAnswerRepository;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * 从指定路径导入单个题目的嵌套压缩包答案
     * 
     * @param answerPath 答案根路径，如：路由与交换技术平时作业与实验/实验1
     * @param questionId 题目ID
     * @return 导入结果
     */
    @Transactional
    public ImportResult importNestedZipAnswersForQuestion(String answerPath, Long questionId) throws IOException {
        log.info("🚀 开始导入嵌套压缩包答案 - 路径: {}, 题目ID: {}", answerPath, questionId);
        
        ImportResult result = new ImportResult();
        result.setSuccessfulStudents(new ArrayList<>());
        result.setFailedStudents(new ArrayList<>());
        result.setErrorMessages(new ArrayList<>());
        
        // 验证题目是否存在
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + questionId));
        
        // 构建完整的答案路径
        File answerDir = new File(getAnswerBasePath(), answerPath);
        if (!answerDir.exists() || !answerDir.isDirectory()) {
            throw new IllegalArgumentException("答案路径不存在: " + answerDir.getAbsolutePath());
        }
        
        log.info("📁 答案目录: {}", answerDir.getAbsolutePath());
        
        // 获取所有班级压缩包
        List<File> classZipFiles = getClassZipFiles(answerDir);
        if (classZipFiles.isEmpty()) {
            throw new IllegalArgumentException("在路径 " + answerPath + " 中未找到班级压缩包");
        }
        
        log.info("📦 找到 {} 个班级压缩包", classZipFiles.size());
        
        int totalStudents = 0;
        int successCount = 0;
        int failedCount = 0;
        
        // 逐个处理班级压缩包
        for (File classZipFile : classZipFiles) {
            log.info("📂 开始处理班级压缩包: {}", classZipFile.getName());
            
            try {
                NestedZipImportResult classResult = processClassZipFile(classZipFile, question);
                totalStudents += classResult.getTotalStudents();
                successCount += classResult.getSuccessCount();
                failedCount += classResult.getFailedCount();
                
                result.getSuccessfulStudents().addAll(classResult.getSuccessfulStudents());
                result.getFailedStudents().addAll(classResult.getFailedStudents());
                result.getErrorMessages().addAll(classResult.getErrorMessages());
                
                log.info("✅ 班级压缩包处理完成: {} - 成功: {}, 失败: {}", 
                        classZipFile.getName(), classResult.getSuccessCount(), classResult.getFailedCount());
                
            } catch (Exception e) {
                log.error("❌ 处理班级压缩包失败: {}", classZipFile.getName(), e);
                result.getErrorMessages().add("班级压缩包处理失败: " + classZipFile.getName() + " - " + e.getMessage());
            }
        }
        
        // 设置导入结果统计
        result.setTotalFiles(totalStudents);
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setSkippedCount(0); // 暂不支持跳过
        
        log.info("🎯 嵌套压缩包答案导入完成 - 总学生数: {}, 成功: {}, 失败: {}", 
                totalStudents, successCount, failedCount);
        
        return result;
    }
    
    /**
     * 获取目录下的所有班级压缩包
     */
    private List<File> getClassZipFiles(File answerDir) {
        List<File> zipFiles = new ArrayList<>();
        
        File[] files = answerDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                    zipFiles.add(file);
                }
            }
        }
        
        // 按文件名排序
        zipFiles.sort(Comparator.comparing(File::getName));
        
        return zipFiles;
    }
    
    /**
     * 处理单个班级压缩包
     */
    private NestedZipImportResult processClassZipFile(File classZipFile, Question question) throws IOException {
        NestedZipImportResult result = new NestedZipImportResult();
        result.setSuccessfulStudents(new ArrayList<>());
        result.setFailedStudents(new ArrayList<>());
        result.setErrorMessages(new ArrayList<>());
        
        // 创建临时目录用于解压
        File tempDir = createTempDirectory("class_zip_" + System.currentTimeMillis());
        
        try (ZipFile zipFile = new ZipFile(classZipFile, StandardCharsets.UTF_8)) {
            // 解压班级压缩包到临时目录
            extractZipFile(zipFile, tempDir);
            
            // 查找学生压缩包
            List<File> studentZipFiles = findStudentZipFiles(tempDir);
            log.info("📚 在班级压缩包 {} 中找到 {} 个学生压缩包", classZipFile.getName(), studentZipFiles.size());
            
            result.setTotalStudents(studentZipFiles.size());
            
            // 逐个处理学生压缩包
            for (File studentZipFile : studentZipFiles) {
                try {
                    boolean success = processStudentZipFile(studentZipFile, question);
                    if (success) {
                        result.incrementSuccessCount();
                        result.getSuccessfulStudents().add(studentZipFile.getName());
                    } else {
                        result.incrementFailedCount();
                        result.getFailedStudents().add(studentZipFile.getName());
                        result.getErrorMessages().add("学生压缩包处理失败: " + studentZipFile.getName());
                    }
                } catch (Exception e) {
                    log.error("处理学生压缩包失败: {}", studentZipFile.getName(), e);
                    result.incrementFailedCount();
                    result.getFailedStudents().add(studentZipFile.getName());
                    result.getErrorMessages().add("学生压缩包处理异常: " + studentZipFile.getName() + " - " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("解压班级压缩包失败: {}", classZipFile.getName(), e);
            throw new IOException("解压班级压缩包失败: " + e.getMessage(), e);
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir);
        }
        
        return result;
    }
    
    /**
     * 解压ZIP文件到指定目录
     */
    private void extractZipFile(ZipFile zipFile, File targetDir) throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryFile = new File(targetDir, entry.getName());
            
            // 安全检查，防止路径遍历攻击
            if (!entryFile.toPath().normalize().startsWith(targetDir.toPath().normalize())) {
                throw new IOException("Entry is outside the target dir: " + entry.getName());
            }
            
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                // 确保父目录存在
                entryFile.getParentFile().mkdirs();
                
                try (InputStream inputStream = zipFile.getInputStream(entry);
                     FileOutputStream outputStream = new FileOutputStream(entryFile)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }
    
    /**
     * 在目录中查找学生压缩包（递归搜索）
     */
    private List<File> findStudentZipFiles(File directory) {
        List<File> studentZipFiles = new ArrayList<>();
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归搜索子目录
                    studentZipFiles.addAll(findStudentZipFiles(file));
                } else if (file.getName().toLowerCase().endsWith(".zip")) {
                    // 检查是否是学生压缩包（通过文件名格式判断）
                    if (isStudentZipFile(file.getName())) {
                        studentZipFiles.add(file);
                    }
                }
            }
        }
        
        // 按文件名排序
        studentZipFiles.sort(Comparator.comparing(File::getName));
        
        return studentZipFiles;
    }
    
    /**
     * 判断是否是学生压缩包（基于文件名格式）
     * 支持格式：任意内容-任意内容.zip 或 任意内容_任意内容.zip
     */
    private boolean isStudentZipFile(String fileName) {
        // 前后内容不限，只要有-或_分隔，后面有内容即可
        Pattern pattern = Pattern.compile("^[^-_]+[-_][^\\s]+\\.zip$", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(fileName).matches();
    }
    
    /**
     * 处理单个学生压缩包
     */
    private boolean processStudentZipFile(File studentZipFile, Question question) throws IOException {
        log.info("👤 开始处理学生压缩包: {}", studentZipFile.getName());
        
        // 从文件名解析学号和姓名
        StudentInfo studentInfo = parseStudentInfoFromFileName(studentZipFile.getName());
        if (studentInfo == null) {
            log.warn("⚠️ 无法解析学生信息: {}", studentZipFile.getName());
            return false;
        }
        
        log.info("📝 解析到学生信息: 学号={}, 姓名={}", studentInfo.getStudentNumber(), studentInfo.getName());
        
        // 查找或创建学生用户
        User student = findOrCreateStudent(studentInfo);
        if (student == null) {
            log.warn("⚠️ 无法创建学生用户: {}", studentInfo.getName());
            return false;
        }
        
        // 创建临时目录用于解压学生压缩包
        File tempDir = createTempDirectory("student_zip_" + System.currentTimeMillis());
        
        try (ZipFile zipFile = new ZipFile(studentZipFile, StandardCharsets.UTF_8)) {
            // 解压学生压缩包
            extractZipFile(zipFile, tempDir);
            
            // 查找答案文档
            File answerDocument = findAnswerDocument(tempDir);
            String answerContent;
            
            if (answerDocument == null) {
                log.warn("⚠️ 在学生压缩包中未找到答案文档: {}", studentZipFile.getName());
                // 即使没有找到文档，也要记录这个情况
                answerContent = String.format("【压缩包名】: %s\n【状态】: 未找到可识别的答案文档\n【说明】: 压缩包中可能包含非文档格式的文件或文件名不符合规范", 
                        studentZipFile.getName());
            } else {
                log.info("📄 找到答案文档: {}", answerDocument.getName());
                // 解析答案文档内容（现在总是会有内容，包含文件信息）
                answerContent = extractAnswerContent(answerDocument);
            }
            
            // 创建或更新学生答案
            createOrUpdateStudentAnswer(student, question, answerContent);
            log.info("✅ 学生答案导入成功: {}", studentInfo.getName());
            return true;
            
        } catch (Exception e) {
            log.error("处理学生压缩包异常: {}", studentZipFile.getName(), e);
            return false;
        } finally {
            // 清理临时目录
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * 从文件名解析学生信息
     * 支持格式：任意内容-任意内容.zip 或 任意内容_任意内容.zip
     */
    private StudentInfo parseStudentInfoFromFileName(String fileName) {
        // 移除扩展名
        String nameWithoutExt = fileName.replaceAll("\\.(zip|rar|7z)$", "");
        // 前后内容不限，只要有-或_分隔，后面有内容即可
        Pattern pattern = Pattern.compile("^([^-_]+)[-_]([^\\s]+)$");
        Matcher matcher = pattern.matcher(nameWithoutExt);
        if (matcher.matches()) {
            String studentNumber = matcher.group(1);
            String name = matcher.group(2);
            return new StudentInfo(studentNumber, name);
        }
        return null;
    }
    
    /**
     * 查找或创建学生用户
     */
    private User findOrCreateStudent(StudentInfo studentInfo) {
        // 首先尝试通过学号查找
        Optional<User> existingUser = userRepository.findByStudentNumber(studentInfo.getStudentNumber());
        if (existingUser.isPresent()) {
            log.info("找到已存在的学生: {}", studentInfo.getName());
            return existingUser.get();
        }
        
        // 如果不存在，创建新用户
        User newUser = new User();
        newUser.setStudentNumber(studentInfo.getStudentNumber());
        newUser.setRealName(studentInfo.getName());
        newUser.setUsername(studentInfo.getStudentNumber()); // 使用学号作为用户名
        newUser.setPassword("123456"); // 默认密码，后续需要修改
        newUser.setEmail(studentInfo.getStudentNumber() + "@student.edu"); // 默认邮箱
        newUser.setRoles(Set.of(Role.STUDENT)); // 设置为学生角色
        
        try {
            User savedUser = userRepository.save(newUser);
            log.info("创建新学生用户: {} (学号: {})", studentInfo.getName(), studentInfo.getStudentNumber());
            return savedUser;
        } catch (Exception e) {
            log.error("创建学生用户失败: {}", studentInfo.getName(), e);
            return null;
        }
    }
    
    /**
     * 在目录中查找答案文档（递归搜索，支持嵌套压缩包）
     */
    private File findAnswerDocument(File directory) {
        return findAnswerDocumentRecursively(directory, 0);
    }
    
    /**
     * 递归查找答案文档，支持解压嵌套压缩包
     * @param directory 搜索目录
     * @param depth 递归深度，防止无限递归
     * @return 找到的答案文档文件
     */
    private File findAnswerDocumentRecursively(File directory, int depth) {
        if (depth > 3) { // 限制递归深度，防止无限递归
            log.warn("达到最大递归深度，停止搜索: {}", directory.getAbsolutePath());
            return null;
        }
        
        File[] files = directory.listFiles();
        if (files == null) return null;
        
        log.debug("🔍 搜索目录 (深度{}): {} - 找到 {} 个文件", depth, directory.getName(), files.length);
        
        // 首先查找直接的答案文档
        for (File file : files) {
            if (file.isFile() && isAnswerDocument(file.getName())) {
                log.info("📄 找到答案文档: {} (深度: {})", file.getName(), depth);
                return file;
            }
        }
        
        // 然后递归搜索子目录
        for (File file : files) {
            if (file.isDirectory()) {
                File answerDoc = findAnswerDocumentRecursively(file, depth + 1);
                if (answerDoc != null) {
                    return answerDoc;
                }
            }
        }
        
        // 最后尝试解压嵌套的压缩包
        for (File file : files) {
            if (file.isFile() && isZipFile(file.getName())) {
                log.info("📦 发现嵌套压缩包，尝试解压: {}", file.getName());
                File answerDoc = extractAndSearchNestedZip(file, depth);
                if (answerDoc != null) {
                    return answerDoc;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否是压缩包文件
     */
    private boolean isZipFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || 
               lowerName.endsWith(".7z") || lowerName.endsWith(".tar") ||
               lowerName.endsWith(".gz") || lowerName.endsWith(".bz2");
    }
    
    /**
     * 解压嵌套的压缩包并搜索答案文档
     */
    private File extractAndSearchNestedZip(File zipFile, int depth) {
        if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
            log.debug("暂不支持非ZIP格式的嵌套压缩包: {}", zipFile.getName());
            return null;
        }
        
        File tempDir = null;
        try {
            // 创建临时目录用于解压嵌套压缩包
            tempDir = createTempDirectory("nested_zip_" + System.currentTimeMillis());
            
            // 解压嵌套压缩包
            try (ZipFile nestedZipFile = new ZipFile(zipFile, StandardCharsets.UTF_8)) {
                extractZipFile(nestedZipFile, tempDir);
                log.debug("📂 嵌套压缩包解压完成: {} -> {}", zipFile.getName(), tempDir.getAbsolutePath());
                
                // 在解压的内容中搜索答案文档
                return findAnswerDocumentRecursively(tempDir, depth + 1);
            }
            
        } catch (Exception e) {
            log.warn("解压嵌套压缩包失败: {} - {}", zipFile.getName(), e.getMessage());
            return null;
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }
    
    /**
     * 判断是否是答案文档（支持文档和图片格式）
     */
    private boolean isAnswerDocument(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || 
               lowerName.endsWith(".pdf") || lowerName.endsWith(".txt") ||
               lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
               lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
               lowerName.endsWith(".bmp") || lowerName.endsWith(".webp") ||
               lowerName.endsWith(".rtf") || lowerName.endsWith(".odt") ||
               lowerName.endsWith(".pages") || lowerName.endsWith(".wps");
    }
    
    /**
     * 提取答案文档内容
     */
    private String extractAnswerContent(File answerDocument) {
        try {
            // 使用专门的嵌套压缩包解析方法 - 直接将整个文档内容作为答案
            StudentAnswerImportData importData = learningAnswerParserService.parseNestedZipAnswerDocument(answerDocument);
            if (importData != null && importData.getAnswers() != null && !importData.getAnswers().isEmpty()) {
                // 获取答案内容（包含文件信息和完整内容）
                return importData.getAnswers().get(0).getAnswerContent();
            }
            
            // 如果解析失败，返回文件信息
            log.warn("文档解析失败或无答案内容: {}", answerDocument.getName());
            return String.format("【文件名】: %s\n【状态】: 文档解析失败或内容为空", answerDocument.getName());
        } catch (Exception e) {
            log.error("提取答案文档内容失败: {}", answerDocument.getName(), e);
            return String.format("【文件名】: %s\n【状态】: 文档解析异常\n【错误信息】: %s", 
                    answerDocument.getName(), e.getMessage());
        }
    }
    
    /**
     * 创建临时目录
     */
    private File createTempDirectory(String prefix) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), prefix);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IOException("无法创建临时目录: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }
    
    /**
     * 删除目录及其所有内容
     */
    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) return;
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
    
    /**
     * 学生信息内部类
     */
    private static class StudentInfo {
        private final String studentNumber;
        private final String name;
        
        public StudentInfo(String studentNumber, String name) {
            this.studentNumber = studentNumber;
            this.name = name;
        }
        
        public String getStudentNumber() { return studentNumber; }
        public String getName() { return name; }
    }
    
    /**
     * 嵌套压缩包导入结果内部类
     */
    private static class NestedZipImportResult {
        private List<String> successfulStudents;
        private List<String> failedStudents;
        private List<String> errorMessages;
        private int totalStudents;
        private int successCount;
        private int failedCount;
        
        public NestedZipImportResult() {
            this.successfulStudents = new ArrayList<>();
            this.failedStudents = new ArrayList<>();
            this.errorMessages = new ArrayList<>();
        }
        
        // Getters and setters
        public List<String> getSuccessfulStudents() { return successfulStudents; }
        public void setSuccessfulStudents(List<String> successfulStudents) { this.successfulStudents = successfulStudents; }
        
        public List<String> getFailedStudents() { return failedStudents; }
        public void setFailedStudents(List<String> failedStudents) { this.failedStudents = failedStudents; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
        
        public int getTotalStudents() { return totalStudents; }
        public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }
        
        public int getSuccessCount() { return successCount; }
        public void incrementSuccessCount() { this.successCount++; }
        
        public int getFailedCount() { return failedCount; }
        public void incrementFailedCount() { this.failedCount++; }
    }

    /**
     * 创建或更新学生答案
     */
    private void createOrUpdateStudentAnswer(User student, Question question, String answerContent) {
        try {
            // 检查是否已存在该学生对该题目的答案
            StudentAnswer existingAnswer = studentAnswerRepository.findByStudentIdAndQuestionId(
                student.getId(), question.getId());
            
            if (existingAnswer != null) {
                // 更新现有答案
                existingAnswer.setAnswerText(answerContent != null ? answerContent : "");
                existingAnswer.setCreatedAt(LocalDateTime.now());
                studentAnswerRepository.save(existingAnswer);
                log.debug("更新学生答案: 学生={}, 题目={}", student.getRealName(), question.getTitle());
            } else {
                // 创建新答案
                StudentAnswer newAnswer = new StudentAnswer();
                newAnswer.setStudent(student);
                newAnswer.setQuestion(question);
                newAnswer.setAnswerText(answerContent != null ? answerContent : "");
                newAnswer.setEvaluated(false);
                newAnswer.setCreatedAt(LocalDateTime.now());
                studentAnswerRepository.save(newAnswer);
                log.debug("创建新学生答案: 学生={}, 题目={}", student.getRealName(), question.getTitle());
            }
        } catch (Exception e) {
            log.error("创建或更新学生答案失败: 学生={}, 题目={}", student.getRealName(), question.getTitle(), e);
            throw new RuntimeException("创建或更新学生答案失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取可用的科目列表
     */
    public List<String> getAvailableSubjects() {
        String basePath = getAnswerBasePath();
        log.info("📁 获取科目列表 - 基础路径: {}", basePath);
        
        Path answerDir = Paths.get(basePath);
        
        if (!Files.exists(answerDir)) {
            log.warn("答案基础目录不存在: {}", basePath);
            return Arrays.asList();
        }
        
        try (Stream<Path> stream = Files.list(answerDir)) {
            List<String> subjects = stream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.startsWith(".")) // 过滤隐藏文件
                    .sorted()
                    .collect(Collectors.toList());
            
            log.info("📋 找到科目列表: {}", subjects);
            return subjects;
        } catch (IOException e) {
            log.error("读取科目目录失败", e);
            return Arrays.asList();
        }
    }
    
    /**
     * 获取指定科目下的作业/实验列表
     */
    public List<String> getAvailableAssignments(String subject) {
        String basePath = getAnswerBasePath();
        Path subjectDir = Paths.get(basePath, subject);
        
        if (!Files.exists(subjectDir)) {
            log.warn("科目目录不存在: {}", subjectDir);
            return Arrays.asList();
        }
        
        try (Stream<Path> stream = Files.list(subjectDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.startsWith(".")) // 过滤隐藏文件
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("读取作业目录失败: subject={}", subject, e);
            return Arrays.asList();
        }
    }
    
    /**
     * 基于科目和作业名称的嵌套压缩包导入
     */
    public ImportResult importNestedZipAnswersBySubjectAndAssignment(String subject, String assignment, Long questionId) {
        log.info("🚀 开始基于科目和作业的嵌套压缩包导入 - 科目: {}, 作业: {}, 题目ID: {}", subject, assignment, questionId);
        
        try {
            // 构建相对于answer目录的路径
            String relativePath = subject + File.separator + assignment;
            
            log.info("构建的相对路径: {}", relativePath);
            
            // 委托给原有的导入方法
            return importNestedZipAnswersForQuestion(relativePath, questionId);
        } catch (IOException e) {
            log.error("嵌套压缩包导入失败", e);
            ImportResult errorResult = new ImportResult();
            errorResult.setSuccessCount(0);
            errorResult.setFailedCount(1);
            errorResult.setErrorMessages(Arrays.asList("导入失败: " + e.getMessage()));
            return errorResult;
        }
    }
    
    /**
     * 获取答案文件的基础路径
     */
    private String getAnswerBasePath() {
        // 使用配置文件中的上传目录 + /answer
        return uploadDir + File.separator + "answer";
    }
} 
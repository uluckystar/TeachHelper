package com.teachhelper.service.answer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.teachhelper.entity.Question;
import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.service.answer.FileNameParserService.ParseResult;
import com.teachhelper.service.student.StudentAnswerService;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.service.UserAIConfigService;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.dto.request.UserAIConfigRequest;
import com.teachhelper.entity.AIProvider;
import java.math.BigDecimal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 文件夹上传答案服务
 * 处理批量文件上传，解析文档内容，使用LLM解析文件名
 */
@Service
public class FolderUploadAnswerService {
    
    private static final Logger log = LoggerFactory.getLogger(FolderUploadAnswerService.class);
    
    @Autowired
    private FileNameParserService fileNameParserService;
    
    @Autowired
    private LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private UserAIConfigService userAIConfigService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 线程池用于并行处理文件
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * 处理结果类
     */
    public static class ProcessResult {
        private int totalFiles;
        private int successCount;
        private int failedCount;
        private List<FileProcessDetail> details;
        private String summary;
        
        public ProcessResult() {
            this.details = new ArrayList<>();
        }
        
        // Getters and Setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        
        public List<FileProcessDetail> getDetails() { return details; }
        public void setDetails(List<FileProcessDetail> details) { this.details = details; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public void addDetail(FileProcessDetail detail) {
            this.details.add(detail);
            if (detail.isSuccess()) {
                this.successCount++;
            } else {
                this.failedCount++;
            }
        }
    }
    
    /**
     * 文件处理详情
     */
    public static class FileProcessDetail {
        private String fileName;
        private String studentName;
        private String studentNumber;
        private boolean success;
        private String errorMessage;
        private String parseMethod;
        private int contentLength;
        private LocalDateTime processTime;
        
        public FileProcessDetail() {
            this.processTime = LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        
        public String getStudentNumber() { return studentNumber; }
        public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getParseMethod() { return parseMethod; }
        public void setParseMethod(String parseMethod) { this.parseMethod = parseMethod; }
        
        public int getContentLength() { return contentLength; }
        public void setContentLength(int contentLength) { this.contentLength = contentLength; }
        
        public LocalDateTime getProcessTime() { return processTime; }
        public void setProcessTime(LocalDateTime processTime) { this.processTime = processTime; }
    }
    
    /**
     * 批量处理上传的文件
     * @param files 上传的文件列表
     * @param questionId 题目ID
     * @return 处理结果
     */
    public ProcessResult processUploadedFiles(MultipartFile[] files, Long questionId) {
        log.info("开始处理批量上传文件，文件数量: {}, 题目ID: {}", files.length, questionId);
        
        // 确保当前用户有AI配置
        ensureUserHasAIConfig();
        
        // 在主线程中获取当前用户ID和安全上下文
        Long currentUserId = null;
        SecurityContext securityContext = SecurityContextHolder.getContext();
        
        try {
            currentUserId = authService.getCurrentUser().getId();
            log.info("[批量导入] 在主线程获取到用户ID: {}", currentUserId);
        } catch (Exception e) {
            log.error("[批量导入] 无法获取当前用户ID: {}", e.getMessage());
            ProcessResult result = new ProcessResult();
            result.setTotalFiles(files.length);
            result.setSummary("无法获取用户认证信息，处理失败");
            return result;
        }
        
        final Long userId = currentUserId;
        
        ProcessResult result = new ProcessResult();
        result.setTotalFiles(files.length);
        
        // 验证题目存在
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        // 并行处理文件，传递用户ID和安全上下文
        List<CompletableFuture<FileProcessDetail>> futures = new ArrayList<>();
        
        for (MultipartFile file : files) {
            CompletableFuture<FileProcessDetail> future = CompletableFuture.supplyAsync(() -> {
                // 在异步线程中设置安全上下文
                SecurityContextHolder.setContext(securityContext);
                try {
                    return processSingleFile(file, question, userId);
                } finally {
                    // 清理安全上下文
                    SecurityContextHolder.clearContext();
                }
            }, executorService);
            futures.add(future);
        }
        
        // 等待所有文件处理完成
        for (CompletableFuture<FileProcessDetail> future : futures) {
            try {
                FileProcessDetail detail = future.get();
                result.addDetail(detail);
            } catch (Exception e) {
                log.error("处理文件异常", e);
                FileProcessDetail errorDetail = new FileProcessDetail();
                errorDetail.setFileName("未知文件");
                errorDetail.setSuccess(false);
                errorDetail.setErrorMessage("处理异常: " + e.getMessage());
                result.addDetail(errorDetail);
            }
        }
        
        // 生成摘要
        String summary = String.format("文件处理完成：总计 %d 个文件，成功 %d 个，失败 %d 个", 
            result.getTotalFiles(), result.getSuccessCount(), result.getFailedCount());
        result.setSummary(summary);
        
        log.info("批量文件处理完成: {}", summary);
        return result;
    }
    
    /**
     * 处理单个文件
     */
    private FileProcessDetail processSingleFile(MultipartFile file, Question question, Long userId) {
        FileProcessDetail detail = new FileProcessDetail();
        detail.setFileName(file.getOriginalFilename());
        long start = System.currentTimeMillis();
        try {
            log.info("[批量导入] 开始处理文件: {}", file.getOriginalFilename());
            // 1. 解析文件名，提取学生信息
            long t1 = System.currentTimeMillis();
            ParseResult parseResult = safeParseFileName(file.getOriginalFilename(), userId);
            log.info("[批量导入] LLM解析结果：成功={}，学生={}，学号={}，解析方法={}，耗时={}ms", 
                parseResult.isSuccess(), 
                parseResult.getStudentName(), 
                parseResult.getStudentNumber(), 
                parseResult.getParseMethod(),
                System.currentTimeMillis() - t1);
            detail.setStudentName(parseResult.getStudentName());
            detail.setStudentNumber(parseResult.getStudentNumber());
            detail.setParseMethod(parseResult.getParseMethod());
            if (!parseResult.isSuccess()) {
                detail.setSuccess(false);
                detail.setErrorMessage("文件名解析失败: " + parseResult.getErrorMessage());
                log.warn("[批量导入] 文件名解析失败: {}，错误: {}", file.getOriginalFilename(), parseResult.getErrorMessage());
                return detail;
            }
            
            // 记录解析结果的详细信息
            if ("LLM_NAME_ONLY".equals(parseResult.getParseMethod())) {
                log.info("[批量导入] 仅解析到姓名，已生成默认学号: {} -> {}({})", 
                    file.getOriginalFilename(), parseResult.getStudentName(), parseResult.getStudentNumber());
            } else {
                log.info("[批量导入] 解析成功: {} -> {}({})", 
                    file.getOriginalFilename(), parseResult.getStudentName(), parseResult.getStudentNumber());
            }
            // 2. 查找或创建学生用户
            log.info("[批量导入] 查找或创建学生: {} {}", parseResult.getStudentName(), parseResult.getStudentNumber());
            User student = findOrCreateStudent(parseResult.getStudentName(), parseResult.getStudentNumber());
            // 3. 解析文档内容
            log.info("[批量导入] 开始解析文档内容: {}", file.getOriginalFilename());
            long t2 = System.currentTimeMillis();
            String content = extractDocumentContent(file);
            log.info("[批量导入] 文档内容长度: {}，耗时: {}ms", content.length(), System.currentTimeMillis() - t2);
            detail.setContentLength(content.length());
            // 4. 保存学生答案
            log.info("[批量导入] 保存学生答案: {} {} {}", student.getUsername(), question.getId(), file.getOriginalFilename());
            saveStudentAnswer(student, question, content, file.getOriginalFilename());
            detail.setSuccess(true);
            log.info("[批量导入] 文件处理成功: {}，总耗时: {}ms", file.getOriginalFilename(), System.currentTimeMillis() - start);
        } catch (DataIntegrityViolationException e) {
            log.error("[批量导入] 数据库唯一约束冲突: {}，异常: {}", file.getOriginalFilename(), e.getMessage());
            detail.setSuccess(false);
            detail.setErrorMessage("数据库唯一约束冲突: " + e.getMessage());
        } catch (Exception e) {
            log.error("[批量导入] 处理文件失败: {}", file.getOriginalFilename(), e);
            detail.setSuccess(false);
            detail.setErrorMessage("处理失败: " + e.getMessage());
        }
        return detail;
    }
    
    /**
     * 确保当前用户有可用的AI配置
     */
    private void ensureUserHasAIConfig() {
        try {
            Long currentUserId = authService.getCurrentUser().getId();
            
            // 检查是否有默认AI配置
            var defaultConfig = userAIConfigService.getUserDefaultAIConfig(currentUserId);
            if (defaultConfig.isPresent()) {
                log.info("[批量导入] 用户{}已有AI配置", currentUserId);
                return;
            }
            
            // 如果没有，创建一个默认的DeepSeek配置
            log.info("[批量导入] 为用户{}创建默认AI配置", currentUserId);
            UserAIConfigRequest aiConfigRequest = new UserAIConfigRequest();
            aiConfigRequest.setProvider(AIProvider.DEEPSEEK);
            aiConfigRequest.setApiKey("sk-demo-key-auto-created-" + System.currentTimeMillis());
            aiConfigRequest.setApiEndpoint("https://api.deepseek.com/v1/chat/completions");
            aiConfigRequest.setModelName("deepseek-chat");
            aiConfigRequest.setMaxTokens(1000);
            aiConfigRequest.setTemperature(new BigDecimal("0.1"));
            aiConfigRequest.setIsDefault(true);
            
            userAIConfigService.saveAIConfig(currentUserId, aiConfigRequest);
            log.info("[批量导入] 默认AI配置创建成功");
            
        } catch (Exception e) {
            log.error("[批量导入] 创建默认AI配置失败", e);
            throw new RuntimeException("无法为用户创建AI配置: " + e.getMessage());
        }
    }

    /**
     * LLM文件名解析（带超时和fallback，使用传入的用户ID）
     */
    private ParseResult safeParseFileName(String fileName, Long userId) {
        long start = System.currentTimeMillis();
        try {
            log.info("[批量导入] [LLM] 开始解析文件名: {} (用户ID: {})", fileName, userId);
            
            // 5秒超时的LLM解析
            CompletableFuture<ParseResult> future = CompletableFuture.supplyAsync(() -> {
                return fileNameParserService.parseFileNameWithUserId(fileName, userId);
            });
            
            ParseResult result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            log.info("[批量导入] [LLM] 解析完成: {}，耗时: {}ms，成功: {}，学生姓名: {}，学号: {}，解析方法: {}，错误: {}", 
                fileName, 
                System.currentTimeMillis() - start, 
                result.isSuccess(),
                result.getStudentName(),
                result.getStudentNumber(),
                result.getParseMethod(),
                result.getErrorMessage()
            );
            return result;
        } catch (Exception e) {
            log.error("[批量导入] [LLM] 解析超时或异常: {}，异常: {}", fileName, e.getMessage());
            // fallback: 返回失败ParseResult
            ParseResult fail = new ParseResult();
            fail.setSuccess(false);
            fail.setErrorMessage("LLM解析超时或异常: " + e.getMessage());
            return fail;
        }
    }
    
    /**
     * 查找或创建学生用户
     */
    private User findOrCreateStudent(String studentName, String studentNumber) {
        try {
            log.info("[批量导入] [用户] 查找/创建: {} {}", studentName, studentNumber);
            Optional<User> existingUser = userRepository.findByStudentNumber(studentNumber);
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                // 检查并更新真实姓名（如果需要）
                if (studentName != null && !studentName.equals(user.getRealName())) {
                    log.info("[批量导入] [用户] 更新学生真实姓名: {} -> {}", user.getRealName(), studentName);
                    user.setRealName(studentName);
                    return userRepository.save(user);
                }
                return user;
            }
            
            log.info("[批量导入] [用户] 创建新学生: {} {}", studentName, studentNumber);
            User newUser = new User();
            
            // 设置基本信息
            newUser.setRealName(studentName);  // 设置真实姓名
            newUser.setStudentNumber(studentNumber);
            
            // 生成唯一用户名
            String baseUsername = studentNumber != null ? studentNumber : studentName;
            String username = generateUniqueUsername(baseUsername);
            newUser.setUsername(username);
            log.debug("[批量导入] [用户] 生成用户名: {}", username);
            
            // 生成唯一邮箱
            String email = generateUniqueEmail(baseUsername);
            newUser.setEmail(email);
            log.debug("[批量导入] [用户] 生成邮箱: {}", email);
            
            // 设置角色
            newUser.setRoles(Set.of(com.teachhelper.entity.Role.STUDENT));
            
            // 设置加密密码（使用系统默认密码）
            newUser.setPassword(passwordEncoder.encode("student123"));  // 加密默认密码
            
            // 设置为未激活状态（需要管理员激活或学生修改密码后激活）
            newUser.setEnabled(false);
            
            User savedUser = userRepository.save(newUser);
            log.info("[批量导入] [用户] 成功创建学生: {}({}), ID: {}, 用户名: {}", 
                studentName, studentNumber, savedUser.getId(), savedUser.getUsername());
            
            return savedUser;
            
        } catch (DataIntegrityViolationException e) {
            log.error("[批量导入] [用户] 唯一约束冲突: {} {}，异常: {}", studentName, studentNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[批量导入] [用户] 查找/创建异常: {} {}", studentName, studentNumber, e);
            throw e;
        }
    }
    
    /**
     * 提取文档内容
     */
    private String extractDocumentContent(MultipartFile file) throws IOException {
        // 清理文件名，移除路径分隔符，只保留文件名部分
        String originalFileName = file.getOriginalFilename();
        String cleanFileName = originalFileName;
        if (originalFileName != null) {
            // 移除路径分隔符，只保留最后的文件名
            cleanFileName = originalFileName.replaceAll("[/\\\\]", "_");
        }
        
        Path tempFile = Files.createTempFile("upload_", "_" + cleanFileName);
        try {
            log.info("[批量导入] [文档] 开始解析: {}", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            StudentAnswerImportData importData = learningAnswerParserService.parseNestedZipAnswerDocument(tempFile.toFile());
            if (importData != null && importData.getAnswers() != null && !importData.getAnswers().isEmpty()) {
                String content = importData.getAnswers().get(0).getAnswerContent();
                if (content == null || content.trim().isEmpty()) {
                    log.warn("[批量导入] [文档] 内容为空: {}", file.getOriginalFilename());
                    return String.format("【文件名】: %s\n【状态】: 文档内容为空", file.getOriginalFilename());
                }
                return content;
            } else {
                log.warn("[批量导入] [文档] 无法解析内容: {}", file.getOriginalFilename());
                return String.format("【文件名】: %s\n【状态】: 无法解析文档内容", file.getOriginalFilename());
            }
        } catch (Exception e) {
            log.error("[批量导入] [文档] 解析异常: {}", file.getOriginalFilename(), e);
            throw e;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    /**
     * 保存学生答案
     */
    @Transactional
    private void saveStudentAnswer(User student, Question question, String content, String originalFileName) {
        try {
            log.info("[批量导入] [答案] 保存: 学生={}({}), 题目ID={}, 文件名={}", student.getUsername(), student.getStudentNumber(), question.getId(), originalFileName);
            StudentAnswer existingAnswer = studentAnswerService.findByStudentIdAndQuestionId(student.getId(), question.getId());
            StudentAnswer answer;
            if (existingAnswer != null) {
                answer = existingAnswer;
                log.info("[批量导入] [答案] 更新现有答案: 学生={}({}), 题目ID={}", student.getUsername(), student.getStudentNumber(), question.getId());
            } else {
                answer = new StudentAnswer();
                answer.setStudent(student);
                answer.setQuestion(question);
                log.info("[批量导入] [答案] 创建新答案: 学生={}({}), 题目ID={}", student.getUsername(), student.getStudentNumber(), question.getId());
            }
            String formattedContent = formatAnswerContent(content, originalFileName);
            answer.setAnswerText(formattedContent);
            answer.setSubmittedAt(LocalDateTime.now());
            studentAnswerService.save(answer);
        } catch (Exception e) {
            log.error("[批量导入] [答案] 保存异常: 学生={}({}), 题目ID={}, 文件名={}, 异常={}", student.getUsername(), student.getStudentNumber(), question.getId(), originalFileName, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 格式化答案内容，保留原文档格式
     */
    private String formatAnswerContent(String content, String fileName) {
        StringBuilder formatted = new StringBuilder();
        
        // 添加文件信息头部
        formatted.append("【原文件名】: ").append(fileName).append("\n");
        formatted.append("【提交时间】: ").append(LocalDateTime.now()).append("\n");
        formatted.append("【答案内容】:\n");
        formatted.append("=" .repeat(50)).append("\n\n");
        
        // 添加文档内容，保留格式
        formatted.append(content);
        
        return formatted.toString();
    }
    
    /**
     * 验证文件类型
     */
    public boolean isSupportedFileType(String fileName) {
        if (fileName == null) return false;
        
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || 
               lowerName.endsWith(".pdf") || lowerName.endsWith(".txt") ||
               lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
               lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
               lowerName.endsWith(".bmp") || lowerName.endsWith(".webp") ||
               lowerName.endsWith(".rtf") || lowerName.endsWith(".odt");
    }
    
    /**
     * 过滤支持的文件
     */
    public List<MultipartFile> filterSupportedFiles(MultipartFile[] files) {
        List<MultipartFile> supportedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (isSupportedFileType(file.getOriginalFilename())) {
                supportedFiles.add(file);
            } else {
                log.warn("跳过不支持的文件类型: {}", file.getOriginalFilename());
            }
        }
        
        return supportedFiles;
    }
    
    /**
     * 转换为ImportResult格式（兼容现有接口）
     */
    public ImportResult convertToImportResult(ProcessResult processResult) {
        ImportResult importResult = new ImportResult();
        importResult.setSuccess(processResult.getFailedCount() == 0);
        importResult.setTotalFiles(processResult.getTotalFiles());
        importResult.setSuccessCount(processResult.getSuccessCount());
        importResult.setFailedCount(processResult.getFailedCount());
        
        // 转换详细信息
        List<String> details = new ArrayList<>();
        for (FileProcessDetail detail : processResult.getDetails()) {
            String detailStr = String.format("%s: %s (%s - %s)", 
                detail.getFileName(),
                detail.isSuccess() ? "成功" : "失败",
                detail.getStudentName() != null ? detail.getStudentName() : "未知",
                detail.getStudentNumber() != null ? detail.getStudentNumber() : "未知"
            );
            if (!detail.isSuccess() && detail.getErrorMessage() != null) {
                detailStr += " - " + detail.getErrorMessage();
            }
            details.add(detailStr);
        }
        // ImportResult没有setDetails方法，使用其他方式存储错误信息
        List<String> errorMessages = new ArrayList<>();
        for (FileProcessDetail detail : processResult.getDetails()) {
            if (!detail.isSuccess()) {
                errorMessages.add(detail.getFileName() + ": " + detail.getErrorMessage());
            }
        }
        importResult.setErrorMessages(errorMessages);
        
        return importResult;
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
        String email = baseUsername + "@temp.edu";
        int counter = 0;
        
        try {
            while (userRepository.existsByEmail(email)) {
                counter++;
                email = baseUsername + "_" + counter + "@temp.edu";
                
                // 防止无限循环
                if (counter > 1000) {
                    email = baseUsername + "_" + System.currentTimeMillis() + "@temp.edu";
                    break;
                }
            }
        } catch (Exception e) {
            // 如果查询失败，使用时间戳确保唯一性
            log.warn("查询邮箱唯一性失败，使用时间戳: {}", e.getMessage());
            email = baseUsername + "_" + System.currentTimeMillis() + "@temp.edu";
        }
        
        return email;
    }
} 
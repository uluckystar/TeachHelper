package com.teachhelper.service.answer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.teachhelper.service.ai.SpringAIProxyService;
import com.teachhelper.service.ai.AIRequest;
import com.teachhelper.service.ai.AIResponse;
import com.teachhelper.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 文件名解析服务
 * 使用LLM和正则表达式解析文件名中的学生姓名和学号
 */
@Service
public class FileNameParserService {
    
    private static final Logger log = LoggerFactory.getLogger(FileNameParserService.class);
    
    @Autowired
    private SpringAIProxyService aiProxyService;
    
    @Autowired
    private AuthService authService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 学号的常见格式正则表达式
    private static final Pattern[] STUDENT_NUMBER_PATTERNS = {
        Pattern.compile("\\b(\\d{8,12})\\b"),           // 8-12位连续数字
        Pattern.compile("\\b([A-Z]?\\d{7,11})\\b"),     // 可能带字母前缀的学号
        Pattern.compile("\\b(\\d{4}\\d{6,8})\\b")       // 年级+学号格式
    };
    
    // 中文姓名正则表达式
    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
    
    /**
     * 解析结果类
     */
    public static class ParseResult {
        private String studentName;
        private String studentNumber;
        private boolean success;
        private String errorMessage;
        private String parseMethod; // 解析方式：REGEX, LLM, FAILED
        
        public ParseResult() {}
        
        public ParseResult(String studentName, String studentNumber, boolean success, String parseMethod) {
            this.studentName = studentName;
            this.studentNumber = studentNumber;
            this.success = success;
            this.parseMethod = parseMethod;
        }
        
        public static ParseResult success(String studentName, String studentNumber, String parseMethod) {
            return new ParseResult(studentName, studentNumber, true, parseMethod);
        }
        
        public static ParseResult failed(String errorMessage) {
            ParseResult result = new ParseResult();
            result.success = false;
            result.errorMessage = errorMessage;
            result.parseMethod = "FAILED";
            return result;
        }
        
        // Getters and Setters
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
    }
    
    /**
     * 解析文件名，提取学生姓名和学号
     * @param fileName 文件名（含扩展名）
     * @return 解析结果
     */
    public ParseResult parseFileName(String fileName) {
        return parseFileNameWithUserId(fileName, null);
    }
    
    /**
     * 解析文件名，提取学生姓名和学号（指定用户ID） - 直接使用LLM解析
     * @param fileName 文件名（含扩展名）
     * @param userId 用户ID，用于LLM解析
     * @return 解析结果
     */
    public ParseResult parseFileNameWithUserId(String fileName, Long userId) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return ParseResult.failed("文件名为空");
        }
        
        log.info("开始解析文件名: {} (用户ID: {})", fileName, userId);
        
        // 移除文件扩展名
        String nameWithoutExt = removeFileExtension(fileName);
        
        // 直接使用LLM解析，跳过正则表达式
        log.info("使用LLM解析文件名: {}", fileName);
        ParseResult llmResult = parseWithLLM(nameWithoutExt, userId);
        if (llmResult.isSuccess()) {
            log.info("LLM解析成功: {} -> 姓名:{}, 学号:{}", 
                fileName, llmResult.getStudentName(), llmResult.getStudentNumber());
            return llmResult;
        }
        
        // LLM解析失败
        log.warn("LLM解析失败: {}", fileName);
        return ParseResult.failed("无法从文件名中提取学生姓名和学号");
    }
    
    /**
     * 使用正则表达式解析文件名
     */
    private ParseResult parseWithRegex(String nameWithoutExt) {
        String studentNumber = null;
        String studentName = null;
        
        // 提取学号
        for (Pattern pattern : STUDENT_NUMBER_PATTERNS) {
            Matcher matcher = pattern.matcher(nameWithoutExt);
            if (matcher.find()) {
                studentNumber = matcher.group(1);
                break;
            }
        }
        
        // 提取姓名
        Matcher nameMatcher = CHINESE_NAME_PATTERN.matcher(nameWithoutExt);
        if (nameMatcher.find()) {
            studentName = nameMatcher.group();
        }
        
        // 检查结果
        if (studentNumber != null && studentName != null) {
            return ParseResult.success(studentName, studentNumber, "REGEX");
        } else if (studentNumber != null) {
            // 只有学号，尝试从剩余部分提取姓名
            String remaining = nameWithoutExt.replaceAll("\\b" + Pattern.quote(studentNumber) + "\\b", "")
                                           .replaceAll("[\\-_\\s\\(\\)\\[\\]]+", "");
            if (CHINESE_NAME_PATTERN.matcher(remaining).find()) {
                Matcher remainingMatcher = CHINESE_NAME_PATTERN.matcher(remaining);
                if (remainingMatcher.find()) {
                    studentName = remainingMatcher.group();
                    return ParseResult.success(studentName, studentNumber, "REGEX");
                }
            }
        }
        
        return ParseResult.failed("正则表达式无法解析");
    }
    
    /**
     * 使用LLM解析文件名
     */
    private ParseResult parseWithLLM(String nameWithoutExt) {
        return parseWithLLM(nameWithoutExt, null);
    }
    
    /**
     * 使用LLM解析文件名（指定用户ID）
     */
    private ParseResult parseWithLLM(String nameWithoutExt, Long userId) {
        try {
            String prompt = buildLLMPrompt(nameWithoutExt);
            
            AIRequest aiRequest = new AIRequest();
            aiRequest.setPrompt(prompt);
            aiRequest.setRequestType("FILENAME_PARSE");
            aiRequest.setMaxTokens(200);
            aiRequest.setTemperature(0.1); // 低温度，提高一致性
            
            // 使用传入的用户ID，如果没有则尝试获取当前用户
            Long effectiveUserId = userId;
            if (effectiveUserId == null) {
                try {
                    effectiveUserId = authService.getCurrentUser().getId();
                    log.info("[LLM解析] 获取当前用户ID: {}", effectiveUserId);
                } catch (Exception e) {
                    log.warn("[LLM解析] 无法获取当前用户ID: {}", e.getMessage());
                    throw new RuntimeException("无法获取用户ID进行LLM解析: " + e.getMessage());
                }
            } else {
                log.info("[LLM解析] 使用指定用户ID: {}", effectiveUserId);
            }
            
            aiRequest.setUserId(effectiveUserId);
            
            AIResponse response = aiProxyService.chatSync(aiRequest);
            
            if (response.isSuccess()) {
                return parseLLMResponse(response.getContent());
            } else {
                log.warn("LLM调用失败: {}", response.getErrorMessage());
                return ParseResult.failed("LLM调用失败: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("LLM解析异常", e);
            return ParseResult.failed("LLM解析异常: " + e.getMessage());
        }
    }
    
    /**
     * 构建LLM提示词
     */
    private String buildLLMPrompt(String fileName) {
        return String.format("""
            请从以下文件名中提取学生的姓名和学号信息：
            
            文件名：%s
            
            请按照以下JSON格式返回结果：
            {
                "studentName": "学生姓名",
                "studentNumber": "学号"
            }
            
            要求：
            1. 学号通常是8-12位数字，可能带字母前缀
            2. 姓名通常是2-4个中文字符
            3. 如果找到了姓名但没有找到学号，学号可以返回null（系统会自动生成默认学号）
            4. 如果无法确定某个信息，请返回null
            5. 只返回JSON，不要其他解释文字
            
            示例：
            - "张三_20231234.docx" -> {"studentName": "张三", "studentNumber": "20231234"}
            - "20231234-李四.pdf" -> {"studentName": "李四", "studentNumber": "20231234"}
            - "王五20231234作业.doc" -> {"studentName": "王五", "studentNumber": "20231234"}
            - "郑开意.doc" -> {"studentName": "郑开意", "studentNumber": null}
            """, fileName);
    }
    
    /**
     * 解析LLM返回的JSON结果
     */
    private ParseResult parseLLMResponse(String llmResponse) {
        try {
            // 清理响应，提取JSON部分
            String jsonStr = extractJsonFromResponse(llmResponse);
            
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            String studentName = getJsonStringValue(jsonNode, "studentName");
            String studentNumber = getJsonStringValue(jsonNode, "studentNumber");
            
            // 验证结果 - 支持只有姓名的情况
            if (isValidStudentName(studentName)) {
                if (isValidStudentNumber(studentNumber)) {
                    // 姓名和学号都有
                    return ParseResult.success(studentName, studentNumber, "LLM");
                } else {
                    // 只有姓名，生成默认学号
                    String defaultStudentNumber = generateDefaultStudentNumber(studentName);
                    log.info("LLM解析到姓名[{}]但无学号，生成默认学号: {}", studentName, defaultStudentNumber);
                    return ParseResult.success(studentName, defaultStudentNumber, "LLM_NAME_ONLY");
                }
            } else {
                return ParseResult.failed("LLM未能解析到有效的学生姓名: name=" + studentName + ", number=" + studentNumber);
            }
            
        } catch (Exception e) {
            log.warn("解析LLM响应失败: {}", llmResponse, e);
            return ParseResult.failed("解析LLM响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 从响应中提取JSON字符串
     */
    private String extractJsonFromResponse(String response) {
        if (response == null) return "{}";
        
        // 寻找JSON的开始和结束
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response.trim();
    }
    
    /**
     * 安全获取JSON字符串值
     */
    private String getJsonStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return (value == null || value.trim().isEmpty() || "null".equals(value)) ? null : value.trim();
    }
    
    /**
     * 验证学生姓名
     */
    private boolean isValidStudentName(String name) {
        return name != null && 
               name.length() >= 2 && 
               name.length() <= 6 && 
               CHINESE_NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * 验证学号
     */
    private boolean isValidStudentNumber(String number) {
        return number != null && 
               number.matches("^[A-Z]?\\d{7,12}$"); // 可选字母前缀 + 7-12位数字
    }
    
    /**
     * 生成默认学号
     * 格式: NO + 当前时间戳后8位 + 姓名首字符的Unicode后2位
     */
    private String generateDefaultStudentNumber(String studentName) {
        if (studentName == null || studentName.isEmpty()) {
            return "NO" + String.valueOf(System.currentTimeMillis()).substring(5);
        }
        
        // 时间戳后8位
        String timestamp = String.valueOf(System.currentTimeMillis());
        String timestampSuffix = timestamp.substring(Math.max(0, timestamp.length() - 8));
        
        // 姓名首字符的Unicode值后2位
        char firstChar = studentName.charAt(0);
        String unicodeSuffix = String.format("%04d", (int) firstChar).substring(2);
        
        return "NO" + timestampSuffix + unicodeSuffix;
    }
    
    /**
     * 移除文件扩展名
     */
    private String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    /**
     * 批量解析文件名
     */
    public java.util.Map<String, ParseResult> batchParseFileNames(java.util.List<String> fileNames) {
        java.util.Map<String, ParseResult> results = new java.util.HashMap<>();
        
        for (String fileName : fileNames) {
            ParseResult result = parseFileName(fileName);
            results.put(fileName, result);
        }
        
        return results;
    }
} 
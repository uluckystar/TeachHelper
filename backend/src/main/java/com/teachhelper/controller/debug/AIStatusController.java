package com.teachhelper.controller.debug;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.teachhelper.service.answer.LearningAnswerParserService;
import com.teachhelper.dto.request.StudentAnswerImportData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

/**
 * AI 服务状态检查控制器
 */
@RestController
@RequestMapping("/api/debug/ai")
@CrossOrigin(origins = "*")
public class AIStatusController {

    private static final Logger log = LoggerFactory.getLogger(AIStatusController.class);

    @Value("${app.ai.preferred-model:openai}")
    private String preferredModel;

    @Autowired(required = false)
    @Qualifier("openAiChatModel")
    private ChatModel openAiChatModel;

    @Autowired(required = false)
    @Qualifier("ollamaChatModel")
    private ChatModel ollamaChatModel;

    @Autowired
    private LearningAnswerParserService learningAnswerParserService;

    /**
     * 检查 AI 服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkAIStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("preferredModel", preferredModel);
        
        // 检查 OpenAI/DeepSeek 服务 (用于聊天)
        status.put("openAiChatAvailable", openAiChatModel != null);
        if (openAiChatModel != null) {
            try {
                String testResponse = openAiChatModel.call(new Prompt("测试连接，请回复'连接成功'")).getResult().getOutput().getText();
                status.put("openAiChatStatus", "connected");
                status.put("openAiChatTestResponse", testResponse);
            } catch (Exception e) {
                status.put("openAiChatStatus", "error");
                status.put("openAiChatError", e.getMessage());
            }
        }
        
        // 检查 Ollama 服务 (用于本地模型)
        status.put("ollamaChatAvailable", ollamaChatModel != null);
        if (ollamaChatModel != null) {
            try {
                String testResponse = ollamaChatModel.call(new Prompt("Test connection, please reply 'Connected'")).getResult().getOutput().getText();
                status.put("ollamaChatStatus", "connected");
                status.put("ollamaChatTestResponse", testResponse);
            } catch (Exception e) {
                status.put("ollamaChatStatus", "error");
                status.put("ollamaChatError", e.getMessage());
            }
        }
        
        return ResponseEntity.ok(status);
    }

    /**
     * 测试 OpenAI/DeepSeek API
     */
    @PostMapping("/test/openai")
    public ResponseEntity<Map<String, Object>> testOpenAI(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        if (openAiChatModel == null) {
            result.put("error", "OpenAI ChatModel not available");
            return ResponseEntity.badRequest().body(result);
        }
        
        try {
            String message = request.getOrDefault("message", "你好，这是一个测试消息");
            String response = openAiChatModel.call(new Prompt(message)).getResult().getOutput().getText();
            
            result.put("success", true);
            result.put("request", message);
            result.put("response", response);
            result.put("model", "openai/deepseek");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试 Ollama API
     */
    @PostMapping("/test/ollama")
    public ResponseEntity<Map<String, Object>> testOllama(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        if (ollamaChatModel == null) {
            result.put("error", "Ollama ChatModel not available");
            return ResponseEntity.badRequest().body(result);
        }
        
        try {
            String message = request.getOrDefault("message", "Hello, this is a test message");
            String response = ollamaChatModel.call(new Prompt(message)).getResult().getOutput().getText();
            
            result.put("success", true);
            result.put("request", message);
            result.put("response", response);
            result.put("model", "ollama");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 切换首选的 AI 模型（仅用于测试）
     */
    @PostMapping("/switch-model/{modelName}")
    public ResponseEntity<Map<String, Object>> switchModel(@PathVariable String modelName) {
        Map<String, Object> result = new HashMap<>();
        
        // 这里只是返回当前配置，实际切换需要重启应用
        result.put("currentPreferredModel", preferredModel);
        result.put("requestedModel", modelName);
        result.put("note", "To actually switch models, update app.ai.preferred-model in configuration and restart");
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test-regex")
    public ResponseEntity<Map<String, Object>> testRegex(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String testContent = request.getOrDefault("content", "");
            
            // 测试题目分割
            String[] lines = testContent.split("\\n");
            response.put("totalLines", lines.length);
            
            // 测试题号匹配
            java.util.regex.Pattern questionPattern = java.util.regex.Pattern.compile("(\\d+)\\s*\\.");
            java.util.regex.Matcher matcher = questionPattern.matcher(testContent);
            java.util.List<String> foundQuestions = new java.util.ArrayList<>();
            while (matcher.find()) {
                foundQuestions.add("题号" + matcher.group(1) + " 位置" + matcher.start());
            }
            response.put("foundQuestions", foundQuestions);
            
            // 测试答案匹配
            java.util.regex.Pattern answerPattern = java.util.regex.Pattern.compile("学生答案[：:]\\s*([^\\n\\r]*?)(?=正确答案|$)", java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher answerMatcher = answerPattern.matcher(testContent);
            java.util.List<String> foundAnswers = new java.util.ArrayList<>();
            while (answerMatcher.find()) {
                String answer = answerMatcher.group(1).trim();
                foundAnswers.add("答案: \"" + answer + "\"");
            }
            response.put("foundAnswers", foundAnswers);
            
            response.put("success", true);
            
        } catch (Exception e) {
            log.error("正则测试失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/debug-learning-doc")
    public ResponseEntity<Map<String, Object>> debugLearningDoc(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 保存临时文件
            File tempFile = File.createTempFile("debug_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            
            log.info("🔍 开始调试学习通文档解析: {}", file.getOriginalFilename());
            
            // 提取文档文本内容
            String content = extractTextFromDocument(tempFile);
            response.put("documentContent", content.length() > 2000 ? content.substring(0, 2000) + "..." : content);
            response.put("contentLength", content.length());
            
            // 测试原有解析方法
            try {
                StudentAnswerImportData oldResult = learningAnswerParserService.parseLearningAnswerDocument(tempFile);
                response.put("oldParsingResult", oldResult);
                response.put("oldAnswerCount", oldResult != null && oldResult.getAnswers() != null ? oldResult.getAnswers().size() : 0);
            } catch (Exception e) {
                response.put("oldParsingError", e.getMessage());
            }
            
            // 测试新的基于模板解析方法
            try {
                StudentAnswerImportData newResult = learningAnswerParserService.parseStudentAnswersOnlyForTemplate(tempFile, 50);
                response.put("newParsingResult", newResult);
                response.put("newAnswerCount", newResult != null && newResult.getAnswers() != null ? newResult.getAnswers().size() : 0);
            } catch (Exception e) {
                response.put("newParsingError", e.getMessage());
            }
            
            // 清理临时文件
            tempFile.delete();
            
            response.put("success", true);
            
        } catch (Exception e) {
            log.error("调试学习通文档解析失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    private String extractTextFromDocument(File file) {
        try {
            String fileName = file.getName().toLowerCase();
            
            if (fileName.endsWith(".docx")) {
                return extractFromDocx(file);
            } else if (fileName.endsWith(".doc")) {
                return extractFromDoc(file);
            } else {
                return "不支持的文档格式";
            }
        } catch (Exception e) {
            log.error("提取文档内容失败", e);
            return "文档内容提取失败: " + e.getMessage();
        }
    }
    
    private String extractFromDocx(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }
    
    private String extractFromDoc(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis)) {
            
            WordExtractor extractor = new WordExtractor(document);
            return extractor.getText();
        }
    }
}

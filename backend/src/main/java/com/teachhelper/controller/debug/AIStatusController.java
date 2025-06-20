package com.teachhelper.controller.debug;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 服务状态检查控制器
 */
@RestController
@RequestMapping("/api/debug/ai")
@CrossOrigin(origins = "*")
public class AIStatusController {

    @Value("${app.ai.preferred-model:openai}")
    private String preferredModel;

    @Autowired(required = false)
    @Qualifier("openAiChatModel")
    private ChatModel openAiChatModel;

    @Autowired(required = false)
    @Qualifier("ollamaChatModel")
    private ChatModel ollamaChatModel;

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
}

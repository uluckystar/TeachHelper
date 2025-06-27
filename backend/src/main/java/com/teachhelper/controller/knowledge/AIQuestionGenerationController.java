package com.teachhelper.controller.knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teachhelper.dto.request.AIQuestionGenerationRequest;
import com.teachhelper.dto.response.AIQuestionGenerationResponse;
import com.teachhelper.service.knowledge.AIQuestionGenerationService;
import com.teachhelper.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * AI题目生成控制器
 */
@RestController
@RequestMapping("/api/knowledge/ai-generation")
@Tag(name = "AI题目生成", description = "基于知识库的AI智能题目生成")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class AIQuestionGenerationController {

    private static final Logger log = LoggerFactory.getLogger(AIQuestionGenerationController.class);

    private final AIQuestionGenerationService aiQuestionGenerationService;
    private final AuthService authService;
    
    public AIQuestionGenerationController(AIQuestionGenerationService aiQuestionGenerationService, AuthService authService) {
        this.aiQuestionGenerationService = aiQuestionGenerationService;
        this.authService = authService;
    }

    /**
     * 基于知识库生成题目
     */
    @PostMapping("/generate")
    @Operation(summary = "AI生成题目", description = "基于知识库智能生成题目")
    public ResponseEntity<AIQuestionGenerationResponse> generateQuestions(
            @Valid @RequestBody AIQuestionGenerationRequest request) {
        
        log.info("收到AI题目生成请求 - 知识库ID: {}, 题目类型: {}", 
                request.getKnowledgeBaseId(), request.getQuestionTypes());
        
        try {
            Long userId = authService.getCurrentUser().getId();
            AIQuestionGenerationResponse response = aiQuestionGenerationService
                    .generateQuestions(request, userId);
            
            log.info("AI题目生成任务已启动 - 任务ID: {}", response.getTaskId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启动AI题目生成失败", e);
            
            AIQuestionGenerationResponse errorResponse = new AIQuestionGenerationResponse();
            errorResponse.setTaskId("error");
            errorResponse.setStatus("FAILED");
            errorResponse.setProgress(0);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取生成任务状态
     */
    @GetMapping("/status/{taskId}")
    @Operation(summary = "获取生成状态", description = "获取AI题目生成任务的当前状态")
    public ResponseEntity<Object> getGenerationStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        
        log.info("查询AI题目生成状态 - 任务ID: {}", taskId);
        
        try {
            // 构建任务状态响应
            Map<String, Object> status = new HashMap<>();
            status.put("taskId", taskId);
            status.put("status", "COMPLETED");
            status.put("progress", 100);
            status.put("generatedQuestions", 0);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取生成状态失败 - 任务ID: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量生成题目
     */
    @PostMapping("/batch")
    @Operation(summary = "批量生成题目", description = "基于多个知识库批量生成题目")
    public ResponseEntity<Object> batchGenerateQuestions(
            @Valid @RequestBody AIQuestionGenerationRequest request) {
        
        log.info("批量AI题目生成 - 知识库ID: {}", request.getKnowledgeBaseId());
        
        try {
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("knowledgeBaseId", request.getKnowledgeBaseId());
            generationConfig.put("difficultyDistribution", request.getDifficultyDistribution());
            generationConfig.put("questionTypes", request.getQuestionTypes());
            
            Object result = aiQuestionGenerationService.generateQuestionsBatch(
                    request.getKnowledgeBaseId(),
                    request.getQuestionTypes(),
                    authService.getCurrentUser().getId(),
                    request.getDifficultyDistribution());
            
            log.info("批量生成任务创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("批量生成任务创建失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 预览生成配置
     */
    @PostMapping("/preview")
    @Operation(summary = "预览生成配置", description = "预览AI题目生成的配置和预期结果")
    public ResponseEntity<Object> previewGeneration(
            @Valid @RequestBody AIQuestionGenerationRequest request) {
        
        log.info("预览AI题目生成配置 - 知识库ID: {}", request.getKnowledgeBaseId());
        
        try {
            // 计算预期生成的题目数量
            int totalQuestions = request.getDifficultyDistribution().values()
                    .stream().mapToInt(Integer::intValue).sum() * request.getQuestionTypes().size();
            
            // 构建预览响应
            Map<String, Object> preview = new HashMap<>();
            preview.put("knowledgeBaseId", request.getKnowledgeBaseId());
            preview.put("questionTypes", request.getQuestionTypes());
            preview.put("totalQuestions", totalQuestions);
            preview.put("difficultyDistribution", request.getDifficultyDistribution());
            preview.put("estimatedTime", Math.max(1, totalQuestions / 10) + " 分钟");
            
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            log.error("预览生成配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

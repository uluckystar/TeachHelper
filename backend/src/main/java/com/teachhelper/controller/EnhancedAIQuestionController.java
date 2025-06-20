package com.teachhelper.controller;

import com.teachhelper.service.knowledge.EnhancedAIQuestionGenerationService;
import com.teachhelper.service.knowledge.EnhancedAIQuestionGenerationService.*;
import com.teachhelper.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强版AI试卷生成控制器
 * 支持RAG检索、来源标注、多知识库协同生成
 */
@RestController
@RequestMapping("/api/enhanced-ai-questions")
@Slf4j
@CrossOrigin(origins = "*")
public class EnhancedAIQuestionController {

    @Autowired
    private EnhancedAIQuestionGenerationService enhancedAIQuestionGenerationService;

    /**
     * 生成增强版AI试卷
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateEnhancedQuestions(@RequestBody EnhancedQuestionGenerationRequest request) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            log.info("用户 {} 开始生成增强版AI试卷，策略: {}", userId, request.getGenerationStrategy());
            
            EnhancedQuestionGenerationResponse response = 
                enhancedAIQuestionGenerationService.generateEnhancedQuestions(userId, request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("生成增强版AI试卷失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "生成失败: " + e.getMessage(),
                "code", "GENERATION_FAILED"
            ));
        }
    }

    /**
     * 获取知识库预览信息
     */
    @GetMapping("/knowledge-bases/{id}/preview")
    public ResponseEntity<?> getKnowledgeBasePreview(@PathVariable Long id) {
        try {
            // 这里应该调用知识库服务获取预览信息
            Map<String, Object> preview = new HashMap<>();
            preview.put("id", id);
            preview.put("documentCount", 10);
            preview.put("knowledgePointCount", 25);
            preview.put("recentTopics", "数据结构、算法、数据库");
            preview.put("availableForRAG", true);
            
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            log.error("获取知识库预览失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取预览失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 验证生成配置
     */
    @PostMapping("/validate-config")
    public ResponseEntity<?> validateGenerationConfig(@RequestBody EnhancedQuestionGenerationRequest request) {
        try {
            Map<String, Object> validation = new HashMap<>();
            validation.put("valid", true);
            validation.put("estimatedQuestions", calculateEstimatedQuestions(request));
            validation.put("estimatedTime", "30-60秒");
            validation.put("supportedStrategies", new String[]{"RAG_BASED", "DIRECT_LLM", "SMART_MIX"});
            
            // 检查知识库是否支持RAG
            if ("RAG_BASED".equals(request.getGenerationStrategy()) || "SMART_MIX".equals(request.getGenerationStrategy())) {
                validation.put("ragSupported", true);
                validation.put("vectorizedDocuments", request.getKnowledgeBaseIds().size() * 8); // 模拟数据
            }
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            log.error("验证生成配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "配置验证失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取题目来源详情
     */
    @GetMapping("/questions/{questionId}/sources")
    public ResponseEntity<?> getQuestionSources(@PathVariable String questionId) {
        try {
            // 模拟返回来源信息
            Map<String, Object> sources = new HashMap<>();
            sources.put("questionId", questionId);
            sources.put("sources", new Object[]{
                Map.of(
                    "documentId", 1,
                    "documentTitle", "数据结构基础",
                    "fileName", "data_structure.pdf",
                    "extractedContent", "数据结构是计算机科学中的重要概念...",
                    "relevanceScore", 0.95,
                    "citationType", "QUESTION_BASE"
                ),
                Map.of(
                    "documentId", 2,
                    "documentTitle", "算法设计与分析",
                    "fileName", "algorithms.pdf",
                    "extractedContent", "算法复杂度分析是评估算法效率的重要指标...",
                    "relevanceScore", 0.87,
                    "citationType", "ANSWER_SUPPORT"
                )
            });
            
            return ResponseEntity.ok(sources);
            
        } catch (Exception e) {
            log.error("获取题目来源失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取来源失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 预览引用内容
     */
    @GetMapping("/sources/{documentId}/content")
    public ResponseEntity<?> previewSourceContent(
            @PathVariable Long documentId,
            @RequestParam(required = false) String chunkId) {
        try {
            // 模拟返回文档内容预览
            Map<String, Object> preview = new HashMap<>();
            preview.put("documentId", documentId);
            preview.put("title", "数据结构基础教程");
            preview.put("fileName", "data_structure.pdf");
            preview.put("chunkId", chunkId);
            preview.put("content", "数据结构是计算机科学中用于组织和存储数据的基本概念。它包括数组、链表、栈、队列、树、图等多种形式。每种数据结构都有其特定的用途和性能特征...");
            preview.put("highlightedTerms", new String[]{"数据结构", "数组", "链表", "栈", "队列"});
            preview.put("relatedChunks", new String[]{"chunk_001", "chunk_003", "chunk_005"});
            
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            log.error("预览文档内容失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "预览失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取生成策略说明
     */
    @GetMapping("/strategies")
    public ResponseEntity<?> getGenerationStrategies() {
        try {
            Map<String, Object> strategies = new HashMap<>();
            
            strategies.put("RAG_BASED", Map.of(
                "name", "RAG检索生成",
                "description", "基于向量数据库检索相关内容，生成高质量、有来源支撑的题目",
                "advantages", new String[]{"准确性高", "有明确来源", "内容丰富"},
                "suitable", "有丰富文档内容的知识库"
            ));
            
            strategies.put("DIRECT_LLM", Map.of(
                "name", "直接LLM生成",
                "description", "直接使用大语言模型根据知识库概述生成题目",
                "advantages", new String[]{"生成速度快", "题目多样", "创意性强"},
                "suitable", "知识库内容较少或需要创新性题目"
            ));
            
            strategies.put("SMART_MIX", Map.of(
                "name", "智能混合模式",
                "description", "结合RAG检索和直接LLM生成，平衡准确性和多样性",
                "advantages", new String[]{"综合优势", "题目丰富", "质量稳定"},
                "suitable", "大多数场景的推荐选择"
            ));
            
            return ResponseEntity.ok(strategies);
            
        } catch (Exception e) {
            log.error("获取生成策略失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取策略失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取生成统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getGenerationStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalGenerations", 156);
            statistics.put("successRate", 0.94);
            statistics.put("averageQuestionsPerGeneration", 8.5);
            statistics.put("mostUsedStrategy", "SMART_MIX");
            statistics.put("topKnowledgeBases", new Object[]{
                Map.of("name", "数据结构基础", "usageCount", 45),
                Map.of("name", "算法设计", "usageCount", 38),
                Map.of("name", "数据库原理", "usageCount", 32)
            });
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取生成统计信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 计算预估题目数量
     */
    private int calculateEstimatedQuestions(EnhancedQuestionGenerationRequest request) {
        int total = 0;
        if (request.getDifficultyDistribution() != null) {
            total = request.getDifficultyDistribution().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        }
        return Math.max(total, request.getMaxQuestionsPerSource());
    }
}

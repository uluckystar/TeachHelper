package com.teachhelper.controller.knowledge;

import com.teachhelper.dto.request.KnowledgeBaseCreateRequest;
import com.teachhelper.dto.response.KnowledgeBaseResponse;
import com.teachhelper.dto.response.KnowledgePointResponse;
import com.teachhelper.entity.KnowledgeBase;
import com.teachhelper.service.knowledge.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库Controller
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@Tag(name = "知识库管理", description = "知识库相关API")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping
    @Operation(summary = "创建知识库", description = "创建新的知识库")
    public ResponseEntity<KnowledgeBaseResponse> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseCreateRequest request) {
        
        log.info("收到创建知识库请求: {}", request.getName());
        
        try {
            KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(request);
            log.info("知识库创建成功: {}", response.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建知识库失败: {}", request.getName(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取知识库列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取知识库列表", description = "分页获取用户的知识库列表")
    public ResponseEntity<Page<KnowledgeBaseResponse>> getKnowledgeBases(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "学科") @RequestParam(required = false) String subject,
            @Parameter(description = "年级") @RequestParam(required = false) String gradeLevel,
            @Parameter(description = "名称搜索") @RequestParam(required = false) String name,
            @Parameter(description = "排序字段") @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @Parameter(description = "排序方向") @RequestParam(required = false, defaultValue = "desc") String direction) {
        
        log.info("获取知识库列表请求 - page: {}, size: {}, subject: {}, gradeLevel: {}, name: {}, sort: {}, direction: {}", 
                page, size, subject, gradeLevel, name, sort, direction);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeBaseResponse> knowledgeBases = knowledgeBaseService.searchKnowledgeBases(
                pageable, subject, gradeLevel, name, sort, direction);
            
            log.info("成功获取知识库列表，共 {} 条记录", knowledgeBases.getTotalElements());
            return ResponseEntity.ok(knowledgeBases);
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据ID获取知识库详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "根据ID获取知识库详细信息")
    public ResponseEntity<KnowledgeBaseResponse> getKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long id) {
        
        log.info("获取知识库详情请求: {}", id);
        
        try {
            KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBaseById(id);
            log.info("成功获取知识库详情: {}", response.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取知识库详情失败: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库信息")
    public ResponseEntity<KnowledgeBaseResponse> updateKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseCreateRequest request) {
        
        log.info("更新知识库请求: {}, 新名称: {}", id, request.getName());
        
        try {
            KnowledgeBaseResponse response = knowledgeBaseService.updateKnowledgeBase(id, request);
            log.info("知识库更新成功: {}", response.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新知识库失败: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库", description = "删除指定的知识库")
    public ResponseEntity<Void> deleteKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long id) {
        
        log.info("删除知识库请求: {}", id);
        
        try {
            knowledgeBaseService.deleteKnowledgeBase(id);
            log.info("知识库删除成功: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("删除知识库失败: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取所有科目列表
     */
    @GetMapping("/subjects")
    @Operation(summary = "获取科目列表", description = "获取所有可用的科目列表")
    public ResponseEntity<List<String>> getSubjects() {
        
        log.info("获取科目列表请求");
        
        try {
            List<String> subjects = knowledgeBaseService.getAllSubjects();
            log.info("成功获取科目列表，共 {} 个科目", subjects.size());
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("获取科目列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取所有年级列表
     */
    @GetMapping("/grade-levels")
    @Operation(summary = "获取年级列表", description = "获取所有可用的年级列表")
    public ResponseEntity<List<String>> getGradeLevels() {
        
        log.info("获取年级列表请求");
        
        try {
            List<String> gradeLevels = knowledgeBaseService.getAllGradeLevels();
            log.info("成功获取年级列表，共 {} 个年级", gradeLevels.size());
            return ResponseEntity.ok(gradeLevels);
        } catch (Exception e) {
            log.error("获取年级列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取知识库的知识点列表
     */
    @GetMapping("/{id}/knowledge-points")
    @Operation(summary = "获取知识库的知识点列表", description = "获取指定知识库的所有知识点")
    public ResponseEntity<List<KnowledgePointResponse>> getKnowledgePoints(
            @Parameter(description = "知识库ID") @PathVariable Long id) {
        
        log.info("获取知识库 {} 的知识点列表请求", id);
        
        try {
            List<KnowledgePointResponse> knowledgePoints = knowledgeBaseService.getKnowledgePoints(id);
            log.info("成功获取知识库 {} 的知识点列表，共 {} 个知识点", id, knowledgePoints.size());
            return ResponseEntity.ok(knowledgePoints);
        } catch (Exception e) {
            log.error("获取知识库 {} 的知识点列表失败", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 收藏知识库
     */
    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏知识库", description = "收藏指定的知识库")
    public ResponseEntity<Void> favoriteKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long id) {
        
        log.info("收藏知识库请求: {}", id);
        
        try {
            knowledgeBaseService.favoriteKnowledgeBase(id);
            log.info("知识库收藏成功: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("收藏知识库失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("收藏知识库失败: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 取消收藏知识库
     */
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏知识库", description = "取消收藏指定的知识库")
    public ResponseEntity<Void> unfavoriteKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long id) {
        
        log.info("取消收藏知识库请求: {}", id);
        
        try {
            knowledgeBaseService.unfavoriteKnowledgeBase(id);
            log.info("取消收藏知识库成功: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("取消收藏知识库失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("取消收藏知识库失败: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 切换收藏状态
     */
    @PostMapping("/{id}/toggle-favorite")
    @Operation(summary = "切换收藏状态", description = "切换知识库的收藏状态")
    public ResponseEntity<Boolean> toggleFavorite(
            @Parameter(description = "知识库ID") @PathVariable Long id) {
        
        log.info("切换收藏状态请求: {}", id);
        
        try {
            boolean isFavorited = knowledgeBaseService.toggleFavorite(id);
            log.info("切换收藏状态成功: {}, 新状态: {}", id, isFavorited);
            return ResponseEntity.ok(isFavorited);
        } catch (Exception e) {
            log.error("切换收藏状态失败: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取收藏的知识库列表
     */
    @GetMapping("/favorites")
    @Operation(summary = "获取收藏的知识库列表", description = "获取当前用户收藏的所有知识库")
    public ResponseEntity<List<KnowledgeBaseResponse>> getFavoriteKnowledgeBases() {
        
        log.info("获取收藏知识库列表请求");
        
        try {
            List<KnowledgeBaseResponse> favorites = knowledgeBaseService.getUserFavoriteKnowledgeBases();
            log.info("成功获取收藏知识库列表，共 {} 个", favorites.size());
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            log.error("获取收藏知识库列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取知识库相关的题目列表
     */
    @GetMapping("/{id}/questions")
    @Operation(summary = "获取知识库题目", description = "获取指定知识库相关的所有题目")
    public ResponseEntity<Object> getKnowledgeBaseQuestions(
            @Parameter(description = "知识库ID") @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("获取知识库题目请求: knowledgeBaseId={}, page={}, size={}", id, page, size);
        
        try {
            // 获取知识库相关的真实题目数据
            Page<Map<String, Object>> questionsPage = knowledgeBaseService.getKnowledgeBaseQuestions(id, page, size);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", questionsPage.getContent());
            response.put("totalElements", questionsPage.getTotalElements());
            response.put("totalPages", questionsPage.getTotalPages());
            response.put("size", questionsPage.getSize());
            response.put("number", questionsPage.getNumber());
            response.put("first", questionsPage.isFirst());
            response.put("last", questionsPage.isLast());
            
            log.info("成功获取知识库题目，共 {} 道", questionsPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取知识库题目失败: knowledgeBaseId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建模拟题目数据
     */
    private java.util.Map<String, Object> createMockQuestion(Long id, String questionText, String type, String difficulty, boolean isAIGenerated) {
        java.util.Map<String, Object> question = new java.util.HashMap<>();
        question.put("id", id);
        question.put("questionText", questionText);
        question.put("content", questionText);
        question.put("type", type);
        question.put("difficulty", difficulty);
        question.put("isAIGenerated", isAIGenerated);
        question.put("createdAt", java.time.Instant.now().toString());
        question.put("createdBy", isAIGenerated ? "AI生成" : "教师");
        question.put("practiceCount", (int)(Math.random() * 100));
        question.put("correctRate", (int)(Math.random() * 100));
        
        // 根据题型添加不同的数据
        if ("choice".equals(type)) {
            java.util.List<java.util.Map<String, Object>> options = java.util.Arrays.asList(
                createOption("选项A内容", true),
                createOption("选项B内容", false),
                createOption("选项C内容", false),
                createOption("选项D内容", false)
            );
            question.put("options", options);
        } else if ("blank".equals(type)) {
            question.put("correctAnswer", "正确答案");
        } else if ("subjective".equals(type)) {
            java.util.List<java.util.Map<String, Object>> criteria = java.util.Arrays.asList(
                createCriterion(5, "基本概念理解正确"),
                createCriterion(3, "分析过程清晰"),
                createCriterion(2, "结论正确")
            );
            question.put("scoringCriteria", criteria);
        }
        
        return question;
    }

    private java.util.Map<String, Object> createOption(String text, boolean isCorrect) {
        java.util.Map<String, Object> option = new java.util.HashMap<>();
        option.put("text", text);
        option.put("isCorrect", isCorrect);
        return option;
    }

    private java.util.Map<String, Object> createCriterion(int score, String description) {
        java.util.Map<String, Object> criterion = new java.util.HashMap<>();
        criterion.put("score", score);
        criterion.put("description", description);
        return criterion;
    }
    
    /**
     * 兼容性端点：AI生成题目（重定向到新的API）
     * 为了兼容旧的前端调用，提供重定向功能
     */
    @PostMapping("/{id}/questions/generate")
    @Operation(summary = "AI生成题目（兼容性端点）", description = "兼容旧API，重定向到新的AI题目生成端点")
    public ResponseEntity<Object> generateQuestionsCompat(
            @Parameter(description = "知识库ID") @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        log.info("收到兼容性AI题目生成请求 - 知识库ID: {}", id);
        
        try {
            // 构建重定向响应，告知前端使用新的API端点
            Map<String, Object> response = new HashMap<>();
            response.put("message", "请使用新的API端点");
            response.put("newEndpoint", "/api/knowledge/ai-generation/generate");
            response.put("status", "REDIRECT");
            response.put("knowledgeBaseId", id);
            
            return ResponseEntity.status(302).body(response);
        } catch (Exception e) {
            log.error("处理兼容性AI题目生成请求失败: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 调试：获取用户的所有知识库（无条件查询）
     */
    @GetMapping("/debug/all")
    @Operation(summary = "调试用：获取所有知识库", description = "调试用，获取当前用户的所有知识库")
    public ResponseEntity<Page<KnowledgeBaseResponse>> getAllKnowledgeBasesForDebug(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        
        log.info("调试：获取所有知识库请求 - page: {}, size: {}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeBaseResponse> knowledgeBases = knowledgeBaseService.getUserKnowledgeBases(pageable);
            
            log.info("调试：成功获取知识库列表，共 {} 条记录", knowledgeBases.getTotalElements());
            return ResponseEntity.ok(knowledgeBases);
        } catch (Exception e) {
            log.error("调试：获取知识库列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据ID列表批量查询知识库
     */
    @PostMapping("/by-ids")
    @Operation(summary = "根据ID列表批量查询知识库", description = "用于向量搜索后获取相关知识库详情")
    public ResponseEntity<List<KnowledgeBaseResponse>> getKnowledgeBasesByIds(@RequestBody List<Long> ids) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.findByIds(ids);
        // 转换为响应格式，保持与其他API的一致性
        List<KnowledgeBaseResponse> responses = knowledgeBases.stream()
                .map(kb -> knowledgeBaseService.convertToResponse(kb))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

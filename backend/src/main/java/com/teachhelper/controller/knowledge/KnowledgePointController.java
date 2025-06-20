package com.teachhelper.controller.knowledge;

import com.teachhelper.dto.response.KnowledgePointResponse;
import com.teachhelper.service.knowledge.KnowledgePointService;
import com.teachhelper.service.knowledge.VectorStoreService;
import com.teachhelper.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识点管理控制器
 */
@RestController
@RequestMapping("/api/knowledge/points")
@Tag(name = "知识点管理", description = "知识点的查询、预览、管理等操作")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;
    private final VectorStoreService vectorStoreService;
    private final AuthService authService;

    /**
     * 获取知识库的知识点列表
     */
    @GetMapping
    @Operation(summary = "获取知识点列表", description = "获取指定知识库的知识点列表")
    public ResponseEntity<Page<KnowledgePointResponse>> getKnowledgePoints(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeBaseId,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "难度级别") @RequestParam(required = false) String difficulty,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
        
        log.info("获取知识点列表 - 知识库ID: {}, 页码: {}, 大小: {}", knowledgeBaseId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgePointResponse> knowledgePoints = knowledgePointService
                    .getKnowledgePointsByKnowledgeBase(knowledgeBaseId, pageable, difficulty, keyword);
            
            log.info("成功获取知识点列表，共 {} 条记录", knowledgePoints.getTotalElements());
            return ResponseEntity.ok(knowledgePoints);
        } catch (Exception e) {
            log.error("获取知识点列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取知识点详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取知识点详情", description = "获取指定知识点的详细信息")
    public ResponseEntity<KnowledgePointResponse> getKnowledgePoint(
            @Parameter(description = "知识点ID") @PathVariable Long id) {
        
        log.info("获取知识点详情 - ID: {}", id);
        
        try {
            KnowledgePointResponse knowledgePoint = knowledgePointService.getKnowledgePointById(id);
            return ResponseEntity.ok(knowledgePoint);
        } catch (Exception e) {
            log.error("获取知识点详情失败 - ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 搜索相关知识点
     */
    @GetMapping("/search")
    @Operation(summary = "搜索相关知识点", description = "基于向量搜索找到相关的知识点")
    public ResponseEntity<List<Object>> searchRelatedKnowledgePoints(
            @Parameter(description = "搜索查询") @RequestParam String query,
            @Parameter(description = "知识库ID") @RequestParam(required = false) Long knowledgeBaseId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("搜索相关知识点 - 查询: {}, 知识库ID: {}", query, knowledgeBaseId);
        
        try {
            List<VectorStoreService.RetrievedDocument> results = vectorStoreService
                    .searchSimilarDocuments(query, limit);
            
            // 转换为前端需要的格式
            List<Object> response = results.stream()
                    .map(doc -> Map.of(
                        "id", doc.getId(),
                        "title", doc.getMetadata().get("title"),
                        "content", doc.getContent(),
                        "similarity", doc.getDistance(),
                        "source", doc.getMetadata().get("fileName")
                    ))
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("搜索相关知识点失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取知识点统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取知识点统计", description = "获取知识库的知识点统计信息")
    public ResponseEntity<Map<String, Object>> getKnowledgePointStatistics(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeBaseId) {
        
        log.info("获取知识点统计 - 知识库ID: {}", knowledgeBaseId);
        
        try {
            Map<String, Object> statistics = knowledgePointService.getKnowledgePointStatistics(knowledgeBaseId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取知识点统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取知识点树形结构
     */
    @GetMapping("/tree")
    @Operation(summary = "获取知识点树", description = "获取知识库的知识点树形结构")
    public ResponseEntity<List<Object>> getKnowledgePointTree(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeBaseId) {
        
        log.info("获取知识点树 - 知识库ID: {}", knowledgeBaseId);
        
        try {
            List<Object> tree = knowledgePointService.getKnowledgePointTree(knowledgeBaseId);
            return ResponseEntity.ok(tree);
        } catch (Exception e) {
            log.error("获取知识点树失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取相关内容片段
     */
    @GetMapping("/{id}/related-content")
    @Operation(summary = "获取相关内容", description = "获取知识点的相关文档内容片段")
    public ResponseEntity<List<Object>> getRelatedContent(
            @Parameter(description = "知识点ID") @PathVariable Long id) {
        
        log.info("获取相关内容 - 知识点ID: {}", id);
        
        try {
            List<Object> relatedContent = knowledgePointService.getRelatedContent(id);
            return ResponseEntity.ok(relatedContent);
        } catch (Exception e) {
            log.error("获取相关内容失败 - 知识点ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

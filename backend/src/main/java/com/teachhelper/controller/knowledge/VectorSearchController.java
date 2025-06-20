package com.teachhelper.controller.knowledge;

import com.teachhelper.dto.request.VectorSearchRequest;
import com.teachhelper.dto.response.VectorSearchResponse;
import com.teachhelper.service.knowledge.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 向量搜索Controller
 */
@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "向量搜索", description = "智能向量搜索相关API")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;

    /**
     * 向量搜索
     */
    @PostMapping("/vector-search")
    @Operation(summary = "向量搜索", description = "使用AI向量搜索查找相关内容")
    public ResponseEntity<VectorSearchResponse> vectorSearch(
            @Valid @RequestBody VectorSearchRequest request) {
        
        log.info("收到向量搜索请求: query={}, threshold={}", 
                request.getQuery(), request.getSimilarityThreshold());
        
        try {
            VectorSearchResponse response = vectorSearchService.search(request);
            log.info("向量搜索完成，找到 {} 个结果", response.getResults().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

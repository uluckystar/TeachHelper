package com.teachhelper.controller.metadata;

import com.teachhelper.dto.request.SubjectGradeMappingRequest;
import com.teachhelper.dto.response.SubjectGradeMappingResponse;
import com.teachhelper.service.metadata.SubjectGradeMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学科年级关联管理控制器
 */
@RestController
@RequestMapping("/api/metadata/subject-grade-mappings")
@Tag(name = "学科年级关联管理", description = "学科与年级关联关系的管理")
@RequiredArgsConstructor
@Slf4j
public class SubjectGradeMappingController {
    
    private final SubjectGradeMappingService subjectGradeMappingService;
    
    /**
     * 创建学科年级关联
     */
    @PostMapping
    @Operation(summary = "创建学科年级关联", description = "创建学科与年级的关联关系")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectGradeMappingResponse> createMapping(
            @Valid @RequestBody SubjectGradeMappingRequest request) {
        
        log.info("Creating subject-grade mapping: subjectId={}, gradeLevelId={}", 
            request.getSubjectId(), request.getGradeLevelId());
        
        try {
            SubjectGradeMappingResponse response = subjectGradeMappingService.createMapping(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create mapping: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating subject-grade mapping", e);
            throw new RuntimeException("创建学科年级关联失败");
        }
    }
    
    /**
     * 批量创建学科年级关联
     */
    @PostMapping("/batch")
    @Operation(summary = "批量创建学科年级关联", description = "批量创建学科与年级的关联关系")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubjectGradeMappingResponse>> createMappings(
            @Valid @RequestBody List<SubjectGradeMappingRequest> requests) {
        
        log.info("Creating {} subject-grade mappings", requests.size());
        
        try {
            List<SubjectGradeMappingResponse> responses = subjectGradeMappingService.createMappings(requests);
            return ResponseEntity.status(HttpStatus.CREATED).body(responses);
        } catch (Exception e) {
            log.error("Error creating subject-grade mappings", e);
            throw new RuntimeException("批量创建学科年级关联失败");
        }
    }
    
    /**
     * 更新学科年级关联
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新学科年级关联", description = "更新学科与年级的关联关系")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectGradeMappingResponse> updateMapping(
            @Parameter(description = "关联ID") @PathVariable Long id,
            @Valid @RequestBody SubjectGradeMappingRequest request) {
        
        log.info("Updating subject-grade mapping: {}", id);
        
        try {
            SubjectGradeMappingResponse response = subjectGradeMappingService.updateMapping(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating subject-grade mapping: {}", id, e);
            throw new RuntimeException("更新学科年级关联失败");
        }
    }
    
    /**
     * 删除学科年级关联
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学科年级关联", description = "删除学科与年级的关联关系")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMapping(
            @Parameter(description = "关联ID") @PathVariable Long id) {
        
        log.info("Deleting subject-grade mapping: {}", id);
        
        try {
            subjectGradeMappingService.deleteMapping(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting subject-grade mapping: {}", id, e);
            throw new RuntimeException("删除学科年级关联失败");
        }
    }
    
    /**
     * 获取学科的年级关联列表
     */
    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "获取学科的年级关联", description = "获取指定学科关联的年级列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<SubjectGradeMappingResponse>> getMappingsBySubject(
            @Parameter(description = "学科ID") @PathVariable Long subjectId) {
        
        log.info("Getting grade mappings for subject: {}", subjectId);
        
        try {
            List<SubjectGradeMappingResponse> mappings = subjectGradeMappingService.getMappingsBySubject(subjectId);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            log.error("Error getting mappings for subject: {}", subjectId, e);
            throw new RuntimeException("获取学科年级关联失败");
        }
    }
    
    /**
     * 获取年级的学科关联列表
     */
    @GetMapping("/grade-level/{gradeLevelId}")
    @Operation(summary = "获取年级的学科关联", description = "获取指定年级关联的学科列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<SubjectGradeMappingResponse>> getMappingsByGradeLevel(
            @Parameter(description = "年级ID") @PathVariable Long gradeLevelId) {
        
        log.info("Getting subject mappings for grade level: {}", gradeLevelId);
        
        try {
            List<SubjectGradeMappingResponse> mappings = subjectGradeMappingService.getMappingsByGradeLevel(gradeLevelId);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            log.error("Error getting mappings for grade level: {}", gradeLevelId, e);
            throw new RuntimeException("获取年级学科关联失败");
        }
    }
    
    /**
     * 为学科设置适用年级
     */
    @PutMapping("/subject/{subjectId}/grade-levels")
    @Operation(summary = "设置学科适用年级", description = "为指定学科设置适用的年级列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubjectGradeMappingResponse>> setSubjectGradeLevels(
            @Parameter(description = "学科ID") @PathVariable Long subjectId,
            @Valid @RequestBody List<SubjectGradeMappingRequest> requests) {
        
        log.info("Setting grade levels for subject: {}", subjectId);
        
        try {
            List<SubjectGradeMappingResponse> responses = subjectGradeMappingService.setSubjectGradeLevels(subjectId, requests);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error setting grade levels for subject: {}", subjectId, e);
            throw new RuntimeException("设置学科适用年级失败");
        }
    }
}

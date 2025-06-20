package com.teachhelper.controller.metadata;

import com.teachhelper.dto.request.SubjectRequest;
import com.teachhelper.dto.response.SubjectResponse;
import com.teachhelper.service.metadata.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学科管理控制器
 */
@RestController
@RequestMapping("/api/metadata/subjects")
@Tag(name = "学科管理", description = "学科的增删改查管理")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {
    
    private final SubjectService subjectService;
    
    /**
     * 创建学科
     */
    @PostMapping
    @Operation(summary = "创建学科", description = "创建新的学科")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody SubjectRequest request) {
        
        log.info("Creating subject: {}", request.getName());
        
        try {
            SubjectResponse response = subjectService.createSubject(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create subject: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating subject", e);
            throw new RuntimeException("创建学科失败");
        }
    }
    
    /**
     * 更新学科
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新学科", description = "更新指定学科的信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> updateSubject(
            @Parameter(description = "学科ID") @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        
        log.info("Updating subject: {}", id);
        
        try {
            SubjectResponse response = subjectService.updateSubject(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update subject: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating subject: {}", id, e);
            throw new RuntimeException("更新学科失败");
        }
    }
    
    /**
     * 删除学科
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学科", description = "删除指定的学科")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubject(
            @Parameter(description = "学科ID") @PathVariable Long id) {
        
        log.info("Deleting subject: {}", id);
        
        try {
            subjectService.deleteSubject(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Failed to delete subject: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting subject: {}", id, e);
            throw new RuntimeException("删除学科失败");
        }
    }
    
    /**
     * 根据ID获取学科
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取学科详情", description = "根据ID获取学科详细信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<SubjectResponse> getSubjectById(
            @Parameter(description = "学科ID") @PathVariable Long id) {
        
        log.info("Getting subject: {}", id);
        
        try {
            SubjectResponse response = subjectService.getSubjectById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting subject: {}", id, e);
            throw new RuntimeException("获取学科失败");
        }
    }
    
    /**
     * 分页获取所有学科
     */
    @GetMapping
    @Operation(summary = "获取学科列表", description = "分页获取所有激活的学科")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Page<SubjectResponse>> getAllSubjects(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting all subjects - page: {}, size: {}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SubjectResponse> subjects = subjectService.getAllSubjects(pageable);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("Error getting subjects", e);
            throw new RuntimeException("获取学科列表失败");
        }
    }
    
    /**
     * 获取所有学科列表（不分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有学科", description = "获取所有激活的学科列表，不分页")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<SubjectResponse>> getAllSubjectsList() {
        
        log.info("Getting all subjects list");
        
        try {
            List<SubjectResponse> subjects = subjectService.getAllSubjectsList();
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("Error getting subjects list", e);
            throw new RuntimeException("获取学科列表失败");
        }
    }
    
    /**
     * 获取所有学科名称
     */
    @GetMapping("/names")
    @Operation(summary = "获取学科名称列表", description = "获取所有激活学科的名称列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<List<String>> getAllSubjectNames() {
        
        log.info("Getting all subject names");
        
        try {
            List<String> names = subjectService.getAllSubjectNames();
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            log.error("Error getting subject names", e);
            throw new RuntimeException("获取学科名称失败");
        }
    }

    /**
     * 教师快速创建学科（简化版本）
     */
    @PostMapping("/teacher-create")
    @Operation(summary = "教师创建学科", description = "教师快速创建学科，用于知识库管理等场景")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<SubjectResponse> teacherCreateSubject(
            @Valid @RequestBody SubjectRequest request) {
        
        log.info("Teacher creating subject: {}", request.getName());
        
        try {
            SubjectResponse response = subjectService.createSubject(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create subject: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating subject", e);
            throw new RuntimeException("创建学科失败");
        }
    }
}

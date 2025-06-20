package com.teachhelper.controller.metadata;

import com.teachhelper.dto.request.GradeLevelRequest;
import com.teachhelper.dto.response.GradeLevelResponse;
import com.teachhelper.service.metadata.GradeLevelService;
import com.teachhelper.service.metadata.GradeSubjectRecommendationService;
import com.teachhelper.service.metadata.GradeSubjectRecommendationService;
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
 * 年级管理控制器
 */
@RestController
@RequestMapping("/api/metadata/grade-levels")
@Tag(name = "年级管理", description = "年级的增删改查管理")
@RequiredArgsConstructor
@Slf4j
public class GradeLevelController {
    
    private final GradeLevelService gradeLevelService;
    private final GradeSubjectRecommendationService gradeSubjectRecommendationService;
    
    /**
     * 创建年级
     */
    @PostMapping
    @Operation(summary = "创建年级", description = "创建新的年级")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GradeLevelResponse> createGradeLevel(
            @Valid @RequestBody GradeLevelRequest request) {
        
        log.info("Creating grade level: {}", request.getName());
        
        try {
            GradeLevelResponse response = gradeLevelService.createGradeLevel(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create grade level: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating grade level", e);
            throw new RuntimeException("创建年级失败");
        }
    }
    
    /**
     * 更新年级
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新年级", description = "更新指定年级的信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GradeLevelResponse> updateGradeLevel(
            @Parameter(description = "年级ID") @PathVariable Long id,
            @Valid @RequestBody GradeLevelRequest request) {
        
        log.info("Updating grade level: {}", id);
        
        try {
            GradeLevelResponse response = gradeLevelService.updateGradeLevel(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update grade level: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating grade level: {}", id, e);
            throw new RuntimeException("更新年级失败");
        }
    }
    
    /**
     * 删除年级
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除年级", description = "删除指定的年级")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGradeLevel(
            @Parameter(description = "年级ID") @PathVariable Long id) {
        
        log.info("Deleting grade level: {}", id);
        
        try {
            gradeLevelService.deleteGradeLevel(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Failed to delete grade level: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting grade level: {}", id, e);
            throw new RuntimeException("删除年级失败");
        }
    }
    
    /**
     * 根据ID获取年级
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取年级详情", description = "根据ID获取年级详细信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<GradeLevelResponse> getGradeLevelById(
            @Parameter(description = "年级ID") @PathVariable Long id) {
        
        log.info("Getting grade level: {}", id);
        
        try {
            GradeLevelResponse response = gradeLevelService.getGradeLevelById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting grade level: {}", id, e);
            throw new RuntimeException("获取年级失败");
        }
    }
    
    /**
     * 分页获取所有年级
     */
    @GetMapping
    @Operation(summary = "获取年级列表", description = "分页获取所有激活的年级")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Page<GradeLevelResponse>> getAllGradeLevels(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting all grade levels - page: {}, size: {}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<GradeLevelResponse> gradeLevels = gradeLevelService.getAllGradeLevels(pageable);
            return ResponseEntity.ok(gradeLevels);
        } catch (Exception e) {
            log.error("Error getting grade levels", e);
            throw new RuntimeException("获取年级列表失败");
        }
    }
    
    /**
     * 获取所有年级列表（不分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有年级", description = "获取所有激活的年级列表，不分页")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<GradeLevelResponse>> getAllGradeLevelsList() {
        
        log.info("Getting all grade levels list");
        
        try {
            List<GradeLevelResponse> gradeLevels = gradeLevelService.getAllGradeLevelsList();
            return ResponseEntity.ok(gradeLevels);
        } catch (Exception e) {
            log.error("Error getting grade levels list", e);
            throw new RuntimeException("获取年级列表失败");
        }
    }
    
    /**
     * 根据类别获取年级列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据类别获取年级", description = "根据类别获取年级列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<GradeLevelResponse>> getGradeLevelsByCategory(
            @Parameter(description = "年级类别") @PathVariable String category) {
        
        log.info("Getting grade levels by category: {}", category);
        
        try {
            List<GradeLevelResponse> gradeLevels = gradeLevelService.getGradeLevelsByCategory(category);
            return ResponseEntity.ok(gradeLevels);
        } catch (Exception e) {
            log.error("Error getting grade levels by category", e);
            throw new RuntimeException("获取年级列表失败");
        }
    }
    
    /**
     * 获取所有年级名称
     */
    @GetMapping("/names")
    @Operation(summary = "获取年级名称列表", description = "获取所有激活年级的名称列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<List<String>> getAllGradeLevelNames() {
        
        log.info("Getting all grade level names");
        
        try {
            List<String> names = gradeLevelService.getAllGradeLevelNames();
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            log.error("Error getting grade level names", e);
            throw new RuntimeException("获取年级名称失败");
        }
    }
    
    /**
     * 获取所有年级类别
     */
    @GetMapping("/categories")
    @Operation(summary = "获取年级类别列表", description = "获取所有激活的年级类别")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<String>> getAllCategories() {
        
        log.info("Getting all grade level categories");
        
        try {
            List<String> categories = gradeLevelService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error getting grade level categories", e);
            throw new RuntimeException("获取年级类别失败");
        }
    }
    
    /**
     * 搜索年级
     */
    @GetMapping("/search")
    @Operation(summary = "搜索年级", description = "根据关键词搜索年级")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<GradeLevelResponse>> searchGradeLevels(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        
        log.info("Searching grade levels with keyword: {}", keyword);
        
        try {
            List<GradeLevelResponse> gradeLevels = gradeLevelService.searchGradeLevels(keyword);
            return ResponseEntity.ok(gradeLevels);
        } catch (Exception e) {
            log.error("Error searching grade levels", e);
            throw new RuntimeException("搜索年级失败");
        }
    }
    
    /**
     * 根据年级获取推荐学科
     */
    @GetMapping("/{gradeLevel}/recommended-subjects")
    @Operation(summary = "根据年级获取推荐学科", description = "根据年级获取推荐的学科列表，按重要性排序")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<List<String>> getRecommendedSubjectsByGrade(
            @Parameter(description = "年级名称") @PathVariable String gradeLevel) {
        
        log.info("Getting recommended subjects for grade: {}", gradeLevel);
        
        try {
            List<String> subjects = gradeSubjectRecommendationService.getRecommendedSubjectsByGrade(gradeLevel);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("Error getting recommended subjects for grade: {}", gradeLevel, e);
            throw new RuntimeException("获取推荐学科失败");
        }
    }
    
    /**
     * 获取年级类别
     */
    @GetMapping("/{gradeLevel}/category")
    @Operation(summary = "获取年级类别", description = "获取指定年级的类别信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<String> getGradeCategory(
            @Parameter(description = "年级名称") @PathVariable String gradeLevel) {
        
        log.info("Getting category for grade: {}", gradeLevel);
        
        try {
            String category = gradeSubjectRecommendationService.getGradeCategory(gradeLevel);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            log.error("Error getting category for grade: {}", gradeLevel, e);
            throw new RuntimeException("获取年级类别失败");
        }
    }
}

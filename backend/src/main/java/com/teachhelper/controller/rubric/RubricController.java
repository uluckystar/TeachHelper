package com.teachhelper.controller.rubric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.dto.request.RubricCreateRequest;
import com.teachhelper.dto.request.RubricStatusRequest;
import com.teachhelper.dto.response.RubricResponse;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.service.RubricService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/rubrics")
@Tag(name = "评估标准管理", description = "系统级评估标准的创建、查询、修改、删除等操作")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class RubricController {
    
    @Autowired
    private RubricService rubricService;
    
    @GetMapping
    @Operation(summary = "获取所有评估标准", description = "获取当前用户的所有系统级评估标准")
    public ResponseEntity<List<RubricResponse>> getAllRubrics() {
        List<RubricResponse> responses = rubricService.getAllRubricResponses();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取评估标准详情", description = "根据ID获取评估标准详细信息")
    public ResponseEntity<RubricResponse> getRubricById(@PathVariable Long id) {
        RubricResponse response = rubricService.getRubricResponseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评分标准不存在"));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "创建评估标准", description = "创建新的系统级评估标准")
    public ResponseEntity<RubricResponse> createRubric(@Valid @RequestBody RubricCreateRequest request) {
        RubricResponse response = rubricService.createRubricResponse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新评估标准", description = "更新评估标准信息")
    public ResponseEntity<RubricResponse> updateRubric(
            @PathVariable Long id,
            @Valid @RequestBody RubricCreateRequest request) {
        RubricResponse response = rubricService.updateRubricResponse(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "切换评估标准状态", description = "启用或禁用评估标准")
    public ResponseEntity<RubricResponse> toggleRubricStatus(
            @PathVariable Long id,
            @Valid @RequestBody RubricStatusRequest request) {
        RubricResponse response = rubricService.toggleRubricStatusResponse(id, request.isActive());
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评估标准", description = "删除指定的评估标准")
    public ResponseEntity<Void> deleteRubric(@PathVariable Long id) {
        rubricService.deleteRubric(id);
        return ResponseEntity.noContent().build();
    }
}

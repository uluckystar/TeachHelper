package com.teachhelper.controller.classroom;

import java.util.List;
import java.util.stream.Collectors;

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

import com.teachhelper.dto.request.ClassroomCreateRequest;
import com.teachhelper.dto.request.JoinClassroomRequest;
import com.teachhelper.dto.response.ClassroomResponse;
import com.teachhelper.entity.Classroom;
import com.teachhelper.service.classroom.ClassroomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classrooms")
@Tag(name = "班级管理", description = "班级的创建、查询、修改、删除等操作")
public class ClassroomController {
    
    @Autowired
    private ClassroomService classroomService;
    
    @PostMapping
    @Operation(summary = "创建班级", description = "教师创建新的班级")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> createClassroom(@Valid @RequestBody ClassroomCreateRequest request) {
        try {
            Classroom classroom = classroomService.createClassroom(request);
            ClassroomResponse response = classroomService.convertToResponse(classroom);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情", description = "根据ID获取班级详细信息")
    public ResponseEntity<ClassroomResponse> getClassroom(@PathVariable Long id) {
        try {
            Classroom classroom = classroomService.getClassroomById(id);
            ClassroomResponse response = classroomService.convertToResponse(classroom);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    @Operation(summary = "获取班级列表", description = "根据用户角色获取班级列表：学生看到加入的班级，教师看到创建的班级，管理员看到所有班级")
    public ResponseEntity<List<ClassroomResponse>> getAllClassrooms() {
        try {
            List<Classroom> classrooms = classroomService.getClassroomsByCurrentUser();
            List<ClassroomResponse> responses = classrooms.stream()
                .map(classroomService::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/join")
    @Operation(summary = "加入班级", description = "学生通过班级代码加入班级")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ClassroomResponse> joinClassroom(@Valid @RequestBody JoinClassroomRequest request) {
        try {
            Classroom classroom = classroomService.joinClassroom(request.getClassCode());
            ClassroomResponse response = classroomService.convertToResponse(classroom);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/leave")
    @Operation(summary = "离开班级", description = "学生离开指定班级")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> leaveClassroom(@PathVariable Long id) {
        try {
            classroomService.leaveClassroom(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{classroomId}/students/{studentId}")
    @Operation(summary = "移除学生", description = "教师从班级中移除指定学生")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Void> removeStudent(@PathVariable Long classroomId, @PathVariable Long studentId) {
        try {
            classroomService.removeStudentFromClassroom(classroomId, studentId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新班级", description = "更新班级信息")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> updateClassroom(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomCreateRequest request) {
        try {
            Classroom classroom = classroomService.updateClassroom(id, request);
            ClassroomResponse response = classroomService.convertToResponse(classroom);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级", description = "删除指定班级")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Void> deleteClassroom(@PathVariable Long id) {
        try {
            classroomService.deleteClassroom(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/code/{classCode}")
    @Operation(summary = "根据班级代码获取班级信息", description = "学生加入前预览班级信息")
    public ResponseEntity<ClassroomResponse> getClassroomByCode(@PathVariable String classCode) {
        try {
            Classroom classroom = classroomService.getClassroomByCode(classCode);
            ClassroomResponse response = classroomService.convertToResponse(classroom);
            // 不显示学生列表，只显示基本信息
            response.setStudents(null);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

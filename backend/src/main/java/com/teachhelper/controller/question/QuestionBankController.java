package com.teachhelper.controller.question;

import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionBank;
import com.teachhelper.service.question.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 题目库管理控制器
 */
@RestController
@RequestMapping("/api/question-banks")
@Tag(name = "题目库管理", description = "题目库的创建、管理、题目归类等操作")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @PostMapping
    @Operation(summary = "创建题目库", description = "创建新的题目库")
    public ResponseEntity<QuestionBank> createQuestionBank(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String subject = (String) request.get("subject");
        String gradeLevel = (String) request.get("gradeLevel");
        Boolean isPublic = (Boolean) request.get("isPublic");

        QuestionBank questionBank = questionBankService.createQuestionBank(name, description, subject, gradeLevel, isPublic);
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBank);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的题目库列表", description = "获取当前用户创建的题目库列表")
    public ResponseEntity<Page<QuestionBank>> getMyQuestionBanks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionBank> questionBanks = questionBankService.getUserQuestionBanks(pageable);
        return ResponseEntity.ok(questionBanks);
    }

    @GetMapping
    @Operation(summary = "获取可访问的题目库列表", description = "获取当前用户可访问的题目库列表（包括公开的）")
    public ResponseEntity<Page<QuestionBank>> getAccessibleQuestionBanks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionBank> questionBanks = questionBankService.getAccessibleQuestionBanks(pageable);
        return ResponseEntity.ok(questionBanks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题目库详情", description = "根据ID获取题目库详细信息")
    public ResponseEntity<QuestionBank> getQuestionBank(@PathVariable Long id) {
        QuestionBank questionBank = questionBankService.getQuestionBankById(id);
        return ResponseEntity.ok(questionBank);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目库", description = "更新题目库信息")
    public ResponseEntity<QuestionBank> updateQuestionBank(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String subject = (String) request.get("subject");
        String gradeLevel = (String) request.get("gradeLevel");
        Boolean isPublic = (Boolean) request.get("isPublic");

        QuestionBank questionBank = questionBankService.updateQuestionBank(id, name, description, subject, gradeLevel, isPublic);
        return ResponseEntity.ok(questionBank);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目库", description = "删除指定题目库")
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable Long id) {
        questionBankService.deleteQuestionBank(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/questions")
    @Operation(summary = "获取题目库中的题目", description = "获取指定题目库中的所有题目")
    public ResponseEntity<Page<Question>> getQuestionBankQuestions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questions = questionBankService.getQuestionBankQuestions(id, pageable);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/{id}/questions/{questionId}")
    @Operation(summary = "添加题目到题目库", description = "将指定题目添加到题目库中")
    public ResponseEntity<Void> addQuestionToBank(
            @PathVariable Long id,
            @PathVariable Long questionId) {
        
        questionBankService.addQuestionToBank(id, questionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(summary = "从题目库移除题目", description = "从题目库中移除指定题目")
    public ResponseEntity<Void> removeQuestionFromBank(
            @PathVariable Long id,
            @PathVariable Long questionId) {
        
        questionBankService.removeQuestionFromBank(id, questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取题目库统计信息", description = "获取题目库的题目数量等统计信息")
    public ResponseEntity<Map<String, Object>> getQuestionBankStatistics(@PathVariable Long id) {
        long questionCount = questionBankService.getQuestionCount(id);
        
        Map<String, Object> statistics = Map.of(
            "questionCount", questionCount
        );
        
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/metadata/subjects")
    @Operation(summary = "获取所有科目", description = "获取系统中所有不重复的科目列表")
    public ResponseEntity<List<String>> getAllSubjects() {
        List<String> subjects = questionBankService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/metadata/grade-levels")
    @Operation(summary = "获取所有年级", description = "获取系统中所有不重复的年级列表")
    public ResponseEntity<List<String>> getAllGradeLevels() {
        List<String> gradeLevels = questionBankService.getAllGradeLevels();
        return ResponseEntity.ok(gradeLevels);
    }
}

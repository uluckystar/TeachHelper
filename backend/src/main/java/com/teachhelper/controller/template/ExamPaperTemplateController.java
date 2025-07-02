package com.teachhelper.controller.template;

import com.teachhelper.dto.request.ExamPaperTemplateRequest;
import com.teachhelper.dto.response.ExamPaperTemplateResponse;
import com.teachhelper.dto.response.ExamResponse;
import com.teachhelper.dto.request.ExamPaperTemplateQuestionRequest;
import com.teachhelper.dto.response.ExamPaperTemplateQuestionResponse;
import com.teachhelper.entity.Exam;
import com.teachhelper.service.template.ExamPaperTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷模板控制器
 */
@RestController
@RequestMapping("/api/templates/exam-paper")
@Tag(name = "试卷模板", description = "试卷模板管理相关API")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class ExamPaperTemplateController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExamPaperTemplateController.class);
    
    @Autowired
    private ExamPaperTemplateService templateService;
    
    /**
     * 创建试卷模板
     */
    @PostMapping
    @Operation(summary = "创建试卷模板", description = "创建新的试卷模板")
    public ResponseEntity<ExamPaperTemplateResponse> createTemplate(
            @Valid @RequestBody ExamPaperTemplateRequest request) {
        
        logger.info("创建试卷模板: {}", request.getName());
        
        try {
            ExamPaperTemplateResponse response = templateService.createTemplate(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建试卷模板失败: " + request.getName(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新试卷模板
     */
    @PutMapping("/{templateId}")
    @Operation(summary = "更新试卷模板", description = "更新现有试卷模板")
    public ResponseEntity<ExamPaperTemplateResponse> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody ExamPaperTemplateRequest request) {
        
        logger.info("更新试卷模板: {}", templateId);
        
        try {
            ExamPaperTemplateResponse response = templateService.updateTemplate(templateId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新试卷模板失败: " + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除试卷模板
     */
    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除试卷模板", description = "删除试卷模板")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        
        logger.info("删除试卷模板: {}", templateId);
        
        try {
            templateService.deleteTemplate(templateId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("删除试卷模板失败: " + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取模板详情
     */
    @GetMapping("/{templateId}")
    @Operation(summary = "获取模板详情", description = "获取试卷模板的详细信息")
    public ResponseEntity<ExamPaperTemplateResponse> getTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        
        try {
            ExamPaperTemplateResponse response = templateService.getTemplate(templateId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取模板详情失败: " + templateId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取用户模板列表
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户模板列表", description = "获取当前用户创建的试卷模板")
    public ResponseEntity<List<ExamPaperTemplateResponse>> getUserTemplates() {
        
        try {
            List<ExamPaperTemplateResponse> templates = templateService.getUserTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("获取用户模板列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取公开模板列表
     */
    @GetMapping("/public")
    @Operation(summary = "获取公开模板列表", description = "获取所有公开的试卷模板")
    public ResponseEntity<List<ExamPaperTemplateResponse>> getPublicTemplates() {
        
        try {
            List<ExamPaperTemplateResponse> templates = templateService.getPublicTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("获取公开模板列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取可用模板列表
     */
    @GetMapping("/usable")
    @Operation(summary = "获取可用模板列表", description = "获取所有可用的试卷模板")
    public ResponseEntity<List<ExamPaperTemplateResponse>> getUsableTemplates() {
        
        try {
            List<ExamPaperTemplateResponse> templates = templateService.getUsableTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("获取可用模板列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 搜索模板
     */
    @GetMapping("/search")
    @Operation(summary = "搜索模板", description = "根据关键词、科目、年级搜索模板")
    public ResponseEntity<List<ExamPaperTemplateResponse>> searchTemplates(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "科目") @RequestParam(required = false) String subject,
            @Parameter(description = "年级") @RequestParam(required = false) String gradeLevel) {
        
        try {
            List<ExamPaperTemplateResponse> templates = templateService.searchTemplates(keyword, subject, gradeLevel);
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            logger.error("搜索模板失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 复制模板
     */
    @PostMapping("/{templateId}/duplicate")
    @Operation(summary = "复制模板", description = "复制现有试卷模板")
    public ResponseEntity<ExamPaperTemplateResponse> duplicateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        
        logger.info("复制试卷模板: {}", templateId);
        
        try {
            ExamPaperTemplateResponse response = templateService.duplicateTemplate(templateId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("复制试卷模板失败: " + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 应用模板创建考试
     */
    @PostMapping("/{templateId}/apply")
    @Operation(summary = "应用模板创建考试", description = "使用试卷模板创建新的考试")
    public ResponseEntity<ExamResponse> applyTemplateToExam(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "考试标题") @RequestParam String examTitle,
            @Parameter(description = "考试描述") @RequestParam(required = false) String examDescription) {
        
        logger.info("应用模板创建考试: 模板ID={}, 考试标题={}", templateId, examTitle);
        
        try {
            Exam exam = templateService.applyTemplateToExam(templateId, examTitle, examDescription);
            
            // 转换为ExamResponse
            ExamResponse response = new ExamResponse();
            response.setId(exam.getId());
            response.setTitle(exam.getTitle());
            response.setDescription(exam.getDescription());
            response.setStatus(exam.getStatus());
            response.setDuration(exam.getDuration());
            response.setStartTime(exam.getStartTime());
            response.setEndTime(exam.getEndTime());
            response.setSettings(exam.getSettings());
            response.setCreatedAt(exam.getCreatedAt());
            response.setUpdatedAt(exam.getUpdatedAt());
            response.setCreatedBy(exam.getCreatedBy().getUsername());
            response.setQuestionCount(exam.getQuestions().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("应用模板创建考试失败: 模板ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 应用模板到已有考试
     */
    @PostMapping("/{templateId}/apply-to-existing")
    @Operation(summary = "应用模板到已有考试", description = "将试卷模板应用到已有的考试")
    public ResponseEntity<ExamResponse> applyTemplateToExistingExam(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "考试ID") @RequestParam Long examId,
            @Parameter(description = "是否替换现有题目") @RequestParam(defaultValue = "false") boolean replaceExisting) {
        
        logger.info("应用模板到已有考试: 模板ID={}, 考试ID={}, 替换现有题目={}", templateId, examId, replaceExisting);
        
        try {
            Exam exam = templateService.applyTemplateToExistingExam(templateId, examId, replaceExisting);
            
            // 转换为ExamResponse
            ExamResponse response = new ExamResponse();
            response.setId(exam.getId());
            response.setTitle(exam.getTitle());
            response.setDescription(exam.getDescription());
            response.setStatus(exam.getStatus());
            response.setDuration(exam.getDuration());
            response.setStartTime(exam.getStartTime());
            response.setEndTime(exam.getEndTime());
            response.setSettings(exam.getSettings());
            response.setCreatedAt(exam.getCreatedAt());
            response.setUpdatedAt(exam.getUpdatedAt());
            response.setCreatedBy(exam.getCreatedBy().getUsername());
            response.setQuestionCount(exam.getQuestions().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("应用模板到已有考试失败: 模板ID=" + templateId + ", 考试ID=" + examId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取可应用的考试列表
     */
    @GetMapping("/applicable-exams")
    @Operation(summary = "获取可应用的考试列表", description = "获取当前用户可应用模板的考试列表")
    public ResponseEntity<List<ExamResponse>> getApplicableExams() {
        
        try {
            List<Exam> exams = templateService.getApplicableExams();
            List<ExamResponse> responses = exams.stream()
                .map(exam -> {
                    ExamResponse response = new ExamResponse();
                    response.setId(exam.getId());
                    response.setTitle(exam.getTitle());
                    response.setDescription(exam.getDescription());
                    response.setStatus(exam.getStatus());
                    response.setDuration(exam.getDuration());
                    response.setStartTime(exam.getStartTime());
                    response.setEndTime(exam.getEndTime());
                    response.setSettings(exam.getSettings());
                    response.setCreatedAt(exam.getCreatedAt());
                    response.setUpdatedAt(exam.getUpdatedAt());
                    response.setCreatedBy(exam.getCreatedBy().getUsername());
                    response.setQuestionCount(exam.getQuestions().size());
                    return response;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("获取可应用的考试列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 添加模板题目
     */
    @PostMapping("/{templateId}/questions")
    @Operation(summary = "添加模板题目", description = "向试卷模板添加新题目")
    public ResponseEntity<ExamPaperTemplateQuestionResponse> addTemplateQuestion(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestBody ExamPaperTemplateQuestionRequest questionRequest) {
        
        logger.info("添加模板题目: 模板ID={}", templateId);
        
        try {
            ExamPaperTemplateQuestionResponse response = templateService.addTemplateQuestion(templateId, questionRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("添加模板题目失败: 模板ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新模板题目
     */
    @PutMapping("/{templateId}/questions/{questionId}")
    @Operation(summary = "更新模板题目", description = "更新试卷模板中的题目")
    public ResponseEntity<ExamPaperTemplateQuestionResponse> updateTemplateQuestion(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "题目ID") @PathVariable Long questionId,
            @RequestBody ExamPaperTemplateQuestionRequest questionRequest) {
        
        logger.info("更新模板题目: 模板ID={}, 题目ID={}", templateId, questionId);
        
        try {
            ExamPaperTemplateQuestionResponse response = templateService.updateTemplateQuestion(templateId, questionId, questionRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新模板题目失败: 模板ID=" + templateId + ", 题目ID=" + questionId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除模板题目
     */
    @DeleteMapping("/{templateId}/questions/{questionId}")
    @Operation(summary = "删除模板题目", description = "从试卷模板中删除题目")
    public ResponseEntity<Void> deleteTemplateQuestion(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "题目ID") @PathVariable Long questionId) {
        
        logger.info("删除模板题目: 模板ID={}, 题目ID={}", templateId, questionId);
        
        try {
            templateService.deleteTemplateQuestion(templateId, questionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("删除模板题目失败: 模板ID=" + templateId + ", 题目ID=" + questionId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 批量添加模板题目
     */
    @PostMapping("/{templateId}/questions/batch")
    @Operation(summary = "批量添加模板题目", description = "向试卷模板批量添加题目")
    public ResponseEntity<List<ExamPaperTemplateQuestionResponse>> addTemplateQuestions(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestBody List<ExamPaperTemplateQuestionRequest> questionRequests) {
        
        logger.info("批量添加模板题目: 模板ID={}, 题目数量={}", templateId, questionRequests.size());
        
        try {
            List<ExamPaperTemplateQuestionResponse> responses = templateService.addTemplateQuestions(templateId, questionRequests);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("批量添加模板题目失败: 模板ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新模板状态
     */
    @PutMapping("/{templateId}/status")
    @Operation(summary = "更新模板状态", description = "更新试卷模板的状态")
    public ResponseEntity<ExamPaperTemplateResponse> updateTemplateStatus(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "新状态") @RequestParam String status) {
        
        logger.info("更新模板状态: 模板ID={}, 新状态={}", templateId, status);
        
        try {
            ExamPaperTemplateResponse response = templateService.updateTemplateStatus(templateId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新模板状态失败: 模板ID=" + templateId + ", 状态=" + status, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新题目状态
     */
    @PutMapping("/{templateId}/questions/{questionId}/status")
    @Operation(summary = "更新题目状态", description = "更新模板中题目的状态")
    public ResponseEntity<ExamPaperTemplateQuestionResponse> updateQuestionStatus(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "题目ID") @PathVariable Long questionId,
            @Parameter(description = "新状态") @RequestParam String status) {
        
        logger.info("更新题目状态: 模板ID={}, 题目ID={}, 新状态={}", templateId, questionId, status);
        
        try {
            ExamPaperTemplateQuestionResponse response = templateService.updateQuestionStatus(templateId, questionId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新题目状态失败: 模板ID=" + templateId + ", 题目ID=" + questionId + ", 状态=" + status, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 从学习通文档创建试卷模板
     */
    @PostMapping("/create-from-document")
    @Operation(summary = "从学习通文档创建试卷模板", description = "解析学习通文档并创建试卷模板")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ExamPaperTemplateResponse> createTemplateFromDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateName") String templateName,
            @RequestParam("subject") String subject,
            @RequestParam("gradeLevel") String gradeLevel,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "totalScore", defaultValue = "100") Integer totalScore,
            @RequestParam(value = "duration", required = false) Integer duration) {
        
        logger.info("从学习通文档创建试卷模板: {}", templateName);
        
        try {
            ExamPaperTemplateResponse response = templateService.createTemplateFromDocument(
                    file, templateName, subject, gradeLevel, description, totalScore, duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("从学习通文档创建试卷模板失败: " + templateName, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 重新导入学习通文档
     */
    @PutMapping("/{templateId}/reimport-from-document")
    @Operation(summary = "重新导入学习通文档", description = "重新解析学习通文档并更新现有模板")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ExamPaperTemplateResponse> reimportTemplateFromDocument(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "totalScore", defaultValue = "100") Integer totalScore,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "description", required = false) String description) {
        
        logger.info("重新导入学习通文档到模板: {}", templateId);
        
        try {
            ExamPaperTemplateResponse response = templateService.reimportTemplateFromDocument(
                    templateId, file, totalScore, duration, description);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重新导入学习通文档失败: " + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
} 
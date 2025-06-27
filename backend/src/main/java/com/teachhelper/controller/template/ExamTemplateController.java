package com.teachhelper.controller.template;

import com.teachhelper.dto.request.ExamTemplateRequest;
import com.teachhelper.dto.request.ExamTemplateQuestionUpdateRequest;
import com.teachhelper.dto.response.ApiResponse;
import com.teachhelper.dto.response.ExamTemplateResponse;
import com.teachhelper.dto.response.ImportResult;
import com.teachhelper.entity.ExamTemplateQuestion;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.User;
import com.teachhelper.service.auth.AuthService;
import com.teachhelper.service.template.ExamTemplateExtractionService;
import com.teachhelper.service.template.ExamTemplateService;
import com.teachhelper.service.template.TemplateBasedAnswerImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-templates")
public class ExamTemplateController {
    
    private static final Logger log = LoggerFactory.getLogger(ExamTemplateController.class);

    @Autowired
    private ExamTemplateService examTemplateService;
    
    @Autowired
    private ExamTemplateExtractionService extractionService;
    
    @Autowired
    private TemplateBasedAnswerImportService templateImportService;
    
    @Autowired
    private AuthService authService;

    /**
     * 从学习通文档提取试卷模板
     */
    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<ExamTemplateResponse>> extractTemplate(
            @RequestParam String subject,
            @RequestParam List<String> classFolders,
            @RequestParam(required = false) String templateName,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            
            ExamTemplateResponse template = extractionService.extractTemplateFromLearningDocuments(
                    subject, classFolders, user);
            
            log.info("用户 {} 提取模板成功: {}", user.getUsername(), template.getTemplateName());
            
            return ResponseEntity.ok(ApiResponse.success(template));
            
        } catch (Exception e) {
            log.error("提取模板失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("提取模板失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的模板列表
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ExamTemplateResponse>>> getUserTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            Page<ExamTemplateResponse> templates = examTemplateService.getUserTemplates(user, page, size);
            
            return ResponseEntity.ok(ApiResponse.success(templates));
            
        } catch (Exception e) {
            log.error("获取用户模板列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取模板列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamTemplateResponse>> getTemplateById(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            ExamTemplateResponse template = examTemplateService.getTemplateById(id);
            return ResponseEntity.ok(ApiResponse.success(template));
            
        } catch (Exception e) {
            log.error("获取模板详情失败: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取模板详情失败: " + e.getMessage()));
        }
    }

    /**
     * 手动匹配题目到题库
     */
    @PostMapping("/{id}/questions/{questionNumber}/match")
    public ResponseEntity<ApiResponse<Void>> matchQuestion(
            @PathVariable Long id,
            @PathVariable Integer questionNumber,
            @RequestParam Long questionId,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            examTemplateService.matchQuestionToBank(id, questionNumber, questionId, user);
            
            log.info("用户 {} 匹配题目: 模板{} 题号{} -> 题库{}", 
                    user.getUsername(), id, questionNumber, questionId);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("匹配题目失败: 模板{} 题号{} -> 题库{}", id, questionNumber, questionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("匹配题目失败: " + e.getMessage()));
        }
    }

    /**
     * 取消题目匹配
     */
    @PostMapping("/{id}/questions/{questionNumber}/unmatch")
    public ResponseEntity<ApiResponse<Void>> unmatchQuestion(
            @PathVariable Long id,
            @PathVariable Integer questionNumber,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            examTemplateService.unmatchQuestion(id, questionNumber, user);
            
            log.info("用户 {} 取消匹配题目: 模板{} 题号{}", 
                    user.getUsername(), id, questionNumber);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("取消匹配题目失败: 模板{} 题号{}", id, questionNumber, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("取消匹配题目失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索题库题目以供匹配
     */
    @GetMapping("/{id}/questions/{questionNumber}/candidates")
    public ResponseEntity<ApiResponse<List<com.teachhelper.dto.response.QuestionSummaryDto>>> searchCandidateQuestions(
            @PathVariable Long id,
            @PathVariable Integer questionNumber,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        try {
            List<com.teachhelper.dto.response.QuestionSummaryDto> candidates = examTemplateService.searchCandidateQuestions(
                    id, questionNumber, keyword, limit);
            
            return ResponseEntity.ok(ApiResponse.success(candidates));
            
        } catch (Exception e) {
            log.error("搜索候选题目失败: 模板{} 题号{}", id, questionNumber, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索候选题目失败: " + e.getMessage()));
        }
    }

    /**
     * 标记模板为就绪状态
     */
    @PostMapping("/{id}/ready")
    public ResponseEntity<ApiResponse<ExamTemplateResponse>> markTemplateReady(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            ExamTemplateResponse template = examTemplateService.markTemplateReady(id, user);
            
            log.info("用户 {} 标记模板为就绪: {}", user.getUsername(), id);
            
            return ResponseEntity.ok(ApiResponse.success(template));
            
        } catch (Exception e) {
            log.error("标记模板为就绪失败: 模板{}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("标记模板为就绪失败: " + e.getMessage()));
        }
    }

    /**
     * 基于模板导入学生答案
     */
    @PostMapping("/{id}/import-answers")
    public ResponseEntity<ApiResponse<ImportResult>> importAnswersWithTemplate(
            @PathVariable Long id,
            @RequestParam Long examId,
            @RequestParam String subject,
            @RequestParam(required = false) String classFolder,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            ImportResult result = templateImportService.importAnswersWithTemplate(
                    examId, id, subject, classFolder, user);
            
            log.info("用户 {} 基于模板导入答案: 模板{} -> 考试{}, 成功{}, 失败{}", 
                    user.getUsername(), id, examId, result.getSuccessCount(), result.getFailedCount());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("基于模板导入答案失败: 模板{} -> 考试{}", id, examId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("基于模板导入答案失败: " + e.getMessage()));
        }
    }

    /**
     * 获取模板的题目列表
     */
    @GetMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<List<ExamTemplateQuestion>>> getTemplateQuestions(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            List<ExamTemplateQuestion> questions = examTemplateService.getTemplateQuestions(id);
            
            log.info("获取模板题目列表: 模板{}, 题目数{}", id, questions.size());
            
            return ResponseEntity.ok(ApiResponse.success(questions));
            
        } catch (Exception e) {
            log.error("获取模板题目列表失败: 模板{}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取模板题目列表失败: " + e.getMessage()));
        }
    }

    /**
     * 删除试卷模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            examTemplateService.deleteTemplate(id, user);
            
            log.info("用户 {} 删除模板: {}", user.getUsername(), id);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("删除模板失败: 模板{}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除模板失败: " + e.getMessage()));
        }
    }

    /**
     * 更新模板题目
     */
    @PutMapping("/{id}/questions/{questionId}")
    public ResponseEntity<ApiResponse<ExamTemplateQuestion>> updateTemplateQuestion(
            @PathVariable Long id,
            @PathVariable Long questionId,
            @RequestBody ExamTemplateQuestionUpdateRequest request,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            ExamTemplateQuestion updatedQuestion = examTemplateService.updateTemplateQuestion(id, questionId, request, user);
            
            log.info("用户 {} 更新模板题目: 模板{} 题目{}", 
                    user.getUsername(), id, questionId);
            
            return ResponseEntity.ok(ApiResponse.success(updatedQuestion));
            
        } catch (Exception e) {
            log.error("更新模板题目失败: 模板{} 题目{}", id, questionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("更新模板题目失败: " + e.getMessage()));
        }
    }

    /**
     * 确认模板题目
     */
    @PostMapping("/{id}/questions/{questionId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmTemplateQuestion(
            @PathVariable Long id,
            @PathVariable Long questionId,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            examTemplateService.confirmTemplateQuestion(id, questionId, user);
            
            log.info("用户 {} 确认模板题目: 模板{} 题目{}", 
                    user.getUsername(), id, questionId);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("确认模板题目失败: 模板{} 题目{}", id, questionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("确认模板题目失败: " + e.getMessage()));
        }
    }

    /**
     * 取消确认模板题目
     */
    @PostMapping("/{id}/questions/{questionId}/unconfirm")
    public ResponseEntity<ApiResponse<Void>> unconfirmTemplateQuestion(
            @PathVariable Long id,
            @PathVariable Long questionId,
            Authentication authentication) {
        
        try {
            User user = authService.getCurrentUser();
            examTemplateService.unconfirmTemplateQuestion(id, questionId, user);
            
            log.info("用户 {} 取消确认模板题目: 模板{} 题目{}", 
                    user.getUsername(), id, questionId);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("取消确认模板题目失败: 模板{} 题目{}", id, questionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("取消确认模板题目失败: " + e.getMessage()));
        }
    }

}
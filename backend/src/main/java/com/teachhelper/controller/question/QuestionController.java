package com.teachhelper.controller.question;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teachhelper.ai.AIEvaluationService;
import com.teachhelper.dto.QuestionOptionDTO;
import com.teachhelper.dto.request.QuestionCreateRequest;
import com.teachhelper.dto.response.RubricSuggestionResponse;
import com.teachhelper.dto.response.AIGenerationStatusResponse;
import com.teachhelper.dto.response.AIGenerationTaskResponse;
import com.teachhelper.dto.response.QuestionResponse;
import com.teachhelper.entity.Exam;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionOption;
import com.teachhelper.entity.QuestionBank;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.service.ai.StreamingAIGenerationService;
import com.teachhelper.service.exam.ExamService;
import com.teachhelper.service.question.QuestionService;
import com.teachhelper.service.question.QuestionBankService;
import com.teachhelper.service.student.StudentAnswerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/questions")
@Tag(name = "题目管理", description = "题目的创建、查询、修改、删除等操作")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private QuestionBankService questionBankService;
    
    @Autowired
    private StudentAnswerService studentAnswerService;
    
    @Autowired
    private AIEvaluationService aiEvaluationService;
    
    @Autowired
    private StreamingAIGenerationService streamingAIGenerationService;
    
    @PostMapping
    @Operation(summary = "创建题目", description = "创建新题目并关联到题目库和考试")
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody QuestionCreateRequest questionRequest) {
        System.out.println("=== Question Creation Debug ===");
        System.out.println("Received QuestionCreateRequest: examId=" + questionRequest.getExamId() + 
                          ", title=" + questionRequest.getTitle() + 
                          ", questionType=" + questionRequest.getQuestionType() +
                          ", maxScore=" + questionRequest.getMaxScore());
        
        // 验证examId不能为null或0
        if (questionRequest.getExamId() == null || questionRequest.getExamId() <= 0) {
            System.err.println("ExamId validation failed: " + questionRequest.getExamId());
            throw new IllegalArgumentException("考试ID不能为空且必须大于0");
        }
        
        // 验证考试是否存在
        Exam exam;
        try {
            exam = examService.getExamById(questionRequest.getExamId());
            System.out.println("Found exam: " + exam.getId() + " - " + exam.getTitle());
        } catch (Exception e) {
            System.err.println("Failed to find exam with id: " + questionRequest.getExamId());
            throw new IllegalArgumentException("指定的考试不存在");
        }
        
        // 获取当前用户ID（简化处理）
        Long currentUserId = getCurrentUserId();
        
        // 创建或获取默认题目库
        QuestionBank questionBank = getOrCreateDefaultQuestionBank(currentUserId);
        
        Question question = new Question();
        question.setTitle(questionRequest.getTitle());
        question.setContent(questionRequest.getContent());
        question.setQuestionType(questionRequest.getQuestionType());
        question.setMaxScore(BigDecimal.valueOf(questionRequest.getMaxScore()));
        question.setReferenceAnswer(questionRequest.getReferenceAnswer());
        question.setCreatedBy(currentUserId);
        question.setIsActive(true);
        
        // 设置题目库关联（新架构要求）
        question.setQuestionBank(questionBank);
        
        // 保持向后兼容，同时设置考试关联
        question.setExam(exam);
        
        // 添加选择题选项
        if (questionRequest.getOptions() != null && !questionRequest.getOptions().isEmpty()) {
            for (QuestionOptionDTO optionDTO : questionRequest.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setContent(optionDTO.getContent());
                option.setIsCorrect(optionDTO.getIsCorrect());
                option.setOptionOrder(optionDTO.getOptionOrder());
                question.addOption(option);
            }
        }
        
        System.out.println("Question before save - examId: " + (question.getExam() != null ? question.getExam().getId() : "NULL") +
                          ", questionBankId: " + (question.getQuestionBank() != null ? question.getQuestionBank().getId() : "NULL"));
        
        Question createdQuestion = questionService.createQuestion(question);
        System.out.println("Question created successfully with id: " + createdQuestion.getId());
        
        QuestionResponse response = convertToResponse(createdQuestion);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 获取当前用户ID（简化实现）
     */
    private Long getCurrentUserId() {
        // TODO: 从Spring Security Context中获取真实的用户ID
        // 这里临时返回固定值，实际项目中应该从认证信息中获取
        return 1L;
    }
    
    /**
     * 获取或创建默认题目库
     */
    private QuestionBank getOrCreateDefaultQuestionBank(Long userId) {
        try {
            // 尝试获取指定用户的第一个题目库
            var questionBanks = questionBankService.getUserQuestionBanks(
                org.springframework.data.domain.PageRequest.of(0, 1));
            
            if (questionBanks.hasContent()) {
                QuestionBank existingBank = questionBanks.getContent().get(0);
                System.out.println("Using existing question bank: " + existingBank.getId() + " for user: " + userId);
                return existingBank;
            } else {
                // 如果用户没有题目库，创建一个默认的
                System.out.println("Creating default question bank for user: " + userId);
                return questionBankService.createQuestionBank(
                    "默认题目库-用户" + userId, 
                    "系统自动创建的默认题目库", 
                    "通用", 
                    "通用", 
                    false
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to get or create question bank for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("创建题目失败：无法获取或创建题目库");
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取题目详情", description = "根据ID获取题目详细信息")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        QuestionResponse response = convertToResponse(question);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "获取题目列表", description = "获取所有题目，支持分页和多条件筛选")
    public ResponseEntity<Map<String, Object>> getAllQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long questionBankId,
            @RequestParam(required = false) Long sourceKnowledgeBaseId) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // 构建查询条件
        Map<String, Object> filters = new HashMap<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            filters.put("keyword", keyword.trim());
        }
        if (questionType != null && !questionType.trim().isEmpty()) {
            filters.put("questionType", questionType.trim());
        }
        if (subject != null && !subject.trim().isEmpty()) {
            filters.put("subject", subject.trim());
        }
        if (gradeLevel != null && !gradeLevel.trim().isEmpty()) {
            filters.put("gradeLevel", gradeLevel.trim());
        }
        if (examId != null) {
            filters.put("examId", examId);
        }
        if (source != null && !source.trim().isEmpty()) {
            filters.put("source", source.trim());
        }
        if (questionBankId != null) {
            filters.put("questionBankId", questionBankId);
        }
        if (sourceKnowledgeBaseId != null) {
            filters.put("sourceKnowledgeBaseId", sourceKnowledgeBaseId);
        }
        
        Page<Question> questionPage = questionService.searchQuestionsWithFilters(pageable, filters);
        List<QuestionResponse> responses = questionPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
            
        // 构建分页响应
        Map<String, Object> response = new HashMap<>();
        response.put("content", responses);
        response.put("totalElements", questionPage.getTotalElements());
        response.put("totalPages", questionPage.getTotalPages());
        response.put("number", questionPage.getNumber());
        response.put("size", questionPage.getSize());
        response.put("numberOfElements", questionPage.getNumberOfElements());
        response.put("first", questionPage.isFirst());
        response.put("last", questionPage.isLast());
        response.put("empty", questionPage.isEmpty());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/exam/{examId}")
    @Operation(summary = "获取考试的所有题目", description = "根据考试ID获取该考试的所有题目（教师和管理员）")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByExam(@PathVariable Long examId) {
        System.out.println("=== 获取考试题目权限检查（管理端）===");
        System.out.println("考试ID: " + examId);
        
        // 首先验证用户是否有权限访问这个考试
        try {
            Exam exam = examService.getExamById(examId); // 这个方法包含详细的权限验证
            System.out.println("权限验证通过，考试标题: " + exam.getTitle());
        } catch (Exception e) {
            System.err.println("权限验证失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
        
        try {
            List<Question> questions = questionService.getQuestionsByExamId(examId);
            System.out.println("成功获取题目数量: " + questions.size());
            
            List<QuestionResponse> responses = questions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("获取题目失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/exam/{examId}/take")
    @Operation(summary = "获取考试题目（参加考试）", description = "学生参加考试时获取题目，只返回题目内容不包含答案")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<List<QuestionResponse>> getQuestionsForTakingExam(@PathVariable Long examId) {
        System.out.println("=== 学生参加考试获取题目 ===");
        System.out.println("考试ID: " + examId);
        
        // 首先验证用户是否有权限访问这个考试
        try {
            Exam exam = examService.getExamById(examId); // 这个方法包含详细的权限验证
            System.out.println("权限验证通过，考试标题: " + exam.getTitle());
        } catch (Exception e) {
            System.err.println("权限验证失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
        
        try {
            List<Question> questions = questionService.getQuestionsByExamId(examId);
            System.out.println("成功获取题目数量: " + questions.size());
            
            // 为学生参加考试准备题目，不包含参考答案
            List<QuestionResponse> responses = questions.stream()
                .map(this::convertToResponseForTaking)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("获取题目失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新题目", description = "更新题目信息")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionCreateRequest questionRequest) {
        
        Question questionDetails = new Question();
        questionDetails.setTitle(questionRequest.getTitle());
        questionDetails.setContent(questionRequest.getContent());
        questionDetails.setQuestionType(questionRequest.getQuestionType());
        questionDetails.setMaxScore(BigDecimal.valueOf(questionRequest.getMaxScore()));
        questionDetails.setReferenceAnswer(questionRequest.getReferenceAnswer());
        
        // 处理选择题选项
        if (questionRequest.getOptions() != null) {
            for (QuestionOptionDTO optionDTO : questionRequest.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setContent(optionDTO.getContent());
                option.setIsCorrect(optionDTO.getIsCorrect());
                option.setOptionOrder(optionDTO.getOptionOrder());
                questionDetails.addOption(option);
            }
        }
        
        Question updatedQuestion = questionService.updateQuestion(id, questionDetails);
        QuestionResponse response = convertToResponse(updatedQuestion);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目", description = "删除指定题目")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
    
    // 评分标准相关接口
    @PostMapping("/{questionId}/rubric")
    @Operation(summary = "添加评分标准", description = "为题目添加评分标准")
    public ResponseEntity<RubricCriterion> addRubricCriterion(
            @PathVariable Long questionId,
            @RequestBody RubricCriterion criterion) {
        RubricCriterion created = questionService.addRubricCriterion(questionId, criterion);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/{questionId}/rubric")
    @Operation(summary = "获取评分标准", description = "获取题目的所有评分标准")
    public ResponseEntity<List<RubricCriterion>> getRubricCriteria(@PathVariable Long questionId) {
        List<RubricCriterion> criteria = questionService.getRubricCriteriaByQuestionId(questionId);
        return ResponseEntity.ok(criteria);
    }
    
    @PutMapping("/rubric/{criterionId}")
    @Operation(summary = "更新评分标准", description = "更新评分标准信息")
    public ResponseEntity<RubricCriterion> updateRubricCriterion(
            @PathVariable Long criterionId,
            @RequestBody RubricCriterion criterion) {
        RubricCriterion updated = questionService.updateRubricCriterion(criterionId, criterion);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/rubric/{criterionId}")
    @Operation(summary = "删除评分标准", description = "删除评分标准")
    public ResponseEntity<Void> deleteRubricCriterion(@PathVariable Long criterionId) {
        questionService.deleteRubricCriterion(criterionId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{questionId}/generate-rubric")
    @Operation(summary = "AI生成评分标准", description = "使用AI为题目生成建议的评分标准")
    public ResponseEntity<List<RubricSuggestionResponse>> generateRubric(@PathVariable Long questionId) {
        System.out.println("=== QuestionController: 接收到AI评分标准生成请求 ===");
        System.out.println("题目ID: " + questionId);
        
        Question question = questionService.getQuestionById(questionId);
        System.out.println("获取题目成功:");
        System.out.println("  - 题目标题: " + question.getTitle());
        System.out.println("  - 题目类型: " + question.getQuestionType());
        System.out.println("  - 题目满分: " + question.getMaxScore());
        
        try {
            System.out.println("🚀 调用AIEvaluationService.generateRubricSuggestions...");
            List<RubricSuggestionResponse> suggestions = aiEvaluationService.generateRubricSuggestions(question);
            
            System.out.println("✅ AI评分标准生成完成!");
            System.out.println("  - 生成数量: " + suggestions.size());
            for (int i = 0; i < suggestions.size(); i++) {
                RubricSuggestionResponse suggestion = suggestions.get(i);
                System.out.println("  " + (i+1) + ". " + suggestion.getCriterionText() + " (" + suggestion.getPoints() + "分)");
            }
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            System.err.println("❌ QuestionController: 评分标准生成失败");
            System.err.println("  - 异常类型: " + e.getClass().getSimpleName());
            System.err.println("  - 异常信息: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>()); // 返回空列表而不是错误
        }
    }
    
    @PostMapping("/{questionId}/apply-rubric-suggestions")
    @Operation(summary = "应用AI生成的评分标准", description = "将AI生成的评分标准建议应用为实际的评分标准")
    public ResponseEntity<List<RubricCriterion>> applyRubricSuggestions(
            @PathVariable Long questionId,
            @RequestBody List<RubricSuggestionResponse> suggestions) {
        
        Question question = questionService.getQuestionById(questionId);
        List<RubricCriterion> appliedCriteria = suggestions.stream()
            .map(suggestion -> {
                RubricCriterion criterion = new RubricCriterion();
                criterion.setQuestion(question);
                criterion.setCriterionText(suggestion.getCriterionText());
                criterion.setPoints(suggestion.getPoints());
                return questionService.addRubricCriterion(questionId, criterion);
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.status(HttpStatus.CREATED).body(appliedCriteria);
    }
    
    @PostMapping("/{questionId}/generate-reference-answer")
    @Operation(summary = "AI生成参考答案", description = "使用AI为题目生成建议的参考答案")
    public ResponseEntity<Map<String, String>> generateReferenceAnswer(@PathVariable Long questionId) {
        Question question = questionService.getQuestionById(questionId);
        try {
            String referenceAnswer = aiEvaluationService.generateReferenceAnswer(question);
            // 更新题目实体的参考答案
            question.setReferenceAnswer(referenceAnswer);
            questionService.save(question);
            
            Map<String, String> response = new HashMap<>();
            response.put("referenceAnswer", referenceAnswer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "AI生成参考答案失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // 流式AI生成相关接口
    @PostMapping("/{questionId}/generate-rubric-async")
    @Operation(summary = "异步AI生成评分标准", description = "创建AI评分标准生成任务，返回任务ID")
    public ResponseEntity<AIGenerationTaskResponse> generateRubricAsync(@PathVariable Long questionId) {
        String taskId = streamingAIGenerationService.createGenerationTask(questionId);
        AIGenerationTaskResponse taskResponse = new AIGenerationTaskResponse(taskId, questionId);
        return ResponseEntity.ok(taskResponse);
    }
    
    @GetMapping("/generation-status/{taskId}")
    @Operation(summary = "查询AI生成状态", description = "查询AI评分标准生成任务的状态和进度")
    public ResponseEntity<AIGenerationStatusResponse> getGenerationStatus(@PathVariable String taskId) {
        AIGenerationStatusResponse status = streamingAIGenerationService.getTaskStatus(taskId);
        return ResponseEntity.ok(status);
    }
    
    @DeleteMapping("/generation-task/{taskId}")
    @Operation(summary = "取消AI生成任务", description = "取消正在进行的AI评分标准生成任务")
    public ResponseEntity<Void> cancelGenerationTask(@PathVariable String taskId) {
        streamingAIGenerationService.cancelTask(taskId);
        return ResponseEntity.noContent().build();
    }
    
    // 增强版AI生成相关接口 - 支持现有评分标准检测和模式选择
    @GetMapping("/{questionId}/existing-rubrics")
    @Operation(summary = "检查现有评分标准", description = "检查题目是否已有评分标准，返回现有标准和总分信息")
    public ResponseEntity<Map<String, Object>> checkExistingRubrics(@PathVariable Long questionId) {
        try {
            List<RubricCriterion> existingCriteria = questionService.getRubricCriteriaByQuestionId(questionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("hasExisting", !existingCriteria.isEmpty());
            result.put("count", existingCriteria.size());
            
            if (!existingCriteria.isEmpty()) {
                // 转换为响应DTO
                List<RubricSuggestionResponse> criteriaList = existingCriteria.stream()
                    .map(criterion -> new RubricSuggestionResponse(
                        criterion.getCriterionText(), 
                        criterion.getPoints()))
                    .collect(Collectors.toList());
                
                // 计算总分
                BigDecimal totalScore = existingCriteria.stream()
                    .map(RubricCriterion::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                result.put("criteria", criteriaList);
                result.put("totalScore", totalScore);
                
                // 获取题目信息以比较满分
                Question question = questionService.getQuestionById(questionId);
                result.put("maxScore", question.getMaxScore());
                result.put("isComplete", totalScore.compareTo(question.getMaxScore()) >= 0);
            } else {
                result.put("criteria", new ArrayList<>());
                result.put("totalScore", BigDecimal.ZERO);
                
                Question question = questionService.getQuestionById(questionId);
                result.put("maxScore", question.getMaxScore());
                result.put("isComplete", false);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("hasExisting", false);
            errorResult.put("count", 0);
            errorResult.put("criteria", new ArrayList<>());
            errorResult.put("totalScore", BigDecimal.ZERO);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }
    
    @PostMapping("/{questionId}/generate-rubric-enhanced")
    @Operation(summary = "增强版AI生成评分标准", description = "支持覆盖或补全模式的AI评分标准生成")
    public ResponseEntity<AIGenerationTaskResponse> generateRubricEnhanced(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "OVERWRITE") String mode,
            @RequestParam(required = false) Integer targetScore,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        
        try {
            // 提取自定义提示词
            String customPrompt = null;
            if (requestBody != null && requestBody.containsKey("customPrompt")) {
                customPrompt = (String) requestBody.get("customPrompt");
            }
            
            String taskId = streamingAIGenerationService.createGenerationTaskWithCheck(questionId, mode, targetScore, customPrompt);
            AIGenerationTaskResponse taskResponse = new AIGenerationTaskResponse(taskId, questionId);
            String message = "增强版AI生成任务已创建，模式: " + mode;
            if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                message += "，已应用自定义提示词";
            }
            taskResponse.setMessage(message);
            return ResponseEntity.ok(taskResponse);
        } catch (IllegalArgumentException e) {
            AIGenerationTaskResponse errorResponse = new AIGenerationTaskResponse(null, questionId);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("参数错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            AIGenerationTaskResponse errorResponse = new AIGenerationTaskResponse(null, questionId);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("创建任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{questionId}/generate-rubric-smart")
    @Operation(summary = "智能AI评分标准生成", description = "检测现有评分标准并提供选择模式：覆盖、补全或直接生成")
    public ResponseEntity<Map<String, Object>> generateRubricSmart(
            @PathVariable Long questionId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        try {
            // 提取自定义提示词
            String customPrompt = null;
            if (requestBody != null && requestBody.containsKey("customPrompt")) {
                customPrompt = (String) requestBody.get("customPrompt");
            }
            
            // 检查是否存在评分标准
            List<RubricCriterion> existingCriteria = questionService.getRubricCriteriaByQuestionId(questionId);
            Question question = questionService.getQuestionById(questionId);
            
            Map<String, Object> response = new HashMap<>();
            
            if (existingCriteria.isEmpty()) {
                // 没有现有评分标准，直接生成
                String taskId = streamingAIGenerationService.createGenerationTaskWithCheck(questionId, "OVERWRITE", null, customPrompt);
                response.put("action", "DIRECT_GENERATE");
                response.put("taskId", taskId);
                response.put("message", "未检测到现有评分标准，正在直接生成新的评分标准" + 
                    (customPrompt != null && !customPrompt.trim().isEmpty() ? "（已应用自定义提示词）" : ""));
            } else {
                // 有现有评分标准，需要用户选择
                BigDecimal currentTotal = existingCriteria.stream()
                    .map(RubricCriterion::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                List<RubricSuggestionResponse> existingCriteriaList = existingCriteria.stream()
                    .map(criterion -> new RubricSuggestionResponse(
                        criterion.getCriterionText(), 
                        criterion.getPoints()))
                    .collect(Collectors.toList());
                
                response.put("action", "CHOICE_REQUIRED");
                response.put("existingCriteria", existingCriteriaList);
                response.put("currentTotal", currentTotal);
                response.put("maxScore", question.getMaxScore());
                response.put("isComplete", currentTotal.compareTo(question.getMaxScore()) >= 0);
                response.put("remainingScore", question.getMaxScore().subtract(currentTotal));
                
                // 提供选择模式的建议
                Map<String, Object> modeOptions = new HashMap<>();
                modeOptions.put("OVERWRITE", "覆盖现有评分标准，重新生成完整的评分标准");
                
                if (currentTotal.compareTo(question.getMaxScore()) < 0) {
                    modeOptions.put("COMPLEMENT", "补全现有评分标准，生成额外的评分项目到达满分");
                } else {
                    modeOptions.put("COMPLEMENT", "补全现有评分标准（注意：当前总分已达满分，补全可能导致超分）");
                }
                
                response.put("modeOptions", modeOptions);
                response.put("message", "检测到现有评分标准，请选择生成模式" + 
                    (customPrompt != null && !customPrompt.trim().isEmpty() ? "（已保存自定义提示词）" : ""));
                
                // 如果有自定义提示词，保存到响应中以便后续使用
                if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                    response.put("customPrompt", customPrompt);
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("action", "ERROR");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "检测现有评分标准时发生错误");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{questionId}/generate-rubric-with-mode")
    @Operation(summary = "根据用户选择的模式生成评分标准", description = "用户选择模式后执行相应的AI生成")
    public ResponseEntity<AIGenerationTaskResponse> generateRubricWithMode(
            @PathVariable Long questionId,
            @RequestParam String mode,
            @RequestParam(required = false) Integer targetScore,
            @RequestParam(defaultValue = "false") boolean confirmed,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        
        try {
            // 验证模式参数
            if (!mode.equals("OVERWRITE") && !mode.equals("COMPLEMENT")) {
                throw new IllegalArgumentException("无效的生成模式: " + mode);
            }
            
            // 提取自定义提示词
            String customPrompt = null;
            if (requestBody != null && requestBody.containsKey("customPrompt")) {
                customPrompt = (String) requestBody.get("customPrompt");
            }
            
            // 如果是补全模式但未指定目标分数，使用题目满分
            if ("COMPLEMENT".equals(mode) && targetScore == null) {
                Question question = questionService.getQuestionById(questionId);
                targetScore = question.getMaxScore().intValue();
            }
            
            // 如果是补全模式且当前总分已达满分，需要用户确认
            if ("COMPLEMENT".equals(mode) && !confirmed) {
                List<RubricCriterion> existingCriteria = questionService.getRubricCriteriaByQuestionId(questionId);
                BigDecimal currentTotal = existingCriteria.stream()
                    .map(RubricCriterion::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Question question = questionService.getQuestionById(questionId);
                if (currentTotal.compareTo(question.getMaxScore()) >= 0) {
                    AIGenerationTaskResponse confirmResponse = new AIGenerationTaskResponse(null, questionId);
                    confirmResponse.setStatus("CONFIRMATION_REQUIRED");
                    confirmResponse.setMessage("当前评分标准总分已达到或超过满分，补全模式可能导致总分超出。确定要继续吗？");
                    return ResponseEntity.ok(confirmResponse);
                }
            }
            
            String taskId = streamingAIGenerationService.createGenerationTaskWithCheck(questionId, mode, targetScore, customPrompt);
            AIGenerationTaskResponse taskResponse = new AIGenerationTaskResponse(taskId, questionId);
            String message = "AI生成任务已创建，模式: " + mode + 
                (targetScore != null ? "，目标分数: " + targetScore : "");
            if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                message += "，已应用自定义提示词";
            }
            taskResponse.setMessage(message);
            return ResponseEntity.ok(taskResponse);
            
        } catch (IllegalArgumentException e) {
            AIGenerationTaskResponse errorResponse = new AIGenerationTaskResponse(null, questionId);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("参数错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            AIGenerationTaskResponse errorResponse = new AIGenerationTaskResponse(null, questionId);
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("创建任务失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private QuestionResponse convertToResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setQuestionType(question.getQuestionType());
        response.setMaxScore(question.getMaxScore().doubleValue());
        response.setReferenceAnswer(question.getReferenceAnswer());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
        
        // 转换选择题选项
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<QuestionOptionDTO> optionDTOs = question.getOptions().stream()
                .map(option -> {
                    QuestionOptionDTO dto = new QuestionOptionDTO();
                    dto.setId(option.getId());
                    dto.setContent(option.getContent());
                    dto.setIsCorrect(option.getIsCorrect());
                    dto.setOptionOrder(option.getOptionOrder());
                    return dto;
                })
                .collect(Collectors.toList());
            response.setOptions(optionDTOs);
        }
        
        if (question.getExam() != null) {
            response.setExamId(question.getExam().getId());
            response.setExamTitle(question.getExam().getTitle());
        }
        
        // 设置题目来源信息 - 使用新的分类方式
        if (question.getSourceType() != null) {
            response.setSourceType(question.getSourceType());
        } else {
            // 根据其他字段推断来源类型
            if (question.getAiGenerationPrompt() != null && !question.getAiGenerationPrompt().trim().isEmpty()) {
                response.setSourceType("AI_GENERATED");
            } else if (question.getKeywords() != null && question.getKeywords().contains("互联网")) {
                response.setSourceType("INTERNET");
            } else {
                response.setSourceType("SELF_CREATED"); // 默认为自创
            }
        }
        
        // 设置确认状态
        response.setIsConfirmed(question.getIsConfirmed() != null ? question.getIsConfirmed() : false);
        
        // 题目库信息
        if (question.getQuestionBank() != null) {
            response.setQuestionBankId(question.getQuestionBank().getId());
            response.setQuestionBankName(question.getQuestionBank().getName());
        }
        
        // 知识库信息
        if (question.getSourceKnowledgeBaseId() != null) {
            response.setSourceKnowledgeBaseId(question.getSourceKnowledgeBaseId());
            // TODO: 获取知识库名称
        }
        
        // 知识点信息
        if (question.getSourceKnowledgePointId() != null) {
            response.setSourceKnowledgePointId(question.getSourceKnowledgePointId());
            // TODO: 获取知识点名称
        }
        
        // 难度和关键词
        if (question.getDifficulty() != null) {
            response.setDifficulty(question.getDifficulty().name());
        }
        response.setKeywords(question.getKeywords());
        
        // 转换评分标准
        if (question.getRubricCriteria() != null && !question.getRubricCriteria().isEmpty()) {
            List<QuestionResponse.RubricCriterionResponse> rubricDTOs = question.getRubricCriteria().stream()
                .map(criterion -> {
                    QuestionResponse.RubricCriterionResponse dto = new QuestionResponse.RubricCriterionResponse();
                    dto.setId(criterion.getId());
                    dto.setCriterionText(criterion.getCriterionText());
                    dto.setPoints(criterion.getPoints().doubleValue());
                    return dto;
                })
                .collect(Collectors.toList());
            response.setRubricCriteria(rubricDTOs);
        }
        
        // 获取答案统计数据
        try {
            long totalAnswers = studentAnswerService.getAnswerCountByQuestionId(question.getId());
            long evaluatedAnswers = studentAnswerService.getEvaluatedAnswerCountByQuestionId(question.getId());
            double averageScore = totalAnswers > 0 ? studentAnswerService.getAverageScoreByQuestionId(question.getId()) : 0.0;
            
            response.setTotalAnswers(totalAnswers);
            response.setEvaluatedAnswers(evaluatedAnswers);
            response.setAverageScore(averageScore);
        } catch (Exception e) {
            // 如果获取统计数据失败，设置默认值
            response.setTotalAnswers(0);
            response.setEvaluatedAnswers(0);
            response.setAverageScore(0.0);
        }
        
        return response;
    }
    
    /**
     * 转换题目为学生参加考试时的响应，不包含参考答案和评分标准
     */
    private QuestionResponse convertToResponseForTaking(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setQuestionType(question.getQuestionType());
        response.setMaxScore(question.getMaxScore().doubleValue());
        // 不设置参考答案 - response.setReferenceAnswer(null);
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
        
        // 转换选择题选项，但不包含正确答案信息
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<QuestionOptionDTO> optionDTOs = question.getOptions().stream()
                .map(option -> {
                    QuestionOptionDTO dto = new QuestionOptionDTO();
                    dto.setId(option.getId());
                    dto.setContent(option.getContent());
                    // 不设置正确答案标识 - dto.setIsCorrect(null);
                    dto.setOptionOrder(option.getOptionOrder());
                    return dto;
                })
                .collect(Collectors.toList());
            response.setOptions(optionDTOs);
        }
        
        if (question.getExam() != null) {
            response.setExamId(question.getExam().getId());
            response.setExamTitle(question.getExam().getTitle());
        }
        
        // 不包含评分标准信息
        // response.setRubricCriteria(null);
        
        // 不包含答案统计数据
        response.setTotalAnswers(0);
        response.setEvaluatedAnswers(0);
        response.setAverageScore(0.0);
        
        return response;
    }

    // 选择性替换评分标准相关接口
    @PostMapping("/{questionId}/replace-rubrics-selective")
    @Operation(summary = "选择性替换评分标准", description = "用户可以选择性地替换生成的评分标准")
    public ResponseEntity<Map<String, Object>> replaceRubricsSelective(
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> requestBody) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selectedSuggestions = (List<Map<String, Object>>) requestBody.get("selectedSuggestions");
            Boolean replaceAll = (Boolean) requestBody.getOrDefault("replaceAll", false);
            
            System.out.println("=== 选择性替换评分标准 ===");
            System.out.println("题目ID: " + questionId);
            System.out.println("替换全部: " + replaceAll);
            System.out.println("选中的建议数量: " + (selectedSuggestions != null ? selectedSuggestions.size() : 0));
            
            Question question = questionService.getQuestionById(questionId);
            
            // 无论是全部替换还是选择性替换，在覆盖模式中都需要先删除现有评分标准
            List<RubricCriterion> existingCriteria = questionService.getRubricCriteriaByQuestionId(questionId);
            for (RubricCriterion criterion : existingCriteria) {
                questionService.deleteRubricCriterion(criterion.getId());
            }
            System.out.println("已删除 " + existingCriteria.size() + " 个现有评分标准");
            
            if (replaceAll) {
                System.out.println("执行全部替换模式");
            } else {
                System.out.println("执行选择性替换模式");
            }
            
            // 应用选中的建议
            List<RubricCriterion> appliedCriteria = new ArrayList<>();
            if (selectedSuggestions != null) {
                for (Map<String, Object> suggestionMap : selectedSuggestions) {
                    String criterionText = (String) suggestionMap.get("criterionText");
                    Object pointsObj = suggestionMap.get("points");
                    BigDecimal points;
                    
                    if (pointsObj instanceof Number) {
                        points = BigDecimal.valueOf(((Number) pointsObj).doubleValue());
                    } else {
                        continue; // 跳过无效数据
                    }
                    
                    RubricCriterion criterion = new RubricCriterion();
                    criterion.setQuestion(question);
                    criterion.setCriterionText(criterionText);
                    criterion.setPoints(points);
                    
                    RubricCriterion saved = questionService.saveRubricCriterion(criterion);
                    appliedCriteria.add(saved);
                }
            }
            
            System.out.println("成功应用 " + appliedCriteria.size() + " 个评分标准");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("appliedCount", appliedCriteria.size());
            response.put("appliedCriteria", appliedCriteria);
            response.put("message", "评分标准替换成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("选择性替换评分标准失败: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/{questionId}/regenerate-single-rubric")
    @Operation(summary = "重新生成单个评分标准", description = "重新生成指定的单个评分标准")
    public ResponseEntity<RubricSuggestionResponse> regenerateSingleRubric(
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> requestBody) {
        
        try {
            String criterionIndex = (String) requestBody.get("criterionIndex");
            String customPrompt = (String) requestBody.get("customPrompt");
            
            System.out.println("=== 重新生成单个评分标准 ===");
            System.out.println("题目ID: " + questionId);
            System.out.println("标准索引: " + criterionIndex);
            System.out.println("自定义提示词: " + (customPrompt != null ? "有" : "无"));
            
            Question question = questionService.getQuestionById(questionId);
            
            // 构建单个评分标准生成的提示词
            String singleCriterionPrompt = buildSingleCriterionPrompt(question, criterionIndex, customPrompt);
            
            // 调用AI生成单个评分标准
            List<RubricSuggestionResponse> suggestions = aiEvaluationService.generateRubricSuggestions(question, singleCriterionPrompt);
            
            if (suggestions != null && !suggestions.isEmpty()) {
                RubricSuggestionResponse regeneratedCriterion = suggestions.get(0);
                System.out.println("成功重新生成评分标准: " + regeneratedCriterion.getCriterionText());
                return ResponseEntity.ok(regeneratedCriterion);
            } else {
                System.out.println("AI生成返回空结果，使用fallback");
                // 生成fallback标准
                RubricSuggestionResponse fallback = new RubricSuggestionResponse(
                    "重新生成的评分标准", 
                    BigDecimal.valueOf(5.0)
                );
                return ResponseEntity.ok(fallback);
            }
            
        } catch (Exception e) {
            System.err.println("重新生成单个评分标准失败: " + e.getMessage());
            e.printStackTrace();
            
            // 返回默认标准
            RubricSuggestionResponse defaultCriterion = new RubricSuggestionResponse(
                "评分标准（生成失败）", 
                BigDecimal.valueOf(5.0)
            );
            return ResponseEntity.ok(defaultCriterion);
        }
    }
    
    /**
     * 构建单个评分标准生成的提示词
     */
    private String buildSingleCriterionPrompt(Question question, String criterionIndex, String customPrompt) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的教育评估专家，请为以下题目重新生成一个评分标准。\n\n");
        
        prompt.append("=== 题目信息 ===\n");
        prompt.append("题目类型：").append(getQuestionTypeDescription(question.getQuestionType())).append("\n");
        prompt.append("题目内容：").append(question.getContent() != null ? question.getContent() : "").append("\n");
        prompt.append("总分：").append(question.getMaxScore()).append("分\n");
        
        if (question.getReferenceAnswer() != null && !question.getReferenceAnswer().trim().isEmpty()) {
            prompt.append("参考答案：").append(question.getReferenceAnswer()).append("\n");
        }
        
        prompt.append("\n=== 生成要求 ===\n");
        prompt.append("请生成一个针对题目具体内容的评分标准\n");
        if (criterionIndex != null && !criterionIndex.trim().isEmpty()) {
            prompt.append("标准编号：第").append(criterionIndex).append("个评分标准\n");
        }
        
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            prompt.append("特殊要求：").append(customPrompt).append("\n");
        }
        
        prompt.append("\n要求：\n");
        prompt.append("1. 评分标准必须针对题目的具体内容和知识点\n");
        prompt.append("2. 避免使用通用词汇，要体现题目的核心要素\n");
        prompt.append("3. 分值建议在1-10分之间\n");
        prompt.append("4. 标准名称要明确具体\n");
        
        prompt.append("\n=== 输出格式 ===\n");
        prompt.append("请严格按照以下JSON格式返回结果（只返回一个评分标准）：\n");
        prompt.append("{\n");
        prompt.append("  \"rubrics\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"评分标准名称\",\n");
        prompt.append("      \"points\": 分值(数字)\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * 获取题目类型描述
     */
    private String getQuestionTypeDescription(QuestionType questionType) {
        switch (questionType) {
            case SINGLE_CHOICE:
                return "单选题";
            case MULTIPLE_CHOICE:
                return "多选题";
            case TRUE_FALSE:
                return "判断题";
            case SHORT_ANSWER:
                return "简答题";
            case ESSAY:
                return "论述题";
            case FILL_BLANK:
                return "填空题";
            case CODING:
                return "编程题";
            case CALCULATION:
                return "计算题";
            case CASE_ANALYSIS:
                return "案例分析题";
            default:
                return "其他类型";
        }
    }
    
    @PatchMapping("/{questionId}/confirm-ai-organized")
    @Operation(summary = "确认AI整理的题目", description = "确认AI从互联网整理的题目内容正确")
    public ResponseEntity<QuestionResponse> confirmAIOrganizedQuestion(@PathVariable Long questionId) {
        try {
            Question question = questionService.getQuestionById(questionId);
            
            // 验证题目来源类型
            if (!"AI_ORGANIZED".equals(question.getSourceType())) {
                return ResponseEntity.badRequest().build();
            }
            
            // 设置为已确认
            question.setIsConfirmed(true);
            Question updatedQuestion = questionService.updateQuestion(questionId, question);
            
            QuestionResponse response = convertToResponse(updatedQuestion);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("确认AI整理题目失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

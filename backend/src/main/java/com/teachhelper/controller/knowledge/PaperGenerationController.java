package com.teachhelper.controller.knowledge;

import com.teachhelper.dto.request.PaperGenerationRequest;
import com.teachhelper.dto.request.PaperGenerationTemplateRequest;
import com.teachhelper.dto.response.PaperGenerationResponse;
import com.teachhelper.dto.response.PaperGenerationTemplateResponse;
import com.teachhelper.entity.PaperGenerationTemplate;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionOption;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.User;
import com.teachhelper.service.knowledge.PaperGenerationService;
import com.teachhelper.service.knowledge.PaperGenerationTemplateService;
import com.teachhelper.util.SecurityUtils;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷生成Controller
 */
@RestController
@RequestMapping("/api/knowledge/paper-generation")
@Tag(name = "试卷生成", description = "智能试卷生成相关API")
@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
public class PaperGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaperGenerationController.class);
    
    @Autowired
    private PaperGenerationService paperGenerationService;
    
    @Autowired
    private PaperGenerationTemplateService templateService;
    
    /**
     * 生成试卷
     */
    @PostMapping("/generate")
    @Operation(summary = "生成试卷", description = "基于配置参数智能生成试卷")
    public ResponseEntity<PaperGenerationResponse> generatePaper(
            @Valid @RequestBody PaperGenerationRequest request) {
        
        logger.info("收到试卷生成请求: {}", request.getTitle());
        
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getCurrentUserId();
            
            // 转换请求为配置对象
            PaperGenerationService.PaperConfig config = convertToConfig(request);
            
            // 生成试卷
            PaperGenerationService.PaperGenerationResult result = 
                paperGenerationService.generatePaper(request.getTitle(), request.getDescription(), config, userId);
            
            // 转换为响应对象
            PaperGenerationResponse response = convertToResponse(result, request);
            
            logger.info("试卷生成成功: {}, 题目数量: {}", request.getTitle(), response.getQuestions().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("试卷生成失败: " + request.getTitle(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 基于模板生成试卷
     */
    @PostMapping("/generate-from-template/{templateId}")
    @Operation(summary = "基于模板生成试卷", description = "使用预设模板生成试卷")
    public ResponseEntity<PaperGenerationResponse> generatePaperFromTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "试卷标题") @RequestParam String paperTitle) {
        
        logger.info("基于模板生成试卷: 模板ID={}, 标题={}", templateId, paperTitle);
        
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getCurrentUserId();
            
            // 查找模板
            PaperGenerationTemplate template = templateService.findTemplateById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
            
            // 生成试卷
            PaperGenerationService.PaperGenerationResult result = 
                paperGenerationService.generatePaperFromTemplate(template, paperTitle, userId);
            
            // 转换为响应对象
            PaperGenerationResponse response = convertToResponseFromTemplate(result, template);
            
            logger.info("基于模板生成试卷成功: {}", paperTitle);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("基于模板生成试卷失败: 模板ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 创建试卷生成模板
     */
    @PostMapping("/templates")
    @Operation(summary = "创建试卷生成模板", description = "创建可重复使用的试卷生成模板")
    public ResponseEntity<PaperGenerationTemplateResponse> createTemplate(
            @Valid @RequestBody PaperGenerationTemplateRequest request) {
        
        logger.info("创建试卷生成模板: {}", request.getName());
        
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getCurrentUserId();
            
            // 转换请求为实体
            PaperGenerationTemplate template = convertToTemplate(request, userId);
            
            // 验证模板配置
            if (!templateService.validateTemplate(template)) {
                return ResponseEntity.badRequest().build();
            }
            
            // 创建模板
            PaperGenerationTemplate savedTemplate = templateService.createTemplate(template);
            
            // 转换为响应对象
            PaperGenerationTemplateResponse response = convertToTemplateResponse(savedTemplate);
            
            logger.info("试卷生成模板创建成功: {}", request.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建试卷生成模板失败: " + request.getName(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新试卷生成模板
     */
    @PutMapping("/templates/{templateId}")
    @Operation(summary = "更新试卷生成模板", description = "更新已存在的试卷生成模板")
    public ResponseEntity<PaperGenerationTemplateResponse> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody PaperGenerationTemplateRequest request) {
        
        logger.info("更新试卷生成模板: ID={}", templateId);
        
        try {
            // 转换请求为实体
            PaperGenerationTemplate template = convertToTemplate(request, null);
            
            // 验证模板配置
            if (!templateService.validateTemplate(template)) {
                return ResponseEntity.badRequest().build();
            }
            
            // 更新模板
            PaperGenerationTemplate updatedTemplate = templateService.updateTemplate(templateId, template);
            
            // 转换为响应对象
            PaperGenerationTemplateResponse response = convertToTemplateResponse(updatedTemplate);
            
            logger.info("试卷生成模板更新成功: ID={}", templateId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新试卷生成模板失败: ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除试卷生成模板
     */
    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "删除试卷生成模板", description = "删除指定的试卷生成模板")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        
        logger.info("删除试卷生成模板: ID={}", templateId);
        
        try {
            templateService.deleteTemplate(templateId);
            
            logger.info("试卷生成模板删除成功: ID={}", templateId);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("删除试卷生成模板失败: ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取模板详情
     */
    @GetMapping("/templates/{templateId}")
    @Operation(summary = "获取模板详情", description = "获取指定试卷生成模板的详细信息")
    public ResponseEntity<PaperGenerationTemplateResponse> getTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        
        try {
            PaperGenerationTemplate template = templateService.findTemplateById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
            
            PaperGenerationTemplateResponse response = convertToTemplateResponse(template);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取模板详情失败: ID=" + templateId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取用户的模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "获取模板列表", description = "获取当前用户可访问的所有试卷生成模板")
    public ResponseEntity<List<PaperGenerationTemplateResponse>> getTemplates(
            @Parameter(description = "科目过滤") @RequestParam(required = false) String subject) {
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            
            List<PaperGenerationTemplate> templates;
            if (subject != null && !subject.trim().isEmpty()) {
                templates = templateService.findTemplatesBySubject(subject);
            } else {
                templates = templateService.findAccessibleTemplates(userId);
            }
            
            List<PaperGenerationTemplateResponse> responses = templates.stream()
                .map(this::convertToTemplateResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("获取模板列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 复制模板
     */
    @PostMapping("/templates/{templateId}/duplicate")
    @Operation(summary = "复制模板", description = "复制已存在的试卷生成模板")
    public ResponseEntity<PaperGenerationTemplateResponse> duplicateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        
        logger.info("复制试卷生成模板: ID={}", templateId);
        
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            
            PaperGenerationTemplate duplicatedTemplate = templateService.duplicateTemplate(templateId, userId);
            
            PaperGenerationTemplateResponse response = convertToTemplateResponse(duplicatedTemplate);
            
            logger.info("试卷生成模板复制成功: 原ID={}, 新ID={}", templateId, duplicatedTemplate.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("复制试卷生成模板失败: ID=" + templateId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 转换请求为配置对象
     */
    private PaperGenerationService.PaperConfig convertToConfig(PaperGenerationRequest request) {
        PaperGenerationService.PaperConfig config = new PaperGenerationService.PaperConfig();
        
        config.setSubject(request.getSubject());
        config.setGradeLevel(request.getGradeLevel());
        config.setTotalScore(request.getTotalScore());
        config.setTimeLimit(request.getTimeLimit());
        config.setCustomRequirements(request.getCustomRequirements());
        config.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        
        // 转换题型配置
        List<PaperGenerationService.QuestionTypeConfig> questionTypes = request.getQuestionTypes().stream()
            .map(this::convertToQuestionTypeConfig)
            .collect(Collectors.toList());
        config.setQuestionTypes(questionTypes);
        
        // 转换难度配置
        if (request.getDifficulty() != null) {
            PaperGenerationService.DifficultyConfig difficultyConfig = new PaperGenerationService.DifficultyConfig();
            difficultyConfig.setEasyRatio(request.getDifficulty().getEasyRatio());
            difficultyConfig.setMediumRatio(request.getDifficulty().getMediumRatio());
            difficultyConfig.setHardRatio(request.getDifficulty().getHardRatio());
            config.setDifficultyConfig(difficultyConfig);
        }
        
        return config;
    }
    
    /**
     * 转换题型配置
     */
    private PaperGenerationService.QuestionTypeConfig convertToQuestionTypeConfig(
            PaperGenerationRequest.QuestionTypeConfigRequest request) {
        PaperGenerationService.QuestionTypeConfig config = new PaperGenerationService.QuestionTypeConfig();
        config.setType(request.getType().name()); // 将枚举转换为字符串
        config.setCount(request.getCount());
        config.setScorePerQuestion(request.getScorePerQuestion());
        config.setRequirements(request.getRequirements());
        return config;
    }
    
    /**
     * 转换生成结果为响应对象
     */
    private PaperGenerationResponse convertToResponse(PaperGenerationService.PaperGenerationResult result,
                                                    PaperGenerationRequest request) {
        PaperGenerationResponse response = new PaperGenerationResponse();
        
        response.setExamId(result.getExam().getId());
        response.setExamTitle(result.getExam().getTitle());
        response.setExamDescription(result.getExam().getDescription());
        response.setSubject(request.getSubject());
        response.setGradeLevel(request.getGradeLevel());
        response.setTotalScore(request.getTotalScore());
        response.setTimeLimit(request.getTimeLimit());
        response.setWarnings(result.getWarnings());
        response.setGenerationSummary(result.getGenerationSummary());
        
        // 转换题目列表
        List<PaperGenerationResponse.GeneratedQuestionResponse> questions = result.getQuestions().stream()
            .map(this::convertToQuestionResponse)
            .collect(Collectors.toList());
        response.setQuestions(questions);
        
        // 生成统计信息
        response.setStatistics(generateStatistics(result.getQuestions()));
        
        return response;
    }
    
    /**
     * 转换生成结果为响应对象（基于模板）
     */
    private PaperGenerationResponse convertToResponseFromTemplate(PaperGenerationService.PaperGenerationResult result,
                                                               PaperGenerationTemplate template) {
        PaperGenerationResponse response = new PaperGenerationResponse();
        
        response.setExamId(result.getExam().getId());
        response.setExamTitle(result.getExam().getTitle());
        response.setExamDescription(result.getExam().getDescription());
        response.setSubject(template.getSubject());
        response.setGradeLevel(template.getGradeLevel());
        response.setTotalScore(template.getTotalScore());
        response.setTimeLimit(template.getTimeLimit());
        response.setWarnings(result.getWarnings());
        response.setGenerationSummary(result.getGenerationSummary());
        
        // 转换题目列表
        List<PaperGenerationResponse.GeneratedQuestionResponse> questions = result.getQuestions().stream()
            .map(this::convertToQuestionResponse)
            .collect(Collectors.toList());
        response.setQuestions(questions);
        
        // 生成统计信息
        response.setStatistics(generateStatistics(result.getQuestions()));
        
        return response;
    }
    
    /**
     * 转换题目为响应对象
     */
    private PaperGenerationResponse.GeneratedQuestionResponse convertToQuestionResponse(
            PaperGenerationService.GeneratedQuestion question) {
        PaperGenerationResponse.GeneratedQuestionResponse response = 
            new PaperGenerationResponse.GeneratedQuestionResponse();
        
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setType(QuestionType.valueOf(question.getType())); // 将字符串转换为枚举
        response.setMaxScore(question.getMaxScore());
        response.setReferenceAnswer(question.getReferenceAnswer());
        response.setDifficulty(question.getDifficulty());
        response.setKnowledgePoint(question.getKnowledgePoint());
        response.setSourceDocument(question.getSourceDocument());
        
        // 转换选项
        if (question.getOptions() != null) {
            List<PaperGenerationResponse.QuestionOptionResponse> options = question.getOptions().stream()
                .map(this::convertStringToOptionResponse)
                .collect(Collectors.toList());
            response.setOptions(options);
        }
        
        return response;
    }
    
    /**
     * 转换选项为响应对象
     */
    private PaperGenerationResponse.QuestionOptionResponse convertToOptionResponse(QuestionOption option) {
        PaperGenerationResponse.QuestionOptionResponse response = 
            new PaperGenerationResponse.QuestionOptionResponse();
        
        response.setOptionId(option.getId());
        response.setContent(option.getContent());
        response.setIsCorrect(option.getIsCorrect());
        response.setOptionOrder(option.getOptionOrder());
        
        return response;
    }
    
    /**
     * 转换字符串选项为响应对象
     */
    private PaperGenerationResponse.QuestionOptionResponse convertStringToOptionResponse(String optionText) {
        PaperGenerationResponse.QuestionOptionResponse response = 
            new PaperGenerationResponse.QuestionOptionResponse();
        
        response.setOptionId(null); // 字符串选项没有ID
        response.setContent(optionText);
        response.setIsCorrect(false); // 默认不正确，实际实现时需要更智能的判断
        response.setOptionOrder(null); // 字符串选项没有顺序
        
        return response;
    }
    
    /**
     * 转换请求为模板实体
     */
    private PaperGenerationTemplate convertToTemplate(PaperGenerationTemplateRequest request, Long createdBy) {
        PaperGenerationTemplate template = new PaperGenerationTemplate();
        
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubject(request.getSubject());
        template.setGradeLevel(request.getGradeLevel());
        template.setTotalScore(request.getTotalScore());
        template.setTimeLimit(request.getTimeLimit());
        template.setQuestionConfig(request.getQuestionConfig());
        template.setDifficultyConfig(request.getDifficultyConfig());
        template.setKnowledgeBaseConfig(request.getKnowledgeBaseConfig());
        template.setCreatedBy(createdBy);
        
        return template;
    }
    
    /**
     * 转换模板为响应对象
     */
    private PaperGenerationTemplateResponse convertToTemplateResponse(PaperGenerationTemplate template) {
        PaperGenerationTemplateResponse response = new PaperGenerationTemplateResponse();
        
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setSubject(template.getSubject());
        response.setGradeLevel(template.getGradeLevel());
        response.setTotalScore(template.getTotalScore());
        response.setTimeLimit(template.getTimeLimit());
        response.setQuestionConfig(template.getQuestionConfig());
        response.setDifficultyConfig(template.getDifficultyConfig());
        response.setKnowledgeBaseConfig(template.getKnowledgeBaseConfig());
        response.setCreatedBy(template.getCreatedBy());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setIsPublic(template.getCreatedBy() == null || template.getCreatedBy() == 0);
        
        // 设置创建者姓名
        if (template.getCreator() != null) {
            response.setCreatorName(template.getCreator().getUsername());
        }
        
        return response;
    }
    
    /**
     * 生成统计信息
     */
    private PaperGenerationResponse.GenerationStatistics generateStatistics(
            List<PaperGenerationService.GeneratedQuestion> questions) {
        PaperGenerationResponse.GenerationStatistics statistics = 
            new PaperGenerationResponse.GenerationStatistics();
        
        statistics.setTotalQuestions(questions.size());
        
        // 题型统计
        PaperGenerationResponse.GenerationStatistics.TypeStatistics typeStats = 
            new PaperGenerationResponse.GenerationStatistics.TypeStatistics();
        // 这里可以根据实际需要实现详细的统计逻辑
        statistics.setTypeStatistics(typeStats);
        
        // 难度统计
        PaperGenerationResponse.GenerationStatistics.DifficultyStatistics difficultyStats = 
            new PaperGenerationResponse.GenerationStatistics.DifficultyStatistics();
        // 这里可以根据实际需要实现详细的统计逻辑
        statistics.setDifficultyStatistics(difficultyStats);
        
        // 分值统计
        PaperGenerationResponse.GenerationStatistics.ScoreStatistics scoreStats = 
            new PaperGenerationResponse.GenerationStatistics.ScoreStatistics();
        // 这里可以根据实际需要实现详细的统计逻辑
        statistics.setScoreStatistics(scoreStats);
        
        return statistics;
    }
}

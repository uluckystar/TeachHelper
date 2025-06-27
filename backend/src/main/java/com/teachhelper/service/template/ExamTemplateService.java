package com.teachhelper.service.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.dto.request.ExamTemplateRequest;
import com.teachhelper.dto.request.ExamTemplateQuestionUpdateRequest;
import com.teachhelper.dto.request.ExamCreateRequest;
import com.teachhelper.dto.response.ExamTemplateResponse;
import com.teachhelper.entity.*;
import com.teachhelper.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(ExamTemplateService.class);

    @Autowired
    private ExamTemplateRepository examTemplateRepository;
    
    @Autowired
    private ExamTemplateQuestionRepository examTemplateQuestionRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取用户的所有模板
     */
    public Page<ExamTemplateResponse> getUserTemplates(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExamTemplate> templates = examTemplateRepository.findByCreatedByOrderByCreatedAtDesc(user, pageable);
        return templates.map(this::convertToResponse);
    }

    /**
     * 根据ID获取模板详情
     */
    public ExamTemplateResponse getTemplateById(Long id) {
        ExamTemplate template = examTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + id));
        return convertToResponse(template);
    }

    /**
     * 更新模板信息
     */
    @Transactional
    public ExamTemplateResponse updateTemplate(Long id, ExamTemplateRequest request, User user) {
        ExamTemplate template = examTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + id));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查状态，只有DRAFT状态可以修改基本信息
        if (template.getStatus() != ExamTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的模板可以修改");
        }
        
        // 更新字段
        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getExamTitle() != null) {
            template.setExamTitle(request.getExamTitle());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        
        template.setUpdatedAt(LocalDateTime.now());
        template = examTemplateRepository.save(template);
        
        return convertToResponse(template);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id, User user) {
        ExamTemplate template = examTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + id));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限删除此模板");
        }
        
        // 检查是否已应用，已应用的模板不能删除，只能归档
        if (template.getStatus() == ExamTemplate.TemplateStatus.APPLIED) {
            throw new RuntimeException("已应用的模板不能删除，请先归档");
        }
        
        examTemplateRepository.delete(template);
        log.info("删除模板: {} by user: {}", template.getTemplateName(), user.getUsername());
    }

    /**
     * 手动匹配题目到题库
     */
    @Transactional
    public void matchQuestionToBank(Long templateId, Integer questionNumber, Long questionId, User user) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查状态
        if (template.getStatus() != ExamTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的模板可以匹配题目");
        }
        
        // 获取模板题目
        ExamTemplateQuestion templateQuestion = examTemplateQuestionRepository
                .findByExamTemplateAndQuestionNumber(template, questionNumber)
                .orElseThrow(() -> new RuntimeException("模板题目不存在: " + questionNumber));
        
        // 获取题库题目
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("题库题目不存在: " + questionId));
        
        // 执行匹配
        templateQuestion.setMatchedQuestion(question);
        templateQuestion.setIsMatched(true);
        templateQuestion.setMatchingStrategy("MANUAL");
        templateQuestion.setMatchingConfidence(1.0);
        templateQuestion.setMatchingReason("手动匹配");
        templateQuestion.setUpdatedAt(LocalDateTime.now());
        
        examTemplateQuestionRepository.save(templateQuestion);
        
        // 更新模板匹配统计
        updateTemplateMatchingStats(template);
        
        log.info("手动匹配题目: 模板{} 题号{} -> 题库{}", templateId, questionNumber, questionId);
    }

    /**
     * 取消题目匹配
     */
    @Transactional
    public void unmatchQuestion(Long templateId, Integer questionNumber, User user) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查状态
        if (template.getStatus() != ExamTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的模板可以取消匹配");
        }
        
        ExamTemplateQuestion templateQuestion = examTemplateQuestionRepository
                .findByExamTemplateAndQuestionNumber(template, questionNumber)
                .orElseThrow(() -> new RuntimeException("模板题目不存在: " + questionNumber));
        
        // 取消匹配
        templateQuestion.setMatchedQuestion(null);
        templateQuestion.setIsMatched(false);
        templateQuestion.setMatchingStrategy(null);
        templateQuestion.setMatchingConfidence(null);
        templateQuestion.setMatchingReason(null);
        templateQuestion.setUpdatedAt(LocalDateTime.now());
        
        examTemplateQuestionRepository.save(templateQuestion);
        
        // 更新模板匹配统计
        updateTemplateMatchingStats(template);
        
        log.info("取消题目匹配: 模板{} 题号{}", templateId, questionNumber);
    }

    /**
     * 标记模板为就绪状态
     */
    @Transactional
    public ExamTemplateResponse markTemplateReady(Long templateId, User user) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查当前状态
        if (template.getStatus() != ExamTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的模板可以标记为就绪");
        }
        
        // 检查是否所有题目都已匹配
        if (!template.isReadyForApplication()) {
            throw new RuntimeException("还有未匹配的题目，无法标记为就绪状态");
        }
        
        template.setStatus(ExamTemplate.TemplateStatus.READY);
        template.setUpdatedAt(LocalDateTime.now());
        template = examTemplateRepository.save(template);
        
        log.info("模板标记为就绪: {} by user: {}", template.getTemplateName(), user.getUsername());
        
        return convertToResponse(template);
    }

    /**
     * 获取模板的题目列表
     */
    public List<ExamTemplateQuestion> getTemplateQuestions(Long templateId) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        return examTemplateQuestionRepository.findByExamTemplateOrderByQuestionNumber(template);
    }

    /**
     * 搜索候选题目以供匹配
     */
    public List<com.teachhelper.dto.response.QuestionSummaryDto> searchCandidateQuestions(Long templateId, Integer questionNumber, String keyword, int limit) {
        // 直接从数据库查询，使用简单的JPQL避免实体关联
        String jpql = "SELECT q.id, q.title, q.content, q.questionType, q.difficulty, q.createdAt FROM Question q WHERE q.isActive = true";
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql += " AND (q.title LIKE :keyword OR q.content LIKE :keyword)";
        }
        
        jpql += " ORDER BY q.createdAt DESC";
        
        jakarta.persistence.Query query = entityManager.createQuery(jpql);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        
        query.setMaxResults(limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    com.teachhelper.dto.response.QuestionSummaryDto dto = new com.teachhelper.dto.response.QuestionSummaryDto();
                    dto.setId((Long) row[0]);
                    dto.setTitle((String) row[1]);
                    dto.setContent((String) row[2]);
                    dto.setQuestionType(row[3] != null ? row[3].toString() : null);
                    dto.setDifficulty(row[4] != null ? row[4].toString() : null);
                    dto.setCreatedAt((java.time.LocalDateTime) row[5]);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // 私有辅助方法
    private void updateTemplateMatchingStats(ExamTemplate template) {
        List<ExamTemplateQuestion> questions = examTemplateQuestionRepository
                .findByExamTemplate(template);
        
        int totalQuestions = questions.size();
        int matchedQuestions = (int) questions.stream()
                .filter(q -> q.getIsMatched() != null && q.getIsMatched())
                .count();
        
        template.setTotalQuestions(totalQuestions);
        template.setMatchedQuestions(matchedQuestions);
        template.setUpdatedAt(LocalDateTime.now());
        
        examTemplateRepository.save(template);
    }

    /**
     * 从题目内容推断题目类型
     */
    private String inferQuestionType(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        String lowerContent = content.toLowerCase();
        
        // 判断题
        if (lowerContent.contains("对") && lowerContent.contains("错") ||
            lowerContent.contains("正确") && lowerContent.contains("错误") ||
            lowerContent.contains("√") && lowerContent.contains("×") ||
            lowerContent.contains("true") && lowerContent.contains("false")) {
            return "JUDGE";
        }
        
        // 选择题（单选）
        if (lowerContent.contains("选择") || 
            (lowerContent.contains("a.") && lowerContent.contains("b.")) ||
            (lowerContent.contains("a）") && lowerContent.contains("b）")) ||
            (lowerContent.contains("(a)") && lowerContent.contains("(b)"))) {
            return "SINGLE_CHOICE";
        }
        
        // 填空题
        if (lowerContent.contains("_____") || lowerContent.contains("______") ||
            lowerContent.contains("填空") || content.contains("（）")) {
            return "FILL_BLANK";
        }
        
        // 简答题/主观题
        if (lowerContent.contains("简答") || lowerContent.contains("论述") ||
            lowerContent.contains("分析") || lowerContent.contains("说明") ||
            lowerContent.contains("阐述")) {
            return "ESSAY";
        }
        
        // 默认为简答题
        return "ESSAY";
    }

    /**
     * 转换Question实体为QuestionSummaryDto
     */
    private com.teachhelper.dto.response.QuestionSummaryDto convertToQuestionSummary(Question question) {
        com.teachhelper.dto.response.QuestionSummaryDto dto = new com.teachhelper.dto.response.QuestionSummaryDto();
        dto.setId(question.getId());
        dto.setTitle(question.getTitle());
        dto.setContent(question.getContent());
        dto.setQuestionType(question.getQuestionType() != null ? question.getQuestionType().name() : null);
        dto.setDifficulty(question.getDifficulty() != null ? question.getDifficulty().name() : null);
        dto.setCreatedAt(question.getCreatedAt());
        
        // 简化的选项内容，避免循环引用
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            java.util.List<String> optionContents = question.getOptions().stream()
                    .map(option -> option.getContent())
                    .collect(java.util.stream.Collectors.toList());
            dto.setOptionContents(optionContents);
        }
        
        return dto;
    }

    private ExamTemplateResponse convertToResponse(ExamTemplate template) {
        ExamTemplateResponse response = new ExamTemplateResponse();
        response.setId(template.getId());
        response.setTemplateName(template.getTemplateName());
        response.setSubject(template.getSubject());
        response.setExamTitle(template.getExamTitle());
        response.setDescription(template.getDescription());
        response.setTotalQuestions(template.getTotalQuestions());
        response.setMatchedQuestions(template.getMatchedQuestions());
        response.setStatus(template.getStatus().name());
        response.setCreatedTime(template.getCreatedAt());
        response.setUpdatedTime(template.getUpdatedAt());
        response.setMatchingProgress(template.getMatchingProgress());
        
        // 解析源文件列表
        if (template.getSourceFiles() != null) {
            try {
                List<String> sourceFiles = objectMapper.readValue(template.getSourceFiles(), new TypeReference<List<String>>() {});
                response.setSourceFiles(sourceFiles);
            } catch (Exception e) {
                log.warn("解析源文件列表失败: {}", e.getMessage());
                response.setSourceFiles(new ArrayList<>());
            }
        }
        
        return response;
    }

    /**
     * 更新模板中的单个题目
     * @return 
     */
    @Transactional
    public ExamTemplateQuestion updateTemplateQuestion(Long templateId, Long questionId, ExamTemplateQuestionUpdateRequest request, User user) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        ExamTemplateQuestion question = examTemplateQuestionRepository.findByIdAndExamTemplate(questionId, template)
                .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        // 更新字段
        question.setSectionHeader(request.getSectionHeader());
        question.setQuestionContent(request.getQuestionContent());
        question.setQuestionType(request.getQuestionType());
        if (request.getScore() != null) {
            question.setScore(request.getScore().doubleValue());
        }
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setExplanation(request.getExplanation());
        question.setUpdatedAt(LocalDateTime.now());
        
        // 如果题目类型或段落标题变化，可能需要重新推断
        if (request.getQuestionType() != null && !request.getQuestionType().equals(question.getQuestionType())) {
            // 类型变化，可以触发一些逻辑
        }
        
        return examTemplateQuestionRepository.save(question);
    }
    
    /**
     * 确认模板题目
     */
    @Transactional
    public void confirmTemplateQuestion(Long templateId, Long questionId, User user) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        ExamTemplateQuestion templateQuestion = examTemplateQuestionRepository.findByIdAndExamTemplate(questionId, template)
                .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        // 如果题目尚未匹配到题库，则根据模板题目自动创建并匹配
        if (templateQuestion.getMatchedQuestion() == null) {
            Question newQuestion = new Question();
            newQuestion.setTitle(templateQuestion.getSectionHeader() + " - " + templateQuestion.getQuestionNumber());
            newQuestion.setContent(templateQuestion.getQuestionContent());
            newQuestion.setQuestionType(safeConvertQuestionType(templateQuestion.getQuestionType()));
            
            if (templateQuestion.getScore() != null) {
                newQuestion.setMaxScore(BigDecimal.valueOf(templateQuestion.getScore()));
            } else {
                newQuestion.setMaxScore(BigDecimal.ZERO); // 默认分
            }
            
            newQuestion.setReferenceAnswer(templateQuestion.getCorrectAnswer());
            
            // 解析选项
            if (templateQuestion.getOptions() != null && !templateQuestion.getOptions().isEmpty()) {
                List<QuestionOption> options = Arrays.stream(templateQuestion.getOptions().split("\\r?\\n"))
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> {
                        QuestionOption option = new QuestionOption();
                        option.setContent(line.trim());
                        option.setQuestion(newQuestion);
                        return option;
                    })
                    .collect(Collectors.toList());
                newQuestion.setOptions(options);
            }

            // 设置其他属性
            newQuestion.setCreatedBy(user.getId());
            newQuestion.setCreatedAt(LocalDateTime.now());
            newQuestion.setUpdatedAt(LocalDateTime.now());
            newQuestion.setIsActive(true);
            newQuestion.setSourceType("TEMPLATE_CONFIRM");
            
            Question savedQuestion = questionRepository.save(newQuestion);
            
            templateQuestion.setMatchedQuestion(savedQuestion);
            templateQuestion.setIsMatched(true);
            templateQuestion.setMatchingStrategy("AUTO_CONFIRM");
        }
        
        templateQuestion.setIsConfirmed(true);
        templateQuestion.setUpdatedAt(LocalDateTime.now());
        examTemplateQuestionRepository.save(templateQuestion);
        
        // 更新模板确认统计
        updateTemplateConfirmStats(template);
        
        log.info("确认模板题目成功: 模板{} 题目{}", templateId, questionId);
    }
    
    private QuestionType safeConvertQuestionType(String typeStr) {
        if (typeStr == null) {
            return QuestionType.SHORT_ANSWER; // 默认值
        }
        try {
            // 尝试直接用Enum名称匹配 (e.g., "SINGLE_CHOICE")
            return QuestionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果名称不匹配，则根据中文描述转换
            switch (typeStr) {
                case "单选题":
                    return QuestionType.SINGLE_CHOICE;
                case "多选题":
                    return QuestionType.MULTIPLE_CHOICE;
                case "判断题":
                    return QuestionType.TRUE_FALSE;
                case "填空题":
                    return QuestionType.FILL_BLANK;
                case "简答题":
                case "论述题":
                case "分析题":
                    return QuestionType.SHORT_ANSWER;
                case "计算题":
                    return QuestionType.CALCULATION;
                default:
                    return QuestionType.SHORT_ANSWER; // 使用一个通用且存在的类型作为默认值
            }
        }
    }
    
    /**
     * 取消确认模板题目
     */
    @Transactional
    public void unconfirmTemplateQuestion(Long templateId, Long questionId, User user) {
        ExamTemplate template = examTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        if (!template.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("无权限修改此模板");
        }

        // 允许在草稿或就绪状态下取消确认
        if (template.getStatus() != ExamTemplate.TemplateStatus.DRAFT && template.getStatus() != ExamTemplate.TemplateStatus.READY) {
            throw new RuntimeException("只有草稿或就绪状态的模板可以取消确认题目");
        }
        
        ExamTemplateQuestion templateQuestion = examTemplateQuestionRepository.findByIdAndExamTemplate(questionId, template)
                .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        templateQuestion.setIsConfirmed(false);
        templateQuestion.setUpdatedAt(LocalDateTime.now());
        examTemplateQuestionRepository.save(templateQuestion);
        
        // 如果模板是就绪状态，则回退到草稿状态
        if (template.getStatus() == ExamTemplate.TemplateStatus.READY) {
            template.setStatus(ExamTemplate.TemplateStatus.DRAFT);
            log.info("模板 {} 状态已从 READY 回退到 DRAFT", template.getTemplateName());
        }

        // 更新模板确认统计
        updateTemplateConfirmStats(template);
        
        log.info("取消确认模板题目成功: 模板{} 题目{}", templateId, questionId);
    }
    
    /**
     * 更新模板确认统计
     */
    private void updateTemplateConfirmStats(ExamTemplate template) {
        List<ExamTemplateQuestion> questions = examTemplateQuestionRepository
                .findByExamTemplate(template);
        
        int totalQuestions = questions.size();
        int confirmedQuestions = (int) questions.stream()
                .filter(q -> q.getIsConfirmed() != null && q.getIsConfirmed())
                .count();
        
        template.setTotalQuestions(totalQuestions);
        template.setMatchedQuestions(confirmedQuestions); // 重用这个字段表示已确认数量
        template.setUpdatedAt(LocalDateTime.now());
        
        examTemplateRepository.save(template);
    }
}
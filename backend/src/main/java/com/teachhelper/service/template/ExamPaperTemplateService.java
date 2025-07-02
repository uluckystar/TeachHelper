package com.teachhelper.service.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.teachhelper.dto.request.ExamPaperTemplateRequest;
import com.teachhelper.dto.request.ExamPaperTemplateQuestionRequest;
import com.teachhelper.dto.response.ExamPaperTemplateResponse;
import com.teachhelper.dto.response.ExamPaperTemplateQuestionResponse;
import com.teachhelper.entity.*;
import com.teachhelper.repository.*;
import com.teachhelper.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.teachhelper.service.answer.LearningAnswerParserService;
import com.teachhelper.service.answer.QuestionScoreParsingService;
import com.teachhelper.dto.request.StudentAnswerImportData;
import com.teachhelper.entity.ExamPaperTemplateQuestion.QuestionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.RoundingMode;

/**
 * 试卷模板服务
 */
@Service
public class ExamPaperTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExamPaperTemplateService.class);
    
    @Autowired
    private ExamPaperTemplateRepository templateRepository;
    
    @Autowired
    private ExamPaperTemplateQuestionRepository templateQuestionRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private QuestionScoreParsingService questionScoreParsingService;
    
    @Autowired
    private LearningAnswerParserService learningAnswerParserService;
    
    @Autowired
    private QuestionBankRepository questionBankRepository;
    
    @Autowired
    private QuestionOptionRepository questionOptionRepository;
    
    /**
     * 创建试卷模板
     */
    @Transactional
    public ExamPaperTemplateResponse createTemplate(ExamPaperTemplateRequest request) {
        logger.info("创建试卷模板: {}", request.getName());
        
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 创建模板实体
        ExamPaperTemplate template = new ExamPaperTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubject(request.getSubject());
        template.setGradeLevel(request.getGradeLevel());
        template.setTotalScore(request.getTotalScore());
        template.setDuration(request.getDuration());
        template.setStatus(request.getStatus() != null ? 
            ExamPaperTemplate.TemplateStatus.valueOf(request.getStatus()) : 
            ExamPaperTemplate.TemplateStatus.DRAFT);
        template.setTemplateType(request.getTemplateType() != null ? 
            ExamPaperTemplate.TemplateType.valueOf(request.getTemplateType()) : 
            ExamPaperTemplate.TemplateType.MANUAL);
        template.setQuestionTypeConfig(request.getQuestionTypeConfig());
        template.setDifficultyConfig(request.getDifficultyConfig());
        template.setKnowledgeBaseConfig(request.getKnowledgeBaseConfig());
        template.setTags(request.getTags());
        template.setIsPublic(request.getIsPublic());
        template.setCreatedBy(userId);
        
        // 保存模板
        template = templateRepository.save(template);
        
        // 创建模板题目
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            createTemplateQuestions(template, request.getQuestions());
        }
        
        return convertToResponse(template);
    }
    
    /**
     * 更新试卷模板
     */
    @Transactional
    public ExamPaperTemplateResponse updateTemplate(Long templateId, ExamPaperTemplateRequest request) {
        logger.info("更新试卷模板: {}", templateId);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 更新基本信息
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubject(request.getSubject());
        template.setGradeLevel(request.getGradeLevel());
        template.setTotalScore(request.getTotalScore());
        template.setDuration(request.getDuration());
        template.setStatus(request.getStatus() != null ? 
            ExamPaperTemplate.TemplateStatus.valueOf(request.getStatus()) : 
            template.getStatus());
        template.setTemplateType(request.getTemplateType() != null ? 
            ExamPaperTemplate.TemplateType.valueOf(request.getTemplateType()) : 
            template.getTemplateType());
        template.setQuestionTypeConfig(request.getQuestionTypeConfig());
        template.setDifficultyConfig(request.getDifficultyConfig());
        template.setKnowledgeBaseConfig(request.getKnowledgeBaseConfig());
        template.setTags(request.getTags());
        template.setIsPublic(request.getIsPublic());
        
        // 保存模板
        template = templateRepository.save(template);
        
        // 更新模板题目
        if (request.getQuestions() != null) {
            updateTemplateQuestions(template, request.getQuestions());
        }
        
        return convertToResponse(template);
    }
    
    /**
     * 删除试卷模板
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        logger.info("删除试卷模板: {}", templateId);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限删除此模板");
        }
        
        // 检查是否有关联的考试
        if (!template.getRelatedExams().isEmpty()) {
            throw new RuntimeException("模板已被使用，无法删除");
        }
        
        templateRepository.delete(template);
    }
    
    /**
     * 获取模板详情
     */
    public ExamPaperTemplateResponse getTemplate(Long templateId) {
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        return convertToResponse(template);
    }
    
    /**
     * 获取用户可访问的模板列表
     */
    public List<ExamPaperTemplateResponse> getUserTemplates() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ExamPaperTemplate> templates = templateRepository.findByCreatedByOrderByCreatedAtDesc(userId);
        
        return templates.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取公开模板列表
     */
    public List<ExamPaperTemplateResponse> getPublicTemplates() {
        List<ExamPaperTemplate> templates = templateRepository.findByIsPublicTrueOrderByUsageCountDescCreatedAtDesc();
        
        return templates.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取可用模板列表
     */
    public List<ExamPaperTemplateResponse> getUsableTemplates() {
        List<ExamPaperTemplate> templates = templateRepository.findUsableTemplatesOrderByUsageCountDesc();
        
        return templates.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 搜索模板
     */
    public List<ExamPaperTemplateResponse> searchTemplates(String keyword, String subject, String gradeLevel) {
        List<ExamPaperTemplate> templates = new ArrayList<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            templates.addAll(templateRepository.findByNameContainingOrderByCreatedAtDesc(keyword));
        } else if (subject != null && !subject.trim().isEmpty()) {
            if (gradeLevel != null && !gradeLevel.trim().isEmpty()) {
                templates.addAll(templateRepository.findBySubjectAndGradeLevelOrderByCreatedAtDesc(subject, gradeLevel));
            } else {
                templates.addAll(templateRepository.findBySubjectOrderByCreatedAtDesc(subject));
            }
        } else {
            templates.addAll(templateRepository.findUsableTemplatesOrderByUsageCountDesc());
        }
        
        return templates.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 复制模板
     */
    @Transactional
    public ExamPaperTemplateResponse duplicateTemplate(Long templateId) {
        logger.info("复制试卷模板: {}", templateId);
        
        ExamPaperTemplate original = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 创建新模板
        ExamPaperTemplate newTemplate = new ExamPaperTemplate();
        newTemplate.setName(original.getName() + " (副本)");
        newTemplate.setDescription(original.getDescription());
        newTemplate.setSubject(original.getSubject());
        newTemplate.setGradeLevel(original.getGradeLevel());
        newTemplate.setTotalScore(original.getTotalScore());
        newTemplate.setDuration(original.getDuration());
        newTemplate.setStatus(ExamPaperTemplate.TemplateStatus.DRAFT);
        newTemplate.setTemplateType(ExamPaperTemplate.TemplateType.COPIED);
        newTemplate.setQuestionTypeConfig(original.getQuestionTypeConfig());
        newTemplate.setDifficultyConfig(original.getDifficultyConfig());
        newTemplate.setKnowledgeBaseConfig(original.getKnowledgeBaseConfig());
        newTemplate.setTags(original.getTags());
        newTemplate.setIsPublic(false);
        newTemplate.setCreatedBy(userId);
        
        newTemplate = templateRepository.save(newTemplate);
        
        // 复制模板题目
        List<ExamPaperTemplateQuestion> originalQuestions = templateQuestionRepository.findByTemplateIdOrderByQuestionOrderAsc(templateId);
        for (ExamPaperTemplateQuestion originalQuestion : originalQuestions) {
            ExamPaperTemplateQuestion newQuestion = new ExamPaperTemplateQuestion();
            newQuestion.setTemplate(newTemplate);
            newQuestion.setQuestionOrder(originalQuestion.getQuestionOrder());
            newQuestion.setQuestionType(originalQuestion.getQuestionType());
            newQuestion.setQuestionContent(originalQuestion.getQuestionContent());
            newQuestion.setQuestionId(originalQuestion.getQuestionId());
            newQuestion.setScore(originalQuestion.getScore());
            newQuestion.setDifficultyLevel(originalQuestion.getDifficultyLevel());
            newQuestion.setKnowledgeTags(originalQuestion.getKnowledgeTags());
            newQuestion.setQuestionConfig(originalQuestion.getQuestionConfig());
            newQuestion.setIsRequired(originalQuestion.getIsRequired());
            newQuestion.setStatus(ExamPaperTemplateQuestion.QuestionStatus.DRAFT);
            
            templateQuestionRepository.save(newQuestion);
        }
        
        return convertToResponse(newTemplate);
    }
    
    /**
     * 应用模板创建考试
     */
    @Transactional
    public Exam applyTemplateToExam(Long templateId, String examTitle, String examDescription) {
        logger.info("应用模板创建考试: 模板ID={}, 考试标题={}", templateId, examTitle);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查模板是否可用
        if (!template.isUsable()) {
            throw new RuntimeException("模板不可用，请检查模板状态");
        }
        
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 创建考试
        Exam exam = new Exam(examTitle, examDescription, user);
        exam.setDuration(template.getDuration());
        exam.setSourcePaperTemplate(template);
        
        // 设置考试设置
        Map<String, Object> settings = new HashMap<>();
        settings.put("templateId", templateId);
        settings.put("templateName", template.getName());
        settings.put("totalScore", template.getTotalScore());
        settings.put("appliedAt", LocalDateTime.now());
        
        try {
            exam.setSettings(objectMapper.writeValueAsString(settings));
        } catch (JsonProcessingException e) {
            logger.error("序列化考试设置失败", e);
        }
        
        exam = examRepository.save(exam);
        
        // 复制模板题目到考试
        List<ExamPaperTemplateQuestion> templateQuestions = templateQuestionRepository.findByTemplateIdOrderByQuestionOrderAsc(templateId);
        for (ExamPaperTemplateQuestion templateQuestion : templateQuestions) {
            if (templateQuestion.isConfigured()) {
                Question question = createQuestionFromTemplate(templateQuestion, exam);
                exam.addQuestion(question);
            }
        }
        
        // 更新模板使用统计
        template.incrementUsageCount();
        templateRepository.save(template);
        
        return examRepository.save(exam);
    }
    
    /**
     * 应用模板到已有考试
     */
    @Transactional
    public Exam applyTemplateToExistingExam(Long templateId, Long examId, boolean replaceExisting) {
        logger.info("应用模板到已有考试: 模板ID={}, 考试ID={}, 替换现有题目={}", templateId, examId, replaceExisting);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("考试不存在: " + examId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!exam.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此考试");
        }
        
        // 检查模板是否可用
        if (!template.isUsable()) {
            throw new RuntimeException("模板不可用，请检查模板状态");
        }
        
        // 如果选择替换现有题目，先清空现有题目
        if (replaceExisting) {
            exam.getQuestions().clear();
        }
        
        // 复制模板题目到考试
        List<ExamPaperTemplateQuestion> templateQuestions = templateQuestionRepository.findByTemplateIdOrderByQuestionOrderAsc(templateId);
        for (ExamPaperTemplateQuestion templateQuestion : templateQuestions) {
            if (templateQuestion.isConfigured()) {
                Question question = createQuestionFromTemplate(templateQuestion, exam);
                exam.addQuestion(question);
            }
        }
        
        // 更新考试设置
        Map<String, Object> settings = new HashMap<>();
        try {
            if (exam.getSettings() != null) {
                settings = objectMapper.readValue(exam.getSettings(), Map.class);
            }
        } catch (Exception e) {
            logger.warn("解析考试设置失败，使用新设置", e);
        }
        
        settings.put("appliedTemplateId", templateId);
        settings.put("appliedTemplateName", template.getName());
        settings.put("appliedAt", LocalDateTime.now());
        settings.put("replaceExisting", replaceExisting);
        
        try {
            exam.setSettings(objectMapper.writeValueAsString(settings));
        } catch (JsonProcessingException e) {
            logger.error("序列化考试设置失败", e);
        }
        
        // 更新模板使用统计
        template.incrementUsageCount();
        templateRepository.save(template);
        
        return examRepository.save(exam);
    }
    
    /**
     * 获取可应用的考试列表（当前用户创建的草稿或已发布考试）
     */
    public List<Exam> getApplicableExams() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Exam> userExams = examRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
        
        // 只返回草稿或已发布状态的考试
        return userExams.stream()
            .filter(exam -> exam.getStatus() == ExamStatus.DRAFT || exam.getStatus() == ExamStatus.PUBLISHED)
            .collect(Collectors.toList());
    }
    
    /**
     * 创建模板题目
     */
    private void createTemplateQuestions(ExamPaperTemplate template, List<ExamPaperTemplateQuestionRequest> questionRequests) {
        for (ExamPaperTemplateQuestionRequest request : questionRequests) {
            ExamPaperTemplateQuestion question = new ExamPaperTemplateQuestion();
            question.setTemplate(template);
            question.setQuestionOrder(request.getQuestionOrder());
            question.setQuestionType(request.getQuestionType() != null ? 
                mapStringToQuestionType(request.getQuestionType()) : 
                QuestionType.SHORT_ANSWER);
            question.setQuestionContent(request.getQuestionContent());
            question.setQuestionId(request.getQuestionId());
            question.setScore(request.getScore());
            question.setDifficultyLevel(request.getDifficultyLevel());
            question.setKnowledgeTags(request.getKnowledgeTags());
            question.setQuestionConfig(request.getQuestionConfig());
            question.setIsRequired(request.getIsRequired());
            question.setStatus(request.getStatus() != null ? 
                ExamPaperTemplateQuestion.QuestionStatus.valueOf(request.getStatus()) : 
                ExamPaperTemplateQuestion.QuestionStatus.DRAFT);
            
            templateQuestionRepository.save(question);
        }
    }
    
    /**
     * 更新模板题目
     */
    private void updateTemplateQuestions(ExamPaperTemplate template, List<ExamPaperTemplateQuestionRequest> questionRequests) {
        // 删除现有题目
        templateQuestionRepository.deleteByTemplateId(template.getId());
        
        // 创建新题目
        createTemplateQuestions(template, questionRequests);
    }
    
    /**
     * 从模板题目创建考试题目
     */
    private Question createQuestionFromTemplate(ExamPaperTemplateQuestion templateQuestion, Exam exam) {
        Question question = new Question();
        question.setTitle("题目" + templateQuestion.getQuestionOrder());
        question.setContent(templateQuestion.getQuestionContent());
        question.setQuestionType(templateQuestion.getQuestionType());
        question.setMaxScore(BigDecimal.valueOf(templateQuestion.getScore()));
        question.setExam(exam);
        question.setCreatedBy(SecurityUtils.getCurrentUserId());
        
        // 从questionConfig中提取正确答案和选项
        String answerContent = null;
        List<String> optionsList = null;
        
        if (templateQuestion.getQuestionConfig() != null && !templateQuestion.getQuestionConfig().isEmpty()) {
            try {
                // 尝试解析JSON格式的questionConfig
                ObjectMapper mapper = new ObjectMapper();
                JsonNode configNode = mapper.readTree(templateQuestion.getQuestionConfig());
                
                if (configNode.has("correctAnswer")) {
                    answerContent = configNode.get("correctAnswer").asText();
                }
                
                // 解析选项信息
                if (configNode.has("options")) {
                    JsonNode optionsNode = configNode.get("options");
                    if (optionsNode.isArray()) {
                        optionsList = new ArrayList<>();
                        for (JsonNode optionNode : optionsNode) {
                            optionsList.add(optionNode.asText());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("解析questionConfig失败: {}", e.getMessage());
                // 如果解析失败，直接使用questionConfig作为答案
                answerContent = templateQuestion.getQuestionConfig();
            }
        }
        
        // 设置正确答案到referenceAnswer字段
        if (answerContent != null && !answerContent.isEmpty()) {
            question.setReferenceAnswer(answerContent);
        }
        
        // 先保存Question对象，确保它有ID
        question = questionRepository.save(question);
        logger.debug("已保存题目到数据库，题目ID: {}", question.getId());
        
        // 为有选项的题目类型创建QuestionOption实体
        if (shouldCreateOptions(templateQuestion.getQuestionType()) && optionsList != null && !optionsList.isEmpty()) {
            createQuestionOptions(question, optionsList, answerContent);
            logger.debug("为题目 {} 创建了 {} 个选项", question.getId(), optionsList.size());
        }
        
        return question;
    }
    
    /**
     * 判断题目类型是否需要创建选项
     */
    private boolean shouldCreateOptions(QuestionType questionType) {
        return questionType == QuestionType.SINGLE_CHOICE || 
               questionType == QuestionType.MULTIPLE_CHOICE || 
               questionType == QuestionType.TRUE_FALSE;
    }
    
    /**
     * 为题目创建选项实体
     */
    private void createQuestionOptions(Question question, List<String> optionsList, String correctAnswer) {
        if (optionsList == null || optionsList.isEmpty()) {
            logger.debug("选项列表为空，跳过选项创建");
            return;
        }
        
        logger.debug("开始为题目 {} 创建 {} 个选项", question.getId(), optionsList.size());
        
        int optionOrder = 1;
        for (String optionText : optionsList) {
            if (optionText == null || optionText.trim().isEmpty()) {
                logger.debug("跳过空选项");
                continue;
            }
            
            // 提取选项标识符和内容
            String optionIdentifier = extractOptionIdentifier(optionText);
            String optionContent = extractOptionContent(optionText);
            
            // 判断是否为正确答案
            boolean isCorrect = isCorrectOption(optionIdentifier, optionContent, correctAnswer);
            
            // 创建选项实体
            QuestionOption option = new QuestionOption();
            option.setContent(optionContent);
            option.setIsCorrect(isCorrect);
            option.setOptionOrder(optionOrder);
            option.setQuestion(question);  // 设置关联关系
            
            // 保存选项到数据库
            QuestionOption savedOption = questionOptionRepository.save(option);
            
            logger.debug("✅ 创建选项成功: {} - {} (正确: {}, ID: {})", 
                    optionIdentifier, 
                    optionContent.length() > 20 ? optionContent.substring(0, 20) + "..." : optionContent, 
                    isCorrect, 
                    savedOption.getId());
            
            optionOrder++;
        }
        
        logger.info("✅ 为题目 {} 成功创建了 {} 个选项", question.getId(), optionOrder - 1);
    }
    
    /**
     * 提取选项标识符 (A、B、C、D等)
     */
    private String extractOptionIdentifier(String optionText) {
        if (optionText == null) return "";
        
        // 匹配 "A、xxx" 格式
        Pattern pattern = Pattern.compile("^([A-Z])、");
        Matcher matcher = pattern.matcher(optionText.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    /**
     * 提取选项内容 (去除A、B、C等标识符)
     */
    private String extractOptionContent(String optionText) {
        if (optionText == null) return "";
        
        // 移除开头的 "A、"、"B、" 等标识符
        String content = optionText.replaceAll("^[A-Z]、\\s*", "").trim();
        return content.isEmpty() ? optionText.trim() : content;
    }
    
    /**
     * 判断选项是否为正确答案
     */
    private boolean isCorrectOption(String optionIdentifier, String optionContent, String correctAnswer) {
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCorrectAnswer = correctAnswer.trim();
        
        // 方式1: 直接匹配选项标识符 (A, B, C, D)
        if (!optionIdentifier.isEmpty() && optionIdentifier.equals(trimmedCorrectAnswer)) {
            return true;
        }
        
        // 方式2: 匹配选项内容
        if (optionContent.equals(trimmedCorrectAnswer)) {
            return true;
        }
        
        // 方式3: 包含匹配（用于部分匹配）
        if (trimmedCorrectAnswer.contains(optionContent) || optionContent.contains(trimmedCorrectAnswer)) {
            return true;
        }
        
        // 方式4: 判断题特殊处理
        if (trimmedCorrectAnswer.equals("正确") || trimmedCorrectAnswer.equals("T") || trimmedCorrectAnswer.equals("true")) {
            return optionContent.contains("正确") || optionContent.contains("是") || optionContent.contains("对");
        }
        if (trimmedCorrectAnswer.equals("错误") || trimmedCorrectAnswer.equals("F") || trimmedCorrectAnswer.equals("false")) {
            return optionContent.contains("错误") || optionContent.contains("否") || optionContent.contains("错");
        }
        
        return false;
    }
    
    /**
     * 为模板创建或查找题目库
     */
    private QuestionBank findOrCreateQuestionBankForTemplate(ExamPaperTemplate template) {
        Long userId = SecurityUtils.getCurrentUserId();
        String subject = template.getSubject() != null ? template.getSubject() : "通用";
        String bankName = "学习通导入-" + subject;
        
        // 查找现有题库
        Optional<QuestionBank> existingBank = questionBankRepository.findByNameAndCreatedBy(bankName, userId);
        if (existingBank.isPresent()) {
            return existingBank.get();
        }
        
        // 创建新题库
        QuestionBank newBank = new QuestionBank();
        newBank.setName(bankName);
        newBank.setDescription("从学习通文档导入的" + subject + "科目题目集合");
        newBank.setSubject(subject);
        newBank.setGradeLevel(template.getGradeLevel());
        newBank.setCreatedBy(userId);
        newBank.setIsPublic(false); // 默认私有
        newBank.setIsActive(true);
        
        QuestionBank savedBank = questionBankRepository.save(newBank);
        logger.info("创建新题库: {} (ID: {})", savedBank.getName(), savedBank.getId());
        
        return savedBank;
    }
    
    /**
     * 为题目库创建题目
     */
    private Question createQuestionForBank(LearningAnswerParserService.ExamTemplateData.QuestionInfo questionInfo, 
                                         QuestionType questionType, Double questionScore, String configJson, 
                                         QuestionBank questionBank, int questionOrder) {
        Question bankQuestion = new Question();
        
        // 设置题目标题（从内容中提取前50个字符）
        String title = questionInfo.getQuestionContent().length() > 50 
            ? questionInfo.getQuestionContent().substring(0, 47) + "..." 
            : questionInfo.getQuestionContent();
        bankQuestion.setTitle(title);
        
        // 设置题目内容
        bankQuestion.setContent(questionInfo.getQuestionContent());
        
        // 设置题目类型
        bankQuestion.setQuestionType(questionType);
        
        // 设置分值
        bankQuestion.setMaxScore(BigDecimal.valueOf(questionScore));
        
        // 设置参考答案
        if (questionInfo.getCorrectAnswer() != null && !questionInfo.getCorrectAnswer().isEmpty()) {
            bankQuestion.setReferenceAnswer(questionInfo.getCorrectAnswer());
        }
        
        // 设置题目来源和状态
        bankQuestion.setSourceType(SourceType.LEARNING_IMPORT);
        bankQuestion.setIsConfirmed(false); // 需要教师确认
        bankQuestion.setCreatedBy(SecurityUtils.getCurrentUserId());
        bankQuestion.setQuestionBank(questionBank);
        
        // 暂存题目，稍后会处理选项
        Question savedQuestion = questionRepository.save(bankQuestion);
        
        // 为有选项的题目类型创建QuestionOption实体
        if (shouldCreateOptions(questionType) && questionInfo.getOptions() != null && !questionInfo.getOptions().isEmpty()) {
            createQuestionOptions(savedQuestion, questionInfo.getOptions(), questionInfo.getCorrectAnswer());
            logger.debug("为题目库题目 {} 创建了 {} 个选项", questionOrder, questionInfo.getOptions().size());
        }
        
        return savedQuestion;
    }
    
    /**
     * 转换为响应对象
     */
    private ExamPaperTemplateResponse convertToResponse(ExamPaperTemplate template) {
        ExamPaperTemplateResponse response = new ExamPaperTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setSubject(template.getSubject());
        response.setGradeLevel(template.getGradeLevel());
        response.setTotalScore(template.getTotalScore());
        response.setDuration(template.getDuration());
        response.setStatus(template.getStatus().name());
        response.setTemplateType(template.getTemplateType().name());
        response.setQuestionTypeConfig(template.getQuestionTypeConfig());
        response.setDifficultyConfig(template.getDifficultyConfig());
        response.setKnowledgeBaseConfig(template.getKnowledgeBaseConfig());
        response.setTags(template.getTags());
        response.setIsPublic(template.getIsPublic());
        response.setUsageCount(template.getUsageCount());
        response.setCreatedBy(template.getCreatedBy());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setLastUsedAt(template.getLastUsedAt());
        response.setQuestionCount(template.getQuestionCount());
        response.setConfiguredQuestionCount(template.getConfiguredQuestionCount());
        response.setIsComplete(template.isComplete());
        response.setIsUsable(template.isUsable());
        
        // 设置创建者姓名
        if (template.getCreator() != null) {
            response.setCreatorName(template.getCreator().getUsername());
        }
        
        // 设置题目列表
        List<ExamPaperTemplateQuestion> questions = templateQuestionRepository.findByTemplateIdOrderByQuestionOrderAsc(template.getId());
        List<ExamPaperTemplateQuestionResponse> questionResponses = questions.stream()
            .map(this::convertToQuestionResponse)
            .collect(Collectors.toList());
        response.setQuestions(questionResponses);
        
        return response;
    }
    
    /**
     * 转换为题目响应对象
     */
    private ExamPaperTemplateQuestionResponse convertToQuestionResponse(ExamPaperTemplateQuestion question) {
        ExamPaperTemplateQuestionResponse response = new ExamPaperTemplateQuestionResponse();
        response.setId(question.getId());
        response.setTemplateId(question.getTemplate().getId());
        response.setQuestionOrder(question.getQuestionOrder());
        response.setQuestionType(question.getQuestionType().name());
        response.setQuestionContent(question.getQuestionContent());
        response.setQuestionId(question.getQuestionId());
        response.setScore(question.getScore() != null ? 
            BigDecimal.valueOf(question.getScore()) : null);
        response.setDifficultyLevel(question.getDifficultyLevel());
        response.setKnowledgeTags(question.getKnowledgeTags());
        response.setQuestionConfig(question.getQuestionConfig());
        response.setIsRequired(question.getIsRequired());
        response.setStatus(question.getStatus().name());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
        response.setIsConfigured(question.isConfigured());
        response.setIsReady(question.isReady());
        
        // 如果引用了现有题目，获取题目信息
        if (question.getQuestionId() != null) {
            Question referencedQuestion = questionRepository.findById(question.getQuestionId()).orElse(null);
            if (referencedQuestion != null) {
                ExamPaperTemplateQuestionResponse.QuestionSummary summary = new ExamPaperTemplateQuestionResponse.QuestionSummary();
                summary.setId(referencedQuestion.getId());
                summary.setContent(referencedQuestion.getContent());
                summary.setQuestionType(referencedQuestion.getQuestionType().name());
                summary.setScore(referencedQuestion.getMaxScore());
                response.setReferencedQuestion(summary);
            }
        }
        
        return response;
    }
    
    /**
     * 添加模板题目
     */
    @Transactional
    public ExamPaperTemplateQuestionResponse addTemplateQuestion(Long templateId, ExamPaperTemplateQuestionRequest request) {
        logger.info("添加模板题目: 模板ID={}", templateId);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 创建模板题目
        ExamPaperTemplateQuestion question = new ExamPaperTemplateQuestion();
        question.setTemplate(template);
        question.setQuestionOrder(request.getQuestionOrder());
        question.setQuestionType(request.getQuestionType() != null ? 
            mapStringToQuestionType(request.getQuestionType()) : 
            QuestionType.SHORT_ANSWER);
        question.setQuestionContent(request.getQuestionContent());
        question.setQuestionId(request.getQuestionId());
        question.setScore(request.getScore());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setKnowledgeTags(request.getKnowledgeTags());
        question.setQuestionConfig(request.getQuestionConfig());
        question.setIsRequired(request.getIsRequired());
        question.setStatus(request.getStatus() != null ? 
            ExamPaperTemplateQuestion.QuestionStatus.valueOf(request.getStatus()) : 
            ExamPaperTemplateQuestion.QuestionStatus.DRAFT);
        
        question = templateQuestionRepository.save(question);
        
        return convertToQuestionResponse(question);
    }
    
    /**
     * 更新模板题目
     */
    @Transactional
    public ExamPaperTemplateQuestionResponse updateTemplateQuestion(Long templateId, Long questionId, ExamPaperTemplateQuestionRequest request) {
        logger.info("更新模板题目: 模板ID={}, 题目ID={}", templateId, questionId);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        ExamPaperTemplateQuestion question = templateQuestionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查题目是否属于该模板
        if (!question.getTemplate().getId().equals(templateId)) {
            throw new RuntimeException("题目不属于该模板");
        }
        
        // 更新题目信息
        question.setQuestionOrder(request.getQuestionOrder());
        question.setQuestionType(request.getQuestionType() != null ? 
            mapStringToQuestionType(request.getQuestionType()) : 
            question.getQuestionType());
        question.setQuestionContent(request.getQuestionContent());
        question.setQuestionId(request.getQuestionId());
        question.setScore(request.getScore());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setKnowledgeTags(request.getKnowledgeTags());
        question.setQuestionConfig(request.getQuestionConfig());
        question.setIsRequired(request.getIsRequired());
        question.setStatus(request.getStatus() != null ? 
            ExamPaperTemplateQuestion.QuestionStatus.valueOf(request.getStatus()) : 
            question.getStatus());
        
        question = templateQuestionRepository.save(question);
        
        return convertToQuestionResponse(question);
    }
    
    /**
     * 删除模板题目
     */
    @Transactional
    public void deleteTemplateQuestion(Long templateId, Long questionId) {
        logger.info("删除模板题目: 模板ID={}, 题目ID={}", templateId, questionId);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        ExamPaperTemplateQuestion question = templateQuestionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查题目是否属于该模板
        if (!question.getTemplate().getId().equals(templateId)) {
            throw new RuntimeException("题目不属于该模板");
        }
        
        templateQuestionRepository.delete(question);
    }
    
    /**
     * 批量添加模板题目
     */
    @Transactional
    public List<ExamPaperTemplateQuestionResponse> addTemplateQuestions(Long templateId, List<ExamPaperTemplateQuestionRequest> requests) {
        logger.info("批量添加模板题目: 模板ID={}, 题目数量={}", templateId, requests.size());
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        List<ExamPaperTemplateQuestionResponse> responses = new ArrayList<>();
        
        for (ExamPaperTemplateQuestionRequest request : requests) {
            ExamPaperTemplateQuestion question = new ExamPaperTemplateQuestion();
            question.setTemplate(template);
            question.setQuestionOrder(request.getQuestionOrder());
            question.setQuestionType(request.getQuestionType() != null ? 
                mapStringToQuestionType(request.getQuestionType()) : 
                QuestionType.SHORT_ANSWER);
            question.setQuestionContent(request.getQuestionContent());
            question.setQuestionId(request.getQuestionId());
            question.setScore(request.getScore());
            question.setDifficultyLevel(request.getDifficultyLevel());
            question.setKnowledgeTags(request.getKnowledgeTags());
            question.setQuestionConfig(request.getQuestionConfig());
            question.setIsRequired(request.getIsRequired());
            question.setStatus(request.getStatus() != null ? 
                ExamPaperTemplateQuestion.QuestionStatus.valueOf(request.getStatus()) : 
                ExamPaperTemplateQuestion.QuestionStatus.DRAFT);
            
            question = templateQuestionRepository.save(question);
            responses.add(convertToQuestionResponse(question));
        }
        
        return responses;
    }

    /**
     * 更新模板状态
     */
    @Transactional
    public ExamPaperTemplateResponse updateTemplateStatus(Long templateId, String status) {
        logger.info("更新模板状态: 模板ID={}, 新状态={}", templateId, status);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 验证状态值
        ExamPaperTemplate.TemplateStatus newStatus;
        try {
            newStatus = ExamPaperTemplate.TemplateStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的状态值: " + status);
        }
        
        // 检查状态转换的合法性
        if (template.getStatus() == ExamPaperTemplate.TemplateStatus.DRAFT) {
            if (newStatus == ExamPaperTemplate.TemplateStatus.READY) {
                // 从草稿到就绪，需要检查模板是否完整且所有题目都已就绪
                if (!template.isComplete()) {
                    throw new RuntimeException("模板不完整，无法标记为就绪状态");
                }
                
                // 检查所有题目是否都已就绪
                List<ExamPaperTemplateQuestion> questions = templateQuestionRepository.findByTemplateIdOrderByQuestionOrderAsc(templateId);
                if (questions.isEmpty()) {
                    throw new RuntimeException("模板没有题目，无法标记为就绪状态");
                }
                
                for (ExamPaperTemplateQuestion question : questions) {
                    if (!question.isReady()) {
                        throw new RuntimeException("题目" + question.getQuestionOrder() + "未就绪，无法标记模板为就绪状态");
                    }
                }
            } else if (newStatus == ExamPaperTemplate.TemplateStatus.PUBLISHED) {
                throw new RuntimeException("草稿状态的模板不能直接发布，请先标记为就绪");
            }
        } else if (template.getStatus() == ExamPaperTemplate.TemplateStatus.READY) {
            if (newStatus == ExamPaperTemplate.TemplateStatus.DRAFT) {
                throw new RuntimeException("就绪状态的模板不能回退到草稿状态");
            }
            // 从就绪到已发布，不需要额外检查
        } else if (template.getStatus() == ExamPaperTemplate.TemplateStatus.PUBLISHED) {
            if (newStatus == ExamPaperTemplate.TemplateStatus.DRAFT) {
                throw new RuntimeException("已发布状态的模板不能回退到草稿状态");
            }
            // 允许从已发布回退到就绪状态（取消公开）
            if (newStatus == ExamPaperTemplate.TemplateStatus.READY) {
                // 取消公开，允许回退
            }
        }
        
        // 更新状态
        template.setStatus(newStatus);
        template = templateRepository.save(template);
        
        return convertToResponse(template);
    }

    /**
     * 更新题目状态
     */
    @Transactional
    public ExamPaperTemplateQuestionResponse updateQuestionStatus(Long templateId, Long questionId, String status) {
        logger.info("更新题目状态: 模板ID={}, 题目ID={}, 新状态={}", templateId, questionId, status);
        
        ExamPaperTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        ExamPaperTemplateQuestion question = templateQuestionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + questionId));
        
        // 检查权限
        Long userId = SecurityUtils.getCurrentUserId();
        if (!template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限修改此模板");
        }
        
        // 检查题目是否属于该模板
        if (!question.getTemplate().getId().equals(templateId)) {
            throw new RuntimeException("题目不属于该模板");
        }
        
        // 验证状态值
        ExamPaperTemplateQuestion.QuestionStatus newStatus;
        try {
            newStatus = ExamPaperTemplateQuestion.QuestionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的状态值: " + status);
        }
        
        // 检查状态转换的合法性
        if (question.getStatus() == ExamPaperTemplateQuestion.QuestionStatus.DRAFT) {
            if (newStatus == ExamPaperTemplateQuestion.QuestionStatus.READY) {
                // 从草稿到就绪，需要检查题目是否已配置
                if (!question.isConfigured()) {
                    throw new RuntimeException("题目未配置完成，无法标记为就绪状态");
                }
            } else if (newStatus == ExamPaperTemplateQuestion.QuestionStatus.CONFIGURED) {
                // 从草稿到已配置，需要检查题目是否已配置
                if (!question.isConfigured()) {
                    throw new RuntimeException("题目未配置完成，无法标记为已配置状态");
                }
            }
        } else if (question.getStatus() == ExamPaperTemplateQuestion.QuestionStatus.CONFIGURED) {
            if (newStatus == ExamPaperTemplateQuestion.QuestionStatus.DRAFT) {
                throw new RuntimeException("已配置状态的题目不能回退到草稿状态");
            }
            // 从已配置到就绪，不需要额外检查
        } else if (question.getStatus() == ExamPaperTemplateQuestion.QuestionStatus.READY) {
            if (newStatus == ExamPaperTemplateQuestion.QuestionStatus.DRAFT) {
                throw new RuntimeException("就绪状态的题目不能回退到草稿状态");
            }
            if (newStatus == ExamPaperTemplateQuestion.QuestionStatus.CONFIGURED) {
                throw new RuntimeException("就绪状态的题目不能回退到已配置状态");
            }
        }
        
        // 更新状态
        question.setStatus(newStatus);
        question = templateQuestionRepository.save(question);
        
        return convertToQuestionResponse(question);
    }

    /**
     * 从学习通文档创建试卷模板
     */
    @Transactional
    public ExamPaperTemplateResponse createTemplateFromDocument(
            MultipartFile file, String templateName, String subject, String gradeLevel,
            String description, Integer totalScore, Integer duration) throws IOException {
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 检查文件格式
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList("doc", "docx", "pdf").contains(fileExtension)) {
            throw new IllegalArgumentException("不支持的文件格式，请上传 .doc、.docx 或 .pdf 格式的文件");
        }
        
        // 创建临时文件
        File uploadDir = new File(System.getProperty("java.io.tmpdir"), "teachhelper_uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        File tempFile = new File(uploadDir, System.currentTimeMillis() + "_" + fileName);
        file.transferTo(tempFile);
        
        try {
            // 解析学习通考试模板
            LearningAnswerParserService.ExamTemplateData templateData = learningAnswerParserService.parseExamTemplate(tempFile);
            if (templateData == null || templateData.getSections() == null || templateData.getSections().isEmpty()) {
                throw new IllegalArgumentException("无法从文档中解析出题目模板信息");
            }
            
            // 创建模板
            ExamPaperTemplate template = new ExamPaperTemplate();
            template.setName(templateName);
            template.setDescription(description != null ? description : "从学习通文档自动生成的试卷模板");
            template.setSubject(subject);
            template.setGradeLevel(gradeLevel);
            template.setTotalScore(totalScore);
            template.setDuration(duration);
            template.setStatus(ExamPaperTemplate.TemplateStatus.DRAFT);
            template.setTemplateType(ExamPaperTemplate.TemplateType.DOCUMENT_EXTRACTED);
            template.setIsPublic(false);
            template.setCreatedBy(SecurityUtils.getCurrentUserId());
            
            // 设置默认配置
            template.setQuestionTypeConfig(createDefaultQuestionTypeConfig());
            template.setDifficultyConfig(createDefaultDifficultyConfig());
            template.setKnowledgeBaseConfig(createDefaultKnowledgeConfig(subject));
            template.setTags("[\"学习通\", \"" + subject + "\", \"" + gradeLevel + "\"]");
            
            template = templateRepository.save(template);
            
            // 创建或获取题目库
            QuestionBank questionBank = findOrCreateQuestionBankForTemplate(template);
            
            // 创建模板题目和题目库题目
            List<ExamPaperTemplateQuestion> templateQuestions = new ArrayList<>();
            int questionOrder = 1;
            
            // 遍历每个大题部分
            for (LearningAnswerParserService.ExamTemplateData.SectionInfo section : templateData.getSections()) {
                String sectionTitle = section.getSectionTitle();
                String questionTypeStr = section.getQuestionType();
                Double scorePerQuestion = section.getScorePerQuestion();
                
                logger.debug("处理大题部分: {}, 类型: {}, 每题{}分", sectionTitle, questionTypeStr, scorePerQuestion);
                
                // 遍历该部分的每道题目
                if (section.getQuestions() != null) {
                    for (LearningAnswerParserService.ExamTemplateData.QuestionInfo questionInfo : section.getQuestions()) {
                        // 设置题目类型
                        QuestionType questionType = mapStringToQuestionType(questionTypeStr);
                        String questionContent = questionInfo.getQuestionContent();
                        
                        // 设置分数 - 直接使用模板解析的分数
                        Double questionScore = questionInfo.getScore();
                        if (questionScore == null && scorePerQuestion != null) {
                            questionScore = scorePerQuestion;
                        }
                        if (questionScore == null) {
                            // 兜底：根据题目类型设置默认分数
                            questionScore = getDefaultScoreByType(questionTypeStr);
                        }
                        
                        // 构建完整的题目配置
                        String configJson = buildQuestionConfig(questionInfo);
                        
                        // 1. 创建并保存题目库中的题目 - createQuestionForBank已经保存了题目
                        Question savedBankQuestion = createQuestionForBank(questionInfo, questionType, 
                                questionScore, configJson, questionBank, questionOrder);
                        
                        // 2. 创建模板题目（引用题目库中的题目）
                        ExamPaperTemplateQuestion templateQuestion = new ExamPaperTemplateQuestion();
                        templateQuestion.setTemplate(template);
                        templateQuestion.setQuestionOrder(questionOrder++);
                        templateQuestion.setQuestionType(questionType);
                        templateQuestion.setQuestionContent(questionContent);
                        templateQuestion.setQuestionConfig(configJson);
                        templateQuestion.setScore(questionScore);
                        templateQuestion.setStatus(QuestionStatus.CONFIGURED); // 设置为已配置状态
                        
                        // ✅ 关键修复：直接设置已保存题目的ID
                        templateQuestion.setQuestionId(savedBankQuestion.getId());
                        
                        templateQuestions.add(templateQuestion);
                        logger.debug("✅ 创建题目 {}: 序号={}, 类型={}, 分数={}, 题目库ID={}, 答案={}", 
                            questionOrder-1, questionInfo.getQuestionNumber(), questionType, questionScore, 
                            savedBankQuestion.getId(),
                            questionInfo.getCorrectAnswer() != null ? questionInfo.getCorrectAnswer() : "无");
                    }
                }
            }
            
            // 保存模板题目（已经设置了正确的questionId）
            templateQuestionRepository.saveAll(templateQuestions);
            
            logger.info("成功从文档创建试卷模板: {}, 包含 {} 道题目，已保存到题目库: {}", 
                templateName, templateQuestions.size(), questionBank.getName());
            
            return convertToResponse(template);
            
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * 重新导入学习通文档
     */
    @Transactional
    public ExamPaperTemplateResponse reimportTemplateFromDocument(
            Long templateId, MultipartFile file, Integer totalScore, 
            Integer duration, String description) throws IOException {
        
        // 获取现有模板
        ExamPaperTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        
        // 检查权限
        if (!template.getCreatedBy().equals(SecurityUtils.getCurrentUserId())) {
            throw new IllegalArgumentException("您没有权限修改此模板");
        }
        
        // 检查模板类型
        if (template.getTemplateType() != ExamPaperTemplate.TemplateType.DOCUMENT_EXTRACTED) {
            throw new IllegalArgumentException("只有从文档导入的模板才能重新导入");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 检查文件格式
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList("doc", "docx", "pdf").contains(fileExtension)) {
            throw new IllegalArgumentException("不支持的文件格式，请上传 .doc、.docx 或 .pdf 格式的文件");
        }
        
        // 创建临时文件
        File uploadDir = new File(System.getProperty("java.io.tmpdir"), "teachhelper_uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        File tempFile = new File(uploadDir, System.currentTimeMillis() + "_" + fileName);
        file.transferTo(tempFile);
        
        try {
            // 解析学习通考试模板
            LearningAnswerParserService.ExamTemplateData templateData = learningAnswerParserService.parseExamTemplate(tempFile);
            if (templateData == null || templateData.getSections() == null || templateData.getSections().isEmpty()) {
                throw new IllegalArgumentException("无法从文档中解析出题目模板信息");
            }
            
            // 更新模板基本信息
            if (totalScore != null) {
                template.setTotalScore(totalScore);
            }
            if (duration != null) {
                template.setDuration(duration);
            }
            if (description != null && !description.trim().isEmpty()) {
                template.setDescription(description);
            }
            
            // 删除现有题目
            List<ExamPaperTemplateQuestion> existingQuestions = templateQuestionRepository.findByTemplateIdOrderByQuestionOrderAsc(templateId);
            templateQuestionRepository.deleteAll(existingQuestions);
            logger.info("删除现有题目 {} 道", existingQuestions.size());
            
            // 获取或创建题目库
            QuestionBank questionBank = findOrCreateQuestionBankForTemplate(template);
            
            // 创建新的模板题目和题目库题目
            List<ExamPaperTemplateQuestion> questions = new ArrayList<>();
            int questionOrder = 1;
            
            // 遍历每个大题部分
            for (LearningAnswerParserService.ExamTemplateData.SectionInfo section : templateData.getSections()) {
                String sectionTitle = section.getSectionTitle();
                String questionTypeStr = section.getQuestionType();
                Double scorePerQuestion = section.getScorePerQuestion();
                
                logger.debug("重新导入大题部分: {}, 类型: {}, 每题{}分", sectionTitle, questionTypeStr, scorePerQuestion);
                        
                // 遍历该部分的每道题目
                if (section.getQuestions() != null) {
                    for (LearningAnswerParserService.ExamTemplateData.QuestionInfo questionInfo : section.getQuestions()) {
                        // 设置题目类型
                        QuestionType questionType = mapStringToQuestionType(questionTypeStr);
                        String questionContent = questionInfo.getQuestionContent();
                        
                        // 设置分数 - 直接使用模板解析的分数
                        Double questionScore = questionInfo.getScore();
                        if (questionScore == null && scorePerQuestion != null) {
                            questionScore = scorePerQuestion;
                        }
                        if (questionScore == null) {
                            // 兜底：根据题目类型设置默认分数
                            questionScore = getDefaultScoreByType(questionTypeStr);
                        }
                        
                        // 构建完整的题目配置
                        String configJson = buildQuestionConfig(questionInfo);
                        
                        // 1. 创建并保存题目库中的题目
                        Question savedBankQuestion = createQuestionForBank(questionInfo, questionType, 
                                questionScore, configJson, questionBank, questionOrder);
                        
                        // 2. 创建模板题目（引用题目库中的题目）
                        ExamPaperTemplateQuestion question = new ExamPaperTemplateQuestion();
                        question.setTemplate(template);
                        question.setQuestionOrder(questionOrder++);
                        question.setQuestionType(questionType);
                        question.setQuestionContent(questionContent);
                        question.setQuestionConfig(configJson);
                        question.setScore(questionScore);
                        question.setStatus(QuestionStatus.CONFIGURED); // 设置为已配置状态
                        
                        // ✅ 关键修复：设置题目库关联
                        question.setQuestionId(savedBankQuestion.getId());
                        
                        questions.add(question);
                        logger.debug("✅ 重新创建题目 {}: 序号={}, 类型={}, 分数={}, 题目库ID={}, 答案={}", 
                            questionOrder-1, questionInfo.getQuestionNumber(), questionType, questionScore, 
                            savedBankQuestion.getId(),
                            questionInfo.getCorrectAnswer() != null ? questionInfo.getCorrectAnswer() : "无");
                    }
                }
            }
            
            // 保存新的模板题目（已经设置了正确的questionId）
            templateQuestionRepository.saveAll(questions);
            
            // 更新模板状态为草稿
            template.setStatus(ExamPaperTemplate.TemplateStatus.DRAFT);
            template = templateRepository.save(template);
            
            logger.info("成功重新导入学习通文档到模板: {}, 包含 {} 道题目", template.getName(), questions.size());
            
            return convertToResponse(template);
            
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * 将字符串题目类型映射为枚举 - 优化版本
     */
    private QuestionType mapStringToQuestionType(String questionTypeStr) {
        if (questionTypeStr == null) {
            return QuestionType.SHORT_ANSWER;
        }
        
        String lower = questionTypeStr.toLowerCase();
        
        // 精确匹配题目类型
        if (lower.contains("选择题") || lower.contains("选择") || 
            lower.contains("单选") || lower.contains("多选")) {
            return QuestionType.SINGLE_CHOICE;
        } else if (lower.contains("判断题") || lower.contains("判断")) {
            return QuestionType.TRUE_FALSE;
        } else if (lower.contains("填空题") || lower.contains("填空")) {
            return QuestionType.FILL_BLANK;
        } else if (lower.contains("简答题") || lower.contains("简答")) {
            return QuestionType.SHORT_ANSWER;
        } else if (lower.contains("论述题") || lower.contains("论述") || 
                   lower.contains("阐述") || lower.contains("试述")) {
            return QuestionType.ESSAY;
        } else if (lower.contains("计算题") || lower.contains("计算")) {
            return QuestionType.SHORT_ANSWER; // 计算题作为简答题处理
        } else if (lower.contains("分析题") || lower.contains("分析") || 
                   lower.contains("案例分析")) {
            return QuestionType.CASE_ANALYSIS;
        } else if (lower.contains("应用题") || lower.contains("应用")) {
            return QuestionType.SHORT_ANSWER;
        } else if (lower.contains("编程题") || lower.contains("编程") || 
                   lower.contains("代码")) {
            return QuestionType.SHORT_ANSWER; // 编程题作为简答题处理
        } else {
            return QuestionType.SHORT_ANSWER; // 默认为简答题
        }
    }
    
    /**
     * 创建默认题型配置
     */
    private String createDefaultQuestionTypeConfig() {
        return "{\"questionTypes\":{\"SHORT_ANSWER\":40,\"ESSAY\":30,\"SINGLE_CHOICE\":20,\"MULTIPLE_CHOICE\":10}}";
    }
    
    /**
     * 创建默认难度配置
     */
    private String createDefaultDifficultyConfig() {
        return "{\"difficultyDistribution\":{\"EASY\":30,\"MEDIUM\":50,\"HARD\":20}}";
    }
    
    /**
     * 创建默认知识库配置
     */
    private String createDefaultKnowledgeConfig(String subject) {
        return "{\"knowledgeBases\":[\"" + subject + "\"],\"weightDistribution\":{\"basic\":40,\"intermediate\":40,\"advanced\":20}}";
    }

    /**
     * 根据题目类型获取默认分数
     */
    private Double getDefaultScoreByType(String questionType) {
        if (questionType == null) {
            return 2.0; // 默认分数
        }
        
        String lower = questionType.toLowerCase();
        
        if (lower.contains("选择题") || lower.contains("单选")) {
            return 1.0; // 选择题通常1分
        } else if (lower.contains("判断题")) {
            return 0.5; // 判断题通常0.5分
        } else if (lower.contains("填空题")) {
            return 2.0; // 填空题通常2分
        } else if (lower.contains("简答题")) {
            return 4.0; // 简答题通常4分
        } else if (lower.contains("论述题")) {
            return 15.0; // 论述题通常15分
        } else if (lower.contains("计算题")) {
            return 10.0; // 计算题通常10分
        } else if (lower.contains("分析题")) {
            return 12.5; // 分析题通常12.5分（25分÷2题）
        } else {
            return 3.0; // 其他类型默认3分
        }
    }
    
    /**
     * 优化的分数解析方法 - 根据学习通实际分数结构解析
     */
    private BigDecimal parseScoreOptimized(String questionContent, String sectionHeader, String questionType) {
        logger.debug("开始优化分数解析 - 题目类型: {}, 段落标题: {}", questionType, sectionHeader);
        
        // 1. 优先从段落标题解析分数（学习通格式：共X题，Y分）
        if (sectionHeader != null && !sectionHeader.trim().isEmpty()) {
            BigDecimal sectionScore = parseScoreFromSectionHeader(sectionHeader);
            if (sectionScore != null && sectionScore.compareTo(BigDecimal.ZERO) > 0) {
                logger.debug("从段落标题解析到分数: {}", sectionScore);
                return sectionScore;
            }
        }
        
        // 2. 从题目内容解析分数（学生得分信息）
        if (questionContent != null) {
            BigDecimal contentScore = parseScoreFromContent(questionContent);
            if (contentScore != null && contentScore.compareTo(BigDecimal.ZERO) > 0) {
                logger.debug("从题目内容解析到分数: {}", contentScore);
                return contentScore;
            }
        }
        
        // 3. 根据题目类型智能推断分数（基于学习通标准）
        BigDecimal intelligentScore = getIntelligentScoreByType(questionType);
        logger.debug("根据题目类型智能推断分数: {}", intelligentScore);
        return intelligentScore;
    }
    
    /**
     * 从段落标题解析分数 - 学习通格式解析
     */
    private BigDecimal parseScoreFromSectionHeader(String sectionHeader) {
        if (sectionHeader == null || sectionHeader.trim().isEmpty()) {
            return null;
        }
        
        logger.debug("解析段落标题: {}", sectionHeader);
        
        // 学习通常见格式：
        // 一.单选题(共25题,25分）
        // 二.判断题(共10题,5分）
        // 三.简答题(共5题,20分）
        Pattern[] patterns = {
            // 格式1：(共X题,Y分）或（共X题，Y分）
            Pattern.compile("\\([共]?\\s*(\\d+)\\s*题[，,]\\s*(\\d+(?:\\.\\d+)?)\\s*分[）)]"),
            // 格式2：共X题，Y分
            Pattern.compile("共\\s*(\\d+)\\s*题[，,]\\s*(\\d+(?:\\.\\d+)?)\\s*分"),
            // 格式3：每题X分
            Pattern.compile("每题\\s*(\\d+(?:\\.\\d+)?)\\s*分"),
            // 格式4：单题X分
            Pattern.compile("单题\\s*(\\d+(?:\\.\\d+)?)\\s*分")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(sectionHeader);
            if (matcher.find()) {
                try {
                    if (pattern.pattern().contains("共.*题")) {
                        // 共X题，Y分 -> 每题 Y/X 分
                        int questionCount = Integer.parseInt(matcher.group(1));
                        double totalScore = Double.parseDouble(matcher.group(2));
                        if (questionCount > 0) {
                            BigDecimal scorePerQuestion = BigDecimal.valueOf(totalScore)
                                .divide(BigDecimal.valueOf(questionCount), 2, RoundingMode.HALF_UP);
                            logger.debug("段落分数解析成功: 共{}题，总{}分，每题{}分", 
                                questionCount, totalScore, scorePerQuestion);
                            return scorePerQuestion;
                        }
                    } else {
                        // 每题X分 或 单题X分
                        BigDecimal scorePerQuestion = new BigDecimal(matcher.group(1));
                        logger.debug("段落分数解析成功: 每题{}分", scorePerQuestion);
                        return scorePerQuestion;
                    }
                } catch (NumberFormatException | ArithmeticException e) {
                    logger.warn("解析段落分数失败: {}", matcher.group());
                }
            }
        }
        
        logger.debug("未能从段落标题解析出分数");
        return null;
    }
    
    /**
     * 从题目内容解析分数 - 主要解析学生得分信息
     */
    private BigDecimal parseScoreFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 从学生得分信息推断题目分数
        Pattern[] patterns = {
            // 学生得分：X分（学习通格式）
            Pattern.compile("学生得分[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分"),
            // 得分：X分
            Pattern.compile("得分[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分"),
            // 满分：X分
            Pattern.compile("满分[：:]?\\s*(\\d+(?:\\.\\d+)?)\\s*分"),
            // (X分)题目
            Pattern.compile("\\((\\d+(?:\\.\\d+)?)分\\)"),
            // （X分）题目
            Pattern.compile("（(\\d+(?:\\.\\d+)?)分）")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                try {
                    BigDecimal score = new BigDecimal(matcher.group(1));
                    logger.debug("从题目内容解析到分数: {} (模式: {})", score, pattern.pattern());
                    return score;
                } catch (NumberFormatException e) {
                    logger.warn("解析分数失败: {}", matcher.group(1));
                }
            }
        }
        
        return null;
    }
    
    /**
     * 根据题目类型获取智能分数 - 基于学习通标准分值
     */
    private BigDecimal getIntelligentScoreByType(String questionType) {
        if (questionType == null) {
            return new BigDecimal("5");
        }
        
        String lower = questionType.toLowerCase();
        
        // 根据学习通常见分值设置
        if (lower.contains("选择题") || lower.contains("选择")) {
            return new BigDecimal("1"); // 选择题通常1分
        } else if (lower.contains("判断题") || lower.contains("判断")) {
            return new BigDecimal("0.5"); // 判断题通常0.5分
        } else if (lower.contains("填空题") || lower.contains("填空")) {
            return new BigDecimal("2"); // 填空题通常2分
        } else if (lower.contains("简答题") || lower.contains("简答")) {
            return new BigDecimal("4"); // 简答题通常4分
        } else if (lower.contains("论述题") || lower.contains("论述")) {
            return new BigDecimal("15"); // 论述题通常15分
        } else if (lower.contains("计算题") || lower.contains("计算")) {
            return new BigDecimal("10"); // 计算题通常10分
        } else if (lower.contains("分析题") || lower.contains("分析")) {
            return new BigDecimal("12"); // 分析题通常12分
        } else if (lower.contains("应用题") || lower.contains("应用")) {
            return new BigDecimal("8"); // 应用题通常8分
        } else if (lower.contains("编程题") || lower.contains("编程")) {
            return new BigDecimal("20"); // 编程题通常20分
        }
        
        return new BigDecimal("5"); // 默认5分
    }
    
    /**
     * 构建完整的题目配置，包含正确答案和选项
     */
    private String buildQuestionConfig(LearningAnswerParserService.ExamTemplateData.QuestionInfo questionInfo) {
        try {
            logger.info("🔧 构建题目 {} 的配置", questionInfo.getQuestionNumber());
            
            StringBuilder configBuilder = new StringBuilder("{");
            
            // 添加正确答案
            String correctAnswer = questionInfo.getCorrectAnswer();
            logger.info("📋 正确答案: [{}]", correctAnswer);
            
            if (correctAnswer != null && !correctAnswer.trim().isEmpty()) {
                correctAnswer = correctAnswer.trim().replaceAll("\\s+", " ");
                configBuilder.append("\"correctAnswer\":\"")
                            .append(correctAnswer.replace("\"", "\\\""))
                            .append("\"");
            }
            
            // 添加选项
            List<String> options = questionInfo.getOptions();
            logger.info("📋 传入的选项数量: {}", options != null ? options.size() : 0);
            
            if (options != null && !options.isEmpty()) {
                logger.info("✅ 使用解析到的真实选项");
                for (int i = 0; i < options.size(); i++) {
                    logger.info("📋 选项 {}: [{}]", i + 1, options.get(i));
                }
                
                if (configBuilder.length() > 1) {
                    configBuilder.append(",");
                }
                configBuilder.append("\"options\":[");
                for (int i = 0; i < options.size(); i++) {
                    if (i > 0) {
                        configBuilder.append(",");
                    }
                    String option = options.get(i).replace("\"", "\\\"");
                    configBuilder.append("\"").append(option).append("\"");
                }
                configBuilder.append("]");
            } else {
                // 🔧 修复：如果选项为空，但有正确答案，尝试生成默认选项
                logger.warn("⚠️ 选项为空，生成默认选项");
                if (correctAnswer != null && !correctAnswer.trim().isEmpty()) {
                    List<String> defaultOptions = generateDefaultOptions(correctAnswer, questionInfo.getQuestionContent());
                    logger.info("📋 生成的默认选项数量: {}", defaultOptions.size());
                    
                    for (int i = 0; i < defaultOptions.size(); i++) {
                        logger.info("📋 默认选项 {}: [{}]", i + 1, defaultOptions.get(i));
                    }
                    
                    if (!defaultOptions.isEmpty()) {
                        if (configBuilder.length() > 1) {
                            configBuilder.append(",");
                        }
                        configBuilder.append("\"options\":[");
                        for (int i = 0; i < defaultOptions.size(); i++) {
                            if (i > 0) {
                                configBuilder.append(",");
                            }
                            String option = defaultOptions.get(i).replace("\"", "\\\"");
                            configBuilder.append("\"").append(option).append("\"");
                        }
                        configBuilder.append("]");
                        logger.info("🔧 为题目生成了 {} 个默认选项", defaultOptions.size());
                    } else {
                        logger.warn("❌ 选项列表为空且无法生成默认选项");
                    }
                } else {
                    logger.warn("❌ 选项列表为空或null，且无正确答案");
                }
            }
            
            configBuilder.append("}");
            
            // 如果配置为空，返回null
            String config = configBuilder.toString();
            if ("{".equals(config) || "{}".equals(config)) {
                logger.warn("❌ 生成的配置为空");
                return null;
            }
            
            logger.info("✅ 题目 {} 配置生成完成: [{}]", questionInfo.getQuestionNumber(), config);
            return config;
            
        } catch (Exception e) {
            logger.warn("❌ 构建题目配置失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 生成默认选项（当原始文档中选项解析失败时使用）
     */
    private List<String> generateDefaultOptions(String correctAnswer, String questionContent) {
        List<String> options = new ArrayList<>();
        
        try {
            // 清理正确答案
            String cleanAnswer = correctAnswer.trim().toUpperCase();
            
            // 如果正确答案是单个字母（A、B、C、D），生成标准选项
            if (cleanAnswer.matches("^[A-D]$")) {
                char correctLetter = cleanAnswer.charAt(0);
                
                // 生成4个选项
                for (char letter = 'A'; letter <= 'D'; letter++) {
                    if (letter == correctLetter) {
                        options.add(letter + "、正确选项");
                    } else {
                        options.add(letter + "、选项" + letter);
                    }
                }
                
                logger.debug("🔧 根据正确答案 {} 生成了标准ABCD选项", correctAnswer);
                return options;
            }
            
            // 如果正确答案是"对"或"错"/"是"或"否"（判断题）
            if (cleanAnswer.matches("^(对|错|是|否|TRUE|FALSE|T|F|正确|错误)$")) {
                options.add("A、对");
                options.add("B、错");
                logger.debug("🔧 根据正确答案 {} 生成了判断题选项", correctAnswer);
                return options;
            }
            
            // 如果正确答案是具体内容，尝试生成选项
            if (correctAnswer.length() > 1) {
                options.add("A、" + correctAnswer);
                options.add("B、其他选项1");
                options.add("C、其他选项2");
                options.add("D、其他选项3");
                logger.debug("🔧 根据正确答案内容生成了选项");
                return options;
            }
            
        } catch (Exception e) {
            logger.warn("生成默认选项时出错: {}", e.getMessage());
        }
        
        // 如果以上都失败，返回空列表
        return new ArrayList<>();
    }
} 
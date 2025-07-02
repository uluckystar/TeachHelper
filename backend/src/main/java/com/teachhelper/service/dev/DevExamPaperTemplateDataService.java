package com.teachhelper.service.dev;

import com.teachhelper.entity.ExamPaperTemplate;
import com.teachhelper.entity.ExamPaperTemplateQuestion;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.repository.ExamPaperTemplateRepository;
import com.teachhelper.repository.ExamPaperTemplateQuestionRepository;
import com.teachhelper.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 开发环境试卷模板数据服务
 * 负责创建ExamPaperTemplate的测试数据
 */
@Service
public class DevExamPaperTemplateDataService {
    
    private static final Logger log = LoggerFactory.getLogger(DevExamPaperTemplateDataService.class);
    
    @Autowired
    private ExamPaperTemplateRepository templateRepository;
    
    @Autowired
    private ExamPaperTemplateQuestionRepository templateQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 创建试卷模板数据
     */
    @Transactional
    public void createExamPaperTemplates() {
        List<ExamPaperTemplate> existingTemplates = templateRepository.findAll();
        if (!existingTemplates.isEmpty()) {
            log.info("试卷模板数据已存在，跳过创建");
            return;
        }
        
        List<ExamPaperTemplate> templates = new ArrayList<>();
        
        // 创建公开模板
        templates.add(createTemplate("计算机基础综合考试模板", "适用于计算机科学基础课程的综合性考试", 
                                    "计算机科学", "本科", 100, 120, 
                                    ExamPaperTemplate.TemplateType.AI_GENERATED,
                                    ExamPaperTemplate.TemplateStatus.PUBLISHED,
                                    true, 1L));
                                    
        templates.add(createTemplate("Java编程实践考试模板", "专注于Java编程能力的实践性考试", 
                                    "软件工程", "本科", 100, 150, 
                                    ExamPaperTemplate.TemplateType.MANUAL,
                                    ExamPaperTemplate.TemplateStatus.PUBLISHED,
                                    true, 1L));
                                    
        templates.add(createTemplate("数据库应用考试模板", "数据库设计和SQL应用能力考试", 
                                    "计算机科学", "本科", 100, 90, 
                                    ExamPaperTemplate.TemplateType.DOCUMENT_EXTRACTED,
                                    ExamPaperTemplate.TemplateStatus.PUBLISHED,
                                    true, 1L));
        
        // 创建私有模板
        templates.add(createTemplate("机器学习理论考试模板", "机器学习基础理论和算法考试", 
                                    "人工智能", "研究生", 100, 180, 
                                    ExamPaperTemplate.TemplateType.AI_GENERATED,
                                    ExamPaperTemplate.TemplateStatus.READY,
                                    false, 2L));
                                    
        templates.add(createTemplate("网络安全实践考试模板", "网络安全技术和实践能力考试", 
                                    "网络安全", "本科", 100, 120, 
                                    ExamPaperTemplate.TemplateType.MANUAL,
                                    ExamPaperTemplate.TemplateStatus.DRAFT,
                                    false, 2L));
                                    
        templates.add(createTemplate("软件工程项目考试模板", "软件工程方法和项目管理考试", 
                                    "软件工程", "本科", 100, 120, 
                                    ExamPaperTemplate.TemplateType.COPIED,
                                    ExamPaperTemplate.TemplateStatus.READY,
                                    false, 3L));
                                    
        templates.add(createTemplate("数学基础能力考试模板", "高等数学基础能力测试", 
                                    "数学", "本科", 100, 120, 
                                    ExamPaperTemplate.TemplateType.AI_GENERATED,
                                    ExamPaperTemplate.TemplateStatus.DRAFT,
                                    false, 3L));

        templateRepository.saveAll(templates);
        
        // 为每个模板创建题目
        for (ExamPaperTemplate template : templates) {
            createTemplateQuestions(template);
        }
        
        log.info("✅ 成功创建 {} 个试卷模板", templates.size());
    }
    
    /**
     * 创建单个模板
     */
    private ExamPaperTemplate createTemplate(String name, String description, String subject, 
                                           String gradeLevel, Integer totalScore, Integer duration,
                                           ExamPaperTemplate.TemplateType templateType,
                                           ExamPaperTemplate.TemplateStatus status,
                                           Boolean isPublic, Long createdBy) {
        ExamPaperTemplate template = new ExamPaperTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setSubject(subject);
        template.setGradeLevel(gradeLevel);
        template.setTotalScore(totalScore);
        template.setDuration(duration);
        template.setTemplateType(templateType);
        template.setStatus(status);
        template.setIsPublic(isPublic);
        template.setCreatedBy(createdBy);
        template.setUsageCount(0);
        
        // 设置配置信息
        template.setQuestionTypeConfig(createQuestionTypeConfig(subject));
        template.setDifficultyConfig(createDifficultyConfig());
        template.setKnowledgeBaseConfig(createKnowledgeConfig(subject));
        template.setTags("[\"" + subject + "\", \"" + gradeLevel + "\"]");
        
        return template;
    }
    
    /**
     * 为模板创建题目
     */
    private void createTemplateQuestions(ExamPaperTemplate template) {
        List<ExamPaperTemplateQuestion> questions = new ArrayList<>();
        
        // 创建5-8个题目
        int questionCount = 5 + (int)(Math.random() * 4);
        
        for (int i = 1; i <= questionCount; i++) {
            ExamPaperTemplateQuestion question = new ExamPaperTemplateQuestion();
            question.setTemplate(template);
            question.setQuestionOrder(i);
            question.setQuestionType(getRandomQuestionType());
            question.setQuestionContent("这是第" + i + "道题目，关于" + template.getSubject() + "的内容。");
            question.setScore(100.0 / questionCount);
            question.setDifficultyLevel(getRandomDifficulty());
            question.setIsRequired(true);
            question.setStatus(ExamPaperTemplateQuestion.QuestionStatus.READY);
            
            questions.add(question);
        }
        
        templateQuestionRepository.saveAll(questions);
    }
    
    /**
     * 获取随机题目类型
     */
    private QuestionType getRandomQuestionType() {
        QuestionType[] types = {
            QuestionType.SHORT_ANSWER,
            QuestionType.ESSAY,
            QuestionType.SINGLE_CHOICE,
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.TRUE_FALSE,
            QuestionType.CASE_ANALYSIS
        };
        return types[(int)(Math.random() * types.length)];
    }
    
    /**
     * 获取随机难度
     */
    private String getRandomDifficulty() {
        String[] difficulties = {"EASY", "MEDIUM", "HARD"};
        return difficulties[(int)(Math.random() * difficulties.length)];
    }
    
    /**
     * 创建题型配置
     */
    private String createQuestionTypeConfig(String subject) {
        return "{\"questionTypes\":{\"SHORT_ANSWER\":30,\"ESSAY\":25,\"SINGLE_CHOICE\":20,\"MULTIPLE_CHOICE\":15,\"TRUE_FALSE\":10}}";
    }
    
    /**
     * 创建难度配置
     */
    private String createDifficultyConfig() {
        return "{\"difficultyDistribution\":{\"EASY\":30,\"MEDIUM\":50,\"HARD\":20}}";
    }
    
    /**
     * 创建知识库配置
     */
    private String createKnowledgeConfig(String subject) {
        return "{\"knowledgeBases\":[\"" + subject + "\"],\"weightDistribution\":{\"basic\":40,\"intermediate\":40,\"advanced\":20}}";
    }
    
    /**
     * 清空试卷模板数据
     */
    @Transactional
    public void clearExamPaperTemplateData() {
        templateQuestionRepository.deleteAll();
        templateRepository.deleteAll();
        log.info("试卷模板数据清理完成");
    }
} 
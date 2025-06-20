package com.teachhelper.service.dev;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Exam;
import com.teachhelper.entity.PaperGenerationHistory;
import com.teachhelper.entity.PaperGenerationTemplate;
import com.teachhelper.entity.User;
import com.teachhelper.repository.ExamRepository;
import com.teachhelper.repository.PaperGenerationHistoryRepository;
import com.teachhelper.repository.PaperGenerationTemplateRepository;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境试卷生成数据服务
 * 负责创建试卷生成模板和历史记录数据
 */
@Service
public class DevPaperDataService {

    @Autowired
    private PaperGenerationTemplateRepository templateRepository;
    
    @Autowired
    private PaperGenerationHistoryRepository historyRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExamRepository examRepository;

    /**
     * 创建试卷生成模板数据
     */
    @Transactional
    public void createPaperTemplates() {
        List<User> teachers = userRepository.findAll().stream()
            .filter(u -> u.getUsername().startsWith("teacher"))
            .toList();
            
        if (teachers.isEmpty()) {
            throw new RuntimeException("请先创建教师用户数据");
        }

        List<PaperGenerationTemplate> templates = new ArrayList<>();
        
        // 创建各种类型的试卷模板
        templates.add(createTemplate("计算机基础综合考试模板", "适用于计算机科学基础课程的综合性考试", 
                                    "计算机科学", "本科", 100, 120, 
                                    createQuestionConfig("basic_cs"), 
                                    createDifficultyConfig("balanced"), 
                                    createKnowledgeConfig("cs_foundation"),
                                    teachers.get(0).getId()));
                                    
        templates.add(createTemplate("Java编程实践考试模板", "专注于Java编程能力的实践性考试", 
                                    "软件工程", "本科", 100, 150, 
                                    createQuestionConfig("programming"), 
                                    createDifficultyConfig("practical"), 
                                    createKnowledgeConfig("java_programming"),
                                    teachers.get(0).getId()));
                                    
        templates.add(createTemplate("数据库应用考试模板", "数据库设计和SQL应用能力考试", 
                                    "计算机科学", "本科", 100, 90, 
                                    createQuestionConfig("database"), 
                                    createDifficultyConfig("moderate"), 
                                    createKnowledgeConfig("database_systems"),
                                    teachers.get(1 % teachers.size()).getId()));
                                    
        templates.add(createTemplate("机器学习理论考试模板", "机器学习基础理论和算法考试", 
                                    "人工智能", "研究生", 100, 180, 
                                    createQuestionConfig("theory_heavy"), 
                                    createDifficultyConfig("advanced"), 
                                    createKnowledgeConfig("machine_learning"),
                                    teachers.get(1 % teachers.size()).getId()));
                                    
        templates.add(createTemplate("网络安全实践考试模板", "网络安全技术和实践能力考试", 
                                    "网络安全", "本科", 100, 120, 
                                    createQuestionConfig("security_practice"), 
                                    createDifficultyConfig("practical"), 
                                    createKnowledgeConfig("network_security"),
                                    teachers.get(2 % teachers.size()).getId()));
                                    
        templates.add(createTemplate("软件工程项目考试模板", "软件工程方法和项目管理考试", 
                                    "软件工程", "本科", 100, 120, 
                                    createQuestionConfig("project_based"), 
                                    createDifficultyConfig("balanced"), 
                                    createKnowledgeConfig("software_engineering"),
                                    teachers.get(2 % teachers.size()).getId()));
                                    
        templates.add(createTemplate("数学基础能力考试模板", "高等数学基础能力测试", 
                                    "数学", "本科", 100, 120, 
                                    createQuestionConfig("calculation_heavy"), 
                                    createDifficultyConfig("theoretical"), 
                                    createKnowledgeConfig("mathematics"),
                                    teachers.get(3 % teachers.size()).getId()));

        templateRepository.saveAll(templates);
        
        // 创建试卷生成历史记录
        createPaperHistory(templates);
    }

    /**
     * 创建试卷生成模板
     */
    private PaperGenerationTemplate createTemplate(String name, String description, String subject, 
                                                 String gradeLevel, Integer totalScore, Integer timeLimit,
                                                 String questionConfig, String difficultyConfig, 
                                                 String knowledgeBaseConfig, Long createdBy) {
        PaperGenerationTemplate template = new PaperGenerationTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setSubject(subject);
        template.setGradeLevel(gradeLevel);
        template.setTotalScore(totalScore);
        template.setTimeLimit(timeLimit);
        template.setQuestionConfig(questionConfig);
        template.setDifficultyConfig(difficultyConfig);
        template.setKnowledgeBaseConfig(knowledgeBaseConfig);
        template.setCreatedBy(createdBy);
        return template;
    }

    /**
     * 创建题型配置JSON
     */
    private String createQuestionConfig(String type) {
        switch (type) {
            case "basic_cs":
                return "{\"single_choice\":5,\"multiple_choice\":3,\"true_false\":4,\"short_answer\":4,\"essay\":2,\"coding\":0,\"case_analysis\":1,\"calculation\":1}";
            case "programming":
                return "{\"single_choice\":3,\"multiple_choice\":2,\"true_false\":2,\"short_answer\":2,\"essay\":1,\"coding\":4,\"case_analysis\":1,\"calculation\":0}";
            case "database":
                return "{\"single_choice\":4,\"multiple_choice\":3,\"true_false\":3,\"short_answer\":3,\"essay\":1,\"coding\":2,\"case_analysis\":2,\"calculation\":1}";
            case "theory_heavy":
                return "{\"single_choice\":3,\"multiple_choice\":2,\"true_false\":2,\"short_answer\":5,\"essay\":4,\"coding\":0,\"case_analysis\":2,\"calculation\":2}";
            case "security_practice":
                return "{\"single_choice\":4,\"multiple_choice\":3,\"true_false\":3,\"short_answer\":3,\"essay\":2,\"coding\":1,\"case_analysis\":3,\"calculation\":1}";
            case "project_based":
                return "{\"single_choice\":3,\"multiple_choice\":2,\"true_false\":2,\"short_answer\":4,\"essay\":3,\"coding\":1,\"case_analysis\":4,\"calculation\":1}";
            case "calculation_heavy":
                return "{\"single_choice\":3,\"multiple_choice\":2,\"true_false\":2,\"short_answer\":3,\"essay\":2,\"coding\":0,\"case_analysis\":1,\"calculation\":7}";
            default:
                return "{\"single_choice\":4,\"multiple_choice\":3,\"true_false\":3,\"short_answer\":4,\"essay\":2,\"coding\":1,\"case_analysis\":2,\"calculation\":1}";
        }
    }

    /**
     * 创建难度配置JSON
     */
    private String createDifficultyConfig(String type) {
        switch (type) {
            case "balanced":
                return "{\"easy\":40,\"medium\":40,\"hard\":20}";
            case "practical":
                return "{\"easy\":30,\"medium\":50,\"hard\":20}";
            case "moderate":
                return "{\"easy\":35,\"medium\":45,\"hard\":20}";
            case "advanced":
                return "{\"easy\":20,\"medium\":40,\"hard\":40}";
            case "theoretical":
                return "{\"easy\":25,\"medium\":45,\"hard\":30}";
            default:
                return "{\"easy\":30,\"medium\":50,\"hard\":20}";
        }
    }

    /**
     * 创建知识库配置JSON
     */
    private String createKnowledgeConfig(String domain) {
        switch (domain) {
            case "cs_foundation":
                return "{\"knowledge_bases\":[\"计算机科学基础\"],\"weight_distribution\":{\"data_structures\":30,\"algorithms\":25,\"operating_systems\":25,\"computer_networks\":20}}";
            case "java_programming":
                return "{\"knowledge_bases\":[\"Java编程技术\"],\"weight_distribution\":{\"basic_syntax\":20,\"oop\":30,\"collections\":20,\"frameworks\":30}}";
            case "database_systems":
                return "{\"knowledge_bases\":[\"数据库原理与应用\"],\"weight_distribution\":{\"theory\":40,\"sql\":35,\"design\":25}}";
            case "machine_learning":
                return "{\"knowledge_bases\":[\"机器学习基础\"],\"weight_distribution\":{\"supervised\":35,\"unsupervised\":25,\"neural_networks\":40}}";
            case "network_security":
                return "{\"knowledge_bases\":[\"网络安全技术\"],\"weight_distribution\":{\"cryptography\":30,\"network_security\":35,\"system_security\":35}}";
            case "software_engineering":
                return "{\"knowledge_bases\":[\"软件工程方法\"],\"weight_distribution\":{\"requirements\":25,\"design\":30,\"testing\":25,\"project_management\":20}}";
            case "mathematics":
                return "{\"knowledge_bases\":[\"高等数学\"],\"weight_distribution\":{\"calculus\":40,\"linear_algebra\":30,\"probability\":30}}";
            default:
                return "{\"knowledge_bases\":[],\"weight_distribution\":{}}";
        }
    }

    /**
     * 创建试卷生成历史记录
     */
    private void createPaperHistory(List<PaperGenerationTemplate> templates) {
        List<Exam> exams = examRepository.findAll();
        List<PaperGenerationHistory> historyList = new ArrayList<>();
        
        int examIndex = 0;
        for (PaperGenerationTemplate template : templates) {
            // 为每个模板创建2-3个历史记录
            for (int i = 0; i < 3; i++) {
                PaperGenerationHistory history = new PaperGenerationHistory();
                history.setTitle(template.getName().replace("模板", "") + " - 第" + (i + 1) + "次生成");
                history.setDescription("基于" + template.getName() + "生成的试卷");
                
                if (examIndex < exams.size()) {
                    history.setExamId(exams.get(examIndex).getId());
                    examIndex++;
                }
                
                history.setTemplateId(template.getId());
                history.setSubject(template.getSubject());
                history.setGradeLevel(template.getGradeLevel());
                history.setTotalScore(template.getTotalScore());
                history.setTimeLimit(template.getTimeLimit());
                
                // 根据题型配置计算题目数量
                int questionCount = calculateQuestionCount(template.getQuestionConfig());
                history.setQuestionCount(questionCount);
                
                // 设置生成状态
                if (i == 0) {
                    history.setStatus(PaperGenerationHistory.GenerationStatus.SUCCESS);
                } else if (i == 1) {
                    history.setStatus(PaperGenerationHistory.GenerationStatus.SUCCESS);
                } else {
                    history.setStatus(Math.random() < 0.8 ? PaperGenerationHistory.GenerationStatus.SUCCESS : 
                                    PaperGenerationHistory.GenerationStatus.PARTIAL);
                }
                
                history.setGenerationConfig(template.getQuestionConfig());
                
                if (history.getStatus() == PaperGenerationHistory.GenerationStatus.PARTIAL) {
                    history.setWarnings("部分题型生成数量不足，建议调整知识库内容");
                }
                
                history.setCreatedBy(template.getCreatedBy());
                
                historyList.add(history);
            }
        }
        
        historyRepository.saveAll(historyList);
    }

    /**
     * 根据题型配置计算总题目数量
     */
    private int calculateQuestionCount(String questionConfig) {
        // 简单解析JSON并计算总数
        try {
            return questionConfig.chars()
                .mapToObj(c -> (char) c)
                .filter(Character::isDigit)
                .mapToInt(c -> Character.getNumericValue(c))
                .sum();
        } catch (Exception e) {
            return 20; // 默认值
        }
    }

    /**
     * 清空试卷生成相关数据
     */
    @Transactional
    public void clearPaperData() {
        historyRepository.deleteAll();
        templateRepository.deleteAll();
    }
}

package com.teachhelper.service.dev;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.ScoreLevel;
import com.teachhelper.entity.SystemRubric;
import com.teachhelper.entity.SystemRubricCriterion;
import com.teachhelper.entity.User;
import com.teachhelper.repository.ScoreLevelRepository;
import com.teachhelper.repository.SystemRubricCriterionRepository;
import com.teachhelper.repository.SystemRubricRepository;
import com.teachhelper.repository.UserRepository;

/**
 * 开发环境系统评分标准数据服务
 * 负责创建系统评分标准、评分条目和分数等级数据
 */
@Service
public class DevRubricDataService {

    @Autowired
    private SystemRubricRepository systemRubricRepository;
    
    @Autowired
    private SystemRubricCriterionRepository criterionRepository;
    
    @Autowired
    private ScoreLevelRepository scoreLevelRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 创建系统评分标准数据
     */
    @Transactional
    public void createSystemRubrics() {
        List<User> teachers = userRepository.findAll().stream()
            .filter(u -> u.getUsername().startsWith("teacher"))
            .toList();
            
        if (teachers.isEmpty()) {
            throw new RuntimeException("请先创建教师用户数据");
        }

        List<SystemRubric> rubrics = new ArrayList<>();
        
        // 创建各学科的评分标准
        rubrics.add(createComputerScienceRubric(teachers.get(0)));
        rubrics.add(createProgrammingRubric(teachers.get(0)));
        rubrics.add(createDatabaseRubric(teachers.get(1 % teachers.size())));
        rubrics.add(createMachineLearningRubric(teachers.get(1 % teachers.size())));
        rubrics.add(createNetworkSecurityRubric(teachers.get(2 % teachers.size())));
        rubrics.add(createSoftwareEngineeringRubric(teachers.get(2 % teachers.size())));
        rubrics.add(createMathematicsRubric(teachers.get(3 % teachers.size())));
        rubrics.add(createGeneralRubric(teachers.get(0))); // 通用评分标准

        systemRubricRepository.saveAll(rubrics);
        
        // 为每个评分标准创建条目和分数等级
        for (SystemRubric rubric : rubrics) {
            createCriteriaAndLevels(rubric);
        }
    }

    /**
     * 创建计算机科学评分标准
     */
    private SystemRubric createComputerScienceRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("计算机科学基础评分标准");
        rubric.setDescription("适用于数据结构、算法、操作系统等计算机科学基础课程的评分标准");
        rubric.setSubject("计算机科学");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(15);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建编程评分标准
     */
    private SystemRubric createProgrammingRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("编程能力评分标准");
        rubric.setDescription("适用于Java、Python等编程语言学习的评分标准，重点评估代码质量和逻辑思维");
        rubric.setSubject("编程");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(22);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建数据库评分标准
     */
    private SystemRubric createDatabaseRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("数据库应用评分标准");
        rubric.setDescription("适用于数据库设计、SQL查询等数据库相关课程的评分标准");
        rubric.setSubject("数据库");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(18);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建机器学习评分标准
     */
    private SystemRubric createMachineLearningRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("机器学习理论评分标准");
        rubric.setDescription("适用于机器学习、深度学习等人工智能课程的评分标准");
        rubric.setSubject("人工智能");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(12);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建网络安全评分标准
     */
    private SystemRubric createNetworkSecurityRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("网络安全技术评分标准");
        rubric.setDescription("适用于网络安全、密码学等安全技术课程的评分标准");
        rubric.setSubject("网络安全");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(9);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建软件工程评分标准
     */
    private SystemRubric createSoftwareEngineeringRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("软件工程项目评分标准");
        rubric.setDescription("适用于软件工程方法、项目管理等课程的评分标准");
        rubric.setSubject("软件工程");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(14);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建数学评分标准
     */
    private SystemRubric createMathematicsRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("高等数学评分标准");
        rubric.setDescription("适用于微积分、线性代数、概率统计等数学课程的评分标准");
        rubric.setSubject("数学");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(25);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 创建通用评分标准
     */
    private SystemRubric createGeneralRubric(User creator) {
        SystemRubric rubric = new SystemRubric();
        rubric.setName("通用学科评分标准");
        rubric.setDescription("适用于多种学科的通用评分标准，可灵活调整权重");
        rubric.setSubject("通用");
        rubric.setTotalScore(new BigDecimal("100"));
        rubric.setIsActive(true);
        rubric.setUsageCount(35);
        rubric.setCreatedBy(creator);
        return rubric;
    }

    /**
     * 为评分标准创建条目和分数等级
     */
    private void createCriteriaAndLevels(SystemRubric rubric) {
        List<SystemRubricCriterion> criteria = new ArrayList<>();
        
        switch (rubric.getSubject()) {
            case "计算机科学":
                criteria.add(createCriterion(rubric, "概念理解", "对基本概念和原理的理解程度", 25));
                criteria.add(createCriterion(rubric, "算法分析", "算法设计和复杂度分析能力", 30));
                criteria.add(createCriterion(rubric, "实现能力", "将理论转化为具体实现的能力", 25));
                criteria.add(createCriterion(rubric, "应用拓展", "知识应用和问题解决能力", 20));
                break;
                
            case "编程":
                criteria.add(createCriterion(rubric, "代码正确性", "程序功能的正确性和完整性", 35));
                criteria.add(createCriterion(rubric, "代码质量", "代码结构、可读性和规范性", 25));
                criteria.add(createCriterion(rubric, "算法效率", "算法选择和时间空间复杂度", 20));
                criteria.add(createCriterion(rubric, "创新思路", "解决问题的创新性和优化思路", 20));
                break;
                
            case "数据库":
                criteria.add(createCriterion(rubric, "理论基础", "数据库理论和概念掌握", 25));
                criteria.add(createCriterion(rubric, "SQL能力", "SQL查询和操作的熟练程度", 30));
                criteria.add(createCriterion(rubric, "设计能力", "数据库设计和建模能力", 25));
                criteria.add(createCriterion(rubric, "性能优化", "查询优化和性能调优能力", 20));
                break;
                
            case "人工智能":
                criteria.add(createCriterion(rubric, "理论理解", "机器学习理论和数学基础", 30));
                criteria.add(createCriterion(rubric, "算法掌握", "各种学习算法的理解和应用", 25));
                criteria.add(createCriterion(rubric, "实践能力", "模型构建和调参能力", 25));
                criteria.add(createCriterion(rubric, "分析能力", "结果分析和问题诊断能力", 20));
                break;
                
            case "网络安全":
                criteria.add(createCriterion(rubric, "安全理论", "网络安全基础理论知识", 25));
                criteria.add(createCriterion(rubric, "技术应用", "安全技术和工具的应用能力", 30));
                criteria.add(createCriterion(rubric, "风险评估", "安全风险识别和评估能力", 25));
                criteria.add(createCriterion(rubric, "防护方案", "安全防护方案设计能力", 20));
                break;
                
            case "软件工程":
                criteria.add(createCriterion(rubric, "需求分析", "需求理解和分析能力", 25));
                criteria.add(createCriterion(rubric, "系统设计", "系统架构和详细设计能力", 30));
                criteria.add(createCriterion(rubric, "项目管理", "项目计划和管理能力", 20));
                criteria.add(createCriterion(rubric, "质量保证", "测试和质量控制能力", 25));
                break;
                
            case "数学":
                criteria.add(createCriterion(rubric, "计算准确性", "数学计算的准确性", 30));
                criteria.add(createCriterion(rubric, "方法应用", "数学方法的正确应用", 25));
                criteria.add(createCriterion(rubric, "逻辑推理", "数学推理和证明能力", 25));
                criteria.add(createCriterion(rubric, "问题建模", "实际问题的数学建模能力", 20));
                break;
                
            default: // 通用
                criteria.add(createCriterion(rubric, "内容准确性", "答案内容的准确性和完整性", 30));
                criteria.add(createCriterion(rubric, "逻辑清晰性", "思路逻辑的清晰度", 25));
                criteria.add(createCriterion(rubric, "深度广度", "分析的深度和广度", 25));
                criteria.add(createCriterion(rubric, "表达能力", "语言表达和组织能力", 20));
                break;
        }
        
        criterionRepository.saveAll(criteria);
        
        // 为每个条目创建分数等级
        for (SystemRubricCriterion criterion : criteria) {
            createScoreLevels(criterion);
        }
    }

    /**
     * 创建评分条目
     */
    private SystemRubricCriterion createCriterion(SystemRubric rubric, String name, String description, Integer weight) {
        SystemRubricCriterion criterion = new SystemRubricCriterion();
        criterion.setSystemRubric(rubric);
        criterion.setName(name);
        criterion.setDescription(description);
        criterion.setWeight(weight);
        return criterion;
    }

    /**
     * 为评分条目创建分数等级
     */
    private void createScoreLevels(SystemRubricCriterion criterion) {
        List<ScoreLevel> levels = new ArrayList<>();
        
        // 创建5个等级：优秀、良好、中等、及格、不及格
        BigDecimal maxScore = criterion.getSystemRubric().getTotalScore()
            .multiply(new BigDecimal(criterion.getWeight()))
            .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);
        
        levels.add(createScoreLevel(criterion, "优秀", 
                                  maxScore.multiply(new BigDecimal("0.9")), 
                                  "完全达到要求，表现优异"));
        levels.add(createScoreLevel(criterion, "良好", 
                                  maxScore.multiply(new BigDecimal("0.8")), 
                                  "基本达到要求，表现良好"));
        levels.add(createScoreLevel(criterion, "中等", 
                                  maxScore.multiply(new BigDecimal("0.7")), 
                                  "达到基本要求，有改进空间"));
        levels.add(createScoreLevel(criterion, "及格", 
                                  maxScore.multiply(new BigDecimal("0.6")), 
                                  "勉强达到要求，需要改进"));
        levels.add(createScoreLevel(criterion, "不及格", 
                                  maxScore.multiply(new BigDecimal("0.3")), 
                                  "未达到基本要求"));
        
        scoreLevelRepository.saveAll(levels);
    }

    /**
     * 创建分数等级
     */
    private ScoreLevel createScoreLevel(SystemRubricCriterion criterion, String name, BigDecimal score, String description) {
        ScoreLevel level = new ScoreLevel();
        level.setCriterion(criterion);
        level.setName(name);
        level.setScore(score);
        level.setDescription(description);
        return level;
    }

    /**
     * 清空系统评分标准相关数据
     */
    @Transactional
    public void clearRubricData() {
        scoreLevelRepository.deleteAll();
        criterionRepository.deleteAll();
        systemRubricRepository.deleteAll();
    }
}

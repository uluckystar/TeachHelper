package com.teachhelper.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.dto.request.RubricCreateRequest;
import com.teachhelper.dto.response.RubricResponse;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.entity.ScoreLevel;
import com.teachhelper.entity.SystemRubric;
import com.teachhelper.entity.SystemRubricCriterion;
import com.teachhelper.entity.User;
import com.teachhelper.repository.RubricCriterionRepository;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.ScoreLevelRepository;
import com.teachhelper.repository.SystemRubricCriterionRepository;
import com.teachhelper.repository.SystemRubricRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.service.RubricService;

/**
 * 系统级评分标准服务实现
 */
@Service
@Transactional
public class RubricServiceImpl implements RubricService {
    
    private static final Logger log = LoggerFactory.getLogger(RubricServiceImpl.class);
    
    @Autowired
    private SystemRubricRepository systemRubricRepository;
    
    @Autowired
    private SystemRubricCriterionRepository criterionRepository;
    
    @Autowired
    private ScoreLevelRepository scoreLevelRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<SystemRubric> getAllRubrics() {
        return systemRubricRepository.findAllOrderByCreatedAtDesc();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RubricResponse> getAllRubricResponses() {
        List<SystemRubric> rubrics = systemRubricRepository.findAllOrderByCreatedAtDesc();
        return rubrics.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SystemRubric> getRubricById(Long id) {
        Optional<SystemRubric> rubricOpt = systemRubricRepository.findByIdWithCriteria(id);
        if (rubricOpt.isPresent()) {
            SystemRubric rubric = rubricOpt.get();
            // 手动加载每个criterion的scoreLevels
            if (rubric.getCriteria() != null) {
                for (SystemRubricCriterion criterion : rubric.getCriteria()) {
                    // 通过访问scoreLevels属性来触发懒加载
                    int size = criterion.getScoreLevels().size(); // 这会触发懒加载
                    log.debug("Loaded {} score levels for criterion {}", size, criterion.getName());
                }
            }
        }
        return rubricOpt;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<RubricResponse> getRubricResponseById(Long id) {
        Optional<SystemRubric> rubricOpt = systemRubricRepository.findByIdWithCriteria(id);
        if (rubricOpt.isPresent()) {
            SystemRubric rubric = rubricOpt.get();
            log.debug("Converting rubric {} to response within transaction", rubric.getName());
            
            // 在事务内进行转换，确保懒加载正常工作
            RubricResponse response = convertToResponse(rubric);
            return Optional.of(response);
        }
        return Optional.empty();
    }
    
    @Override
    @Transactional
    public SystemRubric createRubric(RubricCreateRequest request) {
        // 创建主评分标准
        SystemRubric rubric = new SystemRubric();
        rubric.setName(request.getName());
        rubric.setDescription(request.getDescription());
        rubric.setSubject(request.getSubject());
        rubric.setIsActive(true);
        rubric.setUsageCount(0);
        
        // 设置创建者
        User currentUser = getCurrentUser();
        rubric.setCreatedBy(currentUser);
        
        // 计算总分
        BigDecimal totalScore = calculateTotalScore(request.getCriteria());
        rubric.setTotalScore(totalScore);
        
        // 保存主评分标准
        rubric = systemRubricRepository.save(rubric);
        
        // 创建评价标准并获取创建的criteria列表
        List<SystemRubricCriterion> createdCriteria = createCriteria(rubric, request.getCriteria());
        
        // 手动设置关联关系以避免懒加载问题
        rubric.setCriteria(createdCriteria);
        
        log.debug("Created rubric {} with {} criteria", rubric.getName(), 
                  createdCriteria != null ? createdCriteria.size() : 0);
        
        return rubric;
    }
    
    @Override
    @Transactional
    public RubricResponse createRubricResponse(RubricCreateRequest request) {
        SystemRubric rubric = createRubric(request);
        // 在同一事务内转换为响应对象
        return convertToResponse(rubric);
    }
    
    /**
     * 更新评分标准
     */
    @Override
    public SystemRubric updateRubric(Long id, RubricCreateRequest request) {
        SystemRubric rubric = systemRubricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评分标准不存在"));
        
        // 更新基本信息
        rubric.setName(request.getName());
        rubric.setDescription(request.getDescription());
        rubric.setSubject(request.getSubject());
        
        // 计算新的总分
        BigDecimal totalScore = calculateTotalScore(request.getCriteria());
        rubric.setTotalScore(totalScore);
        
        // 删除旧的评价标准和评分级别
        deleteCriteriaAndScoreLevels(id);
        
        // 创建新的评价标准
        List<SystemRubricCriterion> createdCriteria = createCriteria(rubric, request.getCriteria());
        rubric.setCriteria(createdCriteria);
        
        // 保存更新
        rubric = systemRubricRepository.save(rubric);
        
        // 重新加载以获取完整数据
        Optional<SystemRubric> reloadedRubric = getRubricById(rubric.getId());
        return reloadedRubric.orElse(rubric);
    }
    
    @Override
    public RubricResponse updateRubricResponse(Long id, RubricCreateRequest request) {
        SystemRubric rubric = updateRubric(id, request);
        // 在同一事务内转换为响应对象
        return convertToResponse(rubric);
    }
    
    @Override
    public void deleteRubric(Long id) {
        if (!systemRubricRepository.existsById(id)) {
            throw new RuntimeException("评分标准不存在");
        }
        
        // 删除关联的评价标准和评分级别
        deleteCriteriaAndScoreLevels(id);
        
        // 删除主评分标准
        systemRubricRepository.deleteById(id);
    }
    
    @Override
    public SystemRubric toggleRubricStatus(Long id, Boolean isActive) {
        SystemRubric rubric = systemRubricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评分标准不存在"));
        
        rubric.setIsActive(isActive);
        return systemRubricRepository.save(rubric);
    }
    
    @Override
    public RubricResponse toggleRubricStatusResponse(Long id, Boolean isActive) {
        SystemRubric rubric = toggleRubricStatus(id, isActive);
        // 重新加载完整数据并转换
        Optional<RubricResponse> responseOpt = getRubricResponseById(rubric.getId());
        return responseOpt.orElseThrow(() -> new RuntimeException("评分标准不存在"));
    }
    
    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("当前用户不存在"));
    }
    
    /**
     * 计算总分
     * 总分 = 所有标准的权重之和
     */
    private BigDecimal calculateTotalScore(List<RubricCreateRequest.SystemRubricCriterionRequest> criteria) {
        BigDecimal totalScore = BigDecimal.ZERO;
        
        for (RubricCreateRequest.SystemRubricCriterionRequest criterion : criteria) {
            // 使用权重作为总分计算基础
            if (criterion.getWeight() != null) {
                totalScore = totalScore.add(BigDecimal.valueOf(criterion.getWeight()));
            }
        }
        
        return totalScore;
    }
    
    /**
     * 创建评价标准
     */
    private List<SystemRubricCriterion> createCriteria(SystemRubric rubric, List<RubricCreateRequest.SystemRubricCriterionRequest> criteriaRequest) {
        List<SystemRubricCriterion> createdCriteria = new ArrayList<>();
        
        for (RubricCreateRequest.SystemRubricCriterionRequest criterionRequest : criteriaRequest) {
            SystemRubricCriterion criterion = new SystemRubricCriterion();
            criterion.setName(criterionRequest.getName());
            criterion.setDescription(criterionRequest.getDescription());
            criterion.setWeight(criterionRequest.getWeight());
            criterion.setSystemRubric(rubric);
            
            // 保存评价标准
            criterion = criterionRepository.save(criterion);
            
            // 创建评分级别
            List<ScoreLevel> scoreLevels = createScoreLevels(criterion, criterionRequest.getScoreLevels());
            criterion.setScoreLevels(scoreLevels);
            
            createdCriteria.add(criterion);
        }
        
        return createdCriteria;
    }
    
    /**
     * 创建评分级别
     */
    private List<ScoreLevel> createScoreLevels(SystemRubricCriterion criterion, List<RubricCreateRequest.ScoreLevelRequest> scoreLevelsRequest) {
        List<ScoreLevel> createdScoreLevels = new ArrayList<>();
        
        for (RubricCreateRequest.ScoreLevelRequest levelRequest : scoreLevelsRequest) {
            ScoreLevel scoreLevel = new ScoreLevel();
            scoreLevel.setName(levelRequest.getName());
            scoreLevel.setScore(levelRequest.getScore());
            scoreLevel.setDescription(levelRequest.getDescription());
            scoreLevel.setCriterion(criterion);
            
            scoreLevel = scoreLevelRepository.save(scoreLevel);
            createdScoreLevels.add(scoreLevel);
        }
        
        return createdScoreLevels;
    }
    
    /**
     * 删除评价标准和评分级别
     */
    private void deleteCriteriaAndScoreLevels(Long rubricId) {
        List<SystemRubricCriterion> criteria = criterionRepository.findBySystemRubricId(rubricId);
        
        for (SystemRubricCriterion criterion : criteria) {
            // 删除评分级别
            scoreLevelRepository.deleteByCriterionId(criterion.getId());
        }
        
        // 删除评价标准
        criterionRepository.deleteBySystemRubricId(rubricId);
    }
    
    /**
     * 在事务上下文中将SystemRubric转换为RubricResponse
     */
    private RubricResponse convertToResponse(SystemRubric rubric) {
        log.debug("Converting rubric {} with {} criteria", rubric.getName(), 
                  rubric.getCriteria() != null ? rubric.getCriteria().size() : 0);
        
        RubricResponse response = new RubricResponse();
        response.setId(rubric.getId());
        response.setName(rubric.getName());
        response.setDescription(rubric.getDescription());
        response.setSubject(rubric.getSubject());
        response.setTotalScore(rubric.getTotalScore());
        response.setIsActive(rubric.getIsActive());
        response.setUsageCount(rubric.getUsageCount());
        response.setCreatedAt(rubric.getCreatedAt());
        response.setUpdatedAt(rubric.getUpdatedAt());
        response.setCreatedBy(rubric.getCreatedBy() != null ? rubric.getCreatedBy().getUsername() : null);
        
        // 转换评分标准
        if (rubric.getCriteria() != null) {
            log.debug("Converting {} criteria for rubric {}", rubric.getCriteria().size(), rubric.getName());
            response.setCriteria(rubric.getCriteria().stream()
                .map(criterion -> {
                    log.debug("Converting criterion: {}", criterion.getName());
                    RubricResponse.SystemRubricCriterionResponse criterionResponse = 
                        new RubricResponse.SystemRubricCriterionResponse();
                    criterionResponse.setId(criterion.getId());
                    criterionResponse.setName(criterion.getName());
                    criterionResponse.setDescription(criterion.getDescription());
                    criterionResponse.setWeight(criterion.getWeight());
                    
                    // 转换评分等级 - 在事务内强制加载
                    if (criterion.getScoreLevels() != null) {
                        // 强制初始化懒加载集合
                        int scoreLevelCount = criterion.getScoreLevels().size();
                        log.debug("Converting {} score levels for criterion {}", scoreLevelCount, criterion.getName());
                        
                        criterionResponse.setScoreLevels(criterion.getScoreLevels().stream()
                            .map(level -> {
                                RubricResponse.ScoreLevelResponse levelResponse = 
                                    new RubricResponse.ScoreLevelResponse();
                                levelResponse.setId(level.getId());
                                levelResponse.setName(level.getName());
                                levelResponse.setScore(level.getScore());
                                levelResponse.setDescription(level.getDescription());
                                return levelResponse;
                            })
                            .collect(Collectors.toList()));
                    } else {
                        log.warn("Score levels is null for criterion {}", criterion.getName());
                    }
                    
                    return criterionResponse;
                })
                .collect(Collectors.toList()));
        } else {
            log.warn("Criteria is null for rubric {}", rubric.getName());
        }
        
        return response;
    }
    
    @Override
    @Transactional
    public SystemRubric createProportionalRubricForQuestion(String questionContent, BigDecimal totalPoints, String rubricTitle) {
        log.info("创建基于比例的评分标准: 题目分值={}, 标题={}", totalPoints, rubricTitle);
        
        // 创建主评分标准
        SystemRubric rubric = new SystemRubric();
        rubric.setName(rubricTitle != null ? rubricTitle : "题目评分标准（" + totalPoints + "分）");
        rubric.setDescription("基于题目实际分值的比例评分标准");
        rubric.setSubject("通用");
        rubric.setIsActive(true);
        rubric.setUsageCount(0);
        rubric.setTotalScore(totalPoints);
        
        // 设置创建者
        try {
            User currentUser = getCurrentUser();
            rubric.setCreatedBy(currentUser);
        } catch (Exception e) {
            log.warn("无法获取当前用户，使用系统默认设置");
            // 在某些情况下（如系统自动创建），可能没有当前用户
        }
        
        // 保存主评分标准
        rubric = systemRubricRepository.save(rubric);
        
        // 创建比例评分维度
        List<SystemRubricCriterion> criteria = createProportionalCriteria(rubric, totalPoints);
        rubric.setCriteria(criteria);
        
        log.info("成功创建比例评分标准: {}, 总分: {}, 维度数: {}", 
                rubric.getName(), totalPoints, criteria.size());
        
        return rubric;
    }
    
    @Override
    @Transactional
    public RubricResponse createProportionalRubricForQuestionResponse(String questionContent, BigDecimal totalPoints, String rubricTitle) {
        SystemRubric rubric = createProportionalRubricForQuestion(questionContent, totalPoints, rubricTitle);
        return convertToResponse(rubric);
    }
    
    @Override
    @Transactional
    public SystemRubric updateRubricProportions(Long rubricId, BigDecimal newTotalPoints) {
        SystemRubric rubric = systemRubricRepository.findById(rubricId)
                .orElseThrow(() -> new RuntimeException("评分标准不存在"));
        
        if (!isProportionalRubric(rubricId)) {
            throw new RuntimeException("该评分标准不支持比例更新");
        }
        
        log.info("更新评分标准比例: rubricId={}, 原总分={}, 新总分={}", 
                rubricId, rubric.getTotalScore(), newTotalPoints);
        
        // 更新总分
        rubric.setTotalScore(newTotalPoints);
        
        // 删除旧的评价标准和评分级别
        deleteCriteriaAndScoreLevels(rubricId);
        
        // 重新创建比例评分维度
        List<SystemRubricCriterion> criteria = createProportionalCriteria(rubric, newTotalPoints);
        rubric.setCriteria(criteria);
        
        rubric = systemRubricRepository.save(rubric);
        
        log.info("成功更新评分标准比例: {}", rubric.getName());
        
        return rubric;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isProportionalRubric(Long rubricId) {
        Optional<SystemRubric> rubricOpt = systemRubricRepository.findById(rubricId);
        if (rubricOpt.isPresent()) {
            SystemRubric rubric = rubricOpt.get();
            // 检查是否为比例评分标准（通过描述或名称判断）
            return rubric.getDescription() != null && 
                   rubric.getDescription().contains("基于题目实际分值的比例评分标准");
        }
        return false;
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRubricTotalPoints(Long rubricId) {
        Optional<SystemRubric> rubricOpt = systemRubricRepository.findById(rubricId);
        return rubricOpt.map(SystemRubric::getTotalScore).orElse(BigDecimal.ZERO);
    }
    
    /**
     * 创建比例评分维度
     */
    private List<SystemRubricCriterion> createProportionalCriteria(SystemRubric rubric, BigDecimal totalPoints) {
        List<SystemRubricCriterion> criteria = new ArrayList<>();
        
        // 定义四个评分维度及其比例
        String[][] dimensionData = {
            {"知识理解", "对基本概念、原理和方法的理解程度", "0.40"},
            {"分析深度", "对问题的分析深度和逻辑性", "0.30"},
            {"逻辑论证", "论证过程的逻辑性和完整性", "0.20"},
            {"表达规范", "语言表达和格式规范性", "0.10"}
        };
        
        for (String[] data : dimensionData) {
            String name = data[0];
            String description = data[1];
            BigDecimal proportion = new BigDecimal(data[2]);
            BigDecimal dimensionPoints = totalPoints.multiply(proportion);
            
            SystemRubricCriterion criterion = new SystemRubricCriterion();
            criterion.setName(name);
            criterion.setDescription(description);
            criterion.setWeight(dimensionPoints.intValue());
            criterion.setSystemRubric(rubric);
            
            // 保存评价标准
            criterion = criterionRepository.save(criterion);
            
            // 创建该维度的评分级别
            List<ScoreLevel> scoreLevels = createProportionalScoreLevels(criterion, dimensionPoints);
            criterion.setScoreLevels(scoreLevels);
            
            criteria.add(criterion);
        }
        
        return criteria;
    }
    
    /**
     * 创建比例评分级别
     */
    private List<ScoreLevel> createProportionalScoreLevels(SystemRubricCriterion criterion, BigDecimal dimensionPoints) {
        List<ScoreLevel> scoreLevels = new ArrayList<>();
        
        // 定义四个等级及其分数比例范围
        String[][] levelData = {
            {"优秀", "0.90", "1.00"},
            {"良好", "0.75", "0.89"},
            {"及格", "0.60", "0.74"},
            {"不及格", "0.00", "0.59"}
        };
        
        for (String[] data : levelData) {
            String levelName = data[0];
            BigDecimal minRate = new BigDecimal(data[1]);
            BigDecimal maxRate = new BigDecimal(data[2]);
            
            BigDecimal minScore = dimensionPoints.multiply(minRate);
            BigDecimal maxScore = dimensionPoints.multiply(maxRate);
            
            ScoreLevel scoreLevel = new ScoreLevel();
            scoreLevel.setName(levelName);
            scoreLevel.setScore(maxScore); // 使用最高分作为参考分数
            scoreLevel.setDescription(String.format("%s等级：%.1f-%.1f分（%.0f%%-%.0f%%）", 
                    levelName, minScore, maxScore, 
                    minRate.multiply(new BigDecimal("100")), 
                    maxRate.multiply(new BigDecimal("100"))));
            scoreLevel.setCriterion(criterion);
            
            scoreLevel = scoreLevelRepository.save(scoreLevel);
            scoreLevels.add(scoreLevel);
        }
        
        return scoreLevels;
    }
    
    @Override
    @Transactional
    public SystemRubric createEnhancedProportionalRubric(String questionContent, String referenceAnswer, 
                                                        BigDecimal totalPoints, String rubricTitle) {
        log.info("创建增强的比例评分标准: 总分={}, 标题={}", totalPoints, rubricTitle);
        
        // 创建评分标准
        SystemRubric rubric = new SystemRubric();
        rubric.setName(rubricTitle != null ? rubricTitle : "增强评分标准（" + totalPoints + "分）");
        rubric.setDescription("基于参考答案和比例分配的评分标准，总分：" + totalPoints + "分");
        rubric.setSubject("通用");
        rubric.setIsActive(true);
        rubric.setUsageCount(0);
        rubric.setTotalScore(totalPoints);
        
        // 设置创建者
        try {
            User currentUser = getCurrentUser();
            rubric.setCreatedBy(currentUser);
        } catch (Exception e) {
            log.warn("无法获取当前用户，使用系统默认设置");
        }
        
        // 保存评分标准
        SystemRubric savedRubric = systemRubricRepository.save(rubric);
        
        // 创建比例维度
        List<SystemRubricCriterion> criteria = createProportionalCriteriaWithReferenceAnswer(
            savedRubric, totalPoints, referenceAnswer);
        
        savedRubric.setCriteria(criteria);
        log.info("成功创建增强比例评分标准，包含{}个维度", criteria.size());
        
        return savedRubric;
    }
    
    /**
     * 基于参考答案创建比例评分维度
     */
    private List<SystemRubricCriterion> createProportionalCriteriaWithReferenceAnswer(
            SystemRubric rubric, BigDecimal totalPoints, String referenceAnswer) {
        
        List<SystemRubricCriterion> criteria = new ArrayList<>();
        
        // 维度定义（40%, 30%, 20%, 10%）
        String[][] dimensions = {
            {"知识理解", "对基本概念、原理和方法的理解程度", "0.4"},
            {"分析深度", "对问题的分析深度和逻辑性", "0.3"},
            {"逻辑论证", "论证过程的逻辑性和完整性", "0.2"},
            {"表达规范", "语言表达和格式规范性", "0.1"}
        };
        
        for (String[] dim : dimensions) {
            String name = dim[0];
            String baseDescription = dim[1];
            BigDecimal ratio = new BigDecimal(dim[2]);
            BigDecimal dimensionPoints = totalPoints.multiply(ratio).setScale(1, RoundingMode.HALF_UP);
            
            // 如果有参考答案，增强描述
            String enhancedDescription = baseDescription;
            if (referenceAnswer != null && !referenceAnswer.trim().isEmpty()) {
                enhancedDescription = baseDescription + "\n\n得分要点参考：\n" + 
                    extractScorePointsFromReferenceAnswer(referenceAnswer, name);
            }
            
            SystemRubricCriterion criterion = new SystemRubricCriterion();
            criterion.setName(name);
            criterion.setDescription(enhancedDescription);
            criterion.setWeight(dimensionPoints.intValue());
            criterion.setSystemRubric(rubric);
            
            SystemRubricCriterion savedCriterion = criterionRepository.save(criterion);
            
            // 创建该维度的评分等级
            List<ScoreLevel> scoreLevels = createProportionalScoreLevels(savedCriterion, dimensionPoints);
            savedCriterion.setScoreLevels(scoreLevels);
            
            criteria.add(savedCriterion);
        }
        
        return criteria;
    }
    
    /**
     * 从参考答案中提取特定维度的得分要点
     */
    private String extractScorePointsFromReferenceAnswer(String referenceAnswer, String dimensionName) {
        // 简化的得分要点提取逻辑
        StringBuilder points = new StringBuilder();
        
        switch (dimensionName) {
            case "知识理解":
                points.append("- 准确识别和表述核心概念\n");
                points.append("- 正确理解基本原理和方法\n");
                points.append("- 体现扎实的基础知识\n");
                points.append("- 参考答案中的关键知识点要完整覆盖");
                break;
            case "分析深度":
                points.append("- 深入分析问题的多个层面\n");
                points.append("- 体现透彻的思考过程\n");
                points.append("- 展现清晰的逻辑分析\n");
                points.append("- 分析层次应与参考答案的深度相当");
                break;
            case "逻辑论证":
                points.append("- 论证过程严密完整\n");
                points.append("- 结论合理有说服力\n");
                points.append("- 论据支撑充分有效\n");
                points.append("- 逻辑脉络应与参考答案保持一致");
                break;
            case "表达规范":
                points.append("- 语言表达准确规范\n");
                points.append("- 条理清晰易于理解\n");
                points.append("- 格式符合学术要求\n");
                points.append("- 表达质量应达到参考答案标准");
                break;
            default:
                points.append("- 参考标准答案的相应部分\n");
                points.append("- 确保答案质量与参考答案相当");
        }
        
        return points.toString();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RubricCriterion> getRubricCriteriaByQuestion(Question question) {
        return rubricCriterionRepository.findByQuestionId(question.getId());
    }
}

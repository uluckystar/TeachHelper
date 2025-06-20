package com.teachhelper.service.impl;

import java.math.BigDecimal;
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
import com.teachhelper.entity.ScoreLevel;
import com.teachhelper.entity.SystemRubric;
import com.teachhelper.entity.SystemRubricCriterion;
import com.teachhelper.entity.User;
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
}

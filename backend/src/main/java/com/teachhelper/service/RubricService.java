package com.teachhelper.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.teachhelper.dto.request.RubricCreateRequest;
import com.teachhelper.dto.response.RubricResponse;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.entity.SystemRubric;

/**
 * 系统级评分标准服务接口
 */
public interface RubricService {
    
    /**
     * 获取所有评分标准
     * @return 评分标准列表
     */
    List<SystemRubric> getAllRubrics();
    
    /**
     * 根据ID获取评分标准
     * @param id 评分标准ID
     * @return 评分标准
     */
    Optional<SystemRubric> getRubricById(Long id);
    
    /**
     * 根据ID获取评分标准响应对象（包含完整的关联数据）
     * @param id 评分标准ID
     * @return 评分标准响应对象
     */
    Optional<RubricResponse> getRubricResponseById(Long id);
    
    /**
     * 获取所有评分标准响应对象列表
     * @return 评分标准响应对象列表
     */
    List<RubricResponse> getAllRubricResponses();
    
    /**
     * 创建评分标准
     * @param request 创建请求
     * @return 创建的评分标准
     */
    SystemRubric createRubric(RubricCreateRequest request);
    
    /**
     * 创建评分标准并返回响应对象
     * @param request 创建请求
     * @return 创建的评分标准响应对象
     */
    RubricResponse createRubricResponse(RubricCreateRequest request);
    
    /**
     * 更新评分标准
     * @param id 评分标准ID
     * @param request 更新请求
     * @return 更新后的评分标准
     */
    SystemRubric updateRubric(Long id, RubricCreateRequest request);
    
    /**
     * 更新评分标准并返回响应对象
     * @param id 评分标准ID
     * @param request 更新请求
     * @return 更新后的评分标准响应对象
     */
    RubricResponse updateRubricResponse(Long id, RubricCreateRequest request);
    
    /**
     * 删除评分标准
     * @param id 评分标准ID
     */
    void deleteRubric(Long id);
    
    /**
     * 切换评分标准状态
     * @param id 评分标准ID
     * @param isActive 是否激活
     * @return 更新后的评分标准
     */
    SystemRubric toggleRubricStatus(Long id, Boolean isActive);
    
    /**
     * 切换评分标准状态并返回响应对象
     * @param id 评分标准ID
     * @param isActive 是否激活
     * @return 更新后的评分标准响应对象
     */
    RubricResponse toggleRubricStatusResponse(Long id, Boolean isActive);
    
    /**
     * 为题目创建基于比例的评分标准
     * @param questionContent 题目内容
     * @param totalPoints 题目总分
     * @param rubricTitle 评分标准标题（可选）
     * @return 创建的评分标准
     */
    SystemRubric createProportionalRubricForQuestion(String questionContent, BigDecimal totalPoints, String rubricTitle);
    
    /**
     * 为题目创建基于比例的评分标准并返回响应对象
     * @param questionContent 题目内容
     * @param totalPoints 题目总分
     * @param rubricTitle 评分标准标题（可选）
     * @return 创建的评分标准响应对象
     */
    RubricResponse createProportionalRubricForQuestionResponse(String questionContent, BigDecimal totalPoints, String rubricTitle);
    
    /**
     * 更新评分标准的分值比例（当题目分值改变时）
     * @param rubricId 评分标准ID
     * @param newTotalPoints 新的总分
     * @return 更新后的评分标准
     */
    SystemRubric updateRubricProportions(Long rubricId, BigDecimal newTotalPoints);
    
    /**
     * 检查评分标准是否支持比例评分
     * @param rubricId 评分标准ID
     * @return 是否支持比例评分
     */
    boolean isProportionalRubric(Long rubricId);
    
    /**
     * 获取评分标准的总分值
     * @param rubricId 评分标准ID
     * @return 总分值
     */
    BigDecimal getRubricTotalPoints(Long rubricId);
    
    /**
     * 基于参考答案和总分创建增强的比例评分标准（带得分点标注）
     */
    SystemRubric createEnhancedProportionalRubric(String questionContent, String referenceAnswer, 
                                                 BigDecimal totalPoints, String rubricTitle);
    
    /**
     * 获取题目的评分标准列表
     */
    List<RubricCriterion> getRubricCriteriaByQuestion(Question question);
}

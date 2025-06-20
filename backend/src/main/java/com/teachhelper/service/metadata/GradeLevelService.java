package com.teachhelper.service.metadata;

import com.teachhelper.dto.request.GradeLevelRequest;
import com.teachhelper.dto.response.GradeLevelResponse;
import com.teachhelper.entity.GradeLevel;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.GradeLevelRepository;
import com.teachhelper.repository.KnowledgeBaseRepository;
import com.teachhelper.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 年级管理服务
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class GradeLevelService {
    
    private final GradeLevelRepository gradeLevelRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final QuestionBankRepository questionBankRepository;
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return 1L; // 默认管理员ID
    }
    
    /**
     * 创建年级
     */
    public GradeLevelResponse createGradeLevel(GradeLevelRequest request) {
        log.info("Creating grade level: {}", request.getName());
        
        // 检查名称是否重复
        if (gradeLevelRepository.existsByNameAndIsActiveTrue(request.getName())) {
            throw new IllegalArgumentException("年级名称已存在");
        }
        
        GradeLevel gradeLevel = new GradeLevel();
        gradeLevel.setName(request.getName());
        gradeLevel.setDescription(request.getDescription());
        gradeLevel.setCategory(request.getCategory());
        gradeLevel.setSortOrder(request.getSortOrder());
        gradeLevel.setCreatedBy(getCurrentUserId());
        gradeLevel.setIsActive(true);
        
        GradeLevel saved = gradeLevelRepository.save(gradeLevel);
        log.info("Grade level created successfully with ID: {}", saved.getId());
        
        return convertToResponse(saved);
    }
    
    /**
     * 更新年级
     */
    public GradeLevelResponse updateGradeLevel(Long id, GradeLevelRequest request) {
        log.info("Updating grade level: {}", id);
        
        GradeLevel gradeLevel = gradeLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("年级不存在"));
        
        // 如果名称发生变化，检查是否重复
        if (!gradeLevel.getName().equals(request.getName())) {
            if (gradeLevelRepository.existsByNameAndIsActiveTrue(request.getName())) {
                throw new IllegalArgumentException("年级名称已存在");
            }
        }
        
        gradeLevel.setName(request.getName());
        gradeLevel.setDescription(request.getDescription());
        gradeLevel.setCategory(request.getCategory());
        gradeLevel.setSortOrder(request.getSortOrder());
        
        GradeLevel saved = gradeLevelRepository.save(gradeLevel);
        log.info("Grade level updated successfully: {}", id);
        
        return convertToResponse(saved);
    }
    
    /**
     * 删除年级（软删除）
     */
    public void deleteGradeLevel(Long id) {
        log.info("Deleting grade level: {}", id);
        
        GradeLevel gradeLevel = gradeLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("年级不存在"));
        
        // 检查是否有关联的知识库或题目库
        long knowledgeBaseCount = knowledgeBaseRepository.countByGradeLevelAndIsActiveTrue(gradeLevel.getName());
        long questionBankCount = questionBankRepository.countByGradeLevelAndIsActiveTrue(gradeLevel.getName());
        
        if (knowledgeBaseCount > 0 || questionBankCount > 0) {
            throw new IllegalStateException("该年级下还有知识库或题目库，无法删除");
        }
        
        gradeLevel.setIsActive(false);
        gradeLevelRepository.save(gradeLevel);
        
        log.info("Grade level deleted successfully: {}", id);
    }
    
    /**
     * 根据ID获取年级
     */
    @Transactional(readOnly = true)
    public GradeLevelResponse getGradeLevelById(Long id) {
        GradeLevel gradeLevel = gradeLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("年级不存在"));
        
        return convertToResponse(gradeLevel);
    }
    
    /**
     * 分页获取所有年级
     */
    @Transactional(readOnly = true)
    public Page<GradeLevelResponse> getAllGradeLevels(Pageable pageable) {
        Page<GradeLevel> gradeLevels = gradeLevelRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc(pageable);
        return gradeLevels.map(this::convertToResponse);
    }
    
    /**
     * 获取所有年级列表
     */
    @Transactional(readOnly = true)
    public List<GradeLevelResponse> getAllGradeLevelsList() {
        List<GradeLevel> gradeLevels = gradeLevelRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();
        return gradeLevels.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据类别获取年级列表
     */
    @Transactional(readOnly = true)
    public List<GradeLevelResponse> getGradeLevelsByCategory(String category) {
        List<GradeLevel> gradeLevels = gradeLevelRepository.findByCategoryAndIsActiveTrueOrderBySortOrderAscNameAsc(category);
        return gradeLevels.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有年级名称列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllGradeLevelNames() {
        return gradeLevelRepository.findAllActiveGradeLevelNames();
    }
    
    /**
     * 获取所有年级类别
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return gradeLevelRepository.findDistinctActiveCategories();
    }
    
    /**
     * 搜索年级
     */
    @Transactional(readOnly = true)
    public List<GradeLevelResponse> searchGradeLevels(String keyword) {
        List<GradeLevel> gradeLevels = gradeLevelRepository.findByNameContainingAndIsActiveTrue(keyword);
        return gradeLevels.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为响应对象
     */
    private GradeLevelResponse convertToResponse(GradeLevel gradeLevel) {
        GradeLevelResponse response = new GradeLevelResponse();
        response.setId(gradeLevel.getId());
        response.setName(gradeLevel.getName());
        response.setDescription(gradeLevel.getDescription());
        response.setCategory(gradeLevel.getCategory());
        response.setIsActive(gradeLevel.getIsActive());
        response.setSortOrder(gradeLevel.getSortOrder());
        response.setCreatedBy(gradeLevel.getCreatedBy());
        response.setCreatedAt(gradeLevel.getCreatedAt());
        response.setUpdatedAt(gradeLevel.getUpdatedAt());
        
        // 计算使用次数
        long knowledgeBaseCount = knowledgeBaseRepository.countByGradeLevelAndIsActiveTrue(gradeLevel.getName());
        long questionBankCount = questionBankRepository.countByGradeLevelAndIsActiveTrue(gradeLevel.getName());
        response.setUsageCount(knowledgeBaseCount + questionBankCount);
        
        return response;
    }
}

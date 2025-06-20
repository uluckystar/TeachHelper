package com.teachhelper.service.metadata;

import com.teachhelper.dto.request.SubjectRequest;
import com.teachhelper.dto.response.SubjectResponse;
import com.teachhelper.entity.Subject;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.SubjectRepository;
import com.teachhelper.repository.KnowledgeBaseRepository;
import com.teachhelper.repository.QuestionBankRepository;
import com.teachhelper.repository.SubjectGradeMappingRepository;
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
 * 学科管理服务
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class SubjectService {
    
    private final SubjectRepository subjectRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final QuestionBankRepository questionBankRepository;
    private final SubjectGradeMappingRepository subjectGradeMappingRepository;
    
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
     * 创建学科
     */
    public SubjectResponse createSubject(SubjectRequest request) {
        log.info("Creating subject: {}", request.getName());
        
        // 检查名称是否重复
        if (subjectRepository.existsByNameAndIsActiveTrue(request.getName())) {
            throw new IllegalArgumentException("学科名称已存在");
        }
        
        Subject subject = new Subject();
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setSortOrder(request.getSortOrder());
        subject.setCreatedBy(getCurrentUserId());
        subject.setIsActive(true);
        
        Subject saved = subjectRepository.save(subject);
        log.info("Subject created successfully with ID: {}", saved.getId());
        
        return convertToResponse(saved);
    }
    
    /**
     * 更新学科
     */
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        log.info("Updating subject: {}", id);
        
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("学科不存在"));
        
        // 如果名称发生变化，检查是否重复
        if (!subject.getName().equals(request.getName())) {
            if (subjectRepository.existsByNameAndIsActiveTrue(request.getName())) {
                throw new IllegalArgumentException("学科名称已存在");
            }
        }
        
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setSortOrder(request.getSortOrder());
        
        Subject saved = subjectRepository.save(subject);
        log.info("Subject updated successfully: {}", id);
        
        return convertToResponse(saved);
    }
    
    /**
     * 删除学科（软删除）
     */
    public void deleteSubject(Long id) {
        log.info("Deleting subject: {}", id);
        
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("学科不存在"));
        
        // 检查是否有关联的知识库或题目库
        long knowledgeBaseCount = knowledgeBaseRepository.countBySubjectAndIsActiveTrue(subject.getName());
        long questionBankCount = questionBankRepository.countBySubjectAndIsActiveTrue(subject.getName());
        
        if (knowledgeBaseCount > 0 || questionBankCount > 0) {
            throw new IllegalStateException("该学科下还有知识库或题目库，无法删除");
        }
        
        // 删除学科的年级关联
        try {
            subjectGradeMappingRepository.deleteBySubjectId(id);
            log.info("Deleted grade mappings for subject: {}", id);
        } catch (Exception e) {
            log.warn("Failed to delete grade mappings for subject {}: {}", id, e.getMessage());
        }
        
        subject.setIsActive(false);
        subjectRepository.save(subject);
        
        log.info("Subject deleted successfully: {}", id);
    }
    
    /**
     * 根据ID获取学科
     */
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("学科不存在"));
        
        return convertToResponse(subject);
    }
    
    /**
     * 分页获取所有学科
     */
    @Transactional(readOnly = true)
    public Page<SubjectResponse> getAllSubjects(Pageable pageable) {
        Page<Subject> subjects = subjectRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc(pageable);
        return subjects.map(this::convertToResponse);
    }
    
    /**
     * 获取所有学科列表
     */
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjectsList() {
        List<Subject> subjects = subjectRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();
        return subjects.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有学科名称列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllSubjectNames() {
        return subjectRepository.findAllActiveSubjectNames();
    }
    
    /**
     * 搜索学科
     */
    @Transactional(readOnly = true)
    public List<SubjectResponse> searchSubjects(String keyword) {
        List<Subject> subjects = subjectRepository.findByNameContainingAndIsActiveTrue(keyword);
        return subjects.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为响应对象
     */
    private SubjectResponse convertToResponse(Subject subject) {
        SubjectResponse response = new SubjectResponse();
        response.setId(subject.getId());
        response.setName(subject.getName());
        response.setDescription(subject.getDescription());
        response.setIsActive(subject.getIsActive());
        response.setSortOrder(subject.getSortOrder());
        response.setCreatedBy(subject.getCreatedBy());
        response.setCreatedAt(subject.getCreatedAt());
        response.setUpdatedAt(subject.getUpdatedAt());
        
        // 计算使用次数
        long knowledgeBaseCount = knowledgeBaseRepository.countBySubjectAndIsActiveTrue(subject.getName());
        long questionBankCount = questionBankRepository.countBySubjectAndIsActiveTrue(subject.getName());
        response.setUsageCount(knowledgeBaseCount + questionBankCount);
        
        return response;
    }
}

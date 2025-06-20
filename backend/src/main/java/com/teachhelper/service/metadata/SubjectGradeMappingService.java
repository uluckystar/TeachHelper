package com.teachhelper.service.metadata;

import com.teachhelper.dto.request.SubjectGradeMappingRequest;
import com.teachhelper.dto.response.SubjectGradeMappingResponse;
import com.teachhelper.entity.SubjectGradeMapping;
import com.teachhelper.entity.Subject;
import com.teachhelper.entity.GradeLevel;
import com.teachhelper.repository.SubjectGradeMappingRepository;
import com.teachhelper.repository.SubjectRepository;
import com.teachhelper.repository.GradeLevelRepository;
import com.teachhelper.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学科年级关联管理服务
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class SubjectGradeMappingService {
    
    private final SubjectGradeMappingRepository subjectGradeMappingRepository;
    private final SubjectRepository subjectRepository;
    private final GradeLevelRepository gradeLevelRepository;
    
    /**
     * 创建学科年级关联
     */
    public SubjectGradeMappingResponse createMapping(SubjectGradeMappingRequest request) {
        log.info("Creating subject-grade mapping: subjectId={}, gradeLevelId={}", 
            request.getSubjectId(), request.getGradeLevelId());
        
        // 验证学科和年级存在
        Subject subject = subjectRepository.findById(request.getSubjectId())
            .orElseThrow(() -> new ResourceNotFoundException("学科不存在"));
        
        GradeLevel gradeLevel = gradeLevelRepository.findById(request.getGradeLevelId())
            .orElseThrow(() -> new ResourceNotFoundException("年级不存在"));
        
        // 检查是否已存在关联
        if (subjectGradeMappingRepository.existsBySubjectIdAndGradeLevelId(
                request.getSubjectId(), request.getGradeLevelId())) {
            throw new IllegalArgumentException("学科和年级的关联已存在");
        }
        
        SubjectGradeMapping mapping = new SubjectGradeMapping();
        mapping.setSubjectId(request.getSubjectId());
        mapping.setGradeLevelId(request.getGradeLevelId());
        mapping.setPriority(request.getPriority());
        mapping.setIsRecommended(request.getIsRecommended());
        
        SubjectGradeMapping saved = subjectGradeMappingRepository.save(mapping);
        
        return convertToResponse(saved, subject, gradeLevel);
    }
    
    /**
     * 批量创建学科年级关联
     */
    public List<SubjectGradeMappingResponse> createMappings(List<SubjectGradeMappingRequest> requests) {
        log.info("Creating {} subject-grade mappings", requests.size());
        
        return requests.stream()
            .map(this::createMapping)
            .collect(Collectors.toList());
    }
    
    /**
     * 更新学科年级关联
     */
    public SubjectGradeMappingResponse updateMapping(Long id, SubjectGradeMappingRequest request) {
        log.info("Updating subject-grade mapping: id={}", id);
        
        SubjectGradeMapping mapping = subjectGradeMappingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("学科年级关联不存在"));
        
        // 如果更改了学科或年级，需要验证
        if (!mapping.getSubjectId().equals(request.getSubjectId()) || 
            !mapping.getGradeLevelId().equals(request.getGradeLevelId())) {
            
            Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("学科不存在"));
            
            GradeLevel gradeLevel = gradeLevelRepository.findById(request.getGradeLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("年级不存在"));
            
            // 检查新的关联是否已存在
            if (subjectGradeMappingRepository.existsBySubjectIdAndGradeLevelId(
                    request.getSubjectId(), request.getGradeLevelId())) {
                throw new IllegalArgumentException("学科和年级的关联已存在");
            }
            
            mapping.setSubjectId(request.getSubjectId());
            mapping.setGradeLevelId(request.getGradeLevelId());
        }
        
        mapping.setPriority(request.getPriority());
        mapping.setIsRecommended(request.getIsRecommended());
        
        SubjectGradeMapping updated = subjectGradeMappingRepository.save(mapping);
        
        // 重新获取关联的学科和年级信息
        Subject subject = subjectRepository.findById(updated.getSubjectId()).orElse(null);
        GradeLevel gradeLevel = gradeLevelRepository.findById(updated.getGradeLevelId()).orElse(null);
        
        return convertToResponse(updated, subject, gradeLevel);
    }
    
    /**
     * 删除学科年级关联
     */
    public void deleteMapping(Long id) {
        log.info("Deleting subject-grade mapping: id={}", id);
        
        if (!subjectGradeMappingRepository.existsById(id)) {
            throw new ResourceNotFoundException("学科年级关联不存在");
        }
        
        subjectGradeMappingRepository.deleteById(id);
    }
    
    /**
     * 获取学科的年级关联列表
     */
    @Transactional(readOnly = true)
    public List<SubjectGradeMappingResponse> getMappingsBySubject(Long subjectId) {
        log.info("Getting grade mappings for subject: {}", subjectId);
        
        List<SubjectGradeMapping> mappings = subjectGradeMappingRepository.findGradeLevelsBySubjectId(subjectId);
        
        return mappings.stream()
            .map(mapping -> convertToResponse(mapping, mapping.getSubject(), mapping.getGradeLevel()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取年级的学科关联列表
     */
    @Transactional(readOnly = true)
    public List<SubjectGradeMappingResponse> getMappingsByGradeLevel(Long gradeLevelId) {
        log.info("Getting subject mappings for grade level: {}", gradeLevelId);
        
        List<SubjectGradeMapping> mappings = subjectGradeMappingRepository
            .findRecommendedSubjectsByGradeLevelId(gradeLevelId);
        
        return mappings.stream()
            .map(mapping -> convertToResponse(mapping, mapping.getSubject(), mapping.getGradeLevel()))
            .collect(Collectors.toList());
    }
    
    /**
     * 为学科设置适用年级（批量更新）
     */
    public List<SubjectGradeMappingResponse> setSubjectGradeLevels(Long subjectId, 
                                                                   List<SubjectGradeMappingRequest> requests) {
        log.info("Setting grade levels for subject: {}", subjectId);
        
        // 验证学科存在
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("学科不存在"));
        
        // 删除现有关联
        subjectGradeMappingRepository.deleteBySubjectId(subjectId);
        
        // 创建新关联
        List<SubjectGradeMappingResponse> responses = requests.stream()
            .filter(request -> request.getSubjectId().equals(subjectId))
            .map(this::createMapping)
            .collect(Collectors.toList());
        
        log.info("Set {} grade levels for subject: {}", responses.size(), subjectId);
        
        return responses;
    }
    
    /**
     * 转换为响应对象
     */
    private SubjectGradeMappingResponse convertToResponse(SubjectGradeMapping mapping, 
                                                         Subject subject, 
                                                         GradeLevel gradeLevel) {
        SubjectGradeMappingResponse response = new SubjectGradeMappingResponse();
        response.setId(mapping.getId());
        response.setSubjectId(mapping.getSubjectId());
        response.setGradeLevelId(mapping.getGradeLevelId());
        response.setPriority(mapping.getPriority());
        response.setIsRecommended(mapping.getIsRecommended());
        response.setCreatedAt(mapping.getCreatedAt());
        response.setUpdatedAt(mapping.getUpdatedAt());
        
        if (subject != null) {
            response.setSubjectName(subject.getName());
        }
        
        if (gradeLevel != null) {
            response.setGradeLevelName(gradeLevel.getName());
            response.setGradeLevelCategory(gradeLevel.getCategory());
        }
        
        return response;
    }
}

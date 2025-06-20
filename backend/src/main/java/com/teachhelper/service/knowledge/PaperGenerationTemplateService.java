package com.teachhelper.service.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teachhelper.entity.PaperGenerationTemplate;
import com.teachhelper.repository.PaperGenerationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 试卷生成模板服务
 */
@Service
public class PaperGenerationTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaperGenerationTemplateService.class);
    
    @Autowired
    private PaperGenerationTemplateRepository templateRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 创建模板
     */
    @Transactional
    public PaperGenerationTemplate createTemplate(PaperGenerationTemplate template) {
        logger.info("创建试卷生成模板: {}", template.getName());
        
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        
        return templateRepository.save(template);
    }
    
    /**
     * 更新模板
     */
    @Transactional
    public PaperGenerationTemplate updateTemplate(Long id, PaperGenerationTemplate template) {
        logger.info("更新试卷生成模板: {}", id);
        
        Optional<PaperGenerationTemplate> existingOpt = templateRepository.findById(id);
        if (!existingOpt.isPresent()) {
            throw new RuntimeException("模板不存在: " + id);
        }
        
        PaperGenerationTemplate existing = existingOpt.get();
        
        // 更新基本信息
        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setSubject(template.getSubject());
        existing.setGradeLevel(template.getGradeLevel());
        existing.setTotalScore(template.getTotalScore());
        existing.setTimeLimit(template.getTimeLimit());
        
        // 更新配置信息
        existing.setQuestionConfig(template.getQuestionConfig());
        existing.setDifficultyConfig(template.getDifficultyConfig());
        existing.setKnowledgeBaseConfig(template.getKnowledgeBaseConfig());
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        return templateRepository.save(existing);
    }
    
    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        logger.info("删除试卷生成模板: {}", id);
        
        if (!templateRepository.existsById(id)) {
            throw new RuntimeException("模板不存在: " + id);
        }
        
        templateRepository.deleteById(id);
    }
    
    /**
     * 根据ID查找模板
     */
    public Optional<PaperGenerationTemplate> findTemplateById(Long id) {
        return templateRepository.findById(id);
    }
    
    /**
     * 根据创建者查找模板
     */
    public List<PaperGenerationTemplate> findTemplatesByCreator(Long createdBy) {
        return templateRepository.findByCreatedByOrderByCreatedAtDesc(createdBy);
    }
    
    /**
     * 根据科目查找模板
     */
    public List<PaperGenerationTemplate> findTemplatesBySubject(String subject) {
        return templateRepository.findBySubjectOrderByCreatedAtDesc(subject);
    }
    
    /**
     * 根据科目和年级查找模板
     */
    public List<PaperGenerationTemplate> findTemplatesBySubjectAndGrade(String subject, String gradeLevel) {
        return templateRepository.findBySubjectAndGradeLevelOrderByCreatedAtDesc(subject, gradeLevel);
    }
    
    /**
     * 根据名称模糊查找模板
     */
    public List<PaperGenerationTemplate> searchTemplatesByName(String name) {
        return templateRepository.findByNameContainingOrderByCreatedAtDesc(name);
    }
    
    /**
     * 查找公共模板
     */
    public List<PaperGenerationTemplate> findPublicTemplates() {
        return templateRepository.findPublicTemplatesOrderByCreatedAtDesc();
    }
    
    /**
     * 查找用户可访问的模板（包括自己创建的和公共模板）
     */
    public List<PaperGenerationTemplate> findAccessibleTemplates(Long userId) {
        List<PaperGenerationTemplate> userTemplates = templateRepository.findByCreatedByOrderByCreatedAtDesc(userId);
        List<PaperGenerationTemplate> publicTemplates = templateRepository.findPublicTemplatesOrderByCreatedAtDesc();
        
        // 合并结果
        userTemplates.addAll(publicTemplates);
        return userTemplates;
    }
    
    /**
     * 创建默认模板配置
     */
    public String createDefaultQuestionConfig() {
        try {
            List<PaperGenerationService.QuestionTypeConfig> defaultConfig = List.of(
                createQuestionTypeConfig("SINGLE_CHOICE", 10, 5),
                createQuestionTypeConfig("MULTIPLE_CHOICE", 5, 6),
                createQuestionTypeConfig("TRUE_FALSE", 5, 4),
                createQuestionTypeConfig("SHORT_ANSWER", 3, 10),
                createQuestionTypeConfig("ESSAY", 2, 15)
            );
            
            return objectMapper.writeValueAsString(defaultConfig);
        } catch (JsonProcessingException e) {
            logger.error("创建默认题型配置失败", e);
            return "[]";
        }
    }
    
    /**
     * 创建默认难度配置
     */
    public String createDefaultDifficultyConfig() {
        try {
            PaperGenerationService.DifficultyConfig defaultConfig = new PaperGenerationService.DifficultyConfig();
            return objectMapper.writeValueAsString(defaultConfig);
        } catch (JsonProcessingException e) {
            logger.error("创建默认难度配置失败", e);
            return "{\"easyRatio\":0.3,\"mediumRatio\":0.5,\"hardRatio\":0.2}";
        }
    }
    
    /**
     * 创建题型配置
     */
    private PaperGenerationService.QuestionTypeConfig createQuestionTypeConfig(String type, int count, int score) {
        PaperGenerationService.QuestionTypeConfig config = new PaperGenerationService.QuestionTypeConfig();
        config.setType(type);
        config.setCount(count);
        config.setScore(score);
        return config;
    }
    
    /**
     * 验证模板配置
     */
    public boolean validateTemplate(PaperGenerationTemplate template) {
        try {
            // 验证基本信息
            if (template.getName() == null || template.getName().trim().isEmpty()) {
                return false;
            }
            
            if (template.getSubject() == null || template.getSubject().trim().isEmpty()) {
                return false;
            }
            
            if (template.getTotalScore() == null || template.getTotalScore() <= 0) {
                return false;
            }
            
            // 验证题型配置
            if (template.getQuestionConfig() != null) {
                objectMapper.readTree(template.getQuestionConfig());
            }
            
            // 验证难度配置
            if (template.getDifficultyConfig() != null) {
                objectMapper.readTree(template.getDifficultyConfig());
            }
            
            // 验证知识库配置
            if (template.getKnowledgeBaseConfig() != null) {
                objectMapper.readTree(template.getKnowledgeBaseConfig());
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("验证模板配置失败", e);
            return false;
        }
    }
    
    /**
     * 复制模板
     */
    @Transactional
    public PaperGenerationTemplate duplicateTemplate(Long templateId, Long newCreatedBy) {
        Optional<PaperGenerationTemplate> originalOpt = templateRepository.findById(templateId);
        if (!originalOpt.isPresent()) {
            throw new RuntimeException("原始模板不存在: " + templateId);
        }
        
        PaperGenerationTemplate original = originalOpt.get();
        PaperGenerationTemplate copy = new PaperGenerationTemplate();
        
        copy.setName(original.getName() + " (副本)");
        copy.setDescription(original.getDescription());
        copy.setSubject(original.getSubject());
        copy.setGradeLevel(original.getGradeLevel());
        copy.setTotalScore(original.getTotalScore());
        copy.setTimeLimit(original.getTimeLimit());
        copy.setQuestionConfig(original.getQuestionConfig());
        copy.setDifficultyConfig(original.getDifficultyConfig());
        copy.setKnowledgeBaseConfig(original.getKnowledgeBaseConfig());
        copy.setCreatedBy(newCreatedBy);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        
        return templateRepository.save(copy);
    }
}

package com.teachhelper.service.question;

import com.teachhelper.dto.request.KnowledgeBaseCreateRequest;
import com.teachhelper.dto.response.KnowledgeBaseResponse;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionBank;
import com.teachhelper.entity.QuestionKnowledgePoint;
import com.teachhelper.entity.QuestionReference;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.QuestionBankRepository;
import com.teachhelper.repository.QuestionKnowledgePointRepository;
import com.teachhelper.repository.QuestionReferenceRepository;
import com.teachhelper.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 题目库管理服务
 * 提供题目库的创建、管理、题目归类等功能
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final QuestionReferenceRepository questionReferenceRepository;
    private final QuestionKnowledgePointRepository questionKnowledgePointRepository;
    
    // 新增的学科年级管理服务依赖
    private final com.teachhelper.service.metadata.SubjectService subjectService;
    private final com.teachhelper.service.metadata.GradeLevelService gradeLevelService;

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 简化处理，实际项目中应该从UserDetails中获取
            return 1L; // 临时返回固定值
        }
        throw new IllegalStateException("用户未认证");
    }

    /**
     * 创建题目库
     */
    public QuestionBank createQuestionBank(String name, String description, String subject, String gradeLevel, Boolean isPublic) {
        Long userId = getCurrentUserId();
        log.info("Creating question bank '{}' for user: {}", name, userId);

        // 检查名称是否重复
        if (questionBankRepository.existsByNameAndCreatedBy(name, userId)) {
            throw new IllegalArgumentException("题目库名称已存在");
        }

        QuestionBank questionBank = new QuestionBank();
        questionBank.setName(name);
        questionBank.setDescription(description);
        questionBank.setSubject(subject);
        questionBank.setGradeLevel(gradeLevel);
        questionBank.setCreatedBy(userId);
        questionBank.setIsPublic(isPublic != null ? isPublic : false);
        questionBank.setIsActive(true);

        QuestionBank saved = questionBankRepository.save(questionBank);
        log.info("Question bank created successfully with ID: {}", saved.getId());
        return saved;
    }

    /**
     * 获取用户的题目库列表
     */
    @Transactional(readOnly = true)
    public Page<QuestionBank> getUserQuestionBanks(Pageable pageable) {
        Long userId = getCurrentUserId();
        return questionBankRepository.findByCreatedByAndIsActiveTrueOrderByUpdatedAtDesc(userId, pageable);
    }

    /**
     * 获取可访问的题目库列表（包括公开的）
     */
    @Transactional(readOnly = true)
    public Page<QuestionBank> getAccessibleQuestionBanks(Pageable pageable) {
        Long userId = getCurrentUserId();
        return questionBankRepository.findAccessibleByUser(userId, pageable);
    }

    /**
     * 根据ID获取题目库
     */
    @Transactional(readOnly = true)
    public QuestionBank getQuestionBankById(Long id) {
        return questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("题目库不存在"));
    }

    /**
     * 更新题目库
     */
    public QuestionBank updateQuestionBank(Long id, String name, String description, String subject, String gradeLevel, Boolean isPublic) {
        Long userId = getCurrentUserId();
        QuestionBank questionBank = questionBankRepository.findByIdAndCreatedByAndIsActiveTrue(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("题目库不存在或无权限"));

        // 如果名称发生变化，检查是否重复
        if (!questionBank.getName().equals(name)) {
            if (questionBankRepository.existsByNameAndCreatedBy(name, userId)) {
                throw new IllegalArgumentException("题目库名称已存在");
            }
        }

        questionBank.setName(name);
        questionBank.setDescription(description);
        questionBank.setSubject(subject);
        questionBank.setGradeLevel(gradeLevel);
        if (isPublic != null) {
            questionBank.setIsPublic(isPublic);
        }

        return questionBankRepository.save(questionBank);
    }

    /**
     * 删除题目库（软删除）
     */
    public void deleteQuestionBank(Long id) {
        Long userId = getCurrentUserId();
        QuestionBank questionBank = questionBankRepository.findByIdAndCreatedByAndIsActiveTrue(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("题目库不存在或无权限"));

        questionBank.setIsActive(false);
        questionBankRepository.save(questionBank);
        log.info("Question bank deleted: {}", id);
    }

    /**
     * 获取题目库中的题目
     */
    @Transactional(readOnly = true)
    public Page<Question> getQuestionBankQuestions(Long questionBankId, Pageable pageable) {
        // 验证权限
        getQuestionBankById(questionBankId);
        return questionRepository.findByQuestionBankIdAndIsActiveTrueOrderByCreatedAtDesc(questionBankId, pageable);
    }

    /**
     * 将题目添加到题目库
     */
    public void addQuestionToBank(Long questionBankId, Long questionId) {
        Long userId = getCurrentUserId();
        
        QuestionBank questionBank = questionBankRepository.findByIdAndCreatedByAndIsActiveTrue(questionBankId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("题目库不存在或无权限"));
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("题目不存在"));
        
        // 检查题目创建者权限
        if (!question.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此题目");
        }
        
        question.setQuestionBank(questionBank);
        questionRepository.save(question);
        
        log.info("Question {} added to bank {}", questionId, questionBankId);
    }

    /**
     * 从题目库中移除题目
     */
    public void removeQuestionFromBank(Long questionBankId, Long questionId) {
        Long userId = getCurrentUserId();
        
        QuestionBank questionBank = questionBankRepository.findByIdAndCreatedByAndIsActiveTrue(questionBankId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("题目库不存在或无权限"));
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("题目不存在"));
        
        // 检查题目是否属于该题目库
        if (!question.getQuestionBank().getId().equals(questionBankId)) {
            throw new IllegalArgumentException("题目不属于该题目库");
        }
        
        // 检查题目创建者权限
        if (!question.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此题目");
        }
        
        question.setQuestionBank(null);
        questionRepository.save(question);
        
        log.info("Question {} removed from bank {}", questionId, questionBankId);
    }

    /**
     * 获取题目统计信息
     */
    @Transactional(readOnly = true)
    public long getQuestionCount(Long questionBankId) {
        return questionRepository.countByQuestionBankId(questionBankId);
    }

    /**
     * 获取题目被引用次数
     */
    @Transactional(readOnly = true)
    public long getQuestionReferenceCount(Long questionId) {
        return questionReferenceRepository.countByQuestionId(questionId);
    }

    /**
     * 获取所有科目列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllSubjects() {
        log.info("Getting all distinct subjects from question banks");
        
        // 优先从新的学科管理系统获取
        try {
            return subjectService.getAllSubjectNames();
        } catch (Exception e) {
            log.warn("Failed to get subjects from metadata service, falling back to question bank data", e);
            // 如果新系统失败，使用原有方式
            return questionBankRepository.findDistinctSubjects();
        }
    }

    /**
     * 获取所有年级列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllGradeLevels() {
        log.info("Getting all distinct grade levels from question banks");
        
        // 优先从新的年级管理系统获取
        try {
            return gradeLevelService.getAllGradeLevelNames();
        } catch (Exception e) {
            log.warn("Failed to get grade levels from metadata service, falling back to question bank data", e);
            // 如果新系统失败，使用原有方式
            return questionBankRepository.findDistinctGradeLevels();
        }
    }
}

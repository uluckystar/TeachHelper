package com.teachhelper.service.knowledge;

import com.teachhelper.dto.request.KnowledgeBaseCreateRequest;
import com.teachhelper.dto.response.KnowledgeBaseResponse;
import com.teachhelper.dto.response.KnowledgePointResponse;
import com.teachhelper.entity.KnowledgeBase;
import com.teachhelper.entity.KnowledgeBaseFavorite;
import com.teachhelper.entity.KnowledgePoint;
import com.teachhelper.entity.User;
import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionOption;
import com.teachhelper.entity.QuestionType;
import com.teachhelper.entity.DifficultyLevel;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.entity.QuestionKnowledgePoint;
import com.teachhelper.entity.QuestionBank;
import com.teachhelper.entity.SourceType;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.KnowledgeBaseRepository;
import com.teachhelper.repository.KnowledgeBaseFavoriteRepository;
import com.teachhelper.repository.KnowledgePointRepository;
import com.teachhelper.repository.KnowledgeDocumentRepository;
import com.teachhelper.repository.UserRepository;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.QuestionBankRepository;
import com.teachhelper.repository.QuestionKnowledgePointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 知识库服务
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeBaseFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionKnowledgePointRepository questionKnowledgePointRepository;
    
    // 新增的学科年级管理服务依赖
    private final com.teachhelper.service.metadata.SubjectService subjectService;
    private final com.teachhelper.service.metadata.GradeLevelService gradeLevelService;

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Current authentication: {}", authentication);
            
            if (authentication == null) {
                log.error("Authentication is null");
                throw new IllegalStateException("用户未认证");
            }
            
            if (!authentication.isAuthenticated()) {
                log.error("User is not authenticated");
                throw new IllegalStateException("用户未认证");
            }
            
            Object principal = authentication.getPrincipal();
            log.debug("Principal type: {}", principal.getClass().getName());
            
            // 如果principal是User对象，直接获取ID
            if (principal instanceof User user) {
                Long userId = user.getId();
                log.debug("User ID from principal: {}", userId);
                return userId;
            }
            
            // 如果principal是用户名字符串，通过用户名查找用户
            String username = authentication.getName();
            log.debug("Username from authentication: {}", username);
            
            if (!StringUtils.hasText(username)) {
                log.error("Username is empty");
                throw new IllegalStateException("用户信息无效");
            }
            
            // 通过用户名查找用户
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("用户不存在: " + username));
            
            Long userId = user.getId();
            log.debug("User ID from database lookup: {}", userId);
            return userId;
        } catch (IllegalStateException e) {
            log.error("Error getting current user ID", e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Error getting current user ID", e);
            throw new IllegalStateException("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        // 参数验证
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("知识库名称不能为空");
        }
        
        Long userId = getCurrentUserId();
        log.info("Creating knowledge base '{}' for user: {}", request.getName(), userId);
        
        try {
            // 检查名称是否重复
            if (knowledgeBaseRepository.existsByNameAndCreatedBy(request.getName(), userId)) {
                log.warn("Knowledge base name '{}' already exists for user: {}", request.getName(), userId);
                throw new IllegalArgumentException("知识库名称已存在");
            }
            
            KnowledgeBase knowledgeBase = new KnowledgeBase();
            knowledgeBase.setName(request.getName());
            knowledgeBase.setDescription(request.getDescription());
            knowledgeBase.setSubject(request.getSubject());
            knowledgeBase.setGradeLevel(request.getGradeLevel());
            knowledgeBase.setCreatedBy(userId);
            knowledgeBase.setIsActive(true);
            
            KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
            log.info("Knowledge base created successfully with ID: {}", saved.getId());
            
            return convertToResponse(saved);
        } catch (IllegalArgumentException e) {
            log.error("Error creating knowledge base for user: {}", userId, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Error creating knowledge base for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * 获取用户的知识库列表（分页）
     */
    public Page<KnowledgeBaseResponse> getUserKnowledgeBases(Pageable pageable) {
        Long userId = getCurrentUserId();
        log.info("Getting knowledge bases for user: {}, page: {}, size: {}", 
                 userId, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<KnowledgeBase> knowledgeBases = knowledgeBaseRepository
                    .findActiveByUserOrderByUpdatedDesc(userId, pageable);
            
            log.info("Found {} knowledge bases for user: {}", knowledgeBases.getTotalElements(), userId);
            return knowledgeBases.map(this::convertToResponse);
        } catch (DataAccessException e) {
            log.error("Error getting knowledge bases for user: {}", userId, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Error getting knowledge bases for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * 根据ID获取知识库
     */
    public KnowledgeBaseResponse getKnowledgeBaseById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("知识库ID不能为空");
        }
        
        Long userId = getCurrentUserId();
        log.info("Getting knowledge base: {} for user: {}", id, userId);
        
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Knowledge base not found: {}", id);
                        return new ResourceNotFoundException("知识库不存在");
                    });
            
            // 验证权限
            if (!knowledgeBase.getCreatedBy().equals(userId)) {
                log.warn("User {} attempted to access knowledge base {} owned by user {}", 
                         userId, id, knowledgeBase.getCreatedBy());
                throw new ResourceNotFoundException("知识库不存在或无访问权限");
            }
            
            if (!knowledgeBase.getIsActive()) {
                log.warn("User {} attempted to access inactive knowledge base {}", userId, id);
                throw new ResourceNotFoundException("知识库不存在或无访问权限");
            }
            
            log.debug("Successfully retrieved knowledge base: {} for user: {}", id, userId);
            return convertToResponse(knowledgeBase);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Error getting knowledge base: {} for user: {}", id, userId, e);
            throw new RuntimeException("获取知识库失败: " + e.getMessage());
        }
    }

    /**
     * 更新知识库
     */
    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(Long id, KnowledgeBaseCreateRequest request) {
        Long userId = getCurrentUserId();
        log.info("Updating knowledge base: {} for user: {}", id, userId);
        
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found"));
        
        // 验证权限
        if (!knowledgeBase.getCreatedBy().equals(userId) || !knowledgeBase.getIsActive()) {
            throw new ResourceNotFoundException("Knowledge base not found or access denied");
        }
        
        // 如果名称发生变化，检查是否重复
        if (!knowledgeBase.getName().equals(request.getName())) {
            if (knowledgeBaseRepository.existsByNameAndCreatedBy(request.getName(), userId)) {
                throw new IllegalArgumentException("知识库名称已存在");
            }
        }
        
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setSubject(request.getSubject());
        knowledgeBase.setGradeLevel(request.getGradeLevel());
        
        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        log.info("Knowledge base updated: {}", id);
        
        return convertToResponse(saved);
    }

    /**
     * 删除知识库（软删除）
     */
    @Transactional
    public void deleteKnowledgeBase(Long id) {
        Long userId = getCurrentUserId();
        log.info("Deleting knowledge base: {} for user: {}", id, userId);
        
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found"));
        
        // 验证权限
        if (!knowledgeBase.getCreatedBy().equals(userId) || !knowledgeBase.getIsActive()) {
            throw new ResourceNotFoundException("Knowledge base not found or access denied");
        }
        
        knowledgeBase.setIsActive(false);
        knowledgeBaseRepository.save(knowledgeBase);
        
        log.info("Knowledge base deleted: {}", id);
    }

    /**
     * 检查知识库是否属于用户
     */
    public boolean isKnowledgeBaseOwnedByUser(Long knowledgeBaseId, Long userId) {
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .map(kb -> kb.getCreatedBy().equals(userId) && kb.getIsActive())
                .orElse(false);
    }

    /**
     * 获取所有科目列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllSubjects() {
        log.info("Getting all distinct subjects");
        
        // 优先从新的学科管理系统获取
        try {
            return subjectService.getAllSubjectNames();
        } catch (Exception e) {
            log.warn("Failed to get subjects from metadata service, falling back to knowledge base data", e);
            // 如果新系统失败，使用原有方式
            return knowledgeBaseRepository.findDistinctSubjects();
        }
    }

    /**
     * 获取所有年级列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllGradeLevels() {
        log.info("Getting all distinct grade levels");
        
        // 优先从新的年级管理系统获取
        try {
            return gradeLevelService.getAllGradeLevelNames();
        } catch (Exception e) {
            log.warn("Failed to get grade levels from metadata service, falling back to knowledge base data", e);
            // 如果新系统失败，使用原有方式
            return knowledgeBaseRepository.findDistinctGradeLevels();
        }
    }

    /**
     * 获取知识库的知识点列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgePointResponse> getKnowledgePoints(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("知识库ID不能为空");
        }
        
        Long userId = getCurrentUserId();
        log.info("Getting knowledge points for knowledge base: {} for user: {}", knowledgeBaseId, userId);
        
        // 验证知识库权限
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在"));
        
        if (!knowledgeBase.getCreatedBy().equals(userId) || !knowledgeBase.getIsActive()) {
            throw new ResourceNotFoundException("知识库不存在或无访问权限");
        }
        
        // 获取知识点列表
        List<KnowledgePoint> knowledgePoints = knowledgePointRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId);
        
        log.info("Found {} knowledge points for knowledge base: {}", knowledgePoints.size(), knowledgeBaseId);
        
        // 暂时返回空列表，等实体类修复后再实现
        return new ArrayList<>();
    }

    /**
     * 收藏知识库
     */
    @Transactional
    public void favoriteKnowledgeBase(Long knowledgeBaseId) {
        Long userId = getCurrentUserId();
        log.info("User {} favoriting knowledge base: {}", userId, knowledgeBaseId);
        
        // 检查知识库是否存在且有权限访问
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在"));
        
        if (!knowledgeBase.getIsActive()) {
            throw new ResourceNotFoundException("知识库不存在或无访问权限");
        }
        
        // 检查是否已经收藏
        if (favoriteRepository.existsByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId)) {
            throw new IllegalStateException("已经收藏过该知识库");
        }
        
        // 创建收藏记录
        KnowledgeBaseFavorite favorite = new KnowledgeBaseFavorite();
        try {
            // 使用反射设置字段，避免 Lombok 处理器问题
            java.lang.reflect.Field userIdField = KnowledgeBaseFavorite.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
            userIdField.set(favorite, userId);
            
            java.lang.reflect.Field knowledgeBaseIdField = KnowledgeBaseFavorite.class.getDeclaredField("knowledgeBaseId");
            knowledgeBaseIdField.setAccessible(true);
            knowledgeBaseIdField.set(favorite, knowledgeBaseId);
            
            favoriteRepository.save(favorite);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to set fields on KnowledgeBaseFavorite", e);
            throw new RuntimeException("创建收藏记录失败");
        }
        
        log.info("Knowledge base {} favorited by user: {}", knowledgeBaseId, userId);
    }
    
    /**
     * 取消收藏知识库
     */
    @Transactional
    public void unfavoriteKnowledgeBase(Long knowledgeBaseId) {
        Long userId = getCurrentUserId();
        log.info("User {} unfavoriting knowledge base: {}", userId, knowledgeBaseId);
        
        // 检查收藏记录是否存在
        if (!favoriteRepository.existsByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId)) {
            throw new IllegalStateException("未收藏该知识库");
        }
        
        // 删除收藏记录
        favoriteRepository.deleteByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId);
        
        log.info("Knowledge base {} unfavorited by user: {}", knowledgeBaseId, userId);
    }
    
    /**
     * 切换收藏状态
     */
    @Transactional
    public boolean toggleFavorite(Long knowledgeBaseId) {
        Long userId = getCurrentUserId();
        boolean isFavorited = favoriteRepository.existsByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId);
        
        if (isFavorited) {
            unfavoriteKnowledgeBase(knowledgeBaseId);
            return false;
        } else {
            favoriteKnowledgeBase(knowledgeBaseId);
            return true;
        }
    }
    
    /**
     * 获取用户收藏的知识库列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> getUserFavoriteKnowledgeBases() {
        Long userId = getCurrentUserId();
        log.info("Getting favorite knowledge bases for user: {}", userId);
        
        List<Long> favoriteKbIds = favoriteRepository.findKnowledgeBaseIdsByUserId(userId);
        if (favoriteKbIds.isEmpty()) {
            return List.of();
        }
        
        List<KnowledgeBase> knowledgeBases = knowledgeBaseRepository.findAllById(favoriteKbIds);
        return knowledgeBases.stream()
                .filter(kb -> kb.getIsActive())
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 获取知识库相关的题目列表
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getKnowledgeBaseQuestions(Long knowledgeBaseId, int page, int size) {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("知识库ID不能为空");
        }
        
        Long userId = getCurrentUserId();
        log.info("Getting questions for knowledge base: {} for user: {}", knowledgeBaseId, userId);
        
        // 验证知识库权限
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在"));
        
        if (!knowledgeBase.getCreatedBy().equals(userId) || !knowledgeBase.getIsActive()) {
            throw new ResourceNotFoundException("知识库不存在或无访问权限");
        }
        
        // 方法1：通过知识点关联获取题目（新的架构）
        Pageable pageable = PageRequest.of(page, size);
        List<QuestionKnowledgePoint> associations = questionKnowledgePointRepository.findByKnowledgeBaseId(knowledgeBaseId);
        
        if (!associations.isEmpty()) {
            // 从关联表中获取题目ID列表
            List<Long> questionIds = associations.stream()
                    .map(assoc -> assoc.getQuestion().getId())
                    .distinct()
                    .toList();
            
            // 分页获取题目
            int start = page * size;
            int end = Math.min(start + size, questionIds.size());
            List<Long> pagedQuestionIds = questionIds.subList(start, end);
            
            List<Question> questions = questionRepository.findAllById(pagedQuestionIds);
            List<Map<String, Object>> questionMaps = questions.stream()
                    .map(question -> convertQuestionToMap(question, associations))
                    .toList();
            
            Page<Map<String, Object>> result = new PageImpl<>(questionMaps, pageable, questionIds.size());
            log.info("Found {} questions via knowledge point associations for knowledge base: {}", result.getTotalElements(), knowledgeBaseId);
            return result;
        }
        
        // 方法2：通过原有的直接关联获取题目（兼容旧数据）
        Page<Question> questionsPage = questionRepository.findBySourceKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId, pageable);
        
        if (questionsPage.hasContent()) {
            List<Map<String, Object>> questionMaps = questionsPage.getContent().stream()
                    .map(question -> convertQuestionToMap(question, null))
                    .toList();
            
            log.info("Found {} questions via legacy source relation for knowledge base: {}", questionsPage.getTotalElements(), knowledgeBaseId);
            return new PageImpl<>(questionMaps, pageable, questionsPage.getTotalElements());
        }
        
        // 如果都没有数据，返回空页面
        log.info("No questions found for knowledge base: {}", knowledgeBaseId);
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
    
    
    /**
     * 将题目转换为前端需要的Map格式
     */
    private Map<String, Object> convertQuestionToMap(Question question) {
        return convertQuestionToMap(question, null);
    }
    
    /**
     * 将题目转换为前端需要的Map格式（支持关联信息）
     */
    private Map<String, Object> convertQuestionToMap(Question question, List<QuestionKnowledgePoint> associations) {
        Map<String, Object> questionMap = new HashMap<>();
        
        questionMap.put("id", question.getId());
        questionMap.put("questionText", question.getContent());
        questionMap.put("content", question.getContent());
        questionMap.put("type", getQuestionTypeFrontendFormat(question.getQuestionType()));
        questionMap.put("difficulty", getDifficultyFrontendFormat(question.getDifficulty()));
        questionMap.put("isAIGenerated", question.getSourceType() != null && 
                       (question.getSourceType() == SourceType.AI_GENERATED || question.getSourceType() == SourceType.AI_ORGANIZED));
        questionMap.put("createdAt", question.getCreatedAt() != null ? question.getCreatedAt().toString() : null);
        questionMap.put("createdBy", question.getSourceType() != null && 
                       (question.getSourceType() == SourceType.AI_GENERATED || question.getSourceType() == SourceType.AI_ORGANIZED) ? "AI生成" : "教师");
        
        // 获取练习统计 - 暂时使用固定值
        questionMap.put("practiceCount", 0);
        questionMap.put("correctRate", 0.85);
        
        // 处理选择题选项
        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
            question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            List<Map<String, Object>> options = new ArrayList<>();
            if (question.getOptions() != null) {
                for (QuestionOption option : question.getOptions()) {
                    Map<String, Object> optionMap = new HashMap<>();
                    optionMap.put("text", option.getContent());
                    optionMap.put("isCorrect", option.getIsCorrect());
                    options.add(optionMap);
                }
            }
            questionMap.put("options", options);
        }
        
        // 处理填空题和主观题答案
        if (question.getQuestionType() == QuestionType.FILL_BLANK ||
            question.getQuestionType() == QuestionType.SHORT_ANSWER ||
            question.getQuestionType() == QuestionType.ESSAY) {
            questionMap.put("correctAnswer", question.getReferenceAnswer());
        }
        
        // 处理主观题评分标准
        if (question.getQuestionType() == QuestionType.SHORT_ANSWER ||
            question.getQuestionType() == QuestionType.ESSAY) {
            List<Map<String, Object>> criteria = new ArrayList<>();
            
            // 从实际的评分标准获取数据
            if (question.getRubricCriteria() != null && !question.getRubricCriteria().isEmpty()) {
                for (RubricCriterion criterion : question.getRubricCriteria()) {
                    Map<String, Object> criterionMap = new HashMap<>();
                    criterionMap.put("score", criterion.getPoints() != null ? criterion.getPoints().intValue() : 5);
                    criterionMap.put("description", criterion.getCriterionText());
                    criteria.add(criterionMap);
                }
            } else {
                // 如果没有评分标准，提供默认的
                Map<String, Object> criterion1 = new HashMap<>();
                criterion1.put("score", 5);
                criterion1.put("description", "基本概念理解正确");
                criteria.add(criterion1);
                
                Map<String, Object> criterion2 = new HashMap<>();
                criterion2.put("score", 3);
                criterion2.put("description", "分析过程清晰");
                criteria.add(criterion2);
            }
            questionMap.put("scoringCriteria", criteria);
        }
        
        // 添加关联的知识点信息
        List<Map<String, Object>> knowledgePoints = new ArrayList<>();
        
        if (associations != null) {
            // 使用新的关联关系
            List<QuestionKnowledgePoint> questionAssociations = associations.stream()
                    .filter(assoc -> assoc.getQuestion().getId().equals(question.getId()))
                    .toList();
            
            for (QuestionKnowledgePoint assoc : questionAssociations) {
                Map<String, Object> kpMap = new HashMap<>();
                kpMap.put("id", assoc.getKnowledgePoint().getId());
                kpMap.put("title", assoc.getKnowledgePoint().getTitle());
                kpMap.put("relevanceScore", assoc.getRelevanceScore());
                kpMap.put("isPrimary", assoc.getIsPrimary());
                knowledgePoints.add(kpMap);
            }
        } else {
            // 兼容旧的直接关联方式
            if (question.getSourceKnowledgePointId() != null) {
                Map<String, Object> kpMap = new HashMap<>();
                kpMap.put("id", question.getSourceKnowledgePointId());
                kpMap.put("title", "知识点" + question.getSourceKnowledgePointId());
                kpMap.put("relevanceScore", 1.0);
                kpMap.put("isPrimary", true);
                knowledgePoints.add(kpMap);
            }
        }
        questionMap.put("knowledgePoints", knowledgePoints);
        
        // 添加题目库信息
        if (question.getQuestionBank() != null) {
            Map<String, Object> bankInfo = new HashMap<>();
            bankInfo.put("id", question.getQuestionBank().getId());
            bankInfo.put("name", question.getQuestionBank().getName());
            questionMap.put("questionBank", bankInfo);
        }
        
        return questionMap;
    }
    
    /**
     * 获取前端格式的题目类型
     */
    private String getQuestionTypeFrontendFormat(QuestionType questionType) {
        if (questionType == null) return "choice";
        
        return switch (questionType) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> "choice";
            case TRUE_FALSE -> "choice";
            case FILL_BLANK -> "blank";
            case SHORT_ANSWER, ESSAY -> "subjective";
            default -> "choice";
        };
    }
    
    /**
     * 获取前端格式的难度级别
     */
    private String getDifficultyFrontendFormat(DifficultyLevel difficulty) {
        if (difficulty == null) return "medium";
        
        return switch (difficulty) {
            case EASY -> "easy";
            case MEDIUM -> "medium";
            case HARD -> "hard";
            default -> "medium";
        };
    }

    /**
     * 公开的转换方法，供Controller使用
     */
    public KnowledgeBaseResponse convertToResponse(KnowledgeBase knowledgeBase) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        response.setId(knowledgeBase.getId());
        response.setName(knowledgeBase.getName());
        response.setDescription(knowledgeBase.getDescription());
        response.setSubject(knowledgeBase.getSubject());
        response.setGradeLevel(knowledgeBase.getGradeLevel());
        response.setCreatedBy(knowledgeBase.getCreatedBy());
        response.setIsActive(knowledgeBase.getIsActive());
        response.setCreatedAt(knowledgeBase.getCreatedAt());
        response.setUpdatedAt(knowledgeBase.getUpdatedAt());
        
        // 设置扩展字段
        Long userId = getCurrentUserId();
        response.setIsFavorited(favoriteRepository.existsByUserIdAndKnowledgeBaseId(userId, knowledgeBase.getId()));
        response.setFavoriteCount(favoriteRepository.countByKnowledgeBaseId(knowledgeBase.getId()));
        
        // 添加文档数量和知识点数量统计
        response.setDocumentCount(knowledgeDocumentRepository.countByKnowledgeBaseId(knowledgeBase.getId()));
        response.setKnowledgePointCount(knowledgePointRepository.countByKnowledgeBaseId(knowledgeBase.getId()));
        
        return response;
    }

    /**
     * 转换为知识点响应对象
     */
    /*
    private KnowledgePointResponse convertToKnowledgePointResponse(KnowledgePoint knowledgePoint) {
        KnowledgePointResponse response = new KnowledgePointResponse();
        response.setId(knowledgePoint.getId());
        response.setKnowledgeBaseId(knowledgePoint.getKnowledgeBaseId());
        response.setTitle(knowledgePoint.getTitle());
        response.setContent(knowledgePoint.getContent());
        response.setKeywords(knowledgePoint.getKeywords());
        response.setDifficultyLevel(knowledgePoint.getDifficultyLevel() != null ? 
                                  knowledgePoint.getDifficultyLevel().name() : null);
        response.setSourceDocumentId(knowledgePoint.getSourceDocumentId());
        response.setSourcePageNumber(knowledgePoint.getSourcePageNumber());
        response.setVectorId(knowledgePoint.getVectorId());
        response.setCreatedAt(knowledgePoint.getCreatedAt());
        response.setUpdatedAt(knowledgePoint.getUpdatedAt());
        response.setSummary(knowledgePoint.getSummary());
        response.setIsAIGenerated(knowledgePoint.getSourceDocumentId() != null);
        
        // 设置前端期望的格式
        response.setDifficulty(knowledgePoint.getDifficultyLevel() != null ? 
                              knowledgePoint.getDifficultyLevel().name() : "MEDIUM");
        
        // 解析keywords为数组（如果是JSON格式）
        if (knowledgePoint.getKeywords() != null && !knowledgePoint.getKeywords().isEmpty()) {
            try {
                // 简单的逗号分割，后续可以改为JSON解析
                String[] tags = knowledgePoint.getKeywords().split(",");
                for (int i = 0; i < tags.length; i++) {
                    tags[i] = tags[i].trim();
                }
                response.setTags(tags);
            } catch (Exception e) {
                log.debug("Failed to parse keywords as array: {}", knowledgePoint.getKeywords());
                response.setTags(new String[]{knowledgePoint.getKeywords()});
            }
        } else {
            response.setTags(new String[0]);
        }
        
        // TODO: 添加统计信息（相关文档数量、相关题目数量等）
        response.setRelatedDocumentsCount(0);
        response.setRelatedQuestionsCount(0);
        
        return response;
    }
    */

    /**
     * 搜索知识库（支持按学科、年级、名称筛选和排序）
     */
    public Page<KnowledgeBaseResponse> searchKnowledgeBases(Pageable pageable, String subject, String gradeLevel, String name, String sort, String direction) {
        Long userId = getCurrentUserId();
        log.info("Searching knowledge bases for user: {}, page: {}, size: {}, subject: {}, gradeLevel: {}, name: {}, sort: {}, direction: {}", 
                 userId, pageable.getPageNumber(), pageable.getPageSize(), subject, gradeLevel, name, sort, direction);
        
        // 处理空字符串参数，将其转换为null以便SQL查询正确处理
        String normalizedSubject = (subject != null && subject.trim().isEmpty()) ? null : subject;
        String normalizedGradeLevel = (gradeLevel != null && gradeLevel.trim().isEmpty()) ? null : gradeLevel;
        String normalizedName = (name != null && name.trim().isEmpty()) ? null : name;
        
        // 创建带排序的Pageable
        Pageable sortedPageable = createSortedPageable(pageable, sort, direction);
        
        log.debug("Normalized search parameters - subject: {}, gradeLevel: {}, name: {}, sort: {}, direction: {}", 
                 normalizedSubject, normalizedGradeLevel, normalizedName, sort, direction);
        
        try {
            Page<KnowledgeBase> knowledgeBases;
            
            // 根据排序字段选择不同的查询方法
            if ("documentCount".equals(sort)) {
                // 对于复杂排序，使用无排序的 Pageable，因为排序在查询中处理
                Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                Page<Object[]> results = knowledgeBaseRepository
                        .findByConditionsOrderByDocumentCount(userId, normalizedSubject, normalizedGradeLevel, normalizedName, direction, unsortedPageable);
                // 提取KnowledgeBase对象并转换
                List<KnowledgeBase> kbList = results.getContent().stream()
                        .map(arr -> (KnowledgeBase) arr[0])
                        .collect(Collectors.toList());
                knowledgeBases = new PageImpl<>(kbList, unsortedPageable, results.getTotalElements());
            } else if ("knowledgePointCount".equals(sort)) {
                // 对于复杂排序，使用无排序的 Pageable，因为排序在查询中处理
                Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                Page<Object[]> results = knowledgeBaseRepository
                        .findByConditionsOrderByKnowledgePointCount(userId, normalizedSubject, normalizedGradeLevel, normalizedName, direction, unsortedPageable);
                // 提取KnowledgeBase对象并转换
                List<KnowledgeBase> kbList = results.getContent().stream()
                        .map(arr -> (KnowledgeBase) arr[0])
                        .collect(Collectors.toList());
                knowledgeBases = new PageImpl<>(kbList, unsortedPageable, results.getTotalElements());
            } else {
                // 使用常规查询（name, createdAt, updatedAt）
                knowledgeBases = knowledgeBaseRepository
                        .findByConditions(userId, normalizedSubject, normalizedGradeLevel, normalizedName, sortedPageable);
            }
            
            log.info("Found {} knowledge bases for user: {} with search criteria", knowledgeBases.getTotalElements(), userId);
            return knowledgeBases.map(this::convertToResponse);
        } catch (DataAccessException e) {
            log.error("Error searching knowledge bases for user: {}", userId, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Error searching knowledge bases for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * 搜索知识库（向后兼容的重载方法）
     */
    public Page<KnowledgeBaseResponse> searchKnowledgeBases(Pageable pageable, String subject, String gradeLevel, String name) {
        return searchKnowledgeBases(pageable, subject, gradeLevel, name, null, null);
    }

    /**
     * 创建带排序的Pageable对象
     */
    private Pageable createSortedPageable(Pageable originalPageable, String sort, String direction) {
        if (sort == null || sort.trim().isEmpty()) {
            sort = "updatedAt"; // 默认按更新时间排序
        }
        if (direction == null || direction.trim().isEmpty()) {
            direction = "desc"; // 默认降序
        }
        
        // 映射前端排序字段到数据库字段
        String dbSortField = mapSortField(sort);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        Sort sortObj = Sort.by(sortDirection, dbSortField);
        return PageRequest.of(originalPageable.getPageNumber(), originalPageable.getPageSize(), sortObj);
    }

    /**
     * 映射前端排序字段到数据库字段
     */
    private String mapSortField(String frontendField) {
        switch (frontendField) {
            case "name":
                return "name";
            case "createdAt":
                return "createdAt";
            case "updatedAt":
                return "updatedAt";
            case "documentCount":
                return "documentCount"; // 将通过特殊查询处理
            case "knowledgePointCount":
                return "knowledgePointCount"; // 将通过特殊查询处理
            default:
                return "updatedAt";
        }
    }
    
    /**
     * 根据ID列表批量查询知识库
     */
    @Transactional(readOnly = true)
    public List<KnowledgeBase> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        Long userId = getCurrentUserId();
        List<KnowledgeBase> allKnowledgeBases = knowledgeBaseRepository.findAllById(ids);
        
        // 只返回用户有权访问的活跃知识库
        return allKnowledgeBases.stream()
                .filter(kb -> kb.getIsActive() && kb.getCreatedBy().equals(userId))
                .collect(Collectors.toList());
    }
}

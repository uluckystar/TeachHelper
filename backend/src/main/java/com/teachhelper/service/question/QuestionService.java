package com.teachhelper.service.question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teachhelper.entity.Question;
import com.teachhelper.entity.QuestionOption;
import com.teachhelper.entity.RubricCriterion;
import com.teachhelper.exception.ResourceNotFoundException;
import com.teachhelper.repository.QuestionOptionRepository;
import com.teachhelper.repository.QuestionRepository;
import com.teachhelper.repository.RubricCriterionRepository;

@Service
@Transactional
public class QuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionOptionRepository questionOptionRepository;
    
    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;
    
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }
    
    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        
        question.setTitle(questionDetails.getTitle());
        question.setContent(questionDetails.getContent());
        question.setQuestionType(questionDetails.getQuestionType());
        question.setMaxScore(questionDetails.getMaxScore());
        question.setReferenceAnswer(questionDetails.getReferenceAnswer());
        
        // Update options if provided
        if (questionDetails.getOptions() != null) {
            // Clear existing options
            question.clearOptions();
            
            // Add new options
            for (QuestionOption option : questionDetails.getOptions()) {
                question.addOption(option);
            }
        }
        
        question.setUpdatedAt(LocalDateTime.now());
        return questionRepository.save(question);
    }
    
    public Question save(Question question) {
        return questionRepository.save(question);
    }
    
    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        questionRepository.delete(question);
    }
    
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        // 获取题目基本信息
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        
        // 在事务内手动触发所有懒加载，确保数据完整性
        try {
            // 手动触发所有懒加载，避免 no Session 错误
            if (question.getOptions() != null) {
                question.getOptions().size();
            }
            if (question.getRubricCriteria() != null) {
                question.getRubricCriteria().size();
            }
            if (question.getExam() != null) {
                question.getExam().getTitle();
            }
            if (question.getQuestionBank() != null) {
                question.getQuestionBank().getName();
            }
        } catch (Exception e) {
            // 如果懒加载失败，记录错误但不中断处理
            System.err.println("Failed to load question relationships for ID " + question.getId() + ": " + e.getMessage());
        }
        
        return question;
    }
    
    @Transactional(readOnly = true)
    public Page<Question> getAllQuestions(Pageable pageable) {
        Page<Question> questionPage = questionRepository.findAll(pageable);
        
        // 手动触发懒加载，确保在事务内完成加载
        for (Question question : questionPage.getContent()) {
            if (question.getOptions() != null) {
                question.getOptions().size(); // 触发懒加载
            }
            if (question.getRubricCriteria() != null) {
                question.getRubricCriteria().size(); // 触发懒加载
            }
        }
        
        return questionPage;
    }
    
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByExamId(Long examId) {
        // 使用EntityGraph预加载所有关联对象的查询
        List<Question> questions = questionRepository.findByExamId(examId);
        
        // 在事务内手动触发所有懒加载，确保数据完整性
            for (Question question : questions) {
            try {
                // 手动触发所有懒加载，避免 no Session 错误
                if (question.getOptions() != null) {
                    question.getOptions().size();
                }
                if (question.getRubricCriteria() != null) {
                    question.getRubricCriteria().size();
                }
                if (question.getExam() != null) {
                    question.getExam().getTitle();
                }
                if (question.getQuestionBank() != null) {
                    question.getQuestionBank().getName();
                }
            } catch (Exception e) {
                // 如果懒加载失败，记录错误但不中断处理
                System.err.println("Failed to load question relationships for ID " + question.getId() + ": " + e.getMessage());
            }
        }
        
        return questions;
    }
    
    // Rubric Criteria Management
    public RubricCriterion addRubricCriterion(Long questionId, RubricCriterion criterion) {
        Question question = getQuestionById(questionId);
        criterion.setQuestion(question);
        return rubricCriterionRepository.save(criterion);
    }
    
    public RubricCriterion updateRubricCriterion(Long criterionId, RubricCriterion criterionDetails) {
        RubricCriterion criterion = rubricCriterionRepository.findById(criterionId)
            .orElseThrow(() -> new ResourceNotFoundException("Rubric criterion not found with id: " + criterionId));
        
        criterion.setCriterionText(criterionDetails.getCriterionText());
        criterion.setPoints(criterionDetails.getPoints());
        
        return rubricCriterionRepository.save(criterion);
    }
    
    public RubricCriterion saveRubricCriterion(RubricCriterion criterion) {
        return rubricCriterionRepository.save(criterion);
    }
    
    public void deleteRubricCriterion(Long criterionId) {
        RubricCriterion criterion = rubricCriterionRepository.findById(criterionId)
            .orElseThrow(() -> new ResourceNotFoundException("Rubric criterion not found with id: " + criterionId));
        rubricCriterionRepository.delete(criterion);
    }
    
    @Transactional(readOnly = true)
    public List<RubricCriterion> getRubricCriteriaByQuestionId(Long questionId) {
        return rubricCriterionRepository.findByQuestionId(questionId);
    }
    
    // Batch operations
    public List<Question> createQuestionsInBatch(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }
    
    @Transactional(readOnly = true)
    public long getQuestionCountByExamId(Long examId) {
        return questionRepository.countByExamId(examId);
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return questionRepository.existsById(id);
    }
    
    // Question Options Management
    public QuestionOption addQuestionOption(Long questionId, QuestionOption option) {
        Question question = getQuestionById(questionId);
        option.setQuestion(question);
        return questionOptionRepository.save(option);
    }
    
    public QuestionOption updateQuestionOption(Long optionId, QuestionOption optionDetails) {
        QuestionOption option = questionOptionRepository.findById(optionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question option not found with id: " + optionId));
        
        option.setContent(optionDetails.getContent());
        option.setIsCorrect(optionDetails.getIsCorrect());
        option.setOptionOrder(optionDetails.getOptionOrder());
        
        return questionOptionRepository.save(option);
    }
    
    public void deleteQuestionOption(Long optionId) {
        QuestionOption option = questionOptionRepository.findById(optionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question option not found with id: " + optionId));
        questionOptionRepository.delete(option);
    }
    
    @Transactional(readOnly = true)
    public List<QuestionOption> getQuestionOptionsByQuestionId(Long questionId) {
        return questionOptionRepository.findByQuestionIdOrderByOptionOrder(questionId);
    }
    
    @Transactional(readOnly = true)
    public Page<Question> getQuestionsWithSourceFilter(Pageable pageable, String sourceType) {
        // 如果没有指定来源类型，返回所有题目
        if (sourceType == null || sourceType.trim().isEmpty()) {
            Page<Question> questionPage = questionRepository.findAll(pageable);
            
            // 手动触发懒加载，确保在事务内完成加载
            for (Question question : questionPage.getContent()) {
                if (question.getOptions() != null) {
                    question.getOptions().size(); // 触发懒加载
                }
                if (question.getRubricCriteria() != null) {
                    question.getRubricCriteria().size(); // 触发懒加载
                }
                if (question.getExam() != null) {
                    question.getExam().getTitle(); // 触发exam懒加载
                }
            }
            
            return questionPage;
        }
        
        // 根据来源类型筛选
        Page<Question> questionPage = questionRepository.findAll(pageable);
        
        // 手动触发懒加载，确保在事务内完成加载
        for (Question question : questionPage.getContent()) {
            if (question.getOptions() != null) {
                question.getOptions().size(); // 触发懒加载
            }
            if (question.getRubricCriteria() != null) {
                question.getRubricCriteria().size(); // 触发懒加载
            }
            if (question.getExam() != null) {
                question.getExam().getTitle(); // 触发exam懒加载
            }
        }
        
        return questionPage;
    }

    // 新增方法：支持多条件搜索的题目查询
    @Transactional(readOnly = true)
    public Page<Question> searchQuestionsWithFilters(Pageable pageable, Map<String, Object> filters) {
        // 提取筛选参数
        String keyword = (String) filters.get("keyword");
        String questionType = (String) filters.get("questionType");
        String sourceTypeStr = (String) filters.get("sourceType");
        Long examId = (Long) filters.get("examId");
        String startDateStr = (String) filters.get("startDate");
        String endDateStr = (String) filters.get("endDate");
        
        // 转换来源类型字符串为枚举
        com.teachhelper.entity.SourceType sourceType = null;
        if (sourceTypeStr != null && !sourceTypeStr.trim().isEmpty()) {
            sourceType = com.teachhelper.entity.SourceType.fromString(sourceTypeStr);
        }
        
        // 转换日期字符串为LocalDate
        java.time.LocalDate startDate = null;
        java.time.LocalDate endDate = null;
        
        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            try {
                startDate = java.time.LocalDate.parse(startDateStr);
            } catch (Exception e) {
                // 如果日期解析失败，忽略该筛选条件
                System.err.println("Failed to parse startDate: " + startDateStr);
            }
        }
        
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                endDate = java.time.LocalDate.parse(endDateStr);
            } catch (Exception e) {
                // 如果日期解析失败，忽略该筛选条件
                System.err.println("Failed to parse endDate: " + endDateStr);
            }
        }
        
        // 使用支持多条件筛选的查询方法
        Page<Question> questionPage = questionRepository.searchQuestionsWithFilters(
            keyword, questionType, sourceType, examId, startDate, endDate, pageable
        );
        
        // 在事务内手动触发懒加载，确保所有数据都在事务内完成加载
        for (Question question : questionPage.getContent()) {
            try {
                // 手动触发所有懒加载，避免 no Session 错误
                if (question.getOptions() != null) {
                    question.getOptions().size();
                }
                if (question.getRubricCriteria() != null) {
                    question.getRubricCriteria().size();
                }
                if (question.getExam() != null) {
                    question.getExam().getTitle();
                }
                if (question.getQuestionBank() != null) {
                    question.getQuestionBank().getName();
                }
            } catch (Exception e) {
                // 如果懒加载失败，记录错误但不中断处理
                System.err.println("Failed to load question relationships for ID " + question.getId() + ": " + e.getMessage());
            }
        }
        
        return questionPage;
    }
}

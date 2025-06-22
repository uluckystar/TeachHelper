package com.teachhelper.service.question;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
        
        return questionRepository.save(question);
    }
    
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        questionRepository.delete(question);
    }
    
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        // 首先获取题目基本信息和选项
        Question question = questionRepository.findByIdWithOptions(id)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        
        // 然后获取评分标准（通过单独查询避免 MultipleBagFetchException）
        Optional<Question> questionWithCriteria = questionRepository.findByIdWithCriteria(id);
        if (questionWithCriteria.isPresent()) {
            question.setRubricCriteria(questionWithCriteria.get().getRubricCriteria());
        }
        
        return question;
    }
    
    @Transactional(readOnly = true)
    public Page<Question> getAllQuestions(Pageable pageable) {
        return questionRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByExamId(Long examId) {
        // 首先获取题目基本信息和选项
        List<Question> questions = questionRepository.findByExamIdWithOptions(examId);
        
        // 然后为每个题目获取评分标准
        if (!questions.isEmpty()) {
            List<Question> questionsWithCriteria = questionRepository.findByExamIdWithCriteria(examId);
            
            // 将评分标准合并到对应的题目中
            for (Question question : questions) {
                for (Question questionWithCriteria : questionsWithCriteria) {
                    if (question.getId().equals(questionWithCriteria.getId())) {
                        question.setRubricCriteria(questionWithCriteria.getRubricCriteria());
                        break;
                    }
                }
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
}

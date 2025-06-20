package com.teachhelper.dto.response;

import com.teachhelper.entity.QuestionType;
import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷生成响应DTO
 */
public class PaperGenerationResponse {
    
    private Long examId;
    private String examTitle;
    private String examDescription;
    private String subject;
    private String gradeLevel;
    private Integer totalScore;
    private Integer timeLimit;
    private List<GeneratedQuestionResponse> questions;
    private List<String> warnings;
    private String generationSummary;
    private GenerationStatistics statistics;
    
    // Constructors
    public PaperGenerationResponse() {}
    
    // Getters and Setters
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    
    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
    
    public String getExamDescription() { return examDescription; }
    public void setExamDescription(String examDescription) { this.examDescription = examDescription; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
    
    public List<GeneratedQuestionResponse> getQuestions() { return questions; }
    public void setQuestions(List<GeneratedQuestionResponse> questions) { this.questions = questions; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public String getGenerationSummary() { return generationSummary; }
    public void setGenerationSummary(String generationSummary) { this.generationSummary = generationSummary; }
    
    public GenerationStatistics getStatistics() { return statistics; }
    public void setStatistics(GenerationStatistics statistics) { this.statistics = statistics; }
    
    /**
     * 生成的题目响应
     */
    public static class GeneratedQuestionResponse {
        private Long questionId;
        private String title;
        private String content;
        private QuestionType type;
        private BigDecimal maxScore;
        private String referenceAnswer;
        private List<QuestionOptionResponse> options;
        private String difficulty;
        private String knowledgePoint;
        private String sourceDocument;
        
        // Constructors
        public GeneratedQuestionResponse() {}
        
        // Getters and Setters
        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public QuestionType getType() { return type; }
        public void setType(QuestionType type) { this.type = type; }
        
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        
        public String getReferenceAnswer() { return referenceAnswer; }
        public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
        
        public List<QuestionOptionResponse> getOptions() { return options; }
        public void setOptions(List<QuestionOptionResponse> options) { this.options = options; }
        
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        
        public String getKnowledgePoint() { return knowledgePoint; }
        public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
        
        public String getSourceDocument() { return sourceDocument; }
        public void setSourceDocument(String sourceDocument) { this.sourceDocument = sourceDocument; }
    }
    
    /**
     * 题目选项响应
     */
    public static class QuestionOptionResponse {
        private Long optionId;
        private String content;
        private Boolean isCorrect;
        private Integer optionOrder;
        
        // Constructors
        public QuestionOptionResponse() {}
        
        public QuestionOptionResponse(String content, Boolean isCorrect) {
            this.content = content;
            this.isCorrect = isCorrect;
        }
        
        // Getters and Setters
        public Long getOptionId() { return optionId; }
        public void setOptionId(Long optionId) { this.optionId = optionId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Boolean getIsCorrect() { return isCorrect; }
        public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
        
        public Integer getOptionOrder() { return optionOrder; }
        public void setOptionOrder(Integer optionOrder) { this.optionOrder = optionOrder; }
    }
    
    /**
     * 生成统计信息
     */
    public static class GenerationStatistics {
        private Integer totalQuestions;
        private TypeStatistics typeStatistics;
        private DifficultyStatistics difficultyStatistics;
        private ScoreStatistics scoreStatistics;
        
        // Constructors
        public GenerationStatistics() {}
        
        // Getters and Setters
        public Integer getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
        
        public TypeStatistics getTypeStatistics() { return typeStatistics; }
        public void setTypeStatistics(TypeStatistics typeStatistics) { this.typeStatistics = typeStatistics; }
        
        public DifficultyStatistics getDifficultyStatistics() { return difficultyStatistics; }
        public void setDifficultyStatistics(DifficultyStatistics difficultyStatistics) { this.difficultyStatistics = difficultyStatistics; }
        
        public ScoreStatistics getScoreStatistics() { return scoreStatistics; }
        public void setScoreStatistics(ScoreStatistics scoreStatistics) { this.scoreStatistics = scoreStatistics; }
        
        /**
         * 题型统计
         */
        public static class TypeStatistics {
            private Integer singleChoice;
            private Integer multipleChoice;
            private Integer trueFalse;
            private Integer shortAnswer;
            private Integer essay;
            private Integer coding;
            private Integer caseAnalysis;
            private Integer calculation;
            
            // Constructors
            public TypeStatistics() {}
            
            // Getters and Setters
            public Integer getSingleChoice() { return singleChoice; }
            public void setSingleChoice(Integer singleChoice) { this.singleChoice = singleChoice; }
            
            public Integer getMultipleChoice() { return multipleChoice; }
            public void setMultipleChoice(Integer multipleChoice) { this.multipleChoice = multipleChoice; }
            
            public Integer getTrueFalse() { return trueFalse; }
            public void setTrueFalse(Integer trueFalse) { this.trueFalse = trueFalse; }
            
            public Integer getShortAnswer() { return shortAnswer; }
            public void setShortAnswer(Integer shortAnswer) { this.shortAnswer = shortAnswer; }
            
            public Integer getEssay() { return essay; }
            public void setEssay(Integer essay) { this.essay = essay; }
            
            public Integer getCoding() { return coding; }
            public void setCoding(Integer coding) { this.coding = coding; }
            
            public Integer getCaseAnalysis() { return caseAnalysis; }
            public void setCaseAnalysis(Integer caseAnalysis) { this.caseAnalysis = caseAnalysis; }
            
            public Integer getCalculation() { return calculation; }
            public void setCalculation(Integer calculation) { this.calculation = calculation; }
        }
        
        /**
         * 难度统计
         */
        public static class DifficultyStatistics {
            private Integer easy;
            private Integer medium;
            private Integer hard;
            
            // Constructors
            public DifficultyStatistics() {}
            
            // Getters and Setters
            public Integer getEasy() { return easy; }
            public void setEasy(Integer easy) { this.easy = easy; }
            
            public Integer getMedium() { return medium; }
            public void setMedium(Integer medium) { this.medium = medium; }
            
            public Integer getHard() { return hard; }
            public void setHard(Integer hard) { this.hard = hard; }
        }
        
        /**
         * 分值统计
         */
        public static class ScoreStatistics {
            private BigDecimal totalScore;
            private BigDecimal averageScorePerQuestion;
            private BigDecimal minScorePerQuestion;
            private BigDecimal maxScorePerQuestion;
            
            // Constructors
            public ScoreStatistics() {}
            
            // Getters and Setters
            public BigDecimal getTotalScore() { return totalScore; }
            public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
            
            public BigDecimal getAverageScorePerQuestion() { return averageScorePerQuestion; }
            public void setAverageScorePerQuestion(BigDecimal averageScorePerQuestion) { this.averageScorePerQuestion = averageScorePerQuestion; }
            
            public BigDecimal getMinScorePerQuestion() { return minScorePerQuestion; }
            public void setMinScorePerQuestion(BigDecimal minScorePerQuestion) { this.minScorePerQuestion = minScorePerQuestion; }
            
            public BigDecimal getMaxScorePerQuestion() { return maxScorePerQuestion; }
            public void setMaxScorePerQuestion(BigDecimal maxScorePerQuestion) { this.maxScorePerQuestion = maxScorePerQuestion; }
        }
    }
}

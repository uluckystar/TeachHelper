package com.teachhelper.dto.response;

import java.util.Objects;

import java.time.LocalDateTime;
import java.util.List;

public class ExamTemplateResponse {
    
    private Long id;                      // 模板ID
    private String templateName;          // 模板名称
    private String subject;              // 科目
    private String examTitle;            // 考试标题
    private String description;          // 描述
    private Integer totalQuestions;       // 总题目数
    private Integer matchedQuestions;     // 已匹配题目数
    private String status;               // 状态（DRAFT, READY, APPLIED）
    private LocalDateTime createdTime;    // 创建时间
    private LocalDateTime updatedTime;    // 更新时间
    private Double matchingProgress;      // 匹配进度（0.0-1.0）
    private List<String> sourceFiles;    // 源文件列表
    
    // 题目列表
    private List<QuestionTemplateResponse> questions;
    
    // 解析统计
    private ParseStatistics parseStatistics;
    
    public ExamTemplateResponse() {}
    
    public ExamTemplateResponse(Long id, String templateName, String subject, String examTitle,
                               String description, Integer totalQuestions, Integer matchedQuestions,
                               String status, LocalDateTime createdTime, LocalDateTime updatedTime,
                               List<QuestionTemplateResponse> questions, ParseStatistics parseStatistics) {
        this.id = id;
        this.templateName = templateName;
        this.subject = subject;
        this.examTitle = examTitle;
        this.description = description;
        this.totalQuestions = totalQuestions;
        this.matchedQuestions = matchedQuestions;
        this.status = status;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
        this.questions = questions;
        this.parseStatistics = parseStatistics;
    }
    
    // Getter and Setter methods
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getExamTitle() {
        return examTitle;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public Integer getMatchedQuestions() {
        return matchedQuestions;
    }
    
    public void setMatchedQuestions(Integer matchedQuestions) {
        this.matchedQuestions = matchedQuestions;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
    
    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
    
    public List<QuestionTemplateResponse> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<QuestionTemplateResponse> questions) {
        this.questions = questions;
    }
    
    public ParseStatistics getParseStatistics() {
        return parseStatistics;
    }
    
    public void setParseStatistics(ParseStatistics parseStatistics) {
        this.parseStatistics = parseStatistics;
    }
    
    public Double getMatchingProgress() {
        return matchingProgress;
    }
    
    public void setMatchingProgress(Double matchingProgress) {
        this.matchingProgress = matchingProgress;
    }
    
    public List<String> getSourceFiles() {
        return sourceFiles;
    }
    
    public void setSourceFiles(List<String> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExamTemplateResponse that = (ExamTemplateResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(templateName, that.templateName) &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(examTitle, that.examTitle) &&
               Objects.equals(description, that.description) &&
               Objects.equals(totalQuestions, that.totalQuestions) &&
               Objects.equals(matchedQuestions, that.matchedQuestions) &&
               Objects.equals(status, that.status) &&
               Objects.equals(createdTime, that.createdTime) &&
               Objects.equals(updatedTime, that.updatedTime) &&
               Objects.equals(questions, that.questions) &&
               Objects.equals(parseStatistics, that.parseStatistics);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, templateName, subject, examTitle, description, totalQuestions,
                          matchedQuestions, status, createdTime, updatedTime, questions, parseStatistics);
    }
    
    @Override
    public String toString() {
        return "ExamTemplateResponse{" +
               "id=" + id +
               ", templateName='" + templateName + '\'' +
               ", subject='" + subject + '\'' +
               ", examTitle='" + examTitle + '\'' +
               ", description='" + description + '\'' +
               ", totalQuestions=" + totalQuestions +
               ", matchedQuestions=" + matchedQuestions +
               ", status='" + status + '\'' +
               ", createdTime=" + createdTime +
               ", updatedTime=" + updatedTime +
               ", questions=" + questions +
               ", parseStatistics=" + parseStatistics +
               '}';
    }
    
    public static class QuestionTemplateResponse {
        private Integer questionNumber;       // 题目序号
        private String questionContent;       // 题目内容
        private String sectionHeader;         // 段落标题
        private String questionType;          // 题目类型
        private Double score;                // 分数
        private String correctAnswer;         // 正确答案
        private List<String> options;         // 选项
        private String explanation;           // 解释说明
        private Boolean isRequired;           // 是否必答
        
        // 匹配信息
        private Long questionId;             // 匹配到的题目ID
        private Boolean isMatched;           // 是否已匹配
        private String matchingStrategy;     // 匹配策略
        private Double matchingConfidence;   // 匹配置信度
        private String matchingReason;       // 匹配原因
        
        // 验证信息
        private Boolean hasIssues;           // 是否有问题
        private List<String> issues;         // 问题列表
        private List<String> suggestions;    // 建议
        
        public QuestionTemplateResponse() {}
        
        public QuestionTemplateResponse(Integer questionNumber, String questionContent, String sectionHeader,
                                       String questionType, Double score, String correctAnswer, List<String> options,
                                       String explanation, Boolean isRequired, Long questionId, Boolean isMatched,
                                       String matchingStrategy, Double matchingConfidence, String matchingReason,
                                       Boolean hasIssues, List<String> issues, List<String> suggestions) {
            this.questionNumber = questionNumber;
            this.questionContent = questionContent;
            this.sectionHeader = sectionHeader;
            this.questionType = questionType;
            this.score = score;
            this.correctAnswer = correctAnswer;
            this.options = options;
            this.explanation = explanation;
            this.isRequired = isRequired;
            this.questionId = questionId;
            this.isMatched = isMatched;
            this.matchingStrategy = matchingStrategy;
            this.matchingConfidence = matchingConfidence;
            this.matchingReason = matchingReason;
            this.hasIssues = hasIssues;
            this.issues = issues;
            this.suggestions = suggestions;
        }
        
        // Getter and Setter methods for QuestionTemplateResponse
        public Integer getQuestionNumber() {
            return questionNumber;
        }
        
        public void setQuestionNumber(Integer questionNumber) {
            this.questionNumber = questionNumber;
        }
        
        public String getQuestionContent() {
            return questionContent;
        }
        
        public void setQuestionContent(String questionContent) {
            this.questionContent = questionContent;
        }
        
        public String getSectionHeader() {
            return sectionHeader;
        }
        
        public void setSectionHeader(String sectionHeader) {
            this.sectionHeader = sectionHeader;
        }
        
        public String getQuestionType() {
            return questionType;
        }
        
        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }
        
        public Double getScore() {
            return score;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public String getCorrectAnswer() {
            return correctAnswer;
        }
        
        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }
        
        public List<String> getOptions() {
            return options;
        }
        
        public void setOptions(List<String> options) {
            this.options = options;
        }
        
        public String getExplanation() {
            return explanation;
        }
        
        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
        
        public Boolean getIsRequired() {
            return isRequired;
        }
        
        public void setIsRequired(Boolean isRequired) {
            this.isRequired = isRequired;
        }
        
        public Long getQuestionId() {
            return questionId;
        }
        
        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }
        
        public Boolean getIsMatched() {
            return isMatched;
        }
        
        public void setIsMatched(Boolean isMatched) {
            this.isMatched = isMatched;
        }
        
        public String getMatchingStrategy() {
            return matchingStrategy;
        }
        
        public void setMatchingStrategy(String matchingStrategy) {
            this.matchingStrategy = matchingStrategy;
        }
        
        public Double getMatchingConfidence() {
            return matchingConfidence;
        }
        
        public void setMatchingConfidence(Double matchingConfidence) {
            this.matchingConfidence = matchingConfidence;
        }
        
        public String getMatchingReason() {
            return matchingReason;
        }
        
        public void setMatchingReason(String matchingReason) {
            this.matchingReason = matchingReason;
        }
        
        public Boolean getHasIssues() {
            return hasIssues;
        }
        
        public void setHasIssues(Boolean hasIssues) {
            this.hasIssues = hasIssues;
        }
        
        public List<String> getIssues() {
            return issues;
        }
        
        public void setIssues(List<String> issues) {
            this.issues = issues;
        }
        
        public List<String> getSuggestions() {
            return suggestions;
        }
        
        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuestionTemplateResponse that = (QuestionTemplateResponse) o;
            return Objects.equals(questionNumber, that.questionNumber) &&
                   Objects.equals(questionContent, that.questionContent) &&
                   Objects.equals(sectionHeader, that.sectionHeader) &&
                   Objects.equals(questionType, that.questionType) &&
                   Objects.equals(score, that.score) &&
                   Objects.equals(correctAnswer, that.correctAnswer) &&
                   Objects.equals(options, that.options) &&
                   Objects.equals(explanation, that.explanation) &&
                   Objects.equals(isRequired, that.isRequired) &&
                   Objects.equals(questionId, that.questionId) &&
                   Objects.equals(isMatched, that.isMatched) &&
                   Objects.equals(matchingStrategy, that.matchingStrategy) &&
                   Objects.equals(matchingConfidence, that.matchingConfidence) &&
                   Objects.equals(matchingReason, that.matchingReason) &&
                   Objects.equals(hasIssues, that.hasIssues) &&
                   Objects.equals(issues, that.issues) &&
                   Objects.equals(suggestions, that.suggestions);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(questionNumber, questionContent, sectionHeader, questionType, score,
                              correctAnswer, options, explanation, isRequired, questionId, isMatched,
                              matchingStrategy, matchingConfidence, matchingReason, hasIssues, issues, suggestions);
        }
        
        @Override
        public String toString() {
            return "QuestionTemplateResponse{" +
                   "questionNumber=" + questionNumber +
                   ", questionContent='" + questionContent + '\'' +
                   ", sectionHeader='" + sectionHeader + '\'' +
                   ", questionType='" + questionType + '\'' +
                   ", score=" + score +
                   ", correctAnswer='" + correctAnswer + '\'' +
                   ", options=" + options +
                   ", explanation='" + explanation + '\'' +
                   ", isRequired=" + isRequired +
                   ", questionId=" + questionId +
                   ", isMatched=" + isMatched +
                   ", matchingStrategy='" + matchingStrategy + '\'' +
                   ", matchingConfidence=" + matchingConfidence +
                   ", matchingReason='" + matchingReason + '\'' +
                   ", hasIssues=" + hasIssues +
                   ", issues=" + issues +
                   ", suggestions=" + suggestions +
                   '}';
        }
    }
    
    public static class ParseStatistics {
        private Integer totalDocuments;       // 总文档数
        private Integer successfullyParsed;   // 成功解析数
        private Integer failedToParse;        // 解析失败数
        private Integer totalQuestions;       // 总题目数
        private Integer questionsByType;      // 按类型分组的题目数
        private List<String> commonIssues;    // 常见问题
        private List<String> recommendations; // 建议
        
        public ParseStatistics() {}
        
        public ParseStatistics(Integer totalDocuments, Integer successfullyParsed, Integer failedToParse,
                              Integer totalQuestions, Integer questionsByType, List<String> commonIssues,
                              List<String> recommendations) {
            this.totalDocuments = totalDocuments;
            this.successfullyParsed = successfullyParsed;
            this.failedToParse = failedToParse;
            this.totalQuestions = totalQuestions;
            this.questionsByType = questionsByType;
            this.commonIssues = commonIssues;
            this.recommendations = recommendations;
        }
        
        // Getter and Setter methods for ParseStatistics
        public Integer getTotalDocuments() {
            return totalDocuments;
        }
        
        public void setTotalDocuments(Integer totalDocuments) {
            this.totalDocuments = totalDocuments;
        }
        
        public Integer getSuccessfullyParsed() {
            return successfullyParsed;
        }
        
        public void setSuccessfullyParsed(Integer successfullyParsed) {
            this.successfullyParsed = successfullyParsed;
        }
        
        public Integer getFailedToParse() {
            return failedToParse;
        }
        
        public void setFailedToParse(Integer failedToParse) {
            this.failedToParse = failedToParse;
        }
        
        public Integer getTotalQuestions() {
            return totalQuestions;
        }
        
        public void setTotalQuestions(Integer totalQuestions) {
            this.totalQuestions = totalQuestions;
        }
        
        public Integer getQuestionsByType() {
            return questionsByType;
        }
        
        public void setQuestionsByType(Integer questionsByType) {
            this.questionsByType = questionsByType;
        }
        
        public List<String> getCommonIssues() {
            return commonIssues;
        }
        
        public void setCommonIssues(List<String> commonIssues) {
            this.commonIssues = commonIssues;
        }
        
        public List<String> getRecommendations() {
            return recommendations;
        }
        
        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParseStatistics that = (ParseStatistics) o;
            return Objects.equals(totalDocuments, that.totalDocuments) &&
                   Objects.equals(successfullyParsed, that.successfullyParsed) &&
                   Objects.equals(failedToParse, that.failedToParse) &&
                   Objects.equals(totalQuestions, that.totalQuestions) &&
                   Objects.equals(questionsByType, that.questionsByType) &&
                   Objects.equals(commonIssues, that.commonIssues) &&
                   Objects.equals(recommendations, that.recommendations);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(totalDocuments, successfullyParsed, failedToParse, totalQuestions,
                              questionsByType, commonIssues, recommendations);
        }
        
        @Override
        public String toString() {
            return "ParseStatistics{" +
                   "totalDocuments=" + totalDocuments +
                   ", successfullyParsed=" + successfullyParsed +
                   ", failedToParse=" + failedToParse +
                   ", totalQuestions=" + totalQuestions +
                   ", questionsByType=" + questionsByType +
                   ", commonIssues=" + commonIssues +
                   ", recommendations=" + recommendations +
                   '}';
        }
    }
} 
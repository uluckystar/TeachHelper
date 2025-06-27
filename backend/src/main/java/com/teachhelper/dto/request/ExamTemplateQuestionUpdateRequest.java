package com.teachhelper.dto.request;

public class ExamTemplateQuestionUpdateRequest {
    
    private String sectionHeader;
    private String questionContent;
    private String questionType;
    private Integer score;
    private String options;
    private String correctAnswer;
    private String explanation;
    
    // 构造函数
    public ExamTemplateQuestionUpdateRequest() {
    }
    
    // Getter和Setter方法
    public String getSectionHeader() {
        return sectionHeader;
    }
    
    public void setSectionHeader(String sectionHeader) {
        this.sectionHeader = sectionHeader;
    }
    
    public String getQuestionContent() {
        return questionContent;
    }
    
    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    @Override
    public String toString() {
        return "ExamTemplateQuestionUpdateRequest{" +
                "sectionHeader='" + sectionHeader + '\'' +
                ", questionContent='" + questionContent + '\'' +
                ", questionType='" + questionType + '\'' +
                ", score=" + score +
                ", options='" + options + '\'' +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
} 
package com.teachhelper.dto.request;

import java.util.List;
import java.util.Objects;

public class ExamTemplateRequest {
    
    private String templateName;
    private String examTitle;
    private String description;
    private List<QuestionTemplate> questions;
    
    // 无参构造函数
    public ExamTemplateRequest() {}
    
    // 全参构造函数
    public ExamTemplateRequest(String templateName, String examTitle, String description, List<QuestionTemplate> questions) {
        this.templateName = templateName;
        this.examTitle = examTitle;
        this.description = description;
        this.questions = questions;
    }
    
    // Getter方法
    public String getTemplateName() {
        return templateName;
    }
    
    public String getExamTitle() {
        return examTitle;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<QuestionTemplate> getQuestions() {
        return questions;
    }
    
    // Setter方法
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setQuestions(List<QuestionTemplate> questions) {
        this.questions = questions;
    }
    
    // equals方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExamTemplateRequest that = (ExamTemplateRequest) o;
        return Objects.equals(templateName, that.templateName) &&
               Objects.equals(examTitle, that.examTitle) &&
               Objects.equals(description, that.description) &&
               Objects.equals(questions, that.questions);
    }
    
    // hashCode方法
    @Override
    public int hashCode() {
        return Objects.hash(templateName, examTitle, description, questions);
    }
    
    // toString方法
    @Override
    public String toString() {
        return "ExamTemplateRequest{" +
                "templateName='" + templateName + '\'' +
                ", examTitle='" + examTitle + '\'' +
                ", description='" + description + '\'' +
                ", questions=" + questions +
                '}';
    }
    
    // 内部类QuestionTemplate
    public static class QuestionTemplate {
        
        private Integer questionNumber;
        private String questionContent;
        private String expectedAnswer;
        private Double score;
        private String questionType;
        
        // 无参构造函数
        public QuestionTemplate() {}
        
        // 全参构造函数
        public QuestionTemplate(Integer questionNumber, String questionContent, String expectedAnswer, Double score, String questionType) {
            this.questionNumber = questionNumber;
            this.questionContent = questionContent;
            this.expectedAnswer = expectedAnswer;
            this.score = score;
            this.questionType = questionType;
        }
        
        // Getter方法
        public Integer getQuestionNumber() {
            return questionNumber;
        }
        
        public String getQuestionContent() {
            return questionContent;
        }
        
        public String getExpectedAnswer() {
            return expectedAnswer;
        }
        
        public Double getScore() {
            return score;
        }
        
        public String getQuestionType() {
            return questionType;
        }
        
        // Setter方法
        public void setQuestionNumber(Integer questionNumber) {
            this.questionNumber = questionNumber;
        }
        
        public void setQuestionContent(String questionContent) {
            this.questionContent = questionContent;
        }
        
        public void setExpectedAnswer(String expectedAnswer) {
            this.expectedAnswer = expectedAnswer;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }
        
        // equals方法
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuestionTemplate that = (QuestionTemplate) o;
            return Objects.equals(questionNumber, that.questionNumber) &&
                   Objects.equals(questionContent, that.questionContent) &&
                   Objects.equals(expectedAnswer, that.expectedAnswer) &&
                   Objects.equals(score, that.score) &&
                   Objects.equals(questionType, that.questionType);
        }
        
        // hashCode方法
        @Override
        public int hashCode() {
            return Objects.hash(questionNumber, questionContent, expectedAnswer, score, questionType);
        }
        
        // toString方法
        @Override
        public String toString() {
            return "QuestionTemplate{" +
                    "questionNumber=" + questionNumber +
                    ", questionContent='" + questionContent + '\'' +
                    ", expectedAnswer='" + expectedAnswer + '\'' +
                    ", score=" + score +
                    ", questionType='" + questionType + '\'' +
                    '}';
        }
    }
}

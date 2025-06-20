package com.teachhelper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "question_options")
public class QuestionOption extends BaseEntity {
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @NotNull
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;
    
    @Column(name = "option_order")
    private Integer optionOrder;
    
    // Constructors
    public QuestionOption() {}
    
    public QuestionOption(String content, Boolean isCorrect) {
        this.content = content;
        this.isCorrect = isCorrect;
    }
    
    public QuestionOption(String content, Boolean isCorrect, Integer optionOrder) {
        this.content = content;
        this.isCorrect = isCorrect;
        this.optionOrder = optionOrder;
    }
    
    // Getters and Setters
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public Integer getOptionOrder() {
        return optionOrder;
    }
    
    public void setOptionOrder(Integer optionOrder) {
        this.optionOrder = optionOrder;
    }
}

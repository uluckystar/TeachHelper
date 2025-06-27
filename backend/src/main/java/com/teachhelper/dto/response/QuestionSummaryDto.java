package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目摘要DTO，用于显示候选题目信息
 */
public class QuestionSummaryDto {
    
    private Long id;
    private String title;
    private String content;
    private String questionType;
    private String difficulty;
    private LocalDateTime createdAt;
    private List<String> optionContents; // 简化的选项内容
    
    // Constructors
    public QuestionSummaryDto() {}
    
    public QuestionSummaryDto(Long id, String title, String content, String questionType) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.questionType = questionType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<String> getOptionContents() {
        return optionContents;
    }
    
    public void setOptionContents(List<String> optionContents) {
        this.optionContents = optionContents;
    }
} 
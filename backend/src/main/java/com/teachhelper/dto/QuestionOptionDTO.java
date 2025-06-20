package com.teachhelper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QuestionOptionDTO {
    
    private Long id;
    
    @NotBlank(message = "选项内容不能为空")
    private String content;
    
    @NotNull(message = "是否正确不能为空")
    private Boolean isCorrect;
    
    private Integer optionOrder;
    
    // Constructors
    public QuestionOptionDTO() {}
    
    public QuestionOptionDTO(String content, Boolean isCorrect) {
        this.content = content;
        this.isCorrect = isCorrect;
    }
    
    public QuestionOptionDTO(String content, Boolean isCorrect, Integer optionOrder) {
        this.content = content;
        this.isCorrect = isCorrect;
        this.optionOrder = optionOrder;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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

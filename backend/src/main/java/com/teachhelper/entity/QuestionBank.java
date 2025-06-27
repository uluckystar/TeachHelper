package com.teachhelper.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目库实体 - 题目的公共资源池
 * 题目首先属于题目库，然后可以被引用到不同的考试中
 */
@Entity
@Table(name = "question_banks")
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionBank extends BaseEntity {
    
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "grade_level")
    private String gradeLevel;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
    
    // Helper methods
    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuestionBank(this);
    }
    
    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setQuestionBank(null);
    }
    
    // Explicit getters and setters (fallback for Lombok issues)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}

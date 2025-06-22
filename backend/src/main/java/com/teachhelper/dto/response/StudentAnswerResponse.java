package com.teachhelper.dto.response;

import java.time.LocalDateTime;

public class StudentAnswerResponse {
    
    private Long id;
    private Long questionId;
    private String questionTitle;
    private String questionContent;
    private StudentInfo student;
    private String answerText;
    private Double score;
    private String feedback;
    private boolean isEvaluated;
    private Double maxScore; // 题目的满分
    private LocalDateTime submittedAt;
    private LocalDateTime evaluatedAt;
    private String evaluator;
    
    public StudentAnswerResponse() {}
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public String getQuestionTitle() {
        return questionTitle;
    }
    
    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }
    
    public String getQuestionContent() {
        return questionContent;
    }
    
    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }
    
    public StudentInfo getStudent() {
        return student;
    }
    
    public void setStudent(StudentInfo student) {
        this.student = student;
    }
    
    public String getAnswerText() {
        return answerText;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public boolean isEvaluated() {
        return isEvaluated;
    }
    
    public void setEvaluated(boolean evaluated) {
        isEvaluated = evaluated;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(String evaluator) {
        this.evaluator = evaluator;
    }

    // Static factory method to convert from StudentAnswer entity
    public static StudentAnswerResponse fromEntity(com.teachhelper.entity.StudentAnswer studentAnswer) {
        StudentAnswerResponse dto = new StudentAnswerResponse();
        dto.setId(studentAnswer.getId());
        if (studentAnswer.getQuestion() != null) {
            dto.setQuestionId(studentAnswer.getQuestion().getId());
            dto.setQuestionTitle(studentAnswer.getQuestion().getTitle()); // Assuming Question entity has getTitle()
            dto.setQuestionContent(studentAnswer.getQuestion().getContent()); // Assuming Question entity has getContent()
            // 设置题目的满分
            dto.setMaxScore(studentAnswer.getQuestion().getMaxScore() != null ? 
                studentAnswer.getQuestion().getMaxScore().doubleValue() : 0.0);
        }
        if (studentAnswer.getStudent() != null) {
            // Use Student entity's correct method getName() instead of getUsername()
            dto.setStudent(new StudentInfo(
                studentAnswer.getStudent().getId(), 
                studentAnswer.getStudent().getStudentId(),
                studentAnswer.getStudent().getName(),
                studentAnswer.getStudent().getEmail()
            ));
        }
        dto.setAnswerText(studentAnswer.getAnswerText());
        // Convert BigDecimal to Double
        dto.setScore(studentAnswer.getScore() != null ? studentAnswer.getScore().doubleValue() : null);
        dto.setFeedback(studentAnswer.getFeedback());
        dto.setEvaluated(studentAnswer.isEvaluated());
        // Use getCreatedAt() instead of getSubmittedAt() since StudentAnswer extends BaseEntity
        dto.setSubmittedAt(studentAnswer.getCreatedAt());
        dto.setEvaluatedAt(studentAnswer.getEvaluatedAt());
        if (studentAnswer.getEvaluator() != null) {
            dto.setEvaluator(studentAnswer.getEvaluator().getUsername()); // Assuming User entity has getUsername()
        }
        return dto;
    }
    
    public static class StudentInfo {
        private Long id;
        private String studentId;
        private String name;
        private String email;
        
        public StudentInfo() {}
        
        public StudentInfo(Long id, String studentId, String name, String email) {
            this.id = id;
            this.studentId = studentId;
            this.name = name;
            this.email = email;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }
}

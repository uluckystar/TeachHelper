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
    private boolean evaluated;
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
        return evaluated;
    }
    
    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
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
            // 对于学生试卷详情，过滤题目内容中的选项信息
            dto.setQuestionContent(filterQuestionContentForStudent(studentAnswer.getQuestion(), studentAnswer.getAnswerText()));
            // 设置题目的满分
            dto.setMaxScore(studentAnswer.getQuestion().getMaxScore() != null ? 
                studentAnswer.getQuestion().getMaxScore().doubleValue() : 0.0);
        }
        if (studentAnswer.getStudent() != null) {
            // 从User实体获取学生信息
            com.teachhelper.entity.User user = studentAnswer.getStudent();
            
            // 使用User的学号字段而不是ID作为显示的学生ID
            String displayStudentId = user.getStudentNumber() != null 
                ? user.getStudentNumber() 
                : String.valueOf(user.getId()); // 向后兼容，使用用户ID
                
            String displayName = user.getRealName() != null 
                ? user.getRealName() 
                : user.getUsername(); // 优先使用真实姓名，否则使用用户名
                
            dto.setStudent(new StudentInfo(
                user.getId(), 
                displayStudentId, // 使用学号作为显示的学生ID
                displayName, // 使用真实姓名或用户名
                user.getEmail()
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
    
    /**
     * 为学生视图过滤题目内容
     * 返回完整的题目内容，包含选项，但不包含学生答案信息
     */
    private static String filterQuestionContentForStudent(com.teachhelper.entity.Question question, String studentAnswer) {
        if (question == null) {
            return "";
        }
        
        String content = question.getContent();
        if (content == null) {
            return "";
        }
        
        // 直接返回题目完整内容，不附加学生答案信息
        // 学生答案应该在 answerText 字段中单独显示
        return content;
    }
    
    /**
     * 从题目内容中移除选项部分
     */
    private static String removeOptionsFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // 通过正则表达式移除选项部分
        // 匹配格式如：A. xxx  B. xxx  C. xxx 或 A、xxx B、xxx C、xxx
        String result = content;
        
        // 移除 A. B. C. D. 格式的选项
        result = result.replaceAll("\\s*[A-Z]\\s*[.、]\\s*[^\\n\\r]*(?=[\\n\\r]|$)", "");
        
        // 移除 (A) (B) (C) (D) 格式的选项  
        result = result.replaceAll("\\s*\\([A-Z]\\)\\s*[^\\n\\r]*(?=[\\n\\r]|$)", "");
        
        // 移除多余的换行符
        result = result.replaceAll("\\n{2,}", "\n").trim();
        
        return result;
    }
    
    public static class StudentInfo {
        private Long id;
        private String studentId;
        private String studentNumber; // 添加 studentNumber 字段匹配前端
        private String name;
        private String email;
        
        public StudentInfo() {}
        
        public StudentInfo(Long id, String studentNumber, String name, String email) {
            this.id = id;
            this.studentId = studentNumber; // 向后兼容
            this.studentNumber = studentNumber; // 新字段
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
            this.studentNumber = studentId; // 保持同步
        }
        
        public String getStudentNumber() {
            return studentNumber;
        }
        
        public void setStudentNumber(String studentNumber) {
            this.studentNumber = studentNumber;
            this.studentId = studentNumber; // 保持同步
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

package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班级响应 DTO
 */
public class ClassroomResponse {
    
    private Long id;
    private String name;
    private String description;
    private String classCode;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int studentCount;
    private List<StudentResponse> students;
    private int examCount;
    
    // 构造方法
    public ClassroomResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getClassCode() {
        return classCode;
    }
    
    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public int getStudentCount() {
        return studentCount;
    }
    
    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }
    
    public List<StudentResponse> getStudents() {
        return students;
    }
    
    public void setStudents(List<StudentResponse> students) {
        this.students = students;
    }
    
    public int getExamCount() {
        return examCount;
    }
    
    public void setExamCount(int examCount) {
        this.examCount = examCount;
    }
    
    /**
     * 简化的学生信息响应
     */
    public static class StudentResponse {
        private Long id;
        private String username;
        private String email;
        private LocalDateTime joinedAt;
        
        public StudentResponse() {}
        
        public StudentResponse(Long id, String username, String email, LocalDateTime joinedAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.joinedAt = joinedAt;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public LocalDateTime getJoinedAt() {
            return joinedAt;
        }
        
        public void setJoinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
        }
    }
}

package com.teachhelper.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "students")
public class Student extends BaseEntity {
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "student_id", unique = true, nullable = false)
    private String studentId;
    
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;
    
    @Email
    @Column(unique = true)
    private String email;
    
    @Column(name = "class_name")
    private String className;
    
    @Column(name = "major")
    private String major;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentAnswer> answers = new ArrayList<>();
    
    // Constructors
    public Student() {}
    
    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }
    
    public Student(String studentId, String name, String email) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
    }
    
    // Helper methods
    public void addAnswer(StudentAnswer answer) {
        answers.add(answer);
        answer.setStudent(this);
    }
    
    public void removeAnswer(StudentAnswer answer) {
        answers.remove(answer);
        answer.setStudent(null);
    }
    
    // Getters and Setters
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
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public List<StudentAnswer> getAnswers() {
        return answers;
    }
    
    public void setAnswers(List<StudentAnswer> answers) {
        this.answers = answers;
    }
}

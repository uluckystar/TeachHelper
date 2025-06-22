package com.teachhelper.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 班级实体类
 * 用于表示教学班级，包含班级名称、描述、创建者（教师）和学生成员
 */
@Entity
@Table(name = "classrooms")
public class Classroom extends BaseEntity {
    
    /**
     * 班级名称
     * 不能为空，最大长度100字符
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;
    
    /**
     * 班级描述
     * 可选字段，存储班级的详细描述信息
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 班级代码
     * 用于学生加入班级的邀请码
     */
    @Column(name = "class_code", unique = true, nullable = false)
    private String classCode;
    
    /**
     * 班级创建者（教师）
     * 多对一关系，指向创建此班级的用户（必须是教师角色）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    
    /**
     * 班级学生成员
     * 多对多关系，包含此班级的所有学生
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "classroom_students",
        joinColumns = @JoinColumn(name = "classroom_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<User> students = new HashSet<>();
    
    /**
     * 班级相关的考试
     * 多对多关系，包含发布到此班级的所有考试
     */
    @ManyToMany(mappedBy = "targetClassrooms", fetch = FetchType.LAZY)
    private Set<Exam> exams = new HashSet<>();
    
    // 构造方法
    
    /**
     * 默认构造方法
     */
    public Classroom() {}
    
    /**
     * 带参数的构造方法
     * @param name 班级名称
     * @param description 班级描述
     * @param createdBy 创建者（教师）
     * @param classCode 班级代码
     */
    public Classroom(String name, String description, User createdBy, String classCode) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.classCode = classCode;
    }
    
    // 辅助方法
    
    /**
     * 添加学生到班级
     * @param student 要添加的学生
     */
    public void addStudent(User student) {
        students.add(student);
    }
    
    /**
     * 从班级移除学生
     * @param student 要移除的学生
     */
    public void removeStudent(User student) {
        students.remove(student);
    }
    
    /**
     * 检查用户是否是班级成员
     * @param user 要检查的用户
     * @return 如果是班级成员返回true，否则返回false
     */
    public boolean isMember(User user) {
        if (user.getRoles().contains(Role.STUDENT)) {
            return students.contains(user);
        } else if (user.getRoles().contains(Role.TEACHER) || user.getRoles().contains(Role.ADMIN)) {
            return createdBy.equals(user) || user.getRoles().contains(Role.ADMIN);
        }
        return false;
    }
    
    // Getters and Setters
    
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
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public Set<User> getStudents() {
        return students;
    }
    
    public void setStudents(Set<User> students) {
        this.students = students;
    }
    
    public Set<Exam> getExams() {
        return exams;
    }
    
    public void setExams(Set<Exam> exams) {
        this.exams = exams;
    }
}

package com.teachhelper.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 考试实体类
 * 用于表示系统中的考试信息，包含考试标题、描述、创建者和关联的题目
 */
@Entity
@Table(name = "exams")
public class Exam extends BaseEntity {
    
    /**
     * 考试标题
     * 不能为空，最大长度200字符
     */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;
    
    /**
     * 考试描述
     * 可选字段，存储考试的详细描述信息
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 考试状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExamStatus status = ExamStatus.DRAFT;
    
    /**
     * 考试时长（分钟）
     */
    @Column(name = "duration")
    private Integer duration;
    
    /**
     * 考试开始时间
     */
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 考试结束时间
     */
    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 考试设置（JSON格式存储）
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;
    
    /**
     * 考试创建者
     * 多对一关系，指向创建此考试的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    
    /**
     * 目标班级列表
     * 多对多关系，指向此考试发布到的班级列表
     * 可选字段，如果为空则表示全校考试或未指定班级
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "exam_classrooms",
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "classroom_id")
    )
    private Set<Classroom> targetClassrooms = new HashSet<>();
    
    /**
     * 考试题目列表
     * 一对多关系，包含此考试的所有题目
     * 级联删除，当考试被删除时，相关题目也会被删除
     */
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
    
    // 构造方法
    
    /**
     * 默认构造方法
     */
    public Exam() {}
    
    /**
     * 带参数的构造方法
     * @param title 考试标题
     * @param description 考试描述
     * @param createdBy 创建者
     */
    public Exam(String title, String description, User createdBy) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.status = ExamStatus.DRAFT; // 显式设置默认状态
    }
    
    // 辅助方法
    
    /**
     * 添加题目到考试中
     * @param question 要添加的题目
     */
    public void addQuestion(Question question) {
        questions.add(question);
        question.setExam(this);
    }
    
    /**
     * 从考试中移除题目
     * @param question 要移除的题目
     */
    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setExam(null);
    }
    
    // Getter和Setter方法
    
    /**
     * 获取考试标题
     * @return 考试标题
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * 设置考试标题
     * @param title 考试标题
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * 获取考试描述
     * @return 考试描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置考试描述
     * @param description 考试描述
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 获取考试状态
     * @return 考试状态
     */
    public ExamStatus getStatus() {
        return status;
    }
    
    /**
     * 设置考试状态
     * @param status 考试状态
     */
    public void setStatus(ExamStatus status) {
        this.status = status;
    }
    
    /**
     * 获取考试时长
     * @return 考试时长（分钟）
     */
    public Integer getDuration() {
        return duration;
    }
    
    /**
     * 设置考试时长
     * @param duration 考试时长（分钟）
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    /**
     * 获取考试开始时间
     * @return 开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * 设置考试开始时间
     * @param startTime 开始时间
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    /**
     * 获取考试结束时间
     * @return 结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * 设置考试结束时间
     * @param endTime 结束时间
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    /**
     * 获取考试设置
     * @return 考试设置（JSON字符串）
     */
    public String getSettings() {
        return settings;
    }
    
    /**
     * 设置考试设置
     * @param settings 考试设置（JSON字符串）
     */
    public void setSettings(String settings) {
        this.settings = settings;
    }
    
    /**
     * 获取考试创建者
     * @return 创建者用户对象
     */
    public User getCreatedBy() {
        return createdBy;
    }
    
    /**
     * 设置考试创建者
     * @param createdBy 创建者用户对象
     */
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * 获取目标班级列表
     * @return 目标班级列表
     */
    public Set<Classroom> getTargetClassrooms() {
        return targetClassrooms;
    }
    
    /**
     * 设置目标班级列表
     * @param targetClassrooms 目标班级列表
     */
    public void setTargetClassrooms(Set<Classroom> targetClassrooms) {
        this.targetClassrooms = targetClassrooms;
    }
    
    /**
     * 添加目标班级
     * @param classroom 要添加的班级
     */
    public void addTargetClassroom(Classroom classroom) {
        this.targetClassrooms.add(classroom);
    }
    
    /**
     * 移除目标班级
     * @param classroom 要移除的班级
     */
    public void removeTargetClassroom(Classroom classroom) {
        this.targetClassrooms.remove(classroom);
    }
    
    /**
     * 获取考试题目列表
     * @return 题目列表
     */
    public List<Question> getQuestions() {
        return questions;
    }
    
    /**
     * 设置考试题目列表
     * @param questions 题目列表
     */
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}

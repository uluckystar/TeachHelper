package com.teachhelper.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 试卷提交记录实体
 * 用于记录学生正式提交试卷的状态和时间
 */
@Entity
@Table(name = "exam_submissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "student_id"})
})
public class ExamSubmission extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "auto_submitted", nullable = false)
    private boolean autoSubmitted = false; // 是否因时间到期自动提交
    
    @Column(name = "submission_note", columnDefinition = "TEXT")
    private String submissionNote; // 提交备注（如：正常提交、超时自动提交等）
    
    @Column(name = "total_questions")
    private Integer totalQuestions; // 总题目数
    
    @Column(name = "answered_questions")
    private Integer answeredQuestions; // 已答题目数
    
    // 构造函数
    public ExamSubmission() {}
    
    public ExamSubmission(Exam exam, User student, LocalDateTime submittedAt) {
        this.exam = exam;
        this.student = student;
        this.submittedAt = submittedAt;
    }
    
    // Getters and Setters
    public Exam getExam() {
        return exam;
    }
    
    public void setExam(Exam exam) {
        this.exam = exam;
    }
    
    public User getStudent() {
        return student;
    }
    
    public void setStudent(User student) {
        this.student = student;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public Integer getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public Integer getAnsweredQuestions() {
        return answeredQuestions;
    }
    
    public void setAnsweredQuestions(Integer answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }
    
    public boolean isAutoSubmitted() {
        return autoSubmitted;
    }
    
    public void setAutoSubmitted(boolean autoSubmitted) {
        this.autoSubmitted = autoSubmitted;
    }
    
    public String getSubmissionNote() {
        return submissionNote;
    }
    
    public void setSubmissionNote(String submissionNote) {
        this.submissionNote = submissionNote;
    }
}

package com.teachhelper.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 考试统计信息响应类
 */
public class ExamStatistics {
    
    private Long examId;
    private String examTitle;
    private String examDescription;
    
    // 题目统计
    private int totalQuestions;
    
    // 答案统计
    private long totalAnswers;
    private long evaluatedAnswers;
    private long unevaluatedAnswers;
    private double evaluationProgress; // 评估进度 (0-100)
    
    // 分数统计
    private Double averageScore;
    private Double maxScore;
    private Double minScore;
    private Double totalPossibleScore;
    
    // 时间统计
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastEvaluationTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime examCreatedAt;
    
    // 学生统计
    private long totalStudents;
    private long studentsSubmitted;
    private long studentsEvaluated;
    
    // 无参构造函数
    public ExamStatistics() {}
    
    // 全参构造函数
    public ExamStatistics(Long examId, String examTitle, String examDescription,
                         int totalQuestions, long totalAnswers, long evaluatedAnswers,
                         Double averageScore, Double maxScore, Double minScore,
                         LocalDateTime lastEvaluationTime, LocalDateTime examCreatedAt,
                         long totalStudents) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.examDescription = examDescription;
        this.totalQuestions = totalQuestions;
        this.totalAnswers = totalAnswers;
        this.evaluatedAnswers = evaluatedAnswers;
        this.unevaluatedAnswers = totalAnswers - evaluatedAnswers;
        this.evaluationProgress = totalAnswers > 0 ? (evaluatedAnswers * 100.0 / totalAnswers) : 0.0;
        this.averageScore = averageScore;
        this.maxScore = maxScore;
        this.minScore = minScore;
        this.lastEvaluationTime = lastEvaluationTime;
        this.examCreatedAt = examCreatedAt;
        this.totalStudents = totalStudents;
        // studentsSubmitted 和 studentsEvaluated 需要额外计算
    }

    // Getters and Setters
    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getExamDescription() {
        return examDescription;
    }

    public void setExamDescription(String examDescription) {
        this.examDescription = examDescription;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public long getTotalAnswers() {
        return totalAnswers;
    }

    public void setTotalAnswers(long totalAnswers) {
        this.totalAnswers = totalAnswers;
    }

    public long getEvaluatedAnswers() {
        return evaluatedAnswers;
    }

    public void setEvaluatedAnswers(long evaluatedAnswers) {
        this.evaluatedAnswers = evaluatedAnswers;
        this.unevaluatedAnswers = this.totalAnswers - this.evaluatedAnswers;
        this.evaluationProgress = this.totalAnswers > 0 ? (this.evaluatedAnswers * 100.0 / this.totalAnswers) : 0.0;
    }

    public long getUnevaluatedAnswers() {
        return unevaluatedAnswers;
    }

    public void setUnevaluatedAnswers(long unevaluatedAnswers) {
        this.unevaluatedAnswers = unevaluatedAnswers;
    }

    public double getEvaluationProgress() {
        return evaluationProgress;
    }

    public void setEvaluationProgress(double evaluationProgress) {
        this.evaluationProgress = evaluationProgress;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public Double getTotalPossibleScore() {
        return totalPossibleScore;
    }

    public void setTotalPossibleScore(Double totalPossibleScore) {
        this.totalPossibleScore = totalPossibleScore;
    }

    public LocalDateTime getLastEvaluationTime() {
        return lastEvaluationTime;
    }

    public void setLastEvaluationTime(LocalDateTime lastEvaluationTime) {
        this.lastEvaluationTime = lastEvaluationTime;
    }

    public LocalDateTime getExamCreatedAt() {
        return examCreatedAt;
    }

    public void setExamCreatedAt(LocalDateTime examCreatedAt) {
        this.examCreatedAt = examCreatedAt;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getStudentsSubmitted() {
        return studentsSubmitted;
    }

    public void setStudentsSubmitted(long studentsSubmitted) {
        this.studentsSubmitted = studentsSubmitted;
    }

    public long getStudentsEvaluated() {
        return studentsEvaluated;
    }

    public void setStudentsEvaluated(long studentsEvaluated) {
        this.studentsEvaluated = studentsEvaluated;
    }
}

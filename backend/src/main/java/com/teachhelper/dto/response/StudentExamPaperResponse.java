package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.teachhelper.entity.StudentAnswer;
import com.teachhelper.entity.User;

/**
 * 学生试卷响应类
 * 包含学生信息及其在考试中的所有答案
 */
public class StudentExamPaperResponse {
    
    private Long studentId;
    private String studentName;
    private String studentNumber;
    private String studentEmail;
    private Long examId;
    private String examTitle;
    private List<StudentAnswerResponse> answers;
    private int totalQuestions;
    private int answeredQuestions;
    private int evaluatedAnswers;
    private Double totalScore;
    private Double maxPossibleScore;
    private Double scorePercentage;
    private boolean isFullyEvaluated;
    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdated;

    public StudentExamPaperResponse() {}

    public StudentExamPaperResponse(User student, Long examId, String examTitle, List<StudentAnswer> answers) {
        this.studentId = student.getId();
        this.studentName = student.getRealName() != null ? student.getRealName() : student.getUsername();
        this.studentNumber = student.getStudentNumber();
        this.studentEmail = student.getEmail();
        this.examId = examId;
        this.examTitle = examTitle;
        
        // 转换答案
        this.answers = answers.stream()
            .map(StudentAnswerResponse::fromEntity)
            .toList();
            
        // 计算统计信息
        this.totalQuestions = answers.size();
        this.answeredQuestions = (int) answers.stream()
            .filter(answer -> answer.getAnswerText() != null && !answer.getAnswerText().trim().isEmpty())
            .count();
        this.evaluatedAnswers = (int) answers.stream()
            .filter(StudentAnswer::isEvaluated)
            .count();
            
        // 计算分数
        this.totalScore = answers.stream()
            .filter(answer -> answer.getScore() != null)
            .mapToDouble(answer -> answer.getScore().doubleValue())
            .sum();
            
        this.maxPossibleScore = answers.stream()
            .filter(answer -> answer.getQuestion() != null && answer.getQuestion().getMaxScore() != null)
            .mapToDouble(answer -> answer.getQuestion().getMaxScore().doubleValue())
            .sum();
            
        this.scorePercentage = maxPossibleScore > 0 ? (totalScore / maxPossibleScore) * 100 : 0.0;
        // 修复逻辑：当所有学生提交的答案都已评估时，才算完全评估
        this.isFullyEvaluated = answers.size() > 0 && evaluatedAnswers == answers.size();
        
        // 时间信息
        this.submittedAt = answers.stream()
            .filter(answer -> answer.getCreatedAt() != null)
            .map(StudentAnswer::getCreatedAt)
            .min(LocalDateTime::compareTo)
            .orElse(null);
            
        this.lastUpdated = answers.stream()
            .filter(answer -> answer.getUpdatedAt() != null)
            .map(StudentAnswer::getUpdatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
    }

    // Getters and Setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

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

    public List<StudentAnswerResponse> getAnswers() {
        return answers;
    }

    public void setAnswers(List<StudentAnswerResponse> answers) {
        this.answers = answers;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(int answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public int getEvaluatedAnswers() {
        return evaluatedAnswers;
    }

    public void setEvaluatedAnswers(int evaluatedAnswers) {
        this.evaluatedAnswers = evaluatedAnswers;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Double getMaxPossibleScore() {
        return maxPossibleScore;
    }

    public void setMaxPossibleScore(Double maxPossibleScore) {
        this.maxPossibleScore = maxPossibleScore;
    }

    public Double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(Double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public boolean isFullyEvaluated() {
        return isFullyEvaluated;
    }

    public void setFullyEvaluated(boolean fullyEvaluated) {
        isFullyEvaluated = fullyEvaluated;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

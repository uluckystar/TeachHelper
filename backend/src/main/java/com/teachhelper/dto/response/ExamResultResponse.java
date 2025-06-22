package com.teachhelper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 考试结果响应类
 */
public class ExamResultResponse {
    
    private Long examId;
    private String examTitle;
    private String examDescription;
    private Long studentId;
    private String studentName;
    private Double totalScore;
    private Integer answeredQuestions;
    private String status;
    private Double totalPossibleScore; // 考试满分
    private Double scorePercentage; // 得分率
    private String grade; // 成绩等级
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;
    
    private Long duration; // 用时（秒）
    
    private List<StudentAnswerResponse> answers;
    
    // 无参构造函数
    public ExamResultResponse() {}
    
    // 全参构造函数
    public ExamResultResponse(Long examId, Long studentId, String studentName, 
                             Double totalScore, Integer answeredQuestions, String status,
                             LocalDateTime startTime, LocalDateTime submitTime, Long duration) {
        this.examId = examId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.totalScore = totalScore;
        this.answeredQuestions = answeredQuestions;
        this.status = status;
        this.startTime = startTime;
        this.submitTime = submitTime;
        this.duration = duration;
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
    
    public Double getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }
    
    public Integer getAnsweredQuestions() {
        return answeredQuestions;
    }
    
    public void setAnsweredQuestions(Integer answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<StudentAnswerResponse> getAnswers() {
        return answers;
    }

    public void setAnswers(List<StudentAnswerResponse> answers) {
        this.answers = answers;
    }

    public Double getTotalPossibleScore() {
        return totalPossibleScore;
    }

    public void setTotalPossibleScore(Double totalPossibleScore) {
        this.totalPossibleScore = totalPossibleScore;
    }

    public Double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(Double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    // Setter for userId, assuming it was missing and named studentId in the constructor
    public void setUserId(Long userId) {
        this.studentId = userId; // Assuming studentId field stores the user ID
    }

    // Getter for userId, assuming it was missing and named studentId
    public Long getUserId() {
        return this.studentId; // Assuming studentId field stores the user ID
    }
}

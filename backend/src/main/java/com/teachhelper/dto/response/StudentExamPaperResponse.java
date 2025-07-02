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

    public StudentExamPaperResponse(User student, Long examId, String examTitle, List<com.teachhelper.entity.Question> allQuestions, List<StudentAnswer> studentAnswers) {
        this.studentId = student.getId();
        this.studentName = student.getRealName() != null ? student.getRealName() : student.getUsername();
        this.studentNumber = student.getStudentNumber();
        this.studentEmail = student.getEmail();
        this.examId = examId;
        this.examTitle = examTitle;

        // 构建题目ID到答案的映射
        java.util.Map<Long, StudentAnswer> answerMap = new java.util.HashMap<>();
        for (StudentAnswer ans : studentAnswers) {
            if (ans.getQuestion() != null) {
                answerMap.put(ans.getQuestion().getId(), ans);
            }
        }
        java.util.List<StudentAnswerResponse> answerResponses = new java.util.ArrayList<>();
        int answeredCount = 0;
        int evaluatedCount = 0;
        double totalScore = 0.0;
        double maxPossibleScore = 0.0;
        java.time.LocalDateTime minSubmit = null;
        java.time.LocalDateTime maxUpdate = null;
        for (com.teachhelper.entity.Question q : allQuestions) {
            StudentAnswer ans = answerMap.get(q.getId());
            if (ans != null) {
                StudentAnswerResponse resp = StudentAnswerResponse.fromEntity(ans);
                answerResponses.add(resp);
                if (ans.getAnswerText() != null && !ans.getAnswerText().trim().isEmpty()) answeredCount++;
                if (ans.isEvaluated()) evaluatedCount++;
                if (ans.getScore() != null) totalScore += ans.getScore().doubleValue();
                if (ans.getQuestion() != null && ans.getQuestion().getMaxScore() != null) maxPossibleScore += ans.getQuestion().getMaxScore().doubleValue();
                if (ans.getCreatedAt() != null) {
                    if (minSubmit == null || ans.getCreatedAt().isBefore(minSubmit)) minSubmit = ans.getCreatedAt();
                }
                if (ans.getUpdatedAt() != null) {
                    if (maxUpdate == null || ans.getUpdatedAt().isAfter(maxUpdate)) maxUpdate = ans.getUpdatedAt();
                }
            } else {
                // 构造未作答的空答案
                StudentAnswerResponse resp = new StudentAnswerResponse();
                resp.setQuestionId(q.getId());
                resp.setQuestionTitle(q.getTitle());
                resp.setQuestionContent(q.getContent());
                resp.setAnswerText("学生未作答");
                resp.setScore(null);
                resp.setEvaluated(false);
                resp.setMaxScore(q.getMaxScore() != null ? q.getMaxScore().doubleValue() : 0.0);
                answerResponses.add(resp);
                if (q.getMaxScore() != null) maxPossibleScore += q.getMaxScore().doubleValue();
            }
        }
        this.answers = answerResponses;
        this.totalQuestions = allQuestions.size();
        this.answeredQuestions = answeredCount;
        this.evaluatedAnswers = evaluatedCount;
        this.totalScore = totalScore;
        this.maxPossibleScore = maxPossibleScore;
        this.scorePercentage = maxPossibleScore > 0 ? (totalScore / maxPossibleScore) * 100 : 0.0;
        this.isFullyEvaluated = allQuestions.size() > 0 && evaluatedCount == allQuestions.size();
        this.submittedAt = minSubmit;
        this.lastUpdated = maxUpdate;
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

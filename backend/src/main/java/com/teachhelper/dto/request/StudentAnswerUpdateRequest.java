package com.teachhelper.dto.request;

/**
 * 用于更新学生答案的请求体
 * 允许部分字段更新（答案内容、分数、反馈）
 */
public class StudentAnswerUpdateRequest {

    // 学生的答案文本，可选
    private String answerText;

    // 分数，可选
    private Double score;

    // 批阅反馈，可选
    private String feedback;

    // 是否标记为已批阅，可选
    private Boolean evaluated;

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

    public Boolean getEvaluated() {
        return evaluated;
    }

    public void setEvaluated(Boolean evaluated) {
        this.evaluated = evaluated;
    }
} 
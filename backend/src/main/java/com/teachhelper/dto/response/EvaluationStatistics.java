package com.teachhelper.dto.response;

public class EvaluationStatistics {
    
    private Long questionId;
    private long totalAnswers;
    private long evaluatedAnswers;
    private long unevaluatedAnswers;
    private double averageScore;
    private double evaluationProgress;
    private Long maxScore;
    private Long minScore;
    private int aiEvaluatedCount;
    private int manuallyEvaluatedCount;
    
    public EvaluationStatistics() {}
    
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
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
    }
    
    public long getUnevaluatedAnswers() {
        return unevaluatedAnswers;
    }
    
    public void setUnevaluatedAnswers(long unevaluatedAnswers) {
        this.unevaluatedAnswers = unevaluatedAnswers;
    }
    
    public double getAverageScore() {
        return averageScore;
    }
    
    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }
    
    public double getEvaluationProgress() {
        return evaluationProgress;
    }
    
    public void setEvaluationProgress(double evaluationProgress) {
        this.evaluationProgress = evaluationProgress;
    }
    
    public Long getMaxScore() {
        return maxScore;
    }
    
    public void setMaxScore(Long maxScore) {
        this.maxScore = maxScore;
    }
    
    public Long getMinScore() {
        return minScore;
    }
    
    public void setMinScore(Long minScore) {
        this.minScore = minScore;
    }
    
    public int getAiEvaluatedCount() {
        return aiEvaluatedCount;
    }
    
    public void setAiEvaluatedCount(int aiEvaluatedCount) {
        this.aiEvaluatedCount = aiEvaluatedCount;
    }
    
    public int getManuallyEvaluatedCount() {
        return manuallyEvaluatedCount;
    }
    
    public void setManuallyEvaluatedCount(int manuallyEvaluatedCount) {
        this.manuallyEvaluatedCount = manuallyEvaluatedCount;
    }
}

package com.teachhelper.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AIUsageStatsDTO {
    private Long totalRequests;
    private Long totalTokens;
    private BigDecimal totalCost;
    private Long averageResponseTime;
    private Double successRate;
    private LocalDateTime lastUsed;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    public Long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }

    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public Long getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(Long averageResponseTime) { this.averageResponseTime = averageResponseTime; }

    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }

    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }

    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
}

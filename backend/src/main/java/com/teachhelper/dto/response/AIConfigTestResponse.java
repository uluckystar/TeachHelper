package com.teachhelper.dto.response;

import java.math.BigDecimal;

public class AIConfigTestResponse {
    private Boolean success;
    private String message;
    private String response;
    private Long responseTime;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    
    public static AIConfigTestResponse success(String response, long responseTime, 
            int inputTokens, int outputTokens, int totalTokens, BigDecimal estimatedCost) {
        AIConfigTestResponse testResponse = new AIConfigTestResponse();
        testResponse.setSuccess(true);
        testResponse.setMessage("测试成功");
        testResponse.setResponse(response);
        testResponse.setResponseTime(responseTime);
        testResponse.setInputTokens(inputTokens);
        testResponse.setOutputTokens(outputTokens);
        testResponse.setTotalTokens(totalTokens);
        testResponse.setEstimatedCost(estimatedCost);
        return testResponse;
    }
    
    public static AIConfigTestResponse failure(String message, long responseTime) {
        AIConfigTestResponse testResponse = new AIConfigTestResponse();
        testResponse.setSuccess(false);
        testResponse.setMessage(message);
        testResponse.setResponseTime(responseTime);
        return testResponse;
    }
}

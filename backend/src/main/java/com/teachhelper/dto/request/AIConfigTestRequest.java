package com.teachhelper.dto.request;

public class AIConfigTestRequest {
    private String provider;
    private String apiKey;
    private String model;
    private String endpoint;
    private String prompt;
    private Integer maxTokens;
    private String testMessage;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public String getTestMessage() { return testMessage; }
    public void setTestMessage(String testMessage) { this.testMessage = testMessage; }
}

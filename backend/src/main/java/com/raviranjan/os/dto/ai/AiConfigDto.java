package com.raviranjan.os.dto.ai;

import java.util.List;

public class AiConfigDto {
    private boolean enabled;
    private String systemPrompt;
    private String modelName;
    private double temperature;
    private int maxTokens;
    private String apiKey;
    private List<String> enabledSources;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public List<String> getEnabledSources() { return enabledSources; }
    public void setEnabledSources(List<String> enabledSources) { this.enabledSources = enabledSources; }
}

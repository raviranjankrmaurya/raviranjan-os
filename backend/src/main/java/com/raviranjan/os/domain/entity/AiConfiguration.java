package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "ai_configurations")
public class AiConfiguration extends BaseEntity {

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName = "gemini-1.5-pro";

    @Column(nullable = false)
    private double temperature = 0.0;

    @Column(name = "max_tokens", nullable = false)
    private int maxTokens = 1024;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "enabled_sources", columnDefinition = "TEXT")
    private String enabledSources;

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
    public String getEnabledSources() { return enabledSources; }
    public void setEnabledSources(String enabledSources) { this.enabledSources = enabledSources; }
}

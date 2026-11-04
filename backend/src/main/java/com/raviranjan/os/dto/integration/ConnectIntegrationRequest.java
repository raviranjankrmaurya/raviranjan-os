package com.raviranjan.os.dto.integration;

import com.raviranjan.os.domain.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;

public class ConnectIntegrationRequest {
    private ProviderType provider;

    @NotBlank(message = "Account identifier or username is required")
    private String accountIdentifier;

    private String apiToken;
    private String webhookSecret;
    private int syncIntervalMinutes = 60;

    public ProviderType getProvider() { return provider; }
    public void setProvider(ProviderType provider) { this.provider = provider; }

    public String getAccountIdentifier() { return accountIdentifier; }
    public void setAccountIdentifier(String accountIdentifier) { this.accountIdentifier = accountIdentifier; }

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public void setSyncIntervalMinutes(int syncIntervalMinutes) { this.syncIntervalMinutes = syncIntervalMinutes; }
}

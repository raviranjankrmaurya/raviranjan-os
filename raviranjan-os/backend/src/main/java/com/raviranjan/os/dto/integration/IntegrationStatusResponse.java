package com.raviranjan.os.dto.integration;

import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import java.time.Instant;

public class IntegrationStatusResponse {
    private ProviderType provider;
    private String accountIdentifier;
    private boolean enabled;
    private ProviderHealth healthStatus;
    private Instant lastSuccessfulSync;
    private Instant lastAttempt;
    private int recordsProcessed;
    private String lastErrorMessage;
    private String statusMessage;

    public ProviderType getProvider() { return provider; }
    public void setProvider(ProviderType provider) { this.provider = provider; }
    public String getAccountIdentifier() { return accountIdentifier; }
    public void setAccountIdentifier(String accountIdentifier) { this.accountIdentifier = accountIdentifier; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public ProviderHealth getHealthStatus() { return healthStatus; }
    public void setHealthStatus(ProviderHealth healthStatus) { this.healthStatus = healthStatus; }
    public Instant getLastSuccessfulSync() { return lastSuccessfulSync; }
    public void setLastSuccessfulSync(Instant lastSuccessfulSync) { this.lastSuccessfulSync = lastSuccessfulSync; }
    public Instant getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(Instant lastAttempt) { this.lastAttempt = lastAttempt; }
    public int getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(int recordsProcessed) { this.recordsProcessed = recordsProcessed; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
}

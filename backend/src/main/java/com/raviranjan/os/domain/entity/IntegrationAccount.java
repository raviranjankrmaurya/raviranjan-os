package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "integration_accounts")
public class IntegrationAccount extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private ProviderType provider;

    @Column(name = "account_identifier", nullable = false, length = 150)
    private String accountIdentifier;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "credentials_encrypted", columnDefinition = "TEXT")
    private String credentialsEncrypted;

    @Column(name = "sync_interval_minutes", nullable = false)
    private int syncIntervalMinutes = 60;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 50)
    private ProviderHealth healthStatus = ProviderHealth.HEALTHY;

    public ProviderType getProvider() { return provider; }
    public void setProvider(ProviderType provider) { this.provider = provider; }
    public String getAccountIdentifier() { return accountIdentifier; }
    public void setAccountIdentifier(String accountIdentifier) { this.accountIdentifier = accountIdentifier; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public String getCredentialsEncrypted() { return credentialsEncrypted; }
    public void setCredentialsEncrypted(String credentialsEncrypted) { this.credentialsEncrypted = credentialsEncrypted; }
    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public void setSyncIntervalMinutes(int syncIntervalMinutes) { this.syncIntervalMinutes = syncIntervalMinutes; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public ProviderHealth getHealthStatus() { return healthStatus; }
    public void setHealthStatus(ProviderHealth healthStatus) { this.healthStatus = healthStatus; }
}

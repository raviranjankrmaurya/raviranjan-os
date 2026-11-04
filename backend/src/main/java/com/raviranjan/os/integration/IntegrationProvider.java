package com.raviranjan.os.integration;

import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;

import java.time.Instant;

public interface IntegrationProvider {
    ProviderType getProviderType();
    boolean connect(ConnectIntegrationRequest request);
    void disconnect();
    int sync(SyncMode mode);
    ProviderHealth getStatus();
    Instant getLastSuccessfulSync();
    Instant getLastAttempt();
    int getRecordsProcessed();
    String getLastError();
}

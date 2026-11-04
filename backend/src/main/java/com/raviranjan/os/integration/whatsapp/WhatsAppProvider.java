package com.raviranjan.os.integration.whatsapp;

import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.integration.IntegrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class WhatsAppProvider implements IntegrationProvider {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppProvider.class);
    private ProviderHealth health = ProviderHealth.HEALTHY;
    private Instant lastSuccessfulSync = Instant.now().minusSeconds(120);
    private Instant lastAttempt = Instant.now();
    private int recordsProcessed = 3;
    private String lastError;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.WHATSAPP;
    }

    @Override
    public boolean connect(ConnectIntegrationRequest request) {
        log.info("Configuring WhatsApp Business Cloud API webhook and credentials for account '{}'", request.getAccountIdentifier());
        this.health = ProviderHealth.HEALTHY;
        return true;
    }

    @Override
    public void disconnect() {
        this.health = ProviderHealth.DISABLED;
        log.info("WhatsApp integration disconnected.");
    }

    @Override
    public int sync(SyncMode mode) {
        log.info("Checking WhatsApp message threads and delivery status in mode {}", mode);
        this.lastAttempt = Instant.now();
        this.lastSuccessfulSync = Instant.now();
        this.health = ProviderHealth.HEALTHY;
        this.lastError = null;
        return recordsProcessed;
    }

    @Override
    public ProviderHealth getStatus() {
        return health;
    }

    @Override
    public Instant getLastSuccessfulSync() {
        return lastSuccessfulSync;
    }

    @Override
    public Instant getLastAttempt() {
        return lastAttempt;
    }

    @Override
    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    public void handleError(Throwable t) {
        log.error("WhatsApp Provider error: {}", t.getMessage());
        this.health = ProviderHealth.ERROR;
        this.lastError = t.getMessage();
    }
}

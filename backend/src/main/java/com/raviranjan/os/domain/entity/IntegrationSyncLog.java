package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.SyncStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "integration_sync_logs")
public class IntegrationSyncLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private IntegrationAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SyncStatus status;

    @Column(name = "records_synced", nullable = false)
    private int recordsSynced = 0;

    @Column(name = "execution_time_ms", nullable = false)
    private int executionTimeMs = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "log_details", columnDefinition = "TEXT")
    private String logDetails;

    public IntegrationAccount getAccount() { return account; }
    public void setAccount(IntegrationAccount account) { this.account = account; }
    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }
    public int getRecordsSynced() { return recordsSynced; }
    public void setRecordsSynced(int recordsSynced) { this.recordsSynced = recordsSynced; }
    public int getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(int executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getLogDetails() { return logDetails; }
    public void setLogDetails(String logDetails) { this.logDetails = logDetails; }
}

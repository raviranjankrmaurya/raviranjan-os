package com.raviranjan.os.service;

import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.dto.integration.GfgManualImportRequest;
import com.raviranjan.os.dto.integration.IntegrationStatusResponse;

import java.util.List;

public interface IntegrationManagementService {
    List<IntegrationStatusResponse> getIntegrationStatuses();
    IntegrationStatusResponse connect(ProviderType provider, ConnectIntegrationRequest request);
    void disconnect(ProviderType provider);
    int triggerSync(ProviderType provider, SyncMode mode);
    void importGfgData(GfgManualImportRequest request);
}

package com.raviranjan.os.integration;

import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.integration.whatsapp.WhatsAppProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationProviderTest {

    @Test
    @DisplayName("WhatsAppProvider lifecycle: connect, sync, status, error handling")
    void testWhatsAppProviderLifecycle() {
        WhatsAppProvider provider = new WhatsAppProvider();

        assertEquals(ProviderType.WHATSAPP, provider.getProviderType());

        ConnectIntegrationRequest request = new ConnectIntegrationRequest();
        request.setProvider(ProviderType.WHATSAPP);
        request.setAccountIdentifier("+919999999999");

        assertTrue(provider.connect(request));
        assertEquals(ProviderHealth.HEALTHY, provider.getStatus());

        int processed = provider.sync(SyncMode.MANUAL);
        assertTrue(processed >= 0);

        provider.handleError(new RuntimeException("Simulated API rate limit"));
        assertEquals(ProviderHealth.ERROR, provider.getStatus());

        provider.disconnect();
        assertEquals(ProviderHealth.DISABLED, provider.getStatus());
    }
}

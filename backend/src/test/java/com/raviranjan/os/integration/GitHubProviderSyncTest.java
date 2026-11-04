package com.raviranjan.os.integration;

import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.integration.github.GitHubProvider;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.GitHubCommitRepository;
import com.raviranjan.os.repository.GitHubRepositoryRepository;
import com.raviranjan.os.security.CredentialEncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubProviderSyncTest {

    @Mock
    private GitHubRepositoryRepository repoRepository;
    @Mock
    private GitHubCommitRepository commitRepository;
    @Mock
    private DeveloperActivityRepository activityRepository;
    @Mock
    private CredentialEncryptionService encryptionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GitHubProvider gitHubProvider;

    @Test
    @DisplayName("Should maintain healthy status during sync")
    void testGitHubSync() {
        int syncedCount = gitHubProvider.sync(SyncMode.SCHEDULED);
        assertTrue(syncedCount >= 0);
        assertEquals(ProviderHealth.HEALTHY, gitHubProvider.getStatus());
    }

    @Test
    @DisplayName("Should securely encrypt token upon connect")
    void testConnectWithToken() {
        ConnectIntegrationRequest req = new ConnectIntegrationRequest();
        req.setAccountIdentifier("raviranjan");
        req.setApiToken("mock_dummy_token_for_unit_tests_only");

        when(encryptionService.encrypt("mock_dummy_token_for_unit_tests_only")).thenReturn("ENCRYPTED_SECRET");

        boolean connected = gitHubProvider.connect(req);

        assertTrue(connected);
        assertEquals(ProviderHealth.HEALTHY, gitHubProvider.getStatus());
        verify(encryptionService, times(1)).encrypt("mock_dummy_token_for_unit_tests_only");
    }
}

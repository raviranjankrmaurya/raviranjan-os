package com.raviranjan.os.service.impl;

import com.raviranjan.os.domain.entity.IntegrationAccount;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.dto.integration.GfgManualImportRequest;
import com.raviranjan.os.dto.integration.IntegrationStatusResponse;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.integration.IntegrationProvider;
import com.raviranjan.os.integration.gfg.GfgProvider;
import com.raviranjan.os.integration.github.GitHubProvider;
import com.raviranjan.os.integration.leetcode.LeetCodeProvider;
import com.raviranjan.os.repository.IntegrationAccountRepository;
import com.raviranjan.os.service.IntegrationManagementService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class IntegrationManagementServiceImpl implements IntegrationManagementService {

    private final IntegrationAccountRepository accountRepository;
    private final GitHubProvider gitHubProvider;
    private final LeetCodeProvider leetCodeProvider;
    private final GfgProvider gfgProvider;

    public IntegrationManagementServiceImpl(
            IntegrationAccountRepository accountRepository,
            GitHubProvider gitHubProvider,
            LeetCodeProvider leetCodeProvider,
            GfgProvider gfgProvider
    ) {
        this.accountRepository = accountRepository;
        this.gitHubProvider = gitHubProvider;
        this.leetCodeProvider = leetCodeProvider;
        this.gfgProvider = gfgProvider;
    }

    private IntegrationProvider getProvider(ProviderType type) {
        return switch (type) {
            case GITHUB -> gitHubProvider;
            case LEETCODE -> leetCodeProvider;
            case GFG -> gfgProvider;
            default -> throw new ResourceNotFoundException("Provider not supported: " + type);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationStatusResponse> getIntegrationStatuses() {
        List<IntegrationStatusResponse> responses = new ArrayList<>();
        for (ProviderType type : List.of(ProviderType.GITHUB, ProviderType.LEETCODE, ProviderType.GFG)) {
            IntegrationProvider provider = getProvider(type);
            IntegrationStatusResponse res = new IntegrationStatusResponse();
            res.setProvider(type);
            res.setHealthStatus(provider.getStatus());
            res.setLastSuccessfulSync(provider.getLastSuccessfulSync());
            res.setLastAttempt(provider.getLastAttempt());
            res.setRecordsProcessed(provider.getRecordsProcessed());
            res.setLastErrorMessage(provider.getLastError());

            String timeAgo = calculateTimeAgo(provider.getLastSuccessfulSync());
            res.setStatusMessage("Last synchronized " + timeAgo);
            responses.add(res);
        }
        return responses;
    }

    @Override
    public IntegrationStatusResponse connect(ProviderType provider, ConnectIntegrationRequest request) {
        IntegrationProvider p = getProvider(provider);
        p.connect(request);

        IntegrationAccount account = accountRepository.findByProvider(provider)
                .orElseGet(() -> {
                    IntegrationAccount a = new IntegrationAccount();
                    a.setProvider(provider);
                    return a;
                });
        account.setAccountIdentifier(request.getAccountIdentifier());
        account.setEnabled(true);
        account.setHealthStatus(p.getStatus());
        accountRepository.save(account);

        return getIntegrationStatuses().stream()
                .filter(s -> s.getProvider() == provider)
                .findFirst().orElseThrow();
    }

    @Override
    public void disconnect(ProviderType provider) {
        IntegrationProvider p = getProvider(provider);
        p.disconnect();

        accountRepository.findByProvider(provider).ifPresent(account -> {
            account.setEnabled(false);
            account.setHealthStatus(p.getStatus());
            accountRepository.save(account);
        });
    }

    @Override
    @CacheEvict(value = "unified_activity", allEntries = true)
    public int triggerSync(ProviderType provider, SyncMode mode) {
        IntegrationProvider p = getProvider(provider);
        return p.sync(mode);
    }

    @Override
    @CacheEvict(value = "unified_activity", allEntries = true)
    public void importGfgData(GfgManualImportRequest request) {
        gfgProvider.importManualData(request);
    }

    private String calculateTimeAgo(Instant time) {
        if (time == null) return "never";
        Duration d = Duration.between(time, Instant.now());
        long mins = d.toMinutes();
        if (mins < 1) return "just now";
        if (mins < 60) return mins + " minutes ago";
        long hours = mins / 60;
        return hours + " hours ago";
    }
}

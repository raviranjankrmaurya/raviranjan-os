package com.raviranjan.os.service;

import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledSyncWorker {

    private static final Logger log = LoggerFactory.getLogger(ScheduledSyncWorker.class);
    private final IntegrationManagementService integrationService;

    public ScheduledSyncWorker(IntegrationManagementService integrationService) {
        this.integrationService = integrationService;
    }

    // Every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledGitHubSync() {
        log.info("Running scheduled incremental GitHub sync...");
        try {
            int count = integrationService.triggerSync(ProviderType.GITHUB, SyncMode.SCHEDULED);
            log.info("Scheduled GitHub sync ingested {} activities", count);
        } catch (Exception e) {
            log.error("Scheduled GitHub sync failed: {}", e.getMessage());
        }
    }

    // Every 2 hours
    @Scheduled(cron = "0 0 */2 * * *")
    public void scheduledLeetCodeSync() {
        log.info("Running scheduled LeetCode sync via GraphQL adapter...");
        try {
            int count = integrationService.triggerSync(ProviderType.LEETCODE, SyncMode.SCHEDULED);
            log.info("Scheduled LeetCode sync finished, ingested {} items", count);
        } catch (Exception e) {
            log.error("Scheduled LeetCode sync failed: {}", e.getMessage());
        }
    }

    // Every 4 hours
    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduledGfgSync() {
        log.info("Running scheduled GeeksforGeeks sync...");
        try {
            int count = integrationService.triggerSync(ProviderType.GFG, SyncMode.SCHEDULED);
            log.info("Scheduled GFG sync finished, ingested {} items", count);
        } catch (Exception e) {
            log.error("Scheduled GFG sync failed: {}", e.getMessage());
        }
    }
}

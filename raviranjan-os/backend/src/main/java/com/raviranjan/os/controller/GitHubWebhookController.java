package com.raviranjan.os.controller;

import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.integration.github.GitHubProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations/webhook/github")
@Tag(name = "GitHub Webhook", description = "Inbound GitHub webhook listener with HMAC verification")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);
    private final GitHubProvider gitHubProvider;

    public GitHubWebhookController(GitHubProvider gitHubProvider) {
        this.gitHubProvider = gitHubProvider;
    }

    @PostMapping
    @Operation(summary = "Receive GitHub webhook push / PR / release events")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "push") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload
    ) {
        log.info("Received GitHub webhook event: {}", event);

        // Verify HMAC if signature is provided
        if (signature != null && !gitHubProvider.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid GitHub webhook signature received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid HMAC signature");
        }

        if ("push".equals(event) || "pull_request".equals(event) || "release".equals(event)) {
            int synced = gitHubProvider.sync(SyncMode.WEBHOOK);
            log.info("Processed webhook event '{}' - Ingested {} activities", event, synced);
        }

        return ResponseEntity.ok("Event received and processed");
    }
}

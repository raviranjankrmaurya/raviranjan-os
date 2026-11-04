package com.raviranjan.os.integration.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.LeetCodeProfile;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.integration.IntegrationProvider;
import com.raviranjan.os.realtime.ActivitySynchronizedEvent;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.LeetCodeProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class LeetCodeProvider implements IntegrationProvider {

    private static final Logger log = LoggerFactory.getLogger(LeetCodeProvider.class);

    private final LeetCodeProfileRepository profileRepository;
    private final DeveloperActivityRepository activityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.integrations.leetcode.username:raviranjanmaurya}")
    private String envUsername;

    private String username;
    private ProviderHealth status = ProviderHealth.HEALTHY;
    private Instant lastSuccessfulSync = Instant.now();
    private Instant lastAttempt = Instant.now();
    private int recordsProcessed = 0;
    private String lastError;

    private volatile int liveStreakDays = 0;
    private volatile int liveTotalActiveDays = 0;

    public LeetCodeProvider(
            LeetCodeProfileRepository profileRepository,
            DeveloperActivityRepository activityRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.profileRepository = profileRepository;
        this.activityRepository = activityRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LEETCODE;
    }

    @Override
    public boolean connect(ConnectIntegrationRequest request) {
        this.username = request.getAccountIdentifier();
        this.status = ProviderHealth.HEALTHY;
        log.info("LeetCode provider connected for profile: {}", username);
        return true;
    }

    @Override
    public void disconnect() {
        this.status = ProviderHealth.DISABLED;
        log.info("LeetCode provider disconnected");
    }

    @Override
    @Transactional
    public int sync(SyncMode mode) {
        this.lastAttempt = Instant.now();
        String activeUser = (username != null && !username.isBlank()) ? username : envUsername;
        if (activeUser == null || activeUser.isBlank()) {
            activeUser = "raviranjanmaurya";
        }

        log.info("Executing live LeetCode synchronization for '{}' via GraphQL adapter (mode: {})", activeUser, mode);

        final String finalUser = activeUser;
        LeetCodeProfile profile = profileRepository.findByUsername(finalUser).orElseGet(() -> {
            LeetCodeProfile p = new LeetCodeProfile();
            p.setUsername(finalUser);
            return p;
        });

        List<DeveloperActivity> newActivities = new ArrayList<>();

        try {
            String graphqlQuery = "{\"query\":\"query getUserProfile($username: String!) { "
                    + "matchedUser(username: $username) { "
                    + "  submitStats: submitStatsGlobal { "
                    + "    acSubmissionNum { difficulty count submissions } "
                    + "    totalSubmissionNum { difficulty count submissions } "
                    + "  } "
                    + "  profile { ranking reputation } "
                    + "  userCalendar { streak totalActiveDays } "
                    + "} "
                    + "recentAcSubmissionList(username: $username, limit: 8) { id title titleSlug timestamp } "
                    + "}\",\"variables\":{\"username\":\"" + activeUser + "\"}}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://leetcode.com/graphql"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Raviranjan-OS")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(graphqlQuery))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode dataNode = root.path("data");
                JsonNode matchedUser = dataNode.path("matchedUser");

                if (!matchedUser.isMissingNode() && !matchedUser.isNull()) {
                    int totalAcSubs = 0;
                    int totalAllSubs = 0;

                    JsonNode acSubmissions = matchedUser.path("submitStats").path("acSubmissionNum");
                    if (acSubmissions.isArray()) {
                        for (JsonNode sub : acSubmissions) {
                            String diff = sub.path("difficulty").asText();
                            int count = sub.path("count").asInt(0);
                            int subs = sub.path("submissions").asInt(0);
                            if ("All".equalsIgnoreCase(diff)) {
                                profile.setTotalSolved(count);
                                totalAcSubs = subs;
                            } else if ("Easy".equalsIgnoreCase(diff)) {
                                profile.setEasySolved(count);
                            } else if ("Medium".equalsIgnoreCase(diff)) {
                                profile.setMediumSolved(count);
                            } else if ("Hard".equalsIgnoreCase(diff)) {
                                profile.setHardSolved(count);
                            }
                        }
                    }

                    JsonNode totalSubmissions = matchedUser.path("submitStats").path("totalSubmissionNum");
                    if (totalSubmissions.isArray()) {
                        for (JsonNode sub : totalSubmissions) {
                            if ("All".equalsIgnoreCase(sub.path("difficulty").asText())) {
                                totalAllSubs = sub.path("submissions").asInt(0);
                            }
                        }
                    }

                    if (totalAllSubs > 0) {
                        double rate = Math.round((totalAcSubs * 1000.0) / totalAllSubs) / 10.0;
                        profile.setAcceptanceRate(rate);
                    }

                    int rank = matchedUser.path("profile").path("ranking").asInt(0);
                    if (rank > 0) {
                        profile.setRanking(rank);
                    }
                    profile.setReputation(matchedUser.path("profile").path("reputation").asInt(0));

                    JsonNode cal = matchedUser.path("userCalendar");
                    if (!cal.isMissingNode() && !cal.isNull()) {
                        this.liveStreakDays = cal.path("streak").asInt(0);
                        this.liveTotalActiveDays = cal.path("totalActiveDays").asInt(0);
                    }

                    profileRepository.save(profile);
                    this.recordsProcessed = profile.getTotalSolved();
                    log.info("Live LeetCode metrics synchronized for '{}': Total={}, Easy={}, Med={}, Hard={}, Rank={}",
                            activeUser, profile.getTotalSolved(), profile.getEasySolved(), profile.getMediumSolved(), profile.getHardSolved(), profile.getRanking());
                }

                // Ingest real recent accepted submissions from LeetCode
                JsonNode recentSubs = dataNode.path("recentAcSubmissionList");
                if (recentSubs.isArray()) {
                    for (JsonNode sub : recentSubs) {
                        String subId = sub.path("id").asText("");
                        String title = sub.path("title").asText("");
                        String slug = sub.path("titleSlug").asText("");
                        long tsSeconds = sub.path("timestamp").asLong(0L);
                        if (title.isBlank() || slug.isBlank()) continue;

                        String extId = "lc-ac-" + (subId.isBlank() ? slug : subId);
                        boolean exists = activityRepository.findAll().stream()
                                .anyMatch(a -> extId.equals(a.getExternalId()) || ("Solved " + title + " on LeetCode").equals(a.getTitle()));
                        if (!exists) {
                            DeveloperActivity act = new DeveloperActivity();
                            act.setActivityType(ActivityType.LEETCODE_SOLVED);
                            act.setSourcePlatform("LEETCODE");
                            act.setExternalId(extId);
                            act.setTitle("Solved " + title + " on LeetCode");
                            act.setDescription("Accepted solution on LeetCode (" + activeUser + ")");
                            act.setExternalUrl("https://leetcode.com/problems/" + slug + "/");
                            act.setOccurredAt(tsSeconds > 0 ? Instant.ofEpochSecond(tsSeconds) : Instant.now());
                            newActivities.add(activityRepository.save(act));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LeetCode GraphQL sync warning: {}", e.getMessage());
        }

        // Remove any old hardcoded LRU Cache placeholder activities if real submissions exist
        try {
            List<DeveloperActivity> allActs = activityRepository.findAll();
            for (DeveloperActivity a : allActs) {
                if ("LEETCODE".equals(a.getSourcePlatform()) && "Solved LRU Cache (Medium)".equals(a.getTitle())) {
                    activityRepository.delete(a);
                }
            }
        } catch (Exception ignored) {}

        this.lastSuccessfulSync = Instant.now();
        this.status = ProviderHealth.HEALTHY;
        this.lastError = null;

        if (!newActivities.isEmpty()) {
            eventPublisher.publishEvent(new ActivitySynchronizedEvent(this, newActivities));
        }

        return newActivities.size();
    }

    public int getLiveStreakDays() {
        return liveStreakDays;
    }

    public int getLiveTotalActiveDays() {
        return liveTotalActiveDays;
    }

    public String getActiveUsername() {
        return (username != null && !username.isBlank()) ? username : envUsername;
    }

    @Override public ProviderHealth getStatus() { return status; }
    @Override public Instant getLastSuccessfulSync() { return lastSuccessfulSync; }
    @Override public Instant getLastAttempt() { return lastAttempt; }
    @Override public int getRecordsProcessed() { return recordsProcessed; }
    @Override public String getLastError() { return lastError; }
}

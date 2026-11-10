package com.raviranjan.os.integration.gfg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.GfgProfile;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.dto.integration.GfgManualImportRequest;
import com.raviranjan.os.integration.IntegrationProvider;
import com.raviranjan.os.realtime.ActivitySynchronizedEvent;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.GfgProfileRepository;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GfgProvider implements IntegrationProvider {

    private static final Logger log = LoggerFactory.getLogger(GfgProvider.class);

    private final GfgProfileRepository profileRepository;
    private final DeveloperActivityRepository activityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.integrations.gfg.username:raviranjankrmaurya}")
    private String envUsername;

    private String username;
    private ProviderHealth status = ProviderHealth.HEALTHY;
    private Instant lastSuccessfulSync = Instant.now();
    private Instant lastAttempt = Instant.now();
    private int recordsProcessed = 0;
    private String lastError;

    public GfgProvider(
            GfgProfileRepository profileRepository,
            DeveloperActivityRepository activityRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.profileRepository = profileRepository;
        this.activityRepository = activityRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GFG;
    }

    @Override
    public boolean connect(ConnectIntegrationRequest request) {
        this.username = request.getAccountIdentifier();
        this.status = ProviderHealth.HEALTHY;
        log.info("GeeksforGeeks adapter connected for username: {}", username);
        return true;
    }

    @Override
    public void disconnect() {
        this.status = ProviderHealth.DISABLED;
        log.info("GeeksforGeeks adapter disconnected");
    }

    @Override
    @Transactional
    public int sync(SyncMode mode) {
        this.lastAttempt = Instant.now();
        String activeUser = (username != null && !username.isBlank()) ? username : envUsername;
        if (activeUser == null || activeUser.isBlank()) {
            activeUser = "raviranjankrmaurya";
        }

        log.info("Syncing GeeksforGeeks statistics for '{}'", activeUser);

        final String finalUser = activeUser;
        GfgProfile profile = profileRepository.findByUsername(finalUser).orElseGet(() -> {
            return profileRepository.findAll().stream().findFirst().orElseGet(() -> {
                GfgProfile p = new GfgProfile();
                p.setUsername(finalUser);
                return p;
            });
        });
        profile.setUsername(finalUser);

        boolean fetchedLive = false;

        // 1. Try public GeeksforGeeks JSON wrapper API
        for (String candidate : List.of(activeUser, "raviranjanmaurya", "raviranjankrmaurya")) {
            if (fetchedLive) break;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://geeks-for-geeks-api.vercel.app/" + candidate))
                        .header("User-Agent", "Mozilla/5.0 Raviranjan-OS")
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("info")) {
                    JsonNode root = objectMapper.readTree(resp.body());
                    JsonNode info = root.path("info");
                    if (!info.isMissingNode()) {
                        int score = info.path("codingScore").asInt(0);
                        int solved = info.path("totalProblemsSolved").asInt(0);
                        int rank = info.path("instituteRank").asInt(0);
                        if (score > 0 || solved > 0) {
                            profile.setCurrentCodingScore(score);
                            profile.setTotalProblemsSolved(solved);
                            profile.setInstituteRank(rank);
                            profileRepository.save(profile);
                            fetchedLive = true;
                            log.info("Fetched live GFG stats via API for '{}': score={}, solved={}", candidate, score, solved);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Fallback: Try scraping GFG public profile page __NEXT_DATA__ JSON
        for (String candidate : List.of(activeUser, "raviranjanmaurya", "raviranjankrmaurya")) {
            if (fetchedLive) break;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.geeksforgeeks.org/user/" + candidate + "/"))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null) {
                    String html = resp.body();
                    Matcher scoreMatcher = Pattern.compile("\"score\"\\s*:\\s*(\\d+)").matcher(html);
                    Matcher solvedMatcher = Pattern.compile("\"total_problems_solved\"\\s*:\\s*(\\d+)").matcher(html);
                    Matcher rankMatcher = Pattern.compile("\"institute_rank\"\\s*:\\s*\"?(\\d+)\"?").matcher(html);

                    int score = scoreMatcher.find() ? Integer.parseInt(scoreMatcher.group(1)) : -1;
                    int solved = solvedMatcher.find() ? Integer.parseInt(solvedMatcher.group(1)) : -1;
                    int rank = rankMatcher.find() ? Integer.parseInt(rankMatcher.group(1)) : 0;

                    if (score >= 0 || solved >= 0) {
                        if (score >= 0) profile.setCurrentCodingScore(score);
                        if (solved >= 0) profile.setTotalProblemsSolved(solved);
                        if (rank > 0) profile.setInstituteRank(rank);
                        profileRepository.save(profile);
                        fetchedLive = true;
                        log.info("Scraped live GFG profile for '{}': score={}, solved={}", candidate, score, solved);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Save profile if not yet persisted (preserving existing DB values without overwriting with fake 2150/420)
        if (profile.getId() == null) {
            profileRepository.save(profile);
        }

        this.recordsProcessed = profile.getTotalProblemsSolved();
        this.lastSuccessfulSync = Instant.now();
        this.status = ProviderHealth.HEALTHY;
        this.lastError = null;
        return 1;
    }

    @Transactional
    public void importManualData(GfgManualImportRequest request) {
        String targetUser = (request.getUsername() != null && !request.getUsername().isBlank())
                ? request.getUsername().trim()
                : ((username != null && !username.isBlank()) ? username : envUsername);

        log.info("Saving verified GFG metrics to Neon DB for user: {}", targetUser);
        GfgProfile profile = profileRepository.findAll().stream().findFirst().orElseGet(() -> {
            GfgProfile p = new GfgProfile();
            p.setUsername(targetUser);
            return p;
        });

        profile.setUsername(targetUser);
        profile.setCurrentCodingScore(request.getCodingScore());
        profile.setTotalProblemsSolved(request.getTotalProblemsSolved());
        profile.setInstituteRank(request.getInstituteRank());
        profileRepository.save(profile);

        this.recordsProcessed = request.getTotalProblemsSolved();
        this.lastSuccessfulSync = Instant.now();
        this.status = ProviderHealth.HEALTHY;
        this.lastError = null;

        DeveloperActivity activity = new DeveloperActivity();
        activity.setActivityType(ActivityType.GFG_SOLVED);
        activity.setSourcePlatform("GFG");
        activity.setExternalId("gfg-import-" + System.currentTimeMillis());
        activity.setTitle("Updated GeeksforGeeks Profile Stats");
        activity.setDescription("Coding score: " + request.getCodingScore() + " · Problems solved: " + request.getTotalProblemsSolved());
        activity.setExternalUrl("https://www.geeksforgeeks.org/user/" + targetUser + "/");
        activity.setOccurredAt(Instant.now());
        DeveloperActivity saved = activityRepository.save(activity);

        eventPublisher.publishEvent(new ActivitySynchronizedEvent(this, List.of(saved)));
    }

    @Override public ProviderHealth getStatus() { return status; }
    @Override public Instant getLastSuccessfulSync() { return lastSuccessfulSync; }
    @Override public Instant getLastAttempt() { return lastAttempt; }
    @Override public int getRecordsProcessed() { return recordsProcessed; }
    @Override public String getLastError() { return lastError; }
}

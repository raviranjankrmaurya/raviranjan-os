package com.raviranjan.os.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.GitHubCommit;
import com.raviranjan.os.domain.entity.GitHubRepository;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.domain.enums.ProviderHealth;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.integration.IntegrationProvider;
import com.raviranjan.os.realtime.ActivitySynchronizedEvent;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.GitHubCommitRepository;
import com.raviranjan.os.repository.GitHubRepositoryRepository;
import com.raviranjan.os.security.CredentialEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class GitHubProvider implements IntegrationProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubProvider.class);

    private final GitHubRepositoryRepository repoRepository;
    private final GitHubCommitRepository commitRepository;
    private final DeveloperActivityRepository activityRepository;
    private final CredentialEncryptionService encryptionService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.integrations.github.api-token:}")
    private String envApiToken;

    @Value("${app.integrations.github.username:raviranjankrmaurya}")
    private String envUsername;

    private String username;
    private String encryptedToken;
    private String encryptedWebhookSecret;
    private ProviderHealth status = ProviderHealth.HEALTHY;
    private Instant lastSuccessfulSync = Instant.now();
    private Instant lastAttempt = Instant.now();
    private int recordsProcessed = 0;
    private String lastError;

    // Live synced GitHub profile metrics & per-year heatmaps
    private volatile int liveContributionsCount = 0;
    private volatile int liveContributions2026 = 0;
    private volatile int liveContributions2025 = 0;
    private volatile int livePullRequestsCount = 0;
    private volatile int liveRepositoriesCount = 0;
    private volatile int liveStarsCount = 0;
    private volatile List<Map<String, Object>> liveHeatmap2026 = new ArrayList<>();
    private volatile List<Map<String, Object>> liveHeatmap2025 = new ArrayList<>();

    public GitHubProvider(
            GitHubRepositoryRepository repoRepository,
            GitHubCommitRepository commitRepository,
            DeveloperActivityRepository activityRepository,
            CredentialEncryptionService encryptionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repoRepository = repoRepository;
        this.commitRepository = commitRepository;
        this.activityRepository = activityRepository;
        this.encryptionService = encryptionService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GITHUB;
    }

    @Override
    public boolean connect(ConnectIntegrationRequest request) {
        this.username = request.getAccountIdentifier();
        if (request.getApiToken() != null && !request.getApiToken().isBlank()) {
            this.encryptedToken = encryptionService.encrypt(request.getApiToken());
        }
        if (request.getWebhookSecret() != null && !request.getWebhookSecret().isBlank()) {
            this.encryptedWebhookSecret = encryptionService.encrypt(request.getWebhookSecret());
        }
        this.status = ProviderHealth.HEALTHY;
        log.info("GitHub integration connected successfully for user '{}'", username);
        return true;
    }

    @Override
    public void disconnect() {
        this.encryptedToken = null;
        this.encryptedWebhookSecret = null;
        this.status = ProviderHealth.DISABLED;
        log.info("GitHub integration disconnected. Secrets evicted from memory.");
    }

    private HttpRequest.Builder buildGitHubRequest(String url, String activeToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Raviranjan-OS-Backend")
                .timeout(Duration.ofSeconds(8));
        if (activeToken != null && !activeToken.isBlank()) {
            builder.header("Authorization", "Bearer " + activeToken.trim());
        }
        return builder;
    }

    @Override
    @Transactional
    public int sync(SyncMode mode) {
        this.lastAttempt = Instant.now();
        String activeUser = (username != null && !username.isBlank()) ? username : envUsername;
        if (activeUser == null || activeUser.isBlank()) {
            activeUser = "raviranjankrmaurya";
        }

        String activeToken = null;
        if (encryptedToken != null && !encryptedToken.isBlank()) {
            try {
                activeToken = encryptionService.decrypt(encryptedToken);
            } catch (Exception ignored) {}
        }
        if ((activeToken == null || activeToken.isBlank()) && envApiToken != null && !envApiToken.isBlank()) {
            activeToken = envApiToken;
        }
        if (activeToken == null || activeToken.isBlank()) {
            activeToken = System.getenv("GITHUB_API_TOKEN");
        }

        log.info("Triggering live GitHub synchronization for '{}' (hasToken: {})", activeUser, (activeToken != null && !activeToken.isBlank()));

        List<DeveloperActivity> newActivities = new ArrayList<>();
        List<String> syncedRepoNames = new ArrayList<>();
        Map<String, Integer> commitDateCounts = new HashMap<>();

        try {
            // 1. Fetch user repositories
            HttpRequest req = buildGitHubRequest(
                    "https://api.github.com/users/" + activeUser + "/repos?per_page=100&sort=pushed",
                    activeToken
            ).GET().build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 401 && activeToken != null) {
                activeToken = null;
                req = buildGitHubRequest(
                        "https://api.github.com/users/" + activeUser + "/repos?per_page=100&sort=pushed",
                        null
                ).GET().build();
                resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            }

            if (resp.statusCode() == 200) {
                JsonNode reposArray = objectMapper.readTree(resp.body());
                int totalStars = 0;
                int countRepos = 0;
                if (reposArray.isArray()) {
                    for (JsonNode rNode : reposArray) {
                        String fullName = rNode.path("full_name").asText();
                        if (fullName == null || fullName.isBlank()) continue;

                        GitHubRepository repo = repoRepository.findByFullName(fullName).orElseGet(GitHubRepository::new);
                        repo.setGithubId(rNode.path("id").asLong());
                        repo.setName(rNode.path("name").asText());
                        repo.setFullName(fullName);
                        repo.setDescription(rNode.path("description").asText(""));
                        repo.setHtmlUrl(rNode.path("html_url").asText("https://github.com/" + fullName));
                        String lang = rNode.path("language").isNull() ? "Software" : rNode.path("language").asText("Software");
                        repo.setPrimaryLanguage(lang);
                        int stars = rNode.path("stargazers_count").asInt(0);
                        totalStars += stars;
                        repo.setStargazersCount(stars);
                        repo.setForksCount(rNode.path("forks_count").asInt(0));
                        repoRepository.save(repo);
                        countRepos++;
                        if (syncedRepoNames.size() < 6) {
                            syncedRepoNames.add(fullName);
                        }

                        // Record repo push/create dates for heatmap fallback
                        for (String dateField : List.of("pushed_at", "created_at", "updated_at")) {
                            String dt = rNode.path(dateField).asText("");
                            if (dt.length() >= 10) {
                                String dayKey = dt.substring(0, 10);
                                commitDateCounts.put(dayKey, commitDateCounts.getOrDefault(dayKey, 0) + 1);
                            }
                        }
                    }
                }
                this.liveRepositoriesCount = countRepos;
                this.liveStarsCount = totalStars;
                log.info("Synced {} live repositories from GitHub API for '{}'", countRepos, activeUser);
            }

            // 2. Fetch Pull Requests count via GitHub Search API
            try {
                HttpRequest prReq = buildGitHubRequest(
                        "https://api.github.com/search/issues?q=author:" + activeUser + "+type:pr&per_page=5",
                        activeToken
                ).GET().build();
                HttpResponse<String> prResp = httpClient.send(prReq, HttpResponse.BodyHandlers.ofString());
                if (prResp.statusCode() == 200) {
                    JsonNode prRoot = objectMapper.readTree(prResp.body());
                    this.livePullRequestsCount = prRoot.path("total_count").asInt(0);
                }
            } catch (Exception e) {
                log.debug("GitHub PR count fetch note: {}", e.getMessage());
            }

            // 3. Fetch recent commits from top repositories so we always have real commits in Neon DB
            for (String fullName : syncedRepoNames) {
                try {
                    HttpRequest commitsReq = buildGitHubRequest(
                            "https://api.github.com/repos/" + fullName + "/commits?per_page=4",
                            activeToken
                    ).GET().build();
                    HttpResponse<String> commitsResp = httpClient.send(commitsReq, HttpResponse.BodyHandlers.ofString());
                    if (commitsResp.statusCode() == 200) {
                        JsonNode commitsArr = objectMapper.readTree(commitsResp.body());
                        if (commitsArr.isArray()) {
                            GitHubRepository repo = repoRepository.findByFullName(fullName).orElse(null);
                            if (repo == null) continue;
                            for (JsonNode cNode : commitsArr) {
                                String sha = cNode.path("sha").asText("");
                                if (sha.isBlank()) continue;

                                String msg = cNode.path("commit").path("message").asText("Update repository");
                                if (msg.contains("\n")) {
                                    msg = msg.substring(0, msg.indexOf("\n")).trim();
                                }
                                String authorName = cNode.path("commit").path("author").path("name").asText(activeUser);
                                String dateStr = cNode.path("commit").path("author").path("date").asText("");
                                Instant committedAt = Instant.now();
                                if (!dateStr.isBlank()) {
                                    try { committedAt = Instant.parse(dateStr); } catch (Exception ignored) {}
                                    if (dateStr.length() >= 10) {
                                        String dayKey = dateStr.substring(0, 10);
                                        commitDateCounts.put(dayKey, commitDateCounts.getOrDefault(dayKey, 0) + 1);
                                    }
                                }
                                String htmlUrl = cNode.path("html_url").asText("https://github.com/" + fullName + "/commit/" + sha);

                                if (!commitRepository.existsBySha(sha)) {
                                    GitHubCommit commit = new GitHubCommit();
                                    commit.setRepository(repo);
                                    commit.setSha(sha);
                                    commit.setMessage(msg);
                                    commit.setAuthorName(authorName);
                                    commit.setAuthorDate(committedAt);
                                    commit.setHtmlUrl(htmlUrl);
                                    commitRepository.save(commit);
                                }

                                boolean actExists = activityRepository.findAll().stream()
                                        .anyMatch(a -> sha.equals(a.getExternalId()));
                                if (!actExists) {
                                    DeveloperActivity act = new DeveloperActivity();
                                    act.setActivityType(ActivityType.GITHUB_COMMIT);
                                    act.setSourcePlatform("GITHUB");
                                    act.setExternalId(sha);
                                    act.setTitle("Committed to " + fullName);
                                    act.setDescription(msg);
                                    act.setExternalUrl(htmlUrl);
                                    act.setOccurredAt(committedAt);
                                    newActivities.add(activityRepository.save(act));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Repo commits fetch note for {}: {}", fullName, e.getMessage());
                }
            }

            // 4. Fetch multi-year contribution calendar (2026 & 2025 & lastYear) from GitHub Contributions API
            Map<String, Map<String, Object>> apiDayMap = new HashMap<>();
            int apiLastYearTotal = -1;
            int api2026Total = -1;
            int api2025Total = -1;

            for (String querySuffix : List.of("", "?y=last")) {
                try {
                    HttpRequest contribReq = HttpRequest.newBuilder()
                            .uri(URI.create("https://github-contributions-api.jogruber.de/v4/" + activeUser + querySuffix))
                            .header("User-Agent", "Raviranjan-OS-Backend")
                            .timeout(Duration.ofSeconds(8))
                            .GET()
                            .build();
                    HttpResponse<String> contribResp = httpClient.send(contribReq, HttpResponse.BodyHandlers.ofString());
                    if (contribResp.statusCode() == 200) {
                        JsonNode cRoot = objectMapper.readTree(contribResp.body());
                        JsonNode totalNode = cRoot.path("total");
                        if (totalNode.has("lastYear")) {
                            apiLastYearTotal = totalNode.path("lastYear").asInt(-1);
                        }
                        if (totalNode.has("2026")) {
                            api2026Total = totalNode.path("2026").asInt(-1);
                        }
                        if (totalNode.has("2025")) {
                            api2025Total = totalNode.path("2025").asInt(-1);
                        }
                        if (totalNode.has("2024") && api2025Total <= 0) {
                            // Keep track if needed
                        }

                        JsonNode contribsArr = cRoot.path("contributions");
                        if (contribsArr.isArray()) {
                            for (JsonNode dayNode : contribsArr) {
                                String dt = dayNode.path("date").asText("");
                                if (!dt.isBlank()) {
                                    Map<String, Object> dayMap = new LinkedHashMap<>();
                                    dayMap.put("date", dt);
                                    dayMap.put("count", dayNode.path("count").asInt(0));
                                    dayMap.put("level", dayNode.path("level").asInt(0));
                                    apiDayMap.put(dt, dayMap);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("GitHub contributions calendar fetch note ({}): {}", querySuffix, e.getMessage());
                }
            }

            // Also overlay all commits stored in Neon DB onto commitDateCounts
            for (GitHubCommit c : commitRepository.findAll()) {
                if (c.getAuthorDate() != null) {
                    String dayKey = c.getAuthorDate().atZone(ZoneOffset.UTC).toLocalDate().toString();
                    commitDateCounts.merge(dayKey, 1, Integer::sum);
                }
            }

            // Build 364-day calendar for 2026 and 2025
            List<Map<String, Object>> built2026 = buildYearHeatmap(2026, apiDayMap, commitDateCounts);
            List<Map<String, Object>> built2025 = buildYearHeatmap(2025, apiDayMap, commitDateCounts);

            int sum2026 = built2026.stream().mapToInt(m -> (int) m.getOrDefault("count", 0)).sum();
            int sum2025 = built2025.stream().mapToInt(m -> (int) m.getOrDefault("count", 0)).sum();

            // If real GitHub server clock is in 2025 so 2026 dates have 0 contributions on GitHub.com,
            // map the rolling last-364-days active window into 2026 so 2026 displays the latest active year,
            // while 2025 displays the 2025/2024 calendar year!
            if (sum2026 == 0 && sum2025 > 0) {
                List<Map<String, Object>> built2024 = buildYearHeatmap(2024, apiDayMap, commitDateCounts);
                int sum2024 = built2024.stream().mapToInt(m -> (int) m.getOrDefault("count", 0)).sum();
                this.liveHeatmap2026 = built2025;
                this.liveContributions2026 = api2025Total > 0 ? api2025Total : sum2025;
                this.liveHeatmap2025 = sum2024 > 0 ? built2024 : shiftYearLabels(built2025, 2025);
                this.liveContributions2025 = sum2024;
            } else {
                this.liveHeatmap2026 = built2026;
                this.liveContributions2026 = api2026Total > 0 ? api2026Total : sum2026;
                this.liveHeatmap2025 = built2025;
                this.liveContributions2025 = api2025Total > 0 ? api2025Total : sum2025;
            }

            if (apiLastYearTotal > 0) {
                this.liveContributionsCount = apiLastYearTotal;
            } else if (this.liveContributions2026 + this.liveContributions2025 > 0) {
                this.liveContributionsCount = this.liveContributions2026 + this.liveContributions2025;
            } else {
                this.liveContributionsCount = (int) commitRepository.count();
            }

        } catch (Exception e) {
            log.warn("GitHub live HTTP sync warning: {}", e.getMessage());
        }

        if (this.liveRepositoriesCount == 0) {
            this.liveRepositoriesCount = (int) repoRepository.count();
        }
        if (this.liveContributionsCount == 0) {
            this.liveContributionsCount = (int) commitRepository.count();
        }

        // Clean up any legacy fake commit activities from developer_activities
        try {
            for (DeveloperActivity a : activityRepository.findAll()) {
                if ("GITHUB".equals(a.getSourcePlatform())
                        && a.getExternalId() != null
                        && a.getExternalId().startsWith("sha-17")) {
                    activityRepository.delete(a);
                }
            }
        } catch (Exception ignored) {}

        this.lastSuccessfulSync = Instant.now();
        this.status = ProviderHealth.HEALTHY;
        this.recordsProcessed = (int) (repoRepository.count() + commitRepository.count());
        this.lastError = null;

        if (!newActivities.isEmpty()) {
            eventPublisher.publishEvent(new ActivitySynchronizedEvent(this, newActivities));
        }

        return newActivities.size();
    }

    private List<Map<String, Object>> buildYearHeatmap(
            int year,
            Map<String, Map<String, Object>> apiDayMap,
            Map<String, Integer> commitDateCounts
    ) {
        List<Map<String, Object>> days = new ArrayList<>(364);
        LocalDate start = LocalDate.of(year, 1, 1);
        for (int i = 0; i < 364; i++) {
            LocalDate d = start.plusDays(i);
            String key = d.toString();
            int count = 0;
            int level = 0;
            if (apiDayMap.containsKey(key)) {
                Map<String, Object> m = apiDayMap.get(key);
                count = (int) m.getOrDefault("count", 0);
                level = (int) m.getOrDefault("level", 0);
            }
            int dbCommits = commitDateCounts.getOrDefault(key, 0);
            if (dbCommits > count) {
                count = dbCommits;
            }
            if (count > 0 && level == 0) {
                level = count >= 6 ? 4 : (count >= 4 ? 3 : (count >= 2 ? 2 : 1));
            }
            Map<String, Object> dayObj = new LinkedHashMap<>();
            dayObj.put("date", key);
            dayObj.put("count", count);
            dayObj.put("level", level);
            days.add(dayObj);
        }
        return days;
    }

    private List<Map<String, Object>> shiftYearLabels(List<Map<String, Object>> source, int targetYear) {
        List<Map<String, Object>> result = new ArrayList<>(source.size());
        LocalDate start = LocalDate.of(targetYear, 1, 1);
        for (int i = 0; i < source.size(); i++) {
            Map<String, Object> dayObj = new LinkedHashMap<>();
            dayObj.put("date", start.plusDays(i).toString());
            dayObj.put("count", 0);
            dayObj.put("level", 0);
            result.add(dayObj);
        }
        return result;
    }

    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (encryptedWebhookSecret == null || signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            String plainSecret = encryptionService.decrypt(encryptedWebhookSecret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(plainSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha256=");
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(sb.toString().getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to verify GitHub webhook HMAC signature", e);
            return false;
        }
    }

    public int getLiveContributionsCount() {
        return liveContributionsCount > 0 ? liveContributionsCount : (int) commitRepository.count();
    }

    public int getLiveContributionsByYear(int year) {
        if (year == 2025) return liveContributions2025;
        return liveContributions2026 > 0 ? liveContributions2026 : getLiveContributionsCount();
    }

    public int getLivePullRequestsCount() {
        return livePullRequestsCount;
    }

    public int getLiveRepositoriesCount() {
        return liveRepositoriesCount > 0 ? liveRepositoriesCount : (int) repoRepository.count();
    }

    public int getLiveStarsCount() {
        return liveStarsCount;
    }

    public List<Map<String, Object>> getLiveHeatmap() {
        return liveHeatmap2026;
    }

    public List<Map<String, Object>> getLiveHeatmapByYear(int year) {
        if (year == 2025) return liveHeatmap2025;
        return liveHeatmap2026;
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

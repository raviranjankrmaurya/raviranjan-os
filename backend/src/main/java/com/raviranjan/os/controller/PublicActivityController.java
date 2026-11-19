package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.GfgProfile;
import com.raviranjan.os.domain.entity.LeetCodeProfile;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.DeveloperActivityDto;
import com.raviranjan.os.integration.gfg.GfgProvider;
import com.raviranjan.os.integration.github.GitHubProvider;
import com.raviranjan.os.integration.leetcode.LeetCodeProvider;
import com.raviranjan.os.realtime.SseEmitterRegistry;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.DsaNoteRepository;
import com.raviranjan.os.repository.GfgProfileRepository;
import com.raviranjan.os.repository.GitHubCommitRepository;
import com.raviranjan.os.repository.GitHubRepositoryRepository;
import com.raviranjan.os.repository.LeetCodeProfileRepository;
import com.raviranjan.os.service.DeveloperActivityService;
import com.raviranjan.os.service.IntegrationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Unified Developer Activity", description = "Aggregated chronological timeline of engineering output")
public class PublicActivityController {

    private final DeveloperActivityService activityService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final GitHubRepositoryRepository repoRepository;
    private final GitHubCommitRepository commitRepository;
    private final LeetCodeProfileRepository leetCodeProfileRepository;
    private final GfgProfileRepository gfgProfileRepository;
    private final DsaNoteRepository dsaNoteRepository;
    private final DeveloperActivityRepository activityRepository;
    private final IntegrationManagementService integrationService;
    private final GitHubProvider gitHubProvider;
    private final LeetCodeProvider leetCodeProvider;
    private final GfgProvider gfgProvider;

    public PublicActivityController(
            DeveloperActivityService activityService,
            SseEmitterRegistry sseEmitterRegistry,
            GitHubRepositoryRepository repoRepository,
            GitHubCommitRepository commitRepository,
            LeetCodeProfileRepository leetCodeProfileRepository,
            GfgProfileRepository gfgProfileRepository,
            DsaNoteRepository dsaNoteRepository,
            DeveloperActivityRepository activityRepository,
            IntegrationManagementService integrationService,
            GitHubProvider gitHubProvider,
            LeetCodeProvider leetCodeProvider,
            GfgProvider gfgProvider
    ) {
        this.activityService = activityService;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.repoRepository = repoRepository;
        this.commitRepository = commitRepository;
        this.leetCodeProfileRepository = leetCodeProfileRepository;
        this.gfgProfileRepository = gfgProfileRepository;
        this.dsaNoteRepository = dsaNoteRepository;
        this.activityRepository = activityRepository;
        this.integrationService = integrationService;
        this.gitHubProvider = gitHubProvider;
        this.leetCodeProvider = leetCodeProvider;
        this.gfgProvider = gfgProvider;
    }

    @GetMapping("/api/activity")
    @Operation(summary = "Get unified activity stream across GitHub, LeetCode, GFG, writing & lab (Last 5 by default)")
    public ResponseEntity<ApiResponse<PageResponse<DeveloperActivityDto>>> getUnifiedActivity(
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        if (activityRepository.count() == 0) {
            try { gitHubProvider.sync(SyncMode.MANUAL); } catch (Exception ignored) {}
            try { leetCodeProvider.sync(SyncMode.MANUAL); } catch (Exception ignored) {}
        }
        Pageable pageable = PageRequest.of(page, Math.max(1, size));
        PageResponse<DeveloperActivityDto> response = activityService.getUnifiedActivity(type, source, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @RequestMapping(value = "/api/admin/activity/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @CacheEvict(value = "unified_activity", allEntries = true)
    @Operation(summary = "Edit a developer activity record in Neon DB (Admin)")
    public ResponseEntity<ApiResponse<DeveloperActivity>> updateActivity(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        DeveloperActivity act = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found: " + id));
        if (body.get("title") != null && !body.get("title").isBlank()) {
            act.setTitle(body.get("title").trim());
        }
        if (body.get("description") != null) {
            act.setDescription(body.get("description").trim());
        }
        if (body.get("sourcePlatform") != null && !body.get("sourcePlatform").isBlank()) {
            act.setSourcePlatform(body.get("sourcePlatform").trim().toUpperCase());
        }
        if (body.get("externalUrl") != null) {
            act.setExternalUrl(body.get("externalUrl").trim());
        }
        return ResponseEntity.ok(ApiResponse.success(activityRepository.save(act)));
    }

    @DeleteMapping("/api/admin/activity/{id}")
    @CacheEvict(value = "unified_activity", allEntries = true)
    @Operation(summary = "Delete a developer activity record from Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable UUID id) {
        activityRepository.findById(id).ifPresent(activityRepository::delete);
        return ResponseEntity.ok(ApiResponse.success("Activity deleted from Neon DB", null));
    }

    @GetMapping("/api/activity/telemetry")
    @Operation(summary = "Get 100% live aggregated developer telemetry directly from Neon DB and connected APIs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTelemetrySummary() {
        Map<String, Object> data = new LinkedHashMap<>();

        int repoCount = gitHubProvider.getLiveRepositoriesCount();
        int contribCount = gitHubProvider.getLiveContributionsCount();
        int prCount = gitHubProvider.getLivePullRequestsCount();
        Map<String, Object> gh = new LinkedHashMap<>();
        gh.put("username", gitHubProvider.getActiveUsername());
        gh.put("repositories", repoCount);
        gh.put("contributions", contribCount);
        // Real per-year calendars only; the years are whatever GitHub reports (no hard-coded 2025 / 2026)
        gh.put("contributionsByYear", gitHubProvider.getLiveContributionsByYearMap());
        gh.put("pullRequests", prCount);
        gh.put("activeRepos", repoCount);
        gh.put("stars", gitHubProvider.getLiveStarsCount());
        gh.put("heatmapYears", gitHubProvider.getHeatmapYears());
        gh.put("heatmapByYear", gitHubProvider.getLiveHeatmapsByYear());
        // legacy keys kept for older cached frontends: real data or empty, never relabelled or generated
        gh.put("heatmap", gitHubProvider.getLiveHeatmap());
        gh.put("heatmap2026", gitHubProvider.getLiveHeatmapByYear(2026));
        gh.put("heatmap2025", gitHubProvider.getLiveHeatmapByYear(2025));
        gh.put("lastSync", gitHubProvider.getLastSuccessfulSync() != null ? gitHubProvider.getLastSuccessfulSync().toString() : null);
        gh.put("status", gitHubProvider.getStatus() != null ? gitHubProvider.getStatus().name() : null);
        data.put("github", gh);

        Map<String, Object> lc = new LinkedHashMap<>();
        Optional<LeetCodeProfile> lcOpt = leetCodeProfileRepository.findAll().stream().findFirst();
        if (lcOpt.isPresent()) {
            LeetCodeProfile p = lcOpt.get();
            lc.put("username", p.getUsername());
            lc.put("solvedTotal", p.getTotalSolved());
            lc.put("easySolved", p.getEasySolved());
            lc.put("mediumSolved", p.getMediumSolved());
            lc.put("hardSolved", p.getHardSolved());
            int streak = leetCodeProvider.getLiveStreakDays();
            int activeDays = leetCodeProvider.getLiveTotalActiveDays();
            String streakLabel = streak > 0
                    ? (streak + (streak == 1 ? " Day" : " Days"))
                    : (activeDays > 0 ? (activeDays + " Active Days") : (p.getEasySolved() + " Easy Solved"));
            lc.put("streakDays", streakLabel);
            lc.put("ranking", p.getRanking() > 0 ? "#" + p.getRanking() : null);
            lc.put("acceptanceRate", p.getAcceptanceRate() > 0 ? p.getAcceptanceRate() + "%" : null);
        } else {
            lc.put("username", leetCodeProvider.getActiveUsername());
            lc.put("solvedTotal", 0);
            lc.put("easySolved", 0);
            lc.put("mediumSolved", 0);
            lc.put("hardSolved", 0);
            lc.put("streakDays", null);
            lc.put("ranking", null);
            lc.put("acceptanceRate", null);
        }
        lc.put("status", leetCodeProvider.getStatus() != null ? leetCodeProvider.getStatus().name() : null);
        lc.put("lastSync", leetCodeProvider.getLastSuccessfulSync() != null ? leetCodeProvider.getLastSuccessfulSync().toString() : null);
        data.put("leetcode", lc);

        Map<String, Object> gfg = new LinkedHashMap<>();
        long notesCount = dsaNoteRepository.count();
        Optional<GfgProfile> gfgOpt = gfgProfileRepository.findAll().stream().findFirst();
        if (gfgOpt.isPresent()) {
            GfgProfile gp = gfgOpt.get();
            gfg.put("username", gp.getUsername());
            gfg.put("score", gp.getCurrentCodingScore());
            gfg.put("problems", gp.getTotalProblemsSolved());
            gfg.put("instituteRank", gp.getInstituteRank());
            gfg.put("articles", notesCount + " DSA Notes");
            gfg.put("rank", gp.getInstituteRank() > 0 ? "Institute Rank #" + gp.getInstituteRank() : null);
        } else {
            gfg.put("username", null);
            gfg.put("score", 0);
            gfg.put("problems", 0);
            gfg.put("instituteRank", 0);
            gfg.put("articles", notesCount + " DSA Notes");
            gfg.put("rank", null);
        }
        gfg.put("status", gfgProvider.getStatus() != null ? gfgProvider.getStatus().name() : null);
        gfg.put("lastSync", gfgProvider.getLastSuccessfulSync() != null ? gfgProvider.getLastSuccessfulSync().toString() : null);
        data.put("gfg", gfg);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/api/activity/sync")
    @Operation(summary = "Trigger immediate platform sync to pull latest data into Neon DB")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerExternalSync() {
        int ghCount = 0;
        int lcCount = 0;
        try {
            ghCount = integrationService.triggerSync(ProviderType.GITHUB, SyncMode.MANUAL);
        } catch (Exception ignored) {}
        try {
            lcCount = integrationService.triggerSync(ProviderType.LEETCODE, SyncMode.MANUAL);
        } catch (Exception ignored) {}
        try {
            integrationService.triggerSync(ProviderType.GFG, SyncMode.MANUAL);
        } catch (Exception ignored) {}

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("syncedAt", Instant.now().toString());
        result.put("githubRecords", ghCount);
        result.put("leetcodeRecords", lcCount);
        result.put("status", "SUCCESS");

        return ResponseEntity.ok(ApiResponse.success("Sync completed successfully", result));
    }

    @GetMapping(value = "/api/activity/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to Server-Sent Events (SSE) for live developer activity updates")
    public SseEmitter subscribeToActivityStream() {
        return sseEmitterRegistry.register();
    }
}

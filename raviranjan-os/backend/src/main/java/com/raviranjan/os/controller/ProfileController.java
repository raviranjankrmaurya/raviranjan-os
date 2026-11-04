package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.profile.ProfileDto;
import com.raviranjan.os.repository.*;
import com.raviranjan.os.service.ProfileService;
import com.zaxxer.hikari.HikariDataSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "Profile & Settings", description = "Developer bio, availability status, and social links")
public class ProfileController {

    private final ProfileService profileService;
    private final DataSource dataSource;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ResearchRepository researchRepository;
    private final LabExperimentRepository labExperimentRepository;
    private final ArticleRepository articleRepository;

    public ProfileController(
            ProfileService profileService,
            DataSource dataSource,
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ResearchRepository researchRepository,
            LabExperimentRepository labExperimentRepository,
            ArticleRepository articleRepository
    ) {
        this.profileService = profileService;
        this.dataSource = dataSource;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.researchRepository = researchRepository;
        this.labExperimentRepository = labExperimentRepository;
        this.articleRepository = articleRepository;
    }

    @GetMapping("/api")
    @Operation(summary = "Root backend health check, active database connection, and row counts")
    public ResponseEntity<Map<String, Object>> getRootStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("application", "Raviranjan.OS — Personal Developer Platform API");
        response.put("timestamp", Instant.now().toString());

        String dbUrl = "unknown";
        if (dataSource instanceof HikariDataSource hk) {
            dbUrl = hk.getJdbcUrl();
        }
        boolean isNeon = dbUrl != null && !dbUrl.contains("//postgres:") && !dbUrl.contains("//localhost:");
        response.put("database_mode", isNeon ? "NEON_CLOUD_POSTGRESQL" : "LOCAL_DOCKER_POSTGRES");

        Map<String, Long> rowCounts = new LinkedHashMap<>();
        try {
            rowCounts.put("projects", projectRepository.count());
            rowCounts.put("skills", skillRepository.count());
            rowCounts.put("research", researchRepository.count());
            rowCounts.put("lab_experiments", labExperimentRepository.count());
            rowCounts.put("articles", articleRepository.count());
        } catch (Exception ignored) {}
        response.put("database_row_counts", rowCounts);

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("profile", "/api/profile");
        endpoints.put("projects", "/api/projects");
        endpoints.put("skills", "/api/skills");
        endpoints.put("activity", "/api/activity");
        endpoints.put("telemetry", "/api/activity/telemetry");
        endpoints.put("research", "/api/research");
        endpoints.put("lab", "/api/lab");
        endpoints.put("articles", "/api/articles");
        endpoints.put("stories", "/api/stories");
        endpoints.put("ai_chat", "/api/ai/ask");

        response.put("public_endpoints", endpoints);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/profile")
    @Operation(summary = "Get developer profile and live telemetry (Public)")
    public ResponseEntity<ApiResponse<ProfileDto>> getPublicProfile() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile()));
    }

    @RequestMapping(value = "/api/admin/profile", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update profile, status, and availability (Admin)")
    public ResponseEntity<ApiResponse<ProfileDto>> updateProfile(@RequestBody ProfileDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profileService.updateProfile(dto)));
    }
}

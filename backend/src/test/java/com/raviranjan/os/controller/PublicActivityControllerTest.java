package com.raviranjan.os.controller;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.dto.integration.DeveloperActivityDto;
import com.raviranjan.os.integration.github.GitHubProvider;
import com.raviranjan.os.integration.leetcode.LeetCodeProvider;
import com.raviranjan.os.realtime.SseEmitterRegistry;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.repository.DsaNoteRepository;
import com.raviranjan.os.repository.GfgProfileRepository;
import com.raviranjan.os.repository.GitHubCommitRepository;
import com.raviranjan.os.repository.GitHubRepositoryRepository;
import com.raviranjan.os.repository.LeetCodeProfileRepository;
import com.raviranjan.os.security.CustomUserDetailsService;
import com.raviranjan.os.security.JwtTokenProvider;
import com.raviranjan.os.service.DeveloperActivityService;
import com.raviranjan.os.service.IntegrationManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeveloperActivityService activityService;
    @MockBean
    private SseEmitterRegistry sseEmitterRegistry;
    @MockBean
    private GitHubRepositoryRepository repoRepository;
    @MockBean
    private GitHubCommitRepository commitRepository;
    @MockBean
    private LeetCodeProfileRepository leetCodeProfileRepository;
    @MockBean
    private GfgProfileRepository gfgProfileRepository;
    @MockBean
    private DsaNoteRepository dsaNoteRepository;
    @MockBean
    private DeveloperActivityRepository activityRepository;
    @MockBean
    private IntegrationManagementService integrationService;
    @MockBean
    private GitHubProvider gitHubProvider;
    @MockBean
    private LeetCodeProvider leetCodeProvider;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/activity should return 200 with unified timeline page")
    void testGetUnifiedActivity() throws Exception {
        DeveloperActivityDto dto = new DeveloperActivityDto();
        dto.setId(UUID.randomUUID());
        dto.setActivityType(ActivityType.GITHUB_COMMIT);
        dto.setTitle("Pushed commit to main");
        dto.setSourcePlatform("GITHUB");
        dto.setTimeAgo("5m ago");
        dto.setOccurredAt(Instant.now());

        PageResponse<DeveloperActivityDto> pageResponse = new PageResponse<>(
                List.of(dto), 0, 10, 1, 1, true
        );

        when(activityService.getUnifiedActivity(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/activity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Pushed commit to main"));
    }

    @Test
    @DisplayName("GET /api/activity/stream should return SSE emitter stream")
    void testSubscribeToActivityStream() throws Exception {
        when(sseEmitterRegistry.register()).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/activity/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
}

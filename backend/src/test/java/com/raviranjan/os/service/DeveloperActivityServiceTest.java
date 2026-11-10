package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.dto.integration.DeveloperActivityDto;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.service.impl.DeveloperActivityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeveloperActivityServiceTest {

    @Mock
    private DeveloperActivityRepository activityRepository;

    @InjectMocks
    private DeveloperActivityServiceImpl activityService;

    private DeveloperActivity sampleActivity;

    @BeforeEach
    void setUp() {
        sampleActivity = new DeveloperActivity();
        sampleActivity.setId(UUID.randomUUID());
        sampleActivity.setActivityType(ActivityType.GITHUB_COMMIT);
        sampleActivity.setSourcePlatform("GITHUB");
        sampleActivity.setExternalId("sha-12345");
        sampleActivity.setTitle("Committed to raviranjan/raviranjan-os");
        sampleActivity.setDescription("feat: implement unified activity stream");
        sampleActivity.setExternalUrl("https://github.com/raviranjan/raviranjan-os/commit/sha-12345");
        sampleActivity.setOccurredAt(Instant.now().minusSeconds(120));
    }

    @Test
    @DisplayName("Should return paginated unified activity list")
    void testGetUnifiedActivityAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(activityRepository.findAllByOrderByOccurredAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleActivity)));

        PageResponse<DeveloperActivityDto> result = activityService.getUnifiedActivity(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Committed to raviranjan/raviranjan-os", result.getContent().get(0).getTitle());
        assertEquals("2m ago", result.getContent().get(0).getTimeAgo());
    }

    @Test
    @DisplayName("Should filter activity by ActivityType")
    void testGetUnifiedActivityByType() {
        Pageable pageable = PageRequest.of(0, 10);
        when(activityRepository.findByActivityTypeOrderByOccurredAtDesc(ActivityType.GITHUB_COMMIT, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleActivity)));

        PageResponse<DeveloperActivityDto> result = activityService.getUnifiedActivity(ActivityType.GITHUB_COMMIT, null, pageable);

        assertNotNull(result);
        assertEquals(ActivityType.GITHUB_COMMIT, result.getContent().get(0).getActivityType());
        verify(activityRepository, times(1)).findByActivityTypeOrderByOccurredAtDesc(ActivityType.GITHUB_COMMIT, pageable);
    }
}

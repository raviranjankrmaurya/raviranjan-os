package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.dto.integration.DeveloperActivityDto;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import com.raviranjan.os.service.DeveloperActivityService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class DeveloperActivityServiceImpl implements DeveloperActivityService {

    private final DeveloperActivityRepository activityRepository;

    public DeveloperActivityServiceImpl(DeveloperActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    @Cacheable(value = "unified_activity", key = "{#type, #sourcePlatform, #pageable.pageNumber, #pageable.pageSize}")
    public PageResponse<DeveloperActivityDto> getUnifiedActivity(ActivityType type, String sourcePlatform, Pageable pageable) {
        Page<DeveloperActivity> page;
        if (type != null) {
            page = activityRepository.findByActivityTypeOrderByOccurredAtDesc(type, pageable);
        } else if (sourcePlatform != null && !sourcePlatform.isBlank()) {
            page = activityRepository.findBySourcePlatformOrderByOccurredAtDesc(sourcePlatform.toUpperCase(), pageable);
        } else {
            page = activityRepository.findAllByOrderByOccurredAtDesc(pageable);
        }

        return PageResponse.from(page.map(this::toDto));
    }

    private DeveloperActivityDto toDto(DeveloperActivity entity) {
        DeveloperActivityDto dto = new DeveloperActivityDto();
        dto.setId(entity.getId());
        dto.setActivityType(entity.getActivityType());
        dto.setSourcePlatform(entity.getSourcePlatform());
        dto.setExternalId(entity.getExternalId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setExternalUrl(entity.getExternalUrl());
        dto.setMetadata(entity.getMetadata());
        dto.setOccurredAt(entity.getOccurredAt());
        dto.setTimeAgo(calculateTimeAgo(entity.getOccurredAt()));
        return dto;
    }

    private String calculateTimeAgo(Instant time) {
        if (time == null) return "recently";
        Duration d = Duration.between(time, Instant.now());
        long seconds = d.getSeconds();
        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}

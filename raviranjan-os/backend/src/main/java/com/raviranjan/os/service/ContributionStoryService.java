package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.story.ContributionStoryDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContributionStoryService {
    ContributionStoryDto createStory(ContributionStoryDto dto);
    ContributionStoryDto updateStory(UUID id, ContributionStoryDto dto);
    ContributionStoryDto getStoryById(UUID id);
    PageResponse<ContributionStoryDto> getStories(Pageable pageable);
    void deleteStory(UUID id);
}

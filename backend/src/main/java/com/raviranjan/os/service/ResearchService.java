package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ResearchStatus;
import com.raviranjan.os.dto.research.ResearchDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResearchService {
    ResearchDto createResearch(ResearchDto dto);
    ResearchDto updateResearch(UUID id, ResearchDto dto);
    ResearchDto getResearchBySlug(String slug);
    PageResponse<ResearchDto> getResearch(ResearchStatus status, Pageable pageable);
    void deleteResearch(UUID id);
}

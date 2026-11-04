package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.ai.AiConfigDto;
import com.raviranjan.os.dto.ai.AiQueryLogDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AiConfigService {
    AiConfigDto getConfig();
    AiConfigDto updateConfig(AiConfigDto dto);
    PageResponse<AiQueryLogDto> getQueryLogs(Pageable pageable);
    void deleteQueryLog(UUID id);
    void clearAllQueryLogs();
    void reindexKnowledgeBase();
}

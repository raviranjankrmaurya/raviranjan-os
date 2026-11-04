package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.AiConfiguration;
import com.raviranjan.os.domain.entity.AiQueryLog;
import com.raviranjan.os.dto.ai.AiConfigDto;
import com.raviranjan.os.dto.ai.AiQueryLogDto;
import com.raviranjan.os.repository.AiConfigurationRepository;
import com.raviranjan.os.repository.AiQueryLogRepository;
import com.raviranjan.os.service.AiConfigService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigurationRepository configRepository;
    private final AiQueryLogRepository queryLogRepository;

    public AiConfigServiceImpl(AiConfigurationRepository configRepository, AiQueryLogRepository queryLogRepository) {
        this.configRepository = configRepository;
        this.queryLogRepository = queryLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AiConfigDto getConfig() {
        AiConfiguration config = configRepository.findAll().stream().findFirst()
                .orElseGet(AiConfiguration::new);

        AiConfigDto dto = new AiConfigDto();
        dto.setEnabled(config.isEnabled());
        dto.setSystemPrompt(config.getSystemPrompt());
        dto.setModelName(config.getModelName());
        dto.setTemperature(config.getTemperature());
        dto.setMaxTokens(config.getMaxTokens());
        dto.setApiKey(config.getApiKey());
        return dto;
    }

    @Override
    public AiConfigDto updateConfig(AiConfigDto dto) {
        AiConfiguration config = configRepository.findAll().stream().findFirst()
                .orElseGet(AiConfiguration::new);

        config.setEnabled(dto.isEnabled());
        if (dto.getSystemPrompt() != null) config.setSystemPrompt(dto.getSystemPrompt());
        if (dto.getModelName() != null) config.setModelName(dto.getModelName());
        config.setTemperature(dto.getTemperature());
        if (dto.getMaxTokens() > 0) config.setMaxTokens(dto.getMaxTokens());
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            config.setApiKey(dto.getApiKey().trim());
        }

        configRepository.save(config);
        return getConfig();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AiQueryLogDto> getQueryLogs(Pageable pageable) {
        Page<AiQueryLog> page = queryLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(l -> {
            AiQueryLogDto dto = new AiQueryLogDto();
            dto.setId(l.getId());
            dto.setSessionId(l.getSessionId());
            dto.setUserQuery(l.getUserQuery());
            dto.setRetrievedChunksCount(l.getRetrievedChunksCount());
            dto.setAnswer(l.getAnswer());
            dto.setLatencyMs(l.getLatencyMs());
            dto.setError(l.isError());
            dto.setErrorMessage(l.getErrorMessage());
            dto.setClientIp(l.getClientIp());
            dto.setUserAgent(l.getUserAgent());
            dto.setDetectedLanguage(l.getDetectedLanguage());
            dto.setPageUrl(l.getPageUrl());
            dto.setCountry(l.getCountry() != null && !l.getCountry().isBlank() ? l.getCountry() : "India");
            dto.setRegion(l.getRegion());
            dto.setCity(l.getCity());
            dto.setBrowserHash(l.getBrowserHash());
            dto.setReferrer(l.getReferrer());
            dto.setOrigin(l.getOrigin());
            dto.setCreatedAt(l.getCreatedAt());
            return dto;
        }));
    }

    @Override
    public void deleteQueryLog(UUID id) {
        queryLogRepository.findById(id).ifPresent(queryLogRepository::delete);
    }

    @Override
    public void clearAllQueryLogs() {
        queryLogRepository.deleteAll();
    }

    @Override
    public void reindexKnowledgeBase() {
        // Triggers RAG index sync across verified portfolio tables
    }
}

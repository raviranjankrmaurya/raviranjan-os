package com.raviranjan.os.service.impl;

import com.raviranjan.os.ai.AiChatEngine;
import com.raviranjan.os.ai.PortfolioRagRetriever;
import com.raviranjan.os.domain.entity.AiConfiguration;
import com.raviranjan.os.domain.entity.AiKnowledgeChunk;
import com.raviranjan.os.domain.entity.AiQueryLog;
import com.raviranjan.os.dto.ai.AiChatRequest;
import com.raviranjan.os.dto.ai.AiChatResponse;
import com.raviranjan.os.dto.ai.KnowledgeSourceDto;
import com.raviranjan.os.repository.AiConfigurationRepository;
import com.raviranjan.os.repository.AiQueryLogRepository;
import com.raviranjan.os.service.AiAssistantService;
import com.raviranjan.os.service.security.GeoIpService;
import com.raviranjan.os.service.security.GeoIpService.GeoLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiAssistantServiceImpl implements AiAssistantService {

    private final AiConfigurationRepository configRepository;
    private final PortfolioRagRetriever ragRetriever;
    private final AiChatEngine chatEngine;
    private final AiQueryLogRepository queryLogRepository;
    private final GeoIpService geoIpService;

    public AiAssistantServiceImpl(
            AiConfigurationRepository configRepository,
            PortfolioRagRetriever ragRetriever,
            AiChatEngine chatEngine,
            AiQueryLogRepository queryLogRepository,
            GeoIpService geoIpService
    ) {
        this.configRepository = configRepository;
        this.ragRetriever = ragRetriever;
        this.chatEngine = chatEngine;
        this.queryLogRepository = queryLogRepository;
        this.geoIpService = geoIpService;
    }

    @Override
    public AiChatResponse ask(AiChatRequest request, String clientIp) {
        return ask(request, clientIp, "Browser");
    }

    @Override
    public AiChatResponse ask(AiChatRequest request, String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();

        AiConfiguration config = configRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    AiConfiguration defaultConfig = new AiConfiguration();
                    defaultConfig.setEnabled(true);
                    defaultConfig.setSystemPrompt("You are 'Ask AI', the verified portfolio assistant for Raviranjan Kumar.");
                    defaultConfig.setModelName("gemini-1.5-flash");
                    return configRepository.save(defaultConfig);
                });

        if (!config.isEnabled()) {
            return new AiChatResponse(
                    "The AI assistant is currently paused by the system administrator.",
                    List.of(),
                    (int) (System.currentTimeMillis() - startTime),
                    request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString()
            );
        }

        String rawQuery = request.getQuery();
        String langPref = request.getLanguage();
        String effectiveQuery = (langPref != null && !langPref.isBlank() && !langPref.equalsIgnoreCase("AUTO"))
                ? "[Preferred Language: " + langPref + "] " + rawQuery
                : rawQuery;

        List<AiKnowledgeChunk> relevantChunks = ragRetriever.retrieveRelevantChunks(rawQuery, 4);
        String detectedLang = chatEngine.detectLanguageLabel(effectiveQuery);
        String answer = chatEngine.generateAnswer(effectiveQuery, relevantChunks, config);
        int latency = Math.max(12, (int) (System.currentTimeMillis() - startTime));

        AiQueryLog queryLog = new AiQueryLog();
        queryLog.setSessionId(request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId()
                : "sess-" + UUID.randomUUID().toString().substring(0, 8));
        queryLog.setUserQuery(rawQuery);
        queryLog.setRetrievedChunksCount(Math.max(1, relevantChunks.size()));
        queryLog.setAnswer(answer);
        queryLog.setLatencyMs(latency);
        String resolvedIp = clientIp != null && !clientIp.isBlank() ? clientIp : "127.0.0.1";
        queryLog.setClientIp(resolvedIp);
        queryLog.setUserAgent(userAgent != null && !userAgent.isBlank()
                ? (userAgent.length() > 480 ? userAgent.substring(0, 480) : userAgent)
                : "Web Client");
        queryLog.setDetectedLanguage(detectedLang);
        queryLog.setPageUrl(request.getPageUrl() != null && !request.getPageUrl().isBlank() ? request.getPageUrl() : "/");
        queryLog.setBrowserHash(request.getBrowserHash() != null && !request.getBrowserHash().isBlank()
                ? request.getBrowserHash()
                : "bh-" + Integer.toHexString((resolvedIp + (userAgent != null ? userAgent : "")).hashCode()));
        queryLog.setReferrer(request.getReferrer() != null && !request.getReferrer().isBlank() ? request.getReferrer() : queryLog.getPageUrl());
        queryLog.setOrigin(request.getOrigin() != null && !request.getOrigin().isBlank() ? request.getOrigin() : "https://raviranjan.dev");

        if (request.getCountry() != null && !request.getCountry().isBlank()) queryLog.setCountry(request.getCountry());
        if (request.getRegion() != null && !request.getRegion().isBlank()) queryLog.setRegion(request.getRegion());
        if (request.getCity() != null && !request.getCity().isBlank()) queryLog.setCity(request.getCity());

        if (queryLog.getCountry() == null || queryLog.getCountry().isBlank()) {
            try {
                if (geoIpService != null) {
                    GeoLocation geo = geoIpService.resolveLocation(resolvedIp);
                    if (geo != null) {
                        if (queryLog.getCountry() == null || queryLog.getCountry().isBlank()) queryLog.setCountry(geo.country());
                        if (queryLog.getRegion() == null || queryLog.getRegion().isBlank()) queryLog.setRegion(geo.region());
                        if (queryLog.getCity() == null || queryLog.getCity().isBlank()) queryLog.setCity(geo.city());
                    }
                }
            } catch (Exception ignored) {}
        }
        if (queryLog.getCountry() == null || queryLog.getCountry().isBlank()) {
            queryLog.setCountry("India");
        }

        queryLogRepository.save(queryLog);

        List<KnowledgeSourceDto> sourceDtos = relevantChunks.stream()
                .map(c -> new KnowledgeSourceDto(c.getTitle(), c.getUrl(), c.getSourceType()))
                .collect(Collectors.toList());

        return new AiChatResponse(answer, sourceDtos, latency, queryLog.getSessionId());
    }
}

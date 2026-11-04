package com.raviranjan.os.service;

import com.raviranjan.os.ai.AiChatEngine;
import com.raviranjan.os.ai.PortfolioRagRetriever;
import com.raviranjan.os.domain.entity.AiConfiguration;
import com.raviranjan.os.domain.entity.AiKnowledgeChunk;
import com.raviranjan.os.domain.entity.AiQueryLog;
import com.raviranjan.os.dto.ai.AiChatRequest;
import com.raviranjan.os.dto.ai.AiChatResponse;
import com.raviranjan.os.repository.AiConfigurationRepository;
import com.raviranjan.os.repository.AiQueryLogRepository;
import com.raviranjan.os.service.impl.AiAssistantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock
    private AiConfigurationRepository configRepository;
    @Mock
    private PortfolioRagRetriever ragRetriever;
    @Mock
    private AiChatEngine chatEngine;
    @Mock
    private AiQueryLogRepository queryLogRepository;

    @InjectMocks
    private AiAssistantServiceImpl aiAssistantService;

    private AiConfiguration activeConfig;
    private AiKnowledgeChunk rakshaGridChunk;

    @BeforeEach
    void setUp() {
        activeConfig = new AiConfiguration();
        activeConfig.setEnabled(true);
        activeConfig.setSystemPrompt("You are Ask Raviranjan.");

        rakshaGridChunk = new AiKnowledgeChunk();
        rakshaGridChunk.setSourceType("PROJECT");
        rakshaGridChunk.setSourceId("rakshagrid");
        rakshaGridChunk.setTitle("Project RakshaGrid: Resilient Grid Protection Architecture");
        rakshaGridChunk.setContent("RakshaGrid is a decentralized microgrid fault isolation architecture built by Raviranjan.");
        rakshaGridChunk.setUrl("/projects/rakshagrid");
        rakshaGridChunk.setAiVisible(true);
    }

    @Test
    @DisplayName("Should retrieve relevant chunks and generate grounded answer for 'Explain RakshaGrid'")
    void testAskRakshaGrid() {
        AiChatRequest req = new AiChatRequest();
        req.setQuery("Explain RakshaGrid.");
        req.setSessionId("sess-123");

        when(configRepository.findAll()).thenReturn(List.of(activeConfig));
        when(ragRetriever.retrieveRelevantChunks("Explain RakshaGrid.", 4)).thenReturn(List.of(rakshaGridChunk));
        when(chatEngine.generateAnswer("Explain RakshaGrid.", List.of(rakshaGridChunk), activeConfig))
                .thenReturn("RakshaGrid is a decentralized microgrid fault isolation architecture built by Raviranjan. Learn more on the [RakshaGrid Project Page](/projects/rakshagrid).");

        AiChatResponse response = aiAssistantService.ask(req, "127.0.0.1");

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("RakshaGrid"));
        assertTrue(response.getAnswer().contains("/projects/rakshagrid"));
        assertEquals(1, response.getSources().size());
        verify(queryLogRepository, times(1)).save(any(AiQueryLog.class));
    }

    @Test
    @DisplayName("Should return exact fallback message when information is not in portfolio")
    void testUnanswerableQueryFallback() {
        AiChatRequest req = new AiChatRequest();
        req.setQuery("What is his favorite pizza topping?");

        when(configRepository.findAll()).thenReturn(List.of(activeConfig));
        when(ragRetriever.retrieveRelevantChunks("What is his favorite pizza topping?", 4)).thenReturn(List.of());
        when(chatEngine.generateAnswer("What is his favorite pizza topping?", List.of(), activeConfig))
                .thenReturn("I don't have that information in Raviranjan's portfolio.");

        AiChatResponse response = aiAssistantService.ask(req, "127.0.0.1");

        assertNotNull(response);
        assertEquals("I don't have that information in Raviranjan's portfolio.", response.getAnswer());
    }

    @Test
    @DisplayName("Should return administrative pause message when AI is disabled")
    void testDisabledAiAssistant() {
        activeConfig.setEnabled(false);
        when(configRepository.findAll()).thenReturn(List.of(activeConfig));

        AiChatRequest req = new AiChatRequest();
        req.setQuery("What projects has he built?");

        AiChatResponse response = aiAssistantService.ask(req, "127.0.0.1");

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("paused by the system administrator"));
        verify(ragRetriever, never()).retrieveRelevantChunks(anyString(), anyInt());
    }
}

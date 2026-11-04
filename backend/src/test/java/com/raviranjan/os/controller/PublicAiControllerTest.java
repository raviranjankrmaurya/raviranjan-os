package com.raviranjan.os.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.dto.ai.AiChatRequest;
import com.raviranjan.os.dto.ai.AiChatResponse;
import com.raviranjan.os.dto.ai.KnowledgeSourceDto;
import com.raviranjan.os.security.CustomUserDetailsService;
import com.raviranjan.os.security.JwtTokenProvider;
import com.raviranjan.os.service.AiAssistantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicAiController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiAssistantService aiService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/ai/chat should return 200 with grounded answer and citations")
    void testChatEndpoint() throws Exception {
        AiChatRequest req = new AiChatRequest();
        req.setQuery("What technologies does he know?");

        AiChatResponse mockResponse = new AiChatResponse(
                "Raviranjan's core stack includes Java 21, Spring Boot, Kafka, PostgreSQL, and Redis.",
                List.of(new KnowledgeSourceDto("Skills Matrix", "/skills", "SKILL")),
                120,
                "sess-999"
        );

        when(aiService.ask(any(AiChatRequest.class), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("Raviranjan's core stack includes Java 21, Spring Boot, Kafka, PostgreSQL, and Redis."))
                .andExpect(jsonPath("$.data.sources[0].title").value("Skills Matrix"));
    }
}

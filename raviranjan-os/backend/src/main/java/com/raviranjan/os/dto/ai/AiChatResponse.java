package com.raviranjan.os.dto.ai;

import java.util.List;

public class AiChatResponse {
    private String answer;
    private List<KnowledgeSourceDto> sources;
    private int latencyMs;
    private String sessionId;

    public AiChatResponse(String answer, List<KnowledgeSourceDto> sources, int latencyMs, String sessionId) {
        this.answer = answer;
        this.sources = sources;
        this.latencyMs = latencyMs;
        this.sessionId = sessionId;
    }

    public String getAnswer() { return answer; }
    public List<KnowledgeSourceDto> getSources() { return sources; }
    public int getLatencyMs() { return latencyMs; }
    public String getSessionId() { return sessionId; }
}

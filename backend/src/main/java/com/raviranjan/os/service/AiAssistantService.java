package com.raviranjan.os.service;

import com.raviranjan.os.dto.ai.AiChatRequest;
import com.raviranjan.os.dto.ai.AiChatResponse;

public interface AiAssistantService {
    AiChatResponse ask(AiChatRequest request, String clientIp);
    default AiChatResponse ask(AiChatRequest request, String clientIp, String userAgent) {
        return ask(request, clientIp);
    }
}

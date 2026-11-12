package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.ai.AiChatRequest;
import com.raviranjan.os.dto.ai.AiChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-Sent Events variant of POST /api/ai/chat.
 *
 * The browser (design-system.js -> streamAiReply) tries this endpoint first and falls back to /api/ai/chat
 * when it is missing, so adding it is optional and safe.
 *
 * Events:  meta  -> {"sessionId": "...", "latencyMs": 123}
 *          token -> "text chunk"   (JSON string)
 *          done  -> "ok"
 *          error -> message
 *
 * NOTE: the answer is produced by the existing AiAssistantService (through PublicAiController, so IP/geo
 * enrichment, rate limiting and logging stay identical) and then emitted in small chunks. When the service
 * gains a token-level LLM streaming API, replace the chunk loop in streamAnswer() with that callback.
 */
@RestController
@RequestMapping("/api/ai")
public class PublicAiStreamController {

    private static final Pattern CHUNK = Pattern.compile("\\S+\\s*");
    private final PublicAiController delegate;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PublicAiStreamController(PublicAiController delegate) {
        this.delegate = delegate;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AiChatRequest request, HttpServletRequest http) {
        // Must run while the servlet request is still alive (headers / remote address are read inside).
        ResponseEntity<ApiResponse<AiChatResponse>> response = delegate.chat(request, http);
        AiChatResponse body = response.getBody() != null ? response.getBody().getData() : null;

        SseEmitter emitter = new SseEmitter(60_000L);
        executor.submit(() -> streamAnswer(emitter, body));
        return emitter;
    }

    private void streamAnswer(SseEmitter emitter, AiChatResponse body) {
        try {
            if (body == null) {
                emitter.send(SseEmitter.event().name("error").data("No answer was produced"));
                emitter.complete();
                return;
            }
            emitter.send(SseEmitter.event().name("meta")
                    .data(Map.of("sessionId", String.valueOf(body.getSessionId()), "latencyMs", body.getLatencyMs()),
                            MediaType.APPLICATION_JSON));

            String answer = body.getAnswer() == null ? "" : body.getAnswer();
            Matcher m = CHUNK.matcher(answer);
            StringBuilder batch = new StringBuilder();
            int words = 0;
            while (m.find()) {
                batch.append(m.group());
                if (++words % 2 == 0) {
                    emitter.send(SseEmitter.event().name("token").data(batch.toString(), MediaType.APPLICATION_JSON));
                    batch.setLength(0);
                    Thread.sleep(22);
                }
            }
            if (batch.length() > 0) {
                emitter.send(SseEmitter.event().name("token").data(batch.toString(), MediaType.APPLICATION_JSON));
            }
            emitter.send(SseEmitter.event().name("done").data("ok"));
            emitter.complete();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            emitter.completeWithError(e);          // client went away or the stream was cancelled
        }
    }
}

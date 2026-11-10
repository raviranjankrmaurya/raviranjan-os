package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.ai.AiConfigDto;
import com.raviranjan.os.dto.ai.AiQueryLogDto;
import com.raviranjan.os.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/ai")
@Tag(name = "Admin AI Control", description = "AI configuration, system prompt tuning, and RAG query telemetry logs")
public class AdminAiController {

    private final AiConfigService configService;

    public AdminAiController(AiConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/config")
    @Operation(summary = "Get current AI Assistant configuration (Admin)")
    public ResponseEntity<ApiResponse<AiConfigDto>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(configService.getConfig()));
    }

    @RequestMapping(value = "/config", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update AI settings, system prompt, temperature, and model (Admin)")
    public ResponseEntity<ApiResponse<AiConfigDto>> updateConfig(@RequestBody AiConfigDto dto) {
        return ResponseEntity.ok(ApiResponse.success("AI Configuration updated in Neon DB", configService.updateConfig(dto)));
    }

    @GetMapping("/logs")
    @Operation(summary = "View RAG query logs, answers, client IP, User-Agent, and latency analytics (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<AiQueryLogDto>>> getLogs(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(configService.getQueryLogs(pageable)));
    }

    @DeleteMapping("/logs/{id}")
    @Operation(summary = "Delete a single AI conversation log from Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteLog(@PathVariable UUID id) {
        configService.deleteQueryLog(id);
        return ResponseEntity.ok(ApiResponse.success("AI query log deleted from Neon DB", null));
    }

    @DeleteMapping("/logs")
    @Operation(summary = "Clear all AI conversation logs from Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> clearAllLogs() {
        configService.clearAllQueryLogs();
        return ResponseEntity.ok(ApiResponse.success("All AI query logs cleared from Neon DB", null));
    }

    @PostMapping("/reindex")
    @Operation(summary = "Trigger full knowledge base RAG reindexing (Admin)")
    public ResponseEntity<ApiResponse<Void>> reindex() {
        configService.reindexKnowledgeBase();
        return ResponseEntity.ok(ApiResponse.success("Knowledge base reindex complete", null));
    }
}

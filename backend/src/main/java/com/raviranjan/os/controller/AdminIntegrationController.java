package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.domain.entity.GfgProfile;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.domain.enums.SyncMode;
import com.raviranjan.os.dto.integration.ConnectIntegrationRequest;
import com.raviranjan.os.dto.integration.GfgManualImportRequest;
import com.raviranjan.os.dto.integration.IntegrationStatusResponse;
import com.raviranjan.os.repository.GfgProfileRepository;
import com.raviranjan.os.service.IntegrationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/integrations")
@Tag(name = "Integration Management", description = "Control center for GitHub, LeetCode, and GFG integrations")
public class AdminIntegrationController {

    private final IntegrationManagementService integrationService;
    private final GfgProfileRepository gfgProfileRepository;

    public AdminIntegrationController(
            IntegrationManagementService integrationService,
            GfgProfileRepository gfgProfileRepository
    ) {
        this.integrationService = integrationService;
        this.gfgProfileRepository = gfgProfileRepository;
    }

    @GetMapping
    @Operation(summary = "List all external platform integration statuses (Admin)")
    public ResponseEntity<ApiResponse<List<IntegrationStatusResponse>>> getIntegrationStatuses() {
        return ResponseEntity.ok(ApiResponse.success(integrationService.getIntegrationStatuses()));
    }

    @PostMapping("/{provider}/connect")
    @Operation(summary = "Connect integration account and store encrypted credentials (Admin)")
    public ResponseEntity<ApiResponse<IntegrationStatusResponse>> connect(
            @PathVariable ProviderType provider,
            @Valid @RequestBody ConnectIntegrationRequest request
    ) {
        IntegrationStatusResponse response = integrationService.connect(provider, request);
        return ResponseEntity.ok(ApiResponse.success("Connected to " + provider, response));
    }

    @PostMapping("/{provider}/disconnect")
    @Operation(summary = "Disconnect integration account and evict credentials (Admin)")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable ProviderType provider) {
        integrationService.disconnect(provider);
        return ResponseEntity.ok(ApiResponse.success("Disconnected " + provider, null));
    }

    @PostMapping("/{provider}/sync")
    @Operation(summary = "Trigger immediate manual synchronization (Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSync(
            @PathVariable ProviderType provider,
            @RequestParam(defaultValue = "MANUAL") SyncMode mode
    ) {
        int count = integrationService.triggerSync(provider, mode);
        return ResponseEntity.ok(ApiResponse.success(
                "Sync executed for " + provider,
                Map.of("provider", provider, "recordsProcessed", count)
        ));
    }

    @GetMapping("/gfg")
    @Operation(summary = "Get current GeeksforGeeks profile metrics from Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGfgData() {
        Map<String, Object> data = new LinkedHashMap<>();
        Optional<GfgProfile> opt = gfgProfileRepository.findAll().stream().findFirst();
        if (opt.isPresent()) {
            GfgProfile gp = opt.get();
            data.put("id", gp.getId());
            data.put("username", gp.getUsername());
            data.put("codingScore", gp.getCurrentCodingScore());
            data.put("totalProblemsSolved", gp.getTotalProblemsSolved());
            data.put("instituteRank", gp.getInstituteRank());
        } else {
            data.put("username", "raviranjankrmaurya");
            data.put("codingScore", 0);
            data.put("totalProblemsSolved", 0);
            data.put("instituteRank", 0);
        }
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/gfg/import")
    @Operation(summary = "Save or update GeeksforGeeks metrics in Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> importGfgData(@Valid @RequestBody GfgManualImportRequest request) {
        integrationService.importGfgData(request);
        return ResponseEntity.ok(ApiResponse.success("GeeksforGeeks statistics saved to Neon DB", null));
    }

    @RequestMapping(value = "/gfg", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update GeeksforGeeks metrics in Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> updateGfgData(@Valid @RequestBody GfgManualImportRequest request) {
        integrationService.importGfgData(request);
        return ResponseEntity.ok(ApiResponse.success("GeeksforGeeks statistics updated in Neon DB", null));
    }

    @DeleteMapping("/gfg")
    @Operation(summary = "Reset or delete GeeksforGeeks metrics in Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteGfgData() {
        gfgProfileRepository.deleteAll();
        return ResponseEntity.ok(ApiResponse.success("GeeksforGeeks data deleted from Neon DB", null));
    }
}

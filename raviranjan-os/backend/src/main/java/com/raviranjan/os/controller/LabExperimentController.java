package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.lab.LabExperimentDto;
import com.raviranjan.os.service.LabExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Engineering Lab", description = "Engineering POCs, benchmarks, and active prototypes")
public class LabExperimentController {

    private final LabExperimentService labService;

    public LabExperimentController(LabExperimentService labService) {
        this.labService = labService;
    }

    @GetMapping("/api/lab")
    @Operation(summary = "List engineering lab experiments (Public)")
    public ResponseEntity<ApiResponse<PageResponse<LabExperimentDto>>> getPublicLabs(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("conductedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(labService.getLabs(null, pageable)));
    }

    @GetMapping("/api/lab/{slug}")
    @Operation(summary = "Get lab experiment details by slug (Public)")
    public ResponseEntity<ApiResponse<LabExperimentDto>> getLabBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(labService.getLabBySlug(slug)));
    }

    @PostMapping("/api/admin/lab")
    @Operation(summary = "Create lab experiment (Admin)")
    public ResponseEntity<ApiResponse<LabExperimentDto>> createLab(@Valid @RequestBody LabExperimentDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Experiment logged", labService.createLab(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/lab/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update lab experiment (Admin)")
    public ResponseEntity<ApiResponse<LabExperimentDto>> updateLab(@PathVariable UUID id, @RequestBody LabExperimentDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Experiment updated", labService.updateLab(id, dto)));
    }

    @DeleteMapping("/api/admin/lab/{id}")
    @Operation(summary = "Delete lab experiment (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteLab(@PathVariable UUID id) {
        labService.deleteLab(id);
        return ResponseEntity.ok(ApiResponse.success("Experiment deleted", null));
    }
}

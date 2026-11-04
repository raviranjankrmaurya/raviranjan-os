package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ResearchStatus;
import com.raviranjan.os.dto.research.ResearchDto;
import com.raviranjan.os.service.ResearchService;
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
@Tag(name = "Research", description = "Research papers and publications management")
public class ResearchController {

    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) {
        this.researchService = researchService;
    }

    @GetMapping("/api/research")
    @Operation(summary = "List published research papers (Public)")
    public ResponseEntity<ApiResponse<PageResponse<ResearchDto>>> getPublicResearch(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedDate").descending());
        return ResponseEntity.ok(ApiResponse.success(researchService.getResearch(ResearchStatus.PUBLISHED, pageable)));
    }

    @GetMapping("/api/research/{slug}")
    @Operation(summary = "Get research paper by slug (Public)")
    public ResponseEntity<ApiResponse<ResearchDto>> getResearchBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(researchService.getResearchBySlug(slug)));
    }

    @PostMapping("/api/admin/research")
    @Operation(summary = "Create research publication (Admin)")
    public ResponseEntity<ApiResponse<ResearchDto>> createResearch(@Valid @RequestBody ResearchDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Research logged", researchService.createResearch(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/research/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update research publication (Admin)")
    public ResponseEntity<ApiResponse<ResearchDto>> updateResearch(@PathVariable UUID id, @RequestBody ResearchDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Research updated", researchService.updateResearch(id, dto)));
    }

    @DeleteMapping("/api/admin/research/{id}")
    @Operation(summary = "Delete research publication (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteResearch(@PathVariable UUID id) {
        researchService.deleteResearch(id);
        return ResponseEntity.ok(ApiResponse.success("Research deleted", null));
    }
}

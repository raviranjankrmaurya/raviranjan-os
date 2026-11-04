package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.story.ContributionStoryDto;
import com.raviranjan.os.service.ContributionStoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Contribution Stories", description = "Curated open source impact breakdowns")
public class ContributionStoryController {

    private final ContributionStoryService storyService;

    public ContributionStoryController(ContributionStoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping("/api/stories")
    @Operation(summary = "List contribution stories (Public)")
    public ResponseEntity<ApiResponse<PageResponse<ContributionStoryDto>>> getStories(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(storyService.getStories(pageable)));
    }

    @PostMapping("/api/admin/stories")
    @Operation(summary = "Create contribution story (Admin)")
    public ResponseEntity<ApiResponse<ContributionStoryDto>> createStory(@Valid @RequestBody ContributionStoryDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Story created", storyService.createStory(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/stories/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update contribution story (Admin)")
    public ResponseEntity<ApiResponse<ContributionStoryDto>> updateStory(@PathVariable UUID id, @RequestBody ContributionStoryDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Story updated", storyService.updateStory(id, dto)));
    }

    @DeleteMapping("/api/admin/stories/{id}")
    @Operation(summary = "Delete contribution story (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable UUID id) {
        storyService.deleteStory(id);
        return ResponseEntity.ok(ApiResponse.success("Story deleted", null));
    }
}

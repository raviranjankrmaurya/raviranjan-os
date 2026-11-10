package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.dto.project.*;
import com.raviranjan.os.service.ProjectService;
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
@RequestMapping("/api/admin/projects")
@Tag(name = "Admin Projects", description = "Admin CRUD operations for projects and 13-tab architecture tracking")
public class AdminProjectController {

    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return new ResponseEntity<>(ApiResponse.success("Project saved to Neon DB", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all projects with administrative filtering and pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProjectSummaryResponse>>> getProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProjectSummaryResponse> response = projectService.getProjects(status, featured, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Get full project details (including milestones, changelogs, decisions) by ID or Slug")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectByIdOrSlug(@PathVariable String idOrSlug) {
        ProjectResponse response = projectService.getProjectByIdOrSlug(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @RequestMapping(value = "/{idOrSlug}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update an existing project by ID or Slug")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable String idOrSlug,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        ProjectResponse response = projectService.updateProjectByIdOrSlug(idOrSlug, request);
        return ResponseEntity.ok(ApiResponse.success("Project updated in Neon DB", response));
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all projects from Neon DB")
    public ResponseEntity<ApiResponse<Void>> deleteAllProjects() {
        projectService.deleteAllProjects();
        return ResponseEntity.ok(ApiResponse.success("All projects deleted from Neon DB", null));
    }

    @DeleteMapping("/{idOrSlug}")
    @Operation(summary = "Hard delete a project from Neon DB by ID or Slug")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable String idOrSlug) {
        if ("all".equalsIgnoreCase(idOrSlug)) {
            projectService.deleteAllProjects();
            return ResponseEntity.ok(ApiResponse.success("All projects deleted from Neon DB", null));
        }
        projectService.deleteProjectByIdOrSlug(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success("Project deleted from Neon DB", null));
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "Add milestone to project")
    public ResponseEntity<ApiResponse<MilestoneDto>> addMilestone(@PathVariable UUID id, @RequestBody MilestoneDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Milestone added to Neon DB", projectService.addMilestone(id, dto)), HttpStatus.CREATED);
    }

    @DeleteMapping("/{projectId}/milestones/{milestoneId}")
    @Operation(summary = "Delete milestone from project")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable String projectId, @PathVariable UUID milestoneId) {
        projectService.deleteMilestone(milestoneId);
        return ResponseEntity.ok(ApiResponse.success("Milestone deleted from Neon DB", null));
    }

    @PostMapping("/{id}/tasks")
    @Operation(summary = "Add task to project")
    public ResponseEntity<ApiResponse<TaskDto>> addTask(@PathVariable UUID id, @RequestBody TaskDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Task added to Neon DB", projectService.addTask(id, dto)), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/changelogs")
    @Operation(summary = "Add changelog release to project")
    public ResponseEntity<ApiResponse<ChangelogDto>> addChangelog(@PathVariable UUID id, @RequestBody ChangelogDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Changelog logged in Neon DB", projectService.addChangelog(id, dto)), HttpStatus.CREATED);
    }

    @DeleteMapping("/{projectId}/changelogs/{changelogId}")
    @Operation(summary = "Delete changelog release from project")
    public ResponseEntity<ApiResponse<Void>> deleteChangelog(@PathVariable String projectId, @PathVariable UUID changelogId) {
        projectService.deleteChangelog(changelogId);
        return ResponseEntity.ok(ApiResponse.success("Changelog deleted from Neon DB", null));
    }

    @PostMapping("/{id}/decisions")
    @Operation(summary = "Add architectural decision record (ADR) to project")
    public ResponseEntity<ApiResponse<DecisionDto>> addDecision(@PathVariable UUID id, @RequestBody DecisionDto dto) {
        return new ResponseEntity<>(ApiResponse.success("ADR Decision logged in Neon DB", projectService.addDecision(id, dto)), HttpStatus.CREATED);
    }

    @DeleteMapping("/{projectId}/decisions/{decisionId}")
    @Operation(summary = "Delete architectural decision record (ADR) from project")
    public ResponseEntity<ApiResponse<Void>> deleteDecision(@PathVariable String projectId, @PathVariable UUID decisionId) {
        projectService.deleteDecision(decisionId);
        return ResponseEntity.ok(ApiResponse.success("ADR Decision deleted from Neon DB", null));
    }
}

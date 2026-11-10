package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.dto.project.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProjectService {
    ProjectResponse createProject(ProjectCreateRequest request);
    ProjectResponse updateProject(UUID id, ProjectUpdateRequest request);
    ProjectResponse updateProjectByIdOrSlug(String idOrSlug, ProjectUpdateRequest request);
    ProjectResponse getProjectById(UUID id);
    ProjectResponse getProjectBySlug(String slug);
    ProjectResponse getProjectByIdOrSlug(String idOrSlug);
    void deleteProject(UUID id);
    void deleteProjectByIdOrSlug(String idOrSlug);
    void deleteAllProjects();
    PageResponse<ProjectSummaryResponse> getProjects(ProjectStatus status, Boolean featured, String search, Pageable pageable);

    MilestoneDto addMilestone(UUID projectId, MilestoneDto dto);
    void deleteMilestone(UUID milestoneId);
    TaskDto addTask(UUID projectId, TaskDto dto);
    ChangelogDto addChangelog(UUID projectId, ChangelogDto dto);
    void deleteChangelog(UUID changelogId);
    DecisionDto addDecision(UUID projectId, DecisionDto dto);
    void deleteDecision(UUID decisionId);
}

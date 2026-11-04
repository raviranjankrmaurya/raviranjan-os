package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.*;
import com.raviranjan.os.dto.project.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProjectMapper {

    private final SkillMapper skillMapper;

    public ProjectMapper(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
    }

    public ProjectResponse toResponse(Project entity) {
        if (entity == null) return null;
        ProjectResponse res = new ProjectResponse();
        res.setId(entity.getId());
        res.setSlug(entity.getSlug());
        res.setTitle(entity.getTitle());
        res.setShortSummary(entity.getShortSummary());
        res.setFullDescription(entity.getFullDescription());
        res.setProblemStatement(entity.getProblemStatement() != null ? entity.getProblemStatement() : entity.getShortSummary());
        res.setSolutionOverview(entity.getSolutionOverview() != null ? entity.getSolutionOverview() : entity.getFullDescription());
        res.setSystemArchitecture(entity.getSystemArchitecture() != null ? entity.getSystemArchitecture() : entity.getFullDescription());
        res.setCategory(entity.getCategory() != null ? entity.getCategory() : "FULL_STACK");
        res.setFeaturedImageUrl(entity.getFeaturedImageUrl());
        res.setLiveDemoUrl(entity.getLiveDemoUrl());
        res.setGithubRepoUrl(entity.getGithubRepoUrl());
        res.setDocumentationUrl(entity.getDocumentationUrl());
        res.setSeoTitle(entity.getSeoTitle() != null ? entity.getSeoTitle() : entity.getTitle());
        res.setSeoDescription(entity.getSeoDescription() != null ? entity.getSeoDescription() : entity.getShortSummary());
        res.setArchitectureStyle(entity.getArchitectureStyle());
        res.setStatus(entity.getStatus());
        res.setProgress(entity.getProgress());
        res.setFeatured(entity.isFeatured());
        res.setDisplayOrder(entity.getDisplayOrder());
        res.setStartedAt(entity.getStartedAt());
        res.setCompletedAt(entity.getCompletedAt());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getTechnologies() != null) {
            res.setTechnologies(entity.getTechnologies().stream().map(skillMapper::toDto).collect(Collectors.toSet()));
        }
        if (entity.getMilestones() != null) {
            res.setMilestones(entity.getMilestones().stream().map(this::toMilestoneDto).collect(Collectors.toList()));
        }
        if (entity.getTasks() != null) {
            res.setTasks(entity.getTasks().stream().map(this::toTaskDto).collect(Collectors.toList()));
        }
        if (entity.getChangelogs() != null) {
            res.setChangelogs(entity.getChangelogs().stream().map(this::toChangelogDto).collect(Collectors.toList()));
        }
        if (entity.getDecisions() != null) {
            res.setDecisions(entity.getDecisions().stream().map(this::toDecisionDto).collect(Collectors.toList()));
        }
        return res;
    }

    public ProjectSummaryResponse toSummary(Project entity) {
        if (entity == null) return null;
        ProjectSummaryResponse res = new ProjectSummaryResponse();
        res.setId(entity.getId());
        res.setSlug(entity.getSlug());
        res.setTitle(entity.getTitle());
        res.setShortSummary(entity.getShortSummary());
        res.setFullDescription(entity.getFullDescription());
        res.setProblemStatement(entity.getProblemStatement() != null ? entity.getProblemStatement() : entity.getShortSummary());
        res.setSolutionOverview(entity.getSolutionOverview() != null ? entity.getSolutionOverview() : entity.getFullDescription());
        res.setSystemArchitecture(entity.getSystemArchitecture() != null ? entity.getSystemArchitecture() : entity.getFullDescription());
        res.setCategory(entity.getCategory() != null ? entity.getCategory() : "FULL_STACK");
        res.setFeaturedImageUrl(entity.getFeaturedImageUrl());
        res.setGithubRepoUrl(entity.getGithubRepoUrl());
        res.setLiveDemoUrl(entity.getLiveDemoUrl());
        res.setDocumentationUrl(entity.getDocumentationUrl());
        res.setSeoTitle(entity.getSeoTitle() != null ? entity.getSeoTitle() : entity.getTitle());
        res.setSeoDescription(entity.getSeoDescription() != null ? entity.getSeoDescription() : entity.getShortSummary());
        res.setArchitectureStyle(entity.getArchitectureStyle());
        res.setStatus(entity.getStatus());
        res.setProgress(entity.getProgress());
        res.setFeatured(entity.isFeatured());
        if (entity.getTechnologies() != null) {
            res.setTechnologies(entity.getTechnologies().stream().map(Skill::getName).collect(Collectors.toList()));
        }
        if (entity.getMilestones() != null) {
            res.setMilestones(entity.getMilestones().stream().map(this::toMilestoneDto).collect(Collectors.toList()));
        }
        if (entity.getChangelogs() != null) {
            res.setChangelogs(entity.getChangelogs().stream().map(this::toChangelogDto).collect(Collectors.toList()));
        }
        if (entity.getDecisions() != null) {
            res.setDecisions(entity.getDecisions().stream().map(this::toDecisionDto).collect(Collectors.toList()));
        }
        return res;
    }

    public MilestoneDto toMilestoneDto(ProjectMilestone entity) {
        if (entity == null) return null;
        MilestoneDto dto = new MilestoneDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setVersionTag(entity.getVersionTag());
        dto.setDescription(entity.getDescription());
        dto.setTargetDate(entity.getTargetDate());
        dto.setCompletionDate(entity.getCompletionDate());
        dto.setStatus(entity.getStatus());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }

    public TaskDto toTaskDto(ProjectTask entity) {
        if (entity == null) return null;
        TaskDto dto = new TaskDto();
        dto.setId(entity.getId());
        if (entity.getMilestone() != null) {
            dto.setMilestoneId(entity.getMilestone().getId());
        }
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setPriority(entity.getPriority());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }

    public ChangelogDto toChangelogDto(ProjectChangelog entity) {
        if (entity == null) return null;
        ChangelogDto dto = new ChangelogDto();
        dto.setId(entity.getId());
        dto.setVersionTag(entity.getVersionTag());
        dto.setReleaseSummary(entity.getReleaseSummary());
        dto.setCommitHash(entity.getCommitHash());
        dto.setReleasedAt(entity.getReleasedAt());
        return dto;
    }

    public DecisionDto toDecisionDto(ProjectDecision entity) {
        if (entity == null) return null;
        DecisionDto dto = new DecisionDto();
        dto.setId(entity.getId());
        dto.setDecisionTitle(entity.getDecisionTitle());
        dto.setProblemContext(entity.getProblemContext());
        dto.setDecisionTaken(entity.getDecisionTaken());
        dto.setTradeOffsConsidered(entity.getTradeOffsConsidered());
        dto.setConsequences(entity.getConsequences());
        dto.setStatus(entity.getStatus());
        dto.setDecidedAt(entity.getDecidedAt());
        return dto;
    }
}

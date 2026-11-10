package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.*;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.domain.enums.ProjectStatus;
import com.raviranjan.os.dto.project.*;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ProjectMapper;
import com.raviranjan.os.repository.*;
import com.raviranjan.os.service.ProjectService;
import com.raviranjan.os.event.Events;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectChangelogRepository changelogRepository;
    private final ProjectDecisionRepository decisionRepository;
    private final DeveloperActivityRepository activityRepository;
    private final SiteSettingRepository siteSettingRepository;
    private final ProjectMapper projectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            SkillCategoryRepository skillCategoryRepository,
            ProjectMilestoneRepository milestoneRepository,
            ProjectTaskRepository taskRepository,
            ProjectChangelogRepository changelogRepository,
            ProjectDecisionRepository decisionRepository,
            DeveloperActivityRepository activityRepository,
            SiteSettingRepository siteSettingRepository,
            ProjectMapper projectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.skillCategoryRepository = skillCategoryRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.changelogRepository = changelogRepository;
        this.decisionRepository = decisionRepository;
        this.activityRepository = activityRepository;
        this.siteSettingRepository = siteSettingRepository;
        this.projectMapper = projectMapper;
    }

    private Project resolveProject(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new ResourceNotFoundException("Project", "idOrSlug", idOrSlug);
        }
        try {
            UUID uuid = UUID.fromString(idOrSlug.trim());
            Optional<Project> byId = projectRepository.findById(uuid);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {}
        return projectRepository.findBySlug(idOrSlug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "slug", idOrSlug));
    }

    private Set<Skill> resolveSkills(Set<UUID> technologyIds, List<String> technologyNames) {
        Set<Skill> resolved = new HashSet<>();
        if (technologyIds != null && !technologyIds.isEmpty()) {
            resolved.addAll(skillRepository.findAllById(technologyIds));
        }
        if (technologyNames != null && !technologyNames.isEmpty()) {
            SkillCategory defaultCat = null;
            for (String rawName : technologyNames) {
                if (rawName == null || rawName.isBlank()) continue;
                String cleanName = rawName.trim();
                Optional<Skill> existing = skillRepository.findByNameIgnoreCase(cleanName);
                if (existing.isPresent()) {
                    resolved.add(existing.get());
                } else {
                    if (defaultCat == null) {
                        defaultCat = skillCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
                            SkillCategory c = new SkillCategory();
                            c.setName("Core Engineering");
                            c.setSlug("core-engineering");
                            c.setDescription("Core engineering and development technologies");
                            c.setDisplayOrder(1);
                            return skillCategoryRepository.save(c);
                        });
                    }
                    Skill newSkill = new Skill();
                    newSkill.setCategory(defaultCat);
                    newSkill.setName(cleanName);
                    newSkill.setDescription(cleanName);
                    newSkill.setProficiency(85);
                    newSkill.setFeatured(true);
                    newSkill.setDisplayOrder(10);
                    newSkill.setYearsExperience(BigDecimal.valueOf(2.0));
                    resolved.add(skillRepository.save(newSkill));
                }
            }
        }
        return resolved;
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public ProjectResponse createProject(ProjectCreateRequest request) {
        String cleanSlug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug().trim()
                : request.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

        Optional<Project> existingOpt = projectRepository.findBySlug(cleanSlug);
        Project project = existingOpt.orElseGet(Project::new);
        project.setDeletedAt(null);
        project.setTitle(request.getTitle().trim());
        project.setSlug(cleanSlug);

        String summary = (request.getShortSummary() != null && !request.getShortSummary().isBlank())
                ? request.getShortSummary().trim()
                : (request.getFullDescription() != null && !request.getFullDescription().isBlank()
                    ? request.getFullDescription().trim()
                    : request.getTitle().trim());
        String fullDesc = (request.getFullDescription() != null && !request.getFullDescription().isBlank())
                ? request.getFullDescription().trim()
                : (request.getSystemArchitecture() != null && !request.getSystemArchitecture().isBlank()
                    ? request.getSystemArchitecture().trim()
                    : summary);

        project.setShortSummary(summary);
        project.setFullDescription(fullDesc);
        if (request.getProblemStatement() != null) project.setProblemStatement(request.getProblemStatement());
        if (request.getSolutionOverview() != null) project.setSolutionOverview(request.getSolutionOverview());
        if (request.getSystemArchitecture() != null) project.setSystemArchitecture(request.getSystemArchitecture());
        if (request.getCategory() != null) project.setCategory(request.getCategory());
        project.setFeaturedImageUrl(request.getFeaturedImageUrl());
        project.setLiveDemoUrl(request.getLiveDemoUrl());
        project.setGithubRepoUrl(request.getGithubRepoUrl());
        project.setDocumentationUrl(request.getDocumentationUrl());
        project.setSeoTitle(request.getSeoTitle());
        project.setSeoDescription(request.getSeoDescription());
        project.setArchitectureStyle(request.getArchitectureStyle() != null && !request.getArchitectureStyle().isBlank()
                ? request.getArchitectureStyle()
                : (request.getCategory() != null ? request.getCategory() : "Modular Architecture"));
        project.setStatus(request.getStatus() != null ? request.getStatus() : ProjectStatus.PUBLISHED);
        project.setProgress(request.getProgress() >= 0 ? request.getProgress() : 85);
        project.setFeatured(true);
        project.setDisplayOrder(request.getDisplayOrder());
        project.setStartedAt(request.getStartedAt());
        project.setCompletedAt(request.getCompletedAt());

        Set<Skill> resolvedSkills = resolveSkills(request.getTechnologyIds(), request.getTechnologies());
        if (!resolvedSkills.isEmpty()) {
            project.setTechnologies(resolvedSkills);
        }

        Project saved = projectRepository.save(project);

        try {
            String extKey = "proj-" + saved.getId();
            DeveloperActivity activity = activityRepository.findBySourcePlatformAndExternalId("PROJECT", extKey)
                    .orElseGet(DeveloperActivity::new);
            activity.setActivityType(ActivityType.PROJECT_UPDATE);
            activity.setSourcePlatform("PROJECT");
            activity.setExternalId(extKey);
            activity.setTitle((existingOpt.isPresent() ? "Updated Project: " : "Published Project: ") + saved.getTitle());
            activity.setDescription(saved.getShortSummary());
            activity.setExternalUrl("/projects/" + saved.getSlug());
            activity.setOccurredAt(Instant.now());
            activityRepository.save(activity);
        } catch (Exception ignored) {}

        return projectMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public ProjectResponse updateProject(UUID id, ProjectUpdateRequest request) {
        return updateProjectEntity(projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id)), request);
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public ProjectResponse updateProjectByIdOrSlug(String idOrSlug, ProjectUpdateRequest request) {
        Project project = resolveProject(idOrSlug);
        return updateProjectEntity(project, request);
    }

    private ProjectResponse updateProjectEntity(Project project, ProjectUpdateRequest request) {
        project.setDeletedAt(null);

        if (request.getSlug() != null && !request.getSlug().isBlank() && !request.getSlug().equals(project.getSlug())) {
            Optional<Project> other = projectRepository.findBySlug(request.getSlug().trim());
            if (other.isEmpty() || other.get().getId().equals(project.getId())) {
                project.setSlug(request.getSlug().trim());
            }
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            project.setTitle(request.getTitle().trim());
        }
        if (request.getShortSummary() != null) {
            project.setShortSummary(request.getShortSummary().trim());
        }
        if (request.getProblemStatement() != null) {
            project.setProblemStatement(request.getProblemStatement().trim());
        }
        if (request.getSolutionOverview() != null) {
            project.setSolutionOverview(request.getSolutionOverview().trim());
        }
        if (request.getSystemArchitecture() != null) {
            project.setSystemArchitecture(request.getSystemArchitecture().trim());
        }
        if (request.getCategory() != null) {
            project.setCategory(request.getCategory().trim());
        }
        if (request.getFullDescription() != null && !request.getFullDescription().isBlank()) {
            project.setFullDescription(request.getFullDescription().trim());
        } else if (request.getSystemArchitecture() != null && !request.getSystemArchitecture().isBlank()) {
            project.setFullDescription(request.getSystemArchitecture().trim());
        } else if (request.getShortSummary() != null && !request.getShortSummary().isBlank()) {
            project.setFullDescription(request.getShortSummary().trim());
        }
        if (request.getFeaturedImageUrl() != null) project.setFeaturedImageUrl(request.getFeaturedImageUrl());
        if (request.getLiveDemoUrl() != null) project.setLiveDemoUrl(request.getLiveDemoUrl());
        if (request.getGithubRepoUrl() != null) project.setGithubRepoUrl(request.getGithubRepoUrl());
        if (request.getDocumentationUrl() != null) project.setDocumentationUrl(request.getDocumentationUrl());
        if (request.getSeoTitle() != null) project.setSeoTitle(request.getSeoTitle());
        if (request.getSeoDescription() != null) project.setSeoDescription(request.getSeoDescription());
        if (request.getArchitectureStyle() != null && !request.getArchitectureStyle().isBlank()) {
            project.setArchitectureStyle(request.getArchitectureStyle());
        } else if (request.getCategory() != null && !request.getCategory().isBlank()) {
            project.setArchitectureStyle(request.getCategory());
        }
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        if (request.getProgress() != null) project.setProgress(request.getProgress());
        if (request.getFeatured() != null) project.setFeatured(request.getFeatured());
        if (request.getDisplayOrder() != null) project.setDisplayOrder(request.getDisplayOrder());
        if (request.getStartedAt() != null) project.setStartedAt(request.getStartedAt());
        if (request.getCompletedAt() != null) project.setCompletedAt(request.getCompletedAt());

        if ((request.getTechnologyIds() != null && !request.getTechnologyIds().isEmpty())
                || (request.getTechnologies() != null && !request.getTechnologies().isEmpty())) {
            project.setTechnologies(resolveSkills(request.getTechnologyIds(), request.getTechnologies()));
        }

        Project saved = projectRepository.save(project);

        try {
            String extKey = "proj-" + saved.getId();
            DeveloperActivity activity = activityRepository.findBySourcePlatformAndExternalId("PROJECT", extKey)
                    .orElseGet(() -> activityRepository.findAll().stream()
                            .filter(a -> "PROJECT".equals(a.getSourcePlatform()) && ((a.getExternalUrl() != null && a.getExternalUrl().endsWith("/" + saved.getSlug())) || (a.getTitle() != null && a.getTitle().contains(saved.getTitle()))))
                            .findFirst()
                            .orElseGet(DeveloperActivity::new));
            activity.setActivityType(ActivityType.PROJECT_UPDATE);
            activity.setSourcePlatform("PROJECT");
            activity.setExternalId(extKey);
            activity.setTitle("Updated Project: " + saved.getTitle());
            activity.setDescription(saved.getShortSummary() != null && !saved.getShortSummary().isBlank() ? saved.getShortSummary() : ("Progress at " + saved.getProgress() + "% (" + saved.getStatus() + ")"));
            activity.setExternalUrl("/projects/" + saved.getSlug());
            activity.setOccurredAt(Instant.now());
            activityRepository.save(activity);
        } catch (Exception ignored) {}

        return projectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectBySlug(String slug) {
        Project project = projectRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "slug", slug));
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectByIdOrSlug(String idOrSlug) {
        return projectMapper.toResponse(resolveProject(idOrSlug));
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        hardDeleteProject(project);
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public void deleteProjectByIdOrSlug(String idOrSlug) {
        Project project = resolveProject(idOrSlug);
        hardDeleteProject(project);
    }

    private void hardDeleteProject(Project project) {
        String pIdStr = project.getId() != null ? "proj-" + project.getId() : "";
        String slugUrl = "/projects/" + project.getSlug();
        String pTitle = project.getTitle();
        try {
            List<DeveloperActivity> toRemove = activityRepository.findAll().stream()
                    .filter(a -> (pIdStr.equals(a.getExternalId()))
                            || (a.getExternalUrl() != null && a.getExternalUrl().equals(slugUrl))
                            || ("PROJECT".equals(a.getSourcePlatform()) && pTitle != null && a.getTitle() != null && a.getTitle().contains(pTitle)))
                    .toList();
            if (!toRemove.isEmpty()) {
                activityRepository.deleteAll(toRemove);
            }
        } catch (Exception ignored) {}

        if (project.getTechnologies() != null) {
            project.getTechnologies().clear();
        }
        projectRepository.saveAndFlush(project);
        projectRepository.delete(project);
        projectRepository.flush();
        markProjectsModifiedByAdmin();
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public void deleteAllProjects() {
        try {
            List<DeveloperActivity> projActs = activityRepository.findAll().stream()
                    .filter(a -> "PROJECT".equals(a.getSourcePlatform()))
                    .toList();
            if (!projActs.isEmpty()) {
                activityRepository.deleteAll(projActs);
            }
        } catch (Exception ignored) {}
        List<Project> all = projectRepository.findAll();
        for (Project p : all) {
            if (p.getTechnologies() != null) {
                p.getTechnologies().clear();
            }
        }
        projectRepository.saveAllAndFlush(all);
        projectRepository.deleteAll();
        projectRepository.flush();
        markProjectsModifiedByAdmin();
    }

    private void markProjectsModifiedByAdmin() {
        try {
            SiteSetting setting = siteSettingRepository.findBySettingKey("projects_managed_by_admin")
                    .orElseGet(() -> {
                        SiteSetting s = new SiteSetting();
                        s.setSettingKey("projects_managed_by_admin");
                        s.setPublic(false);
                        return s;
                    });
            setting.setSettingValue("true");
            siteSettingRepository.save(setting);
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectSummaryResponse> getProjects(ProjectStatus status, Boolean featured, String search, Pageable pageable) {
        Page<Project> page;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch) {
            String q = search.trim().toLowerCase();
            page = projectRepository.findAll((root, query, cb) -> {
                jakarta.persistence.criteria.Predicate notDeleted = cb.isNull(root.get("deletedAt"));
                jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + q + "%"),
                        cb.like(cb.lower(root.get("shortSummary")), "%" + q + "%")
                );
                if (status != null && featured != null) {
                    return cb.and(notDeleted, searchPredicate, cb.equal(root.get("status"), status), cb.equal(root.get("featured"), featured));
                } else if (status != null) {
                    return cb.and(notDeleted, searchPredicate, cb.equal(root.get("status"), status));
                } else if (featured != null) {
                    return cb.and(notDeleted, searchPredicate, cb.equal(root.get("featured"), featured));
                } else {
                    return cb.and(notDeleted, searchPredicate);
                }
            }, pageable);
        } else if (status != null && featured != null) {
            page = projectRepository.findByStatusAndFeaturedAndDeletedAtIsNull(status, featured, pageable);
        } else if (status != null) {
            page = projectRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else if (featured != null) {
            page = projectRepository.findByFeaturedAndDeletedAtIsNull(featured, pageable);
        } else {
            page = projectRepository.findAllByDeletedAtIsNull(pageable);
        }

        Page<ProjectSummaryResponse> mapped = page.map(projectMapper::toSummary);
        return PageResponse.from(mapped);
    }

    @Override
    @CacheEvict(value = {"projects", "unified_activity"}, allEntries = true)
    public MilestoneDto addMilestone(UUID projectId, MilestoneDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setProject(project);
        milestone.setTitle(dto.getTitle());
        milestone.setVersionTag(dto.getVersionTag() != null ? dto.getVersionTag() : "v1.0");
        milestone.setDescription(dto.getDescription());
        milestone.setTargetDate(dto.getTargetDate());
        milestone.setCompletionDate(dto.getCompletionDate());
        if (dto.getStatus() != null) milestone.setStatus(dto.getStatus());
        milestone.setDisplayOrder(dto.getDisplayOrder());

        ProjectMilestone saved = milestoneRepository.save(milestone);
        if (saved.getStatus() == com.raviranjan.os.domain.enums.MilestoneStatus.COMPLETED) {
            eventPublisher.publishEvent(new Events.MilestoneCompletedEvent(this, project.getTitle(), saved.getTitle(), saved.getId().toString(), "/admin/projects"));
            try {
                DeveloperActivity activity = new DeveloperActivity();
                activity.setActivityType(ActivityType.PROJECT_UPDATE);
                activity.setSourcePlatform("PROJECT");
                activity.setExternalId("ms-" + saved.getId());
                activity.setTitle("Completed Milestone: " + saved.getTitle() + " (" + project.getTitle() + ")");
                activity.setDescription(saved.getDescription() != null ? saved.getDescription() : "Milestone reached version " + saved.getVersionTag());
                activity.setExternalUrl("/projects/" + project.getSlug());
                activity.setOccurredAt(Instant.now());
                activityRepository.save(activity);
            } catch (Exception ignored) {}
        }
        return projectMapper.toMilestoneDto(saved);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public void deleteMilestone(UUID milestoneId) {
        milestoneRepository.findById(milestoneId).ifPresent(m -> {
            Project p = m.getProject();
            if (p != null && p.getMilestones() != null) {
                p.getMilestones().removeIf(item -> milestoneId.equals(item.getId()));
            }
            milestoneRepository.delete(m);
        });
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public TaskDto addTask(UUID projectId, TaskDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectTask task = new ProjectTask();
        task.setProject(project);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        task.setDisplayOrder(dto.getDisplayOrder());

        if (dto.getMilestoneId() != null) {
            task.setMilestone(milestoneRepository.findById(dto.getMilestoneId()).orElse(null));
        }

        ProjectTask saved = taskRepository.save(task);
        return projectMapper.toTaskDto(saved);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public ChangelogDto addChangelog(UUID projectId, ChangelogDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectChangelog changelog = new ProjectChangelog();
        changelog.setProject(project);
        changelog.setVersionTag(dto.getVersionTag() != null ? dto.getVersionTag() : "v1.0.0");
        changelog.setReleaseSummary(dto.getReleaseSummary());
        changelog.setCommitHash(dto.getCommitHash());
        changelog.setReleasedAt(dto.getReleasedAt() != null ? dto.getReleasedAt() : Instant.now());

        ProjectChangelog saved = changelogRepository.save(changelog);
        return projectMapper.toChangelogDto(saved);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public void deleteChangelog(UUID changelogId) {
        changelogRepository.findById(changelogId).ifPresent(c -> {
            Project p = c.getProject();
            if (p != null && p.getChangelogs() != null) {
                p.getChangelogs().removeIf(item -> changelogId.equals(item.getId()));
            }
            changelogRepository.delete(c);
        });
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public DecisionDto addDecision(UUID projectId, DecisionDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectDecision decision = new ProjectDecision();
        decision.setProject(project);
        decision.setDecisionTitle(dto.getDecisionTitle());
        decision.setProblemContext(dto.getProblemContext() != null ? dto.getProblemContext() : "Architectural trade-off evaluation");
        decision.setDecisionTaken(dto.getDecisionTaken() != null ? dto.getDecisionTaken() : dto.getDecisionTitle());
        decision.setTradeOffsConsidered(dto.getTradeOffsConsidered());
        decision.setConsequences(dto.getConsequences());
        if (dto.getStatus() != null) decision.setStatus(dto.getStatus());

        ProjectDecision saved = decisionRepository.save(decision);
        return projectMapper.toDecisionDto(saved);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public void deleteDecision(UUID decisionId) {
        decisionRepository.findById(decisionId).ifPresent(d -> {
            Project p = d.getProject();
            if (p != null && p.getDecisions() != null) {
                p.getDecisions().removeIf(item -> decisionId.equals(item.getId()));
            }
            decisionRepository.delete(d);
        });
    }
}

package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.ContributionStory;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import java.time.Instant;
import com.raviranjan.os.dto.story.ContributionStoryDto;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ContributionStoryMapper;
import com.raviranjan.os.repository.ContributionStoryRepository;
import com.raviranjan.os.repository.ProjectRepository;
import com.raviranjan.os.service.ContributionStoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ContributionStoryServiceImpl implements ContributionStoryService {

    private final ContributionStoryRepository repository;
    private final ProjectRepository projectRepository;
    private final DeveloperActivityRepository activityRepository;
    private final ContributionStoryMapper mapper;

    public ContributionStoryServiceImpl(ContributionStoryRepository repository, ProjectRepository projectRepository, DeveloperActivityRepository activityRepository, ContributionStoryMapper mapper) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.activityRepository = activityRepository;
        this.mapper = mapper;
    }

    @Override
    @CacheEvict(value = {"stories", "unified_activity"}, allEntries = true)
    public ContributionStoryDto createStory(ContributionStoryDto dto) {
        ContributionStory s = new ContributionStory();
        s.setRepository(dto.getRepository());
        s.setExternalContributionId(dto.getExternalContributionId());
        s.setTitle(dto.getTitle());
        s.setWhatIContributed(dto.getWhatIContributed());
        s.setWhyIContributed(dto.getWhyIContributed());
        s.setProblem(dto.getProblem());
        s.setWhatILearned(dto.getWhatILearned());
        s.setImpact(dto.getImpact());
        s.setTechnologies(dto.getTechnologies());
        s.setContributionDate(dto.getContributionDate());
        if (dto.getRelatedProjectId() != null) {
            s.setRelatedProject(projectRepository.findByIdAndDeletedAtIsNull(dto.getRelatedProjectId()).orElse(null));
        }
        s.setPrUrl(dto.getPrUrl());
        s.setIssueUrl(dto.getIssueUrl());

        ContributionStory saved = repository.save(s);
        try {
            String extKey = "story-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("GITHUB", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.GITHUB_PR);
            act.setSourcePlatform("GITHUB");
            act.setExternalId(extKey);
            act.setTitle("Contribution: " + saved.getTitle() + " (" + saved.getRepository() + ")");
            act.setDescription(saved.getWhatIContributed());
            act.setExternalUrl(saved.getPrUrl() != null ? saved.getPrUrl() : "#contributions");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = {"stories", "unified_activity"}, allEntries = true)
    public ContributionStoryDto updateStory(UUID id, ContributionStoryDto dto) {
        ContributionStory s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContributionStory", "id", id));
        if (dto.getRepository() != null) s.setRepository(dto.getRepository());
        if (dto.getExternalContributionId() != null) s.setExternalContributionId(dto.getExternalContributionId());
        if (dto.getTitle() != null) s.setTitle(dto.getTitle());
        if (dto.getWhatIContributed() != null) s.setWhatIContributed(dto.getWhatIContributed());
        if (dto.getWhyIContributed() != null) s.setWhyIContributed(dto.getWhyIContributed());
        if (dto.getProblem() != null) s.setProblem(dto.getProblem());
        if (dto.getWhatILearned() != null) s.setWhatILearned(dto.getWhatILearned());
        if (dto.getImpact() != null) s.setImpact(dto.getImpact());
        if (dto.getTechnologies() != null) s.setTechnologies(dto.getTechnologies());
        if (dto.getContributionDate() != null) s.setContributionDate(dto.getContributionDate());
        if (dto.getPrUrl() != null) s.setPrUrl(dto.getPrUrl());
        if (dto.getIssueUrl() != null) s.setIssueUrl(dto.getIssueUrl());

        ContributionStory saved = repository.save(s);
        try {
            String extKey = "story-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("GITHUB", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.GITHUB_PR);
            act.setSourcePlatform("GITHUB");
            act.setExternalId(extKey);
            act.setTitle("Contribution: " + saved.getTitle() + " (" + saved.getRepository() + ")");
            act.setDescription(saved.getWhatIContributed());
            act.setExternalUrl(saved.getPrUrl() != null ? saved.getPrUrl() : "#contributions");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionStoryDto getStoryById(UUID id) {
        ContributionStory s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContributionStory", "id", id));
        return mapper.toDto(s);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "stories")
    public PageResponse<ContributionStoryDto> getStories(Pageable pageable) {
        Page<ContributionStory> page = repository.findAllByOrderByContributionDateDesc(pageable);
        return PageResponse.from(page.map(mapper::toDto));
    }

    @Override
    @CacheEvict(value = {"stories", "unified_activity"}, allEntries = true)
    public void deleteStory(UUID id) {
        ContributionStory s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContributionStory", "id", id));
        try {
            String extKey = "story-" + s.getId();
            String sTitle = s.getTitle();
            var acts = activityRepository.findAll().stream()
                    .filter(a -> extKey.equals(a.getExternalId()) || (sTitle != null && a.getTitle() != null && a.getTitle().contains(sTitle)))
                    .toList();
            if (!acts.isEmpty()) activityRepository.deleteAll(acts);
        } catch (Exception ignored) {}
        repository.delete(s);
    }
}

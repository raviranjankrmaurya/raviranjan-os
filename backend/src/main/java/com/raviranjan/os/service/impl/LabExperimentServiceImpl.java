package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.LabExperiment;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import java.time.Instant;
import com.raviranjan.os.domain.enums.LabStatus;
import com.raviranjan.os.dto.lab.LabExperimentDto;
import com.raviranjan.os.exception.BadRequestException;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.LabExperimentMapper;
import com.raviranjan.os.repository.LabExperimentRepository;
import com.raviranjan.os.service.LabExperimentService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class LabExperimentServiceImpl implements LabExperimentService {

    private final LabExperimentRepository repository;
    private final DeveloperActivityRepository activityRepository;
    private final LabExperimentMapper mapper;

    public LabExperimentServiceImpl(LabExperimentRepository repository, DeveloperActivityRepository activityRepository, LabExperimentMapper mapper) {
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.mapper = mapper;
    }

    @Override
    @CacheEvict(value = {"lab", "unified_activity"}, allEntries = true)
    public LabExperimentDto createLab(LabExperimentDto dto) {
        if (repository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("Lab experiment slug already exists: " + dto.getSlug());
        }
        LabExperiment lab = new LabExperiment();
        lab.setSlug(dto.getSlug());
        lab.setTitle(dto.getTitle());
        lab.setObjective(dto.getObjective());
        lab.setDescription(dto.getDescription());
        lab.setTechnologies(dto.getTechnologies());
        lab.setStatus(dto.getStatus());
        lab.setProgress(dto.getProgress());
        lab.setResults(dto.getResults());
        lab.setGithubUrl(dto.getGithubUrl());
        lab.setDemoUrl(dto.getDemoUrl());
        lab.setScreenshots(dto.getScreenshots());
        lab.setLessonsLearned(dto.getLessonsLearned());
        lab.setFutureWork(dto.getFutureWork());
        lab.setConductedAt(dto.getConductedAt());

        LabExperiment saved = repository.save(lab);
        try {
            String extKey = "lab-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("LAB", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.PROJECT_UPDATE);
            act.setSourcePlatform("LAB");
            act.setExternalId(extKey);
            act.setTitle("Lab Experiment: " + saved.getTitle());
            act.setDescription(saved.getDescription() != null ? saved.getDescription() : saved.getObjective());
            act.setExternalUrl(saved.getGithubUrl() != null ? saved.getGithubUrl() : "#lab");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = {"lab", "unified_activity"}, allEntries = true)
    public LabExperimentDto updateLab(UUID id, LabExperimentDto dto) {
        LabExperiment lab = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LabExperiment", "id", id));

        if (dto.getTitle() != null) lab.setTitle(dto.getTitle());
        if (dto.getObjective() != null) lab.setObjective(dto.getObjective());
        if (dto.getDescription() != null) lab.setDescription(dto.getDescription());
        if (dto.getTechnologies() != null) lab.setTechnologies(dto.getTechnologies());
        if (dto.getStatus() != null) lab.setStatus(dto.getStatus());
        lab.setProgress(dto.getProgress());
        if (dto.getResults() != null) lab.setResults(dto.getResults());
        if (dto.getGithubUrl() != null) lab.setGithubUrl(dto.getGithubUrl());
        if (dto.getDemoUrl() != null) lab.setDemoUrl(dto.getDemoUrl());
        if (dto.getScreenshots() != null) lab.setScreenshots(dto.getScreenshots());
        if (dto.getLessonsLearned() != null) lab.setLessonsLearned(dto.getLessonsLearned());
        if (dto.getFutureWork() != null) lab.setFutureWork(dto.getFutureWork());
        if (dto.getConductedAt() != null) lab.setConductedAt(dto.getConductedAt());

        LabExperiment saved = repository.save(lab);
        try {
            String extKey = "lab-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("LAB", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.PROJECT_UPDATE);
            act.setSourcePlatform("LAB");
            act.setExternalId(extKey);
            act.setTitle("Lab Experiment: " + saved.getTitle());
            act.setDescription(saved.getDescription() != null ? saved.getDescription() : saved.getObjective());
            act.setExternalUrl(saved.getGithubUrl() != null ? saved.getGithubUrl() : "#lab");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "lab", key = "#slug")
    public LabExperimentDto getLabBySlug(String slug) {
        LabExperiment lab = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("LabExperiment", "slug", slug));
        return mapper.toDto(lab);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LabExperimentDto> getLabs(LabStatus status, Pageable pageable) {
        Page<LabExperiment> page = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return PageResponse.from(page.map(mapper::toDto));
    }

    @Override
    @CacheEvict(value = {"lab", "unified_activity"}, allEntries = true)
    public void deleteLab(UUID id) {
        LabExperiment lab = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LabExperiment", "id", id));
        try {
            String extKey = "lab-" + lab.getId();
            String lTitle = lab.getTitle();
            var acts = activityRepository.findAll().stream()
                    .filter(a -> extKey.equals(a.getExternalId()) || (lTitle != null && a.getTitle() != null && a.getTitle().contains(lTitle)))
                    .toList();
            if (!acts.isEmpty()) activityRepository.deleteAll(acts);
        } catch (Exception ignored) {}
        repository.delete(lab);
    }
}

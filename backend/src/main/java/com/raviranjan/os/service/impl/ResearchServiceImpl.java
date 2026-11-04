package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.entity.Research;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.repository.DeveloperActivityRepository;
import java.time.Instant;
import com.raviranjan.os.domain.entity.ResearchTag;
import com.raviranjan.os.domain.enums.ResearchStatus;
import com.raviranjan.os.dto.research.ResearchDto;
import com.raviranjan.os.exception.BadRequestException;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ResearchMapper;
import com.raviranjan.os.repository.ResearchRepository;
import com.raviranjan.os.repository.ResearchTagRepository;
import com.raviranjan.os.service.ResearchService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ResearchServiceImpl implements ResearchService {

    private final ResearchRepository researchRepository;
    private final ResearchTagRepository tagRepository;
    private final DeveloperActivityRepository activityRepository;
    private final ResearchMapper mapper;

    public ResearchServiceImpl(ResearchRepository researchRepository, ResearchTagRepository tagRepository, DeveloperActivityRepository activityRepository, ResearchMapper mapper) {
        this.researchRepository = researchRepository;
        this.tagRepository = tagRepository;
        this.activityRepository = activityRepository;
        this.mapper = mapper;
    }

    @Override
    @CacheEvict(value = {"research", "unified_activity"}, allEntries = true)
    public ResearchDto createResearch(ResearchDto dto) {
        if (researchRepository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("Research slug already exists: " + dto.getSlug());
        }
        Research r = new Research();
        r.setSlug(dto.getSlug());
        r.setTitle(dto.getTitle());
        r.setAbstractText(dto.getAbstractText());
        r.setAuthors(dto.getAuthors());
        r.setStatus(dto.getStatus());
        r.setPublication(dto.getPublication());
        r.setDoi(dto.getDoi());
        r.setPdfUrl(dto.getPdfUrl());
        r.setGithubUrl(dto.getGithubUrl());
        r.setDatasetUrl(dto.getDatasetUrl());
        r.setMethodology(dto.getMethodology());
        r.setResults(dto.getResults());
        r.setLessonsLearned(dto.getLessonsLearned());
        r.setPublishedDate(dto.getPublishedDate());

        if (dto.getTags() != null) {
            Set<ResearchTag> tags = new HashSet<>();
            for (String tagName : dto.getTags()) {
                String slug = tagName.toLowerCase().replaceAll("[^a-z0-9]", "-");
                ResearchTag tag = tagRepository.findBySlug(slug).orElseGet(() -> {
                    ResearchTag nt = new ResearchTag();
                    nt.setName(tagName);
                    nt.setSlug(slug);
                    return tagRepository.save(nt);
                });
                tags.add(tag);
            }
            r.setTags(tags);
        }

        Research saved = researchRepository.save(r);
        try {
            String extKey = "research-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("RESEARCH", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.RESEARCH_UPDATE);
            act.setSourcePlatform("RESEARCH");
            act.setExternalId(extKey);
            act.setTitle("Research Paper: " + saved.getTitle());
            act.setDescription(saved.getAbstractText());
            act.setExternalUrl(saved.getGithubUrl() != null ? saved.getGithubUrl() : "#research");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = {"research", "unified_activity"}, allEntries = true)
    public ResearchDto updateResearch(UUID id, ResearchDto dto) {
        Research r = researchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research", "id", id));
        if (dto.getTitle() != null) r.setTitle(dto.getTitle());
        if (dto.getAbstractText() != null) r.setAbstractText(dto.getAbstractText());
        if (dto.getAuthors() != null) r.setAuthors(dto.getAuthors());
        if (dto.getStatus() != null) r.setStatus(dto.getStatus());
        if (dto.getPublication() != null) r.setPublication(dto.getPublication());
        if (dto.getDoi() != null) r.setDoi(dto.getDoi());
        if (dto.getPdfUrl() != null) r.setPdfUrl(dto.getPdfUrl());
        if (dto.getGithubUrl() != null) r.setGithubUrl(dto.getGithubUrl());
        if (dto.getDatasetUrl() != null) r.setDatasetUrl(dto.getDatasetUrl());
        if (dto.getMethodology() != null) r.setMethodology(dto.getMethodology());
        if (dto.getResults() != null) r.setResults(dto.getResults());
        if (dto.getLessonsLearned() != null) r.setLessonsLearned(dto.getLessonsLearned());
        if (dto.getPublishedDate() != null) r.setPublishedDate(dto.getPublishedDate());

        Research saved = researchRepository.save(r);
        try {
            String extKey = "research-" + saved.getId();
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("RESEARCH", extKey)
                    .orElseGet(DeveloperActivity::new);
            act.setActivityType(ActivityType.RESEARCH_UPDATE);
            act.setSourcePlatform("RESEARCH");
            act.setExternalId(extKey);
            act.setTitle("Research Paper: " + saved.getTitle());
            act.setDescription(saved.getAbstractText());
            act.setExternalUrl(saved.getGithubUrl() != null ? saved.getGithubUrl() : "#research");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "research", key = "#slug")
    public ResearchDto getResearchBySlug(String slug) {
        Research r = researchRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Research", "slug", slug));
        return mapper.toDto(r);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResearchDto> getResearch(ResearchStatus status, Pageable pageable) {
        Page<Research> page = (status == null)
                ? researchRepository.findAll(pageable)
                : researchRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(mapper::toDto));
    }

    @Override
    @CacheEvict(value = {"research", "unified_activity"}, allEntries = true)
    public void deleteResearch(UUID id) {
        Research r = researchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research", "id", id));
        try {
            String extKey = "research-" + r.getId();
            String rTitle = r.getTitle();
            var acts = activityRepository.findAll().stream()
                    .filter(a -> extKey.equals(a.getExternalId()) || (rTitle != null && a.getTitle() != null && a.getTitle().contains(rTitle)))
                    .toList();
            if (!acts.isEmpty()) activityRepository.deleteAll(acts);
        } catch (Exception ignored) {}
        researchRepository.delete(r);
    }
}

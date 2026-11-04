package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.LabExperiment;
import com.raviranjan.os.dto.lab.LabExperimentDto;
import org.springframework.stereotype.Component;

@Component
public class LabExperimentMapper {

    public LabExperimentDto toDto(LabExperiment entity) {
        if (entity == null) return null;
        LabExperimentDto dto = new LabExperimentDto();
        dto.setId(entity.getId());
        dto.setSlug(entity.getSlug());
        dto.setTitle(entity.getTitle());
        dto.setObjective(entity.getObjective());
        dto.setDescription(entity.getDescription());
        dto.setTechnologies(entity.getTechnologies());
        dto.setStatus(entity.getStatus());
        dto.setProgress(entity.getProgress());
        dto.setResults(entity.getResults());
        dto.setGithubUrl(entity.getGithubUrl());
        dto.setDemoUrl(entity.getDemoUrl());
        dto.setScreenshots(entity.getScreenshots());
        dto.setLessonsLearned(entity.getLessonsLearned());
        dto.setFutureWork(entity.getFutureWork());
        dto.setConductedAt(entity.getConductedAt());
        return dto;
    }
}

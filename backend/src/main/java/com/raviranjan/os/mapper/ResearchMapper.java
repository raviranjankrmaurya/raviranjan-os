package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.Research;
import com.raviranjan.os.domain.entity.ResearchTag;
import com.raviranjan.os.dto.research.ResearchDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ResearchMapper {

    public ResearchDto toDto(Research entity) {
        if (entity == null) return null;
        ResearchDto dto = new ResearchDto();
        dto.setId(entity.getId());
        dto.setSlug(entity.getSlug());
        dto.setTitle(entity.getTitle());
        dto.setAbstractText(entity.getAbstractText());
        dto.setAuthors(entity.getAuthors());
        dto.setStatus(entity.getStatus());
        dto.setPublication(entity.getPublication());
        dto.setDoi(entity.getDoi());
        dto.setPdfUrl(entity.getPdfUrl());
        dto.setGithubUrl(entity.getGithubUrl());
        dto.setDatasetUrl(entity.getDatasetUrl());
        dto.setMethodology(entity.getMethodology());
        dto.setResults(entity.getResults());
        dto.setLessonsLearned(entity.getLessonsLearned());
        dto.setPublishedDate(entity.getPublishedDate());
        if (entity.getTags() != null) {
            dto.setTags(entity.getTags().stream().map(ResearchTag::getName).collect(Collectors.toSet()));
        }
        return dto;
    }
}

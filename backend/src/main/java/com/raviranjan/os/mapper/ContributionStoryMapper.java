package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.ContributionStory;
import com.raviranjan.os.dto.story.ContributionStoryDto;
import org.springframework.stereotype.Component;

@Component
public class ContributionStoryMapper {

    public ContributionStoryDto toDto(ContributionStory entity) {
        if (entity == null) return null;
        ContributionStoryDto dto = new ContributionStoryDto();
        dto.setId(entity.getId());
        dto.setRepository(entity.getRepository());
        dto.setExternalContributionId(entity.getExternalContributionId());
        dto.setTitle(entity.getTitle());
        dto.setWhatIContributed(entity.getWhatIContributed());
        dto.setWhyIContributed(entity.getWhyIContributed());
        dto.setProblem(entity.getProblem());
        dto.setWhatILearned(entity.getWhatILearned());
        dto.setImpact(entity.getImpact());
        dto.setTechnologies(entity.getTechnologies());
        dto.setContributionDate(entity.getContributionDate());
        if (entity.getRelatedProject() != null) {
            dto.setRelatedProjectId(entity.getRelatedProject().getId());
        }
        dto.setPrUrl(entity.getPrUrl());
        dto.setIssueUrl(entity.getIssueUrl());
        return dto;
    }
}

package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.Project;
import com.raviranjan.os.domain.entity.Skill;
import com.raviranjan.os.domain.entity.SkillCategory;
import com.raviranjan.os.dto.skill.SkillCategoryDto;
import com.raviranjan.os.dto.skill.SkillDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SkillMapper {

    public SkillDto toDto(Skill entity) {
        if (entity == null) return null;
        SkillDto dto = new SkillDto();
        dto.setId(entity.getId());
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setIcon(entity.getIcon());
        dto.setProficiency(entity.getProficiency());
        dto.setFeatured(entity.isFeatured());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setYearsExperience(entity.getYearsExperience());
        if (entity.getRelatedProjects() != null) {
            dto.setRelatedProjectTitles(entity.getRelatedProjects().stream()
                    .map(Project::getTitle)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public SkillCategoryDto toCategoryDto(SkillCategory entity) {
        if (entity == null) return null;
        SkillCategoryDto dto = new SkillCategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setDescription(entity.getDescription());
        dto.setDisplayOrder(entity.getDisplayOrder());
        if (entity.getSkills() != null) {
            dto.setSkills(entity.getSkills().stream().map(this::toDto).collect(Collectors.toList()));
        }
        return dto;
    }
}

package com.raviranjan.os.service;

import com.raviranjan.os.dto.skill.SkillCategoryDto;
import com.raviranjan.os.dto.skill.SkillDto;

import java.util.List;
import java.util.UUID;

public interface SkillService {
    List<SkillCategoryDto> getAllCategoriesWithSkills();
    SkillDto createSkill(SkillDto dto);
    SkillDto updateSkill(UUID id, SkillDto dto);
    void deleteSkill(UUID id);
    SkillCategoryDto createCategory(SkillCategoryDto dto);
    void deleteCategory(UUID id);
}

package com.raviranjan.os.service.impl;

import com.raviranjan.os.domain.entity.Skill;
import com.raviranjan.os.domain.entity.SkillCategory;
import com.raviranjan.os.dto.skill.SkillCategoryDto;
import com.raviranjan.os.dto.skill.SkillDto;
import com.raviranjan.os.exception.BadRequestException;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.SkillMapper;
import com.raviranjan.os.repository.SkillCategoryRepository;
import com.raviranjan.os.repository.SkillRepository;
import com.raviranjan.os.service.SkillService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final SkillMapper skillMapper;

    public SkillServiceImpl(SkillRepository skillRepository, SkillCategoryRepository categoryRepository, SkillMapper skillMapper) {
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.skillMapper = skillMapper;
    }

    private SkillCategory resolveCategory(UUID categoryId, String categoryName) {
        if (categoryId != null) {
            Optional<SkillCategory> byId = categoryRepository.findById(categoryId);
            if (byId.isPresent()) return byId.get();
        }
        if (categoryName != null && !categoryName.isBlank()) {
            String clean = categoryName.trim();
            String slug = clean.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            Optional<SkillCategory> existing = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(clean) || c.getSlug().equalsIgnoreCase(slug) || c.getName().toLowerCase().contains(clean.toLowerCase()))
                    .findFirst();
            if (existing.isPresent()) return existing.get();

            SkillCategory created = new SkillCategory();
            created.setName(clean);
            created.setSlug(slug.isBlank() ? "skills-" + System.currentTimeMillis() : slug);
            created.setDescription(clean);
            created.setDisplayOrder(5);
            return categoryRepository.save(created);
        }
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream().findFirst().orElseGet(() -> {
            SkillCategory c = new SkillCategory();
            c.setName("Core Engineering");
            c.setSlug("core-engineering");
            c.setDescription("Core engineering technologies");
            c.setDisplayOrder(1);
            return categoryRepository.save(c);
        });
    }

    private int resolveProficiency(SkillDto dto) {
        if (dto.getProficiency() > 0) return dto.getProficiency();
        if (dto.getProficiencyLevel() != null) {
            String lvl = dto.getProficiencyLevel().toUpperCase();
            if (lvl.contains("ADVANCED")) return 92;
            if (lvl.contains("INTERMEDIATE")) return 80;
            if (lvl.contains("BEGINNER")) return 65;
        }
        return 85;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "skills")
    public List<SkillCategoryDto> getAllCategoriesWithSkills() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(skillMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "skills", allEntries = true)
    public SkillDto createSkill(SkillDto dto) {
        SkillCategory category = resolveCategory(dto.getCategoryId(), dto.getCategoryName());
        Skill skill = skillRepository.findByNameIgnoreCase(dto.getName().trim()).orElseGet(Skill::new);
        skill.setCategory(category);
        skill.setName(dto.getName().trim());
        skill.setDescription(dto.getDescription() != null ? dto.getDescription() : dto.getName().trim());
        skill.setIcon(dto.getIcon() != null ? dto.getIcon() : "code");
        skill.setProficiency(resolveProficiency(dto));
        skill.setFeatured(true);
        skill.setDisplayOrder(dto.getDisplayOrder() > 0 ? dto.getDisplayOrder() : 1);
        skill.setYearsExperience(dto.getYearsExperience() != null ? dto.getYearsExperience() : BigDecimal.valueOf(2.0));

        Skill saved = skillRepository.save(skill);
        return skillMapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "skills", allEntries = true)
    public SkillDto updateSkill(UUID id, SkillDto dto) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));

        if (dto.getCategoryId() != null || (dto.getCategoryName() != null && !dto.getCategoryName().isBlank())) {
            skill.setCategory(resolveCategory(dto.getCategoryId(), dto.getCategoryName()));
        }
        if (dto.getName() != null && !dto.getName().isBlank()) skill.setName(dto.getName().trim());
        if (dto.getDescription() != null) skill.setDescription(dto.getDescription());
        if (dto.getIcon() != null) skill.setIcon(dto.getIcon());
        skill.setProficiency(resolveProficiency(dto));
        skill.setFeatured(dto.isFeatured());
        if (dto.getDisplayOrder() > 0) skill.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getYearsExperience() != null) skill.setYearsExperience(dto.getYearsExperience());

        Skill saved = skillRepository.save(skill);
        return skillMapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "skills", allEntries = true)
    public void deleteSkill(UUID id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));
        if (skill.getRelatedProjects() != null) {
            skill.getRelatedProjects().forEach(p -> p.getTechnologies().remove(skill));
        }
        skillRepository.delete(skill);
    }

    @Override
    @CacheEvict(value = "skills", allEntries = true)
    public SkillCategoryDto createCategory(SkillCategoryDto dto) {
        if (categoryRepository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("Category slug already exists: " + dto.getSlug());
        }
        SkillCategory category = new SkillCategory();
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());

        SkillCategory saved = categoryRepository.save(category);
        return skillMapper.toCategoryDto(saved);
    }

    @Override
    @CacheEvict(value = "skills", allEntries = true)
    public void deleteCategory(UUID id) {
        SkillCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "id", id));
        categoryRepository.delete(category);
    }
}

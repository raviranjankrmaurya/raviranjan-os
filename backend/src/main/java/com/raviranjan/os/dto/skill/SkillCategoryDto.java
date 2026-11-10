package com.raviranjan.os.dto.skill;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public class SkillCategoryDto {
    private UUID id;
    @NotBlank(message = "Category name is required")
    private String name;
    @NotBlank(message = "Slug is required")
    private String slug;
    private String description;
    private int displayOrder;
    private List<SkillDto> skills;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<SkillDto> getSkills() { return skills; }
    public void setSkills(List<SkillDto> skills) { this.skills = skills; }
}

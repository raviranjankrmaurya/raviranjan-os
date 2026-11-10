package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.skill.SkillCategoryDto;
import com.raviranjan.os.dto.skill.SkillDto;
import com.raviranjan.os.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Skills", description = "Technical skills and categories management")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/api/skills")
    @Operation(summary = "List all skill categories and skills (Public)")
    public ResponseEntity<ApiResponse<List<SkillCategoryDto>>> getPublicSkills() {
        return ResponseEntity.ok(ApiResponse.success(skillService.getAllCategoriesWithSkills()));
    }

    @PostMapping("/api/admin/skills")
    @Operation(summary = "Create skill (Admin)")
    public ResponseEntity<ApiResponse<SkillDto>> createSkill(@Valid @RequestBody SkillDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Skill created", skillService.createSkill(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/skills/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update skill (Admin)")
    public ResponseEntity<ApiResponse<SkillDto>> updateSkill(@PathVariable UUID id, @Valid @RequestBody SkillDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Skill updated", skillService.updateSkill(id, dto)));
    }

    @DeleteMapping("/api/admin/skills/{id}")
    @Operation(summary = "Delete skill (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable UUID id) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok(ApiResponse.success("Skill deleted", null));
    }

    @PostMapping("/api/admin/skills/categories")
    @Operation(summary = "Create skill category (Admin)")
    public ResponseEntity<ApiResponse<SkillCategoryDto>> createCategory(@Valid @RequestBody SkillCategoryDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Category created", skillService.createCategory(dto)), HttpStatus.CREATED);
    }
}

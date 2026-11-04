package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.career.*;
import com.raviranjan.os.service.CareerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Career & Credentials", description = "Work history, education, achievements, and certifications")
public class CareerController {

    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping("/api/experiences")
    @Operation(summary = "Get career work history (Public)")
    public ResponseEntity<ApiResponse<List<ExperienceDto>>> getExperiences() {
        return ResponseEntity.ok(ApiResponse.success(careerService.getAllExperiences()));
    }

    @PostMapping("/api/admin/experiences")
    @Operation(summary = "Create experience entry (Admin)")
    public ResponseEntity<ApiResponse<ExperienceDto>> createExperience(@RequestBody ExperienceDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Experience added to Neon DB", careerService.createExperience(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/experiences/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update experience entry (Admin)")
    public ResponseEntity<ApiResponse<ExperienceDto>> updateExperience(@PathVariable UUID id, @RequestBody ExperienceDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Experience updated in Neon DB", careerService.updateExperience(id, dto)));
    }

    @DeleteMapping("/api/admin/experiences/{id}")
    @Operation(summary = "Delete experience entry (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteExperience(@PathVariable UUID id) {
        careerService.deleteExperience(id);
        return ResponseEntity.ok(ApiResponse.success("Experience removed from Neon DB", null));
    }

    @GetMapping("/api/education")
    @Operation(summary = "Get education records (Public)")
    public ResponseEntity<ApiResponse<List<EducationDto>>> getEducation() {
        return ResponseEntity.ok(ApiResponse.success(careerService.getAllEducation()));
    }

    @PostMapping("/api/admin/education")
    @Operation(summary = "Create education record (Admin)")
    public ResponseEntity<ApiResponse<EducationDto>> createEducation(@RequestBody EducationDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Education added to Neon DB", careerService.createEducation(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/education/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update education record (Admin)")
    public ResponseEntity<ApiResponse<EducationDto>> updateEducation(@PathVariable UUID id, @RequestBody EducationDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Education updated in Neon DB", careerService.updateEducation(id, dto)));
    }

    @DeleteMapping("/api/admin/education/{id}")
    @Operation(summary = "Delete education record (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(@PathVariable UUID id) {
        careerService.deleteEducation(id);
        return ResponseEntity.ok(ApiResponse.success("Education removed from Neon DB", null));
    }

    @GetMapping("/api/achievements")
    @Operation(summary = "Get verified achievements (Public)")
    public ResponseEntity<ApiResponse<List<AchievementDto>>> getAchievements() {
        return ResponseEntity.ok(ApiResponse.success(careerService.getAllAchievements()));
    }

    @PostMapping("/api/admin/achievements")
    @Operation(summary = "Create achievement (Admin)")
    public ResponseEntity<ApiResponse<AchievementDto>> createAchievement(@RequestBody AchievementDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Achievement added to Neon DB", careerService.createAchievement(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/achievements/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update achievement (Admin)")
    public ResponseEntity<ApiResponse<AchievementDto>> updateAchievement(@PathVariable UUID id, @RequestBody AchievementDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Achievement updated in Neon DB", careerService.updateAchievement(id, dto)));
    }

    @DeleteMapping("/api/admin/achievements/{id}")
    @Operation(summary = "Delete achievement (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteAchievement(@PathVariable UUID id) {
        careerService.deleteAchievement(id);
        return ResponseEntity.ok(ApiResponse.success("Achievement deleted from Neon DB", null));
    }

    @GetMapping("/api/certifications")
    @Operation(summary = "Get professional certifications (Public)")
    public ResponseEntity<ApiResponse<List<CertificationDto>>> getCertifications() {
        return ResponseEntity.ok(ApiResponse.success(careerService.getAllCertifications()));
    }

    @PostMapping("/api/admin/certifications")
    @Operation(summary = "Create certification (Admin)")
    public ResponseEntity<ApiResponse<CertificationDto>> createCertification(@RequestBody CertificationDto dto) {
        return new ResponseEntity<>(ApiResponse.success("Certification added to Neon DB", careerService.createCertification(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/api/admin/certifications/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update certification (Admin)")
    public ResponseEntity<ApiResponse<CertificationDto>> updateCertification(@PathVariable UUID id, @RequestBody CertificationDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Certification updated in Neon DB", careerService.updateCertification(id, dto)));
    }

    @DeleteMapping("/api/admin/certifications/{id}")
    @Operation(summary = "Delete certification (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteCertification(@PathVariable UUID id) {
        careerService.deleteCertification(id);
        return ResponseEntity.ok(ApiResponse.success("Certification deleted from Neon DB", null));
    }
}

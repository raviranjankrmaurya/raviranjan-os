package com.raviranjan.os.service;

import com.raviranjan.os.dto.career.*;
import java.util.List;
import java.util.UUID;

public interface CareerService {
    List<ExperienceDto> getAllExperiences();
    ExperienceDto createExperience(ExperienceDto dto);
    ExperienceDto updateExperience(UUID id, ExperienceDto dto);
    void deleteExperience(UUID id);

    List<EducationDto> getAllEducation();
    EducationDto createEducation(EducationDto dto);
    EducationDto updateEducation(UUID id, EducationDto dto);
    void deleteEducation(UUID id);

    List<AchievementDto> getAllAchievements();
    AchievementDto createAchievement(AchievementDto dto);
    AchievementDto updateAchievement(UUID id, AchievementDto dto);
    void deleteAchievement(UUID id);

    List<CertificationDto> getAllCertifications();
    CertificationDto createCertification(CertificationDto dto);
    CertificationDto updateCertification(UUID id, CertificationDto dto);
    void deleteCertification(UUID id);
}

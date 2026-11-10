package com.raviranjan.os.service.impl;

import com.raviranjan.os.domain.entity.*;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.dto.career.*;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.CareerMapper;
import com.raviranjan.os.repository.*;
import com.raviranjan.os.service.CareerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CareerServiceImpl implements CareerService {

    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final AchievementRepository achievementRepository;
    private final CertificationRepository certificationRepository;
    private final DeveloperActivityRepository activityRepository;
    private final CareerMapper mapper;

    public CareerServiceImpl(
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            AchievementRepository achievementRepository,
            CertificationRepository certificationRepository,
            DeveloperActivityRepository activityRepository,
            CareerMapper mapper
    ) {
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.achievementRepository = achievementRepository;
        this.certificationRepository = certificationRepository;
        this.activityRepository = activityRepository;
        this.mapper = mapper;
    }

    private void syncCareerActivity(String key, String title, String description, String url) {
        try {
            DeveloperActivity act = activityRepository.findBySourcePlatformAndExternalId("CMS", key)
                    .orElseGet(DeveloperActivity::new);
            act.setSourcePlatform("CMS");
            act.setExternalId(key);
            act.setActivityType(ActivityType.MILESTONE_COMPLETED);
            act.setTitle(title);
            act.setDescription(description);
            act.setExternalUrl(url != null && !url.isBlank() ? url : "/#experience");
            act.setOccurredAt(Instant.now());
            activityRepository.save(act);
        } catch (Exception ignored) {}
    }

    private void deleteCareerActivity(String key) {
        try {
            activityRepository.findBySourcePlatformAndExternalId("CMS", key)
                    .ifPresent(activityRepository::delete);
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "experiences")
    public List<ExperienceDto> getAllExperiences() {
        return experienceRepository.findAllByOrderByStartDateDesc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "experiences", allEntries = true)
    public ExperienceDto createExperience(ExperienceDto dto) {
        Experience e = new Experience();
        e.setCompany(dto.getCompany());
        e.setRole(dto.getRole());
        e.setEmploymentType(dto.getEmploymentType() != null && !dto.getEmploymentType().isBlank() ? dto.getEmploymentType() : "Full-time");
        e.setLocation(dto.getLocation() != null && !dto.getLocation().isBlank() ? dto.getLocation() : "New Delhi, India");
        e.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now());
        e.setEndDate(dto.getEndDate());
        e.setCurrent(dto.isCurrent() || dto.getEndDate() == null);
        e.setDescription(dto.getDescription());
        e.setResponsibilities(dto.getResponsibilities());
        e.setTechnologies(dto.getTechnologies());
        e.setAchievements(dto.getAchievements());
        e.setOfferLetterUrl(dto.getOfferLetterUrl());
        e.setCertificateUrl(dto.getCertificateUrl());
        e.setDisplayOrder(dto.getDisplayOrder());
        Experience saved = experienceRepository.save(e);
        boolean isIntern = saved.getEmploymentType() != null && saved.getEmploymentType().toUpperCase().contains("INTERN");
        syncCareerActivity(
                "EXP_" + saved.getId(),
                (isIntern ? "Internship: " : "Experience: ") + saved.getRole() + " @ " + saved.getCompany(),
                saved.getDescription(),
                saved.getCertificateUrl() != null && !saved.getCertificateUrl().isBlank() ? saved.getCertificateUrl() : saved.getOfferLetterUrl()
        );
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "experiences", allEntries = true)
    public ExperienceDto updateExperience(UUID id, ExperienceDto dto) {
        Experience e = experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", id));
        if (dto.getCompany() != null) e.setCompany(dto.getCompany());
        if (dto.getRole() != null) e.setRole(dto.getRole());
        if (dto.getEmploymentType() != null) e.setEmploymentType(dto.getEmploymentType());
        if (dto.getLocation() != null) e.setLocation(dto.getLocation());
        if (dto.getStartDate() != null) e.setStartDate(dto.getStartDate());
        e.setEndDate(dto.getEndDate());
        e.setCurrent(dto.isCurrent() && dto.getEndDate() == null);
        if (dto.getDescription() != null) e.setDescription(dto.getDescription());
        if (dto.getResponsibilities() != null) e.setResponsibilities(dto.getResponsibilities());
        if (dto.getTechnologies() != null) e.setTechnologies(dto.getTechnologies());
        if (dto.getAchievements() != null) e.setAchievements(dto.getAchievements());
        if (dto.getOfferLetterUrl() != null) e.setOfferLetterUrl(dto.getOfferLetterUrl());
        if (dto.getCertificateUrl() != null) e.setCertificateUrl(dto.getCertificateUrl());
        Experience saved = experienceRepository.save(e);
        boolean isIntern = saved.getEmploymentType() != null && saved.getEmploymentType().toUpperCase().contains("INTERN");
        syncCareerActivity(
                "EXP_" + saved.getId(),
                (isIntern ? "Updated Internship: " : "Updated Experience: ") + saved.getRole() + " @ " + saved.getCompany(),
                saved.getDescription(),
                saved.getCertificateUrl() != null && !saved.getCertificateUrl().isBlank() ? saved.getCertificateUrl() : saved.getOfferLetterUrl()
        );
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "experiences", allEntries = true)
    public void deleteExperience(UUID id) {
        Experience e = experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", id));
        deleteCareerActivity("EXP_" + e.getId());
        experienceRepository.delete(e);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "education")
    public List<EducationDto> getAllEducation() {
        return educationRepository.findAllByOrderByStartDateDesc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "education", allEntries = true)
    public EducationDto createEducation(EducationDto dto) {
        Education e = new Education();
        e.setInstitution(dto.getInstitution());
        e.setDegree(dto.getDegree());
        e.setField(dto.getField() != null && !dto.getField().isBlank() ? dto.getField() : dto.getDegree());
        e.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now());
        e.setEndDate(dto.getEndDate());
        e.setGrade(dto.getGrade() != null && !dto.getGrade().isBlank() ? dto.getGrade() : "Enrolled");
        e.setDescription(dto.getDescription());
        e.setDisplayOrder(dto.getDisplayOrder());
        Education saved = educationRepository.save(e);
        syncCareerActivity("EDU_" + saved.getId(), "Education: " + saved.getDegree() + " @ " + saved.getInstitution(), saved.getDescription(), "/#experience");
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "education", allEntries = true)
    public EducationDto updateEducation(UUID id, EducationDto dto) {
        Education e = educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", id));
        if (dto.getInstitution() != null) e.setInstitution(dto.getInstitution());
        if (dto.getDegree() != null) e.setDegree(dto.getDegree());
        if (dto.getField() != null) e.setField(dto.getField());
        if (dto.getStartDate() != null) e.setStartDate(dto.getStartDate());
        e.setEndDate(dto.getEndDate());
        if (dto.getGrade() != null) e.setGrade(dto.getGrade());
        if (dto.getDescription() != null) e.setDescription(dto.getDescription());
        Education saved = educationRepository.save(e);
        syncCareerActivity("EDU_" + saved.getId(), "Updated Education: " + saved.getDegree() + " @ " + saved.getInstitution(), saved.getDescription(), "/#experience");
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "education", allEntries = true)
    public void deleteEducation(UUID id) {
        Education e = educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", id));
        deleteCareerActivity("EDU_" + e.getId());
        educationRepository.delete(e);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "achievements")
    public List<AchievementDto> getAllAchievements() {
        return achievementRepository.findAllByOrderByDateDesc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "achievements", allEntries = true)
    public AchievementDto createAchievement(AchievementDto dto) {
        Achievement a = new Achievement();
        a.setTitle(dto.getTitle());
        a.setCategory(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory() : "ACHIEVEMENT");
        a.setOrganization(dto.getOrganization() != null && !dto.getOrganization().isBlank() ? dto.getOrganization() : "Verified Platform");
        a.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        a.setDescription(dto.getDescription());
        a.setProofUrl(dto.getProofUrl());
        a.setCertificate(dto.getCertificate());
        a.setFeatured(true);
        a.setDisplayOrder(dto.getDisplayOrder());
        Achievement saved = achievementRepository.save(a);
        syncCareerActivity("ACH_" + saved.getId(), "Achievement: " + saved.getTitle(), saved.getDescription(), saved.getProofUrl());
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "achievements", allEntries = true)
    public AchievementDto updateAchievement(UUID id, AchievementDto dto) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));
        if (dto.getTitle() != null) a.setTitle(dto.getTitle());
        if (dto.getCategory() != null) a.setCategory(dto.getCategory());
        if (dto.getOrganization() != null) a.setOrganization(dto.getOrganization());
        if (dto.getDate() != null) a.setDate(dto.getDate());
        if (dto.getDescription() != null) a.setDescription(dto.getDescription());
        if (dto.getProofUrl() != null) a.setProofUrl(dto.getProofUrl());
        if (dto.getCertificate() != null) a.setCertificate(dto.getCertificate());
        Achievement saved = achievementRepository.save(a);
        syncCareerActivity("ACH_" + saved.getId(), "Updated Achievement: " + saved.getTitle(), saved.getDescription(), saved.getProofUrl());
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "achievements", allEntries = true)
    public void deleteAchievement(UUID id) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));
        deleteCareerActivity("ACH_" + a.getId());
        achievementRepository.delete(a);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "certifications")
    public List<CertificationDto> getAllCertifications() {
        return certificationRepository.findAllByOrderByIssueDateDesc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "certifications", allEntries = true)
    public CertificationDto createCertification(CertificationDto dto) {
        Certification c = new Certification();
        c.setTitle(dto.getTitle());
        c.setIssuer(dto.getIssuer() != null && !dto.getIssuer().isBlank() ? dto.getIssuer() : "Verified Issuer");
        c.setIssueDate(dto.getIssueDate() != null ? dto.getIssueDate() : LocalDate.now());
        c.setExpiryDate(dto.getExpiryDate());
        c.setCredentialId(dto.getCredentialId());
        c.setCredentialUrl(dto.getCredentialUrl());
        c.setFeatured(true);
        c.setDisplayOrder(dto.getDisplayOrder());
        Certification saved = certificationRepository.save(c);
        syncCareerActivity("CERT_" + saved.getId(), "Certification: " + saved.getTitle(), "Issued by " + saved.getIssuer(), saved.getCredentialUrl());
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "certifications", allEntries = true)
    public CertificationDto updateCertification(UUID id, CertificationDto dto) {
        Certification c = certificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", id));
        if (dto.getTitle() != null) c.setTitle(dto.getTitle());
        if (dto.getIssuer() != null) c.setIssuer(dto.getIssuer());
        if (dto.getIssueDate() != null) c.setIssueDate(dto.getIssueDate());
        c.setExpiryDate(dto.getExpiryDate());
        if (dto.getCredentialId() != null) c.setCredentialId(dto.getCredentialId());
        if (dto.getCredentialUrl() != null) c.setCredentialUrl(dto.getCredentialUrl());
        Certification saved = certificationRepository.save(c);
        syncCareerActivity("CERT_" + saved.getId(), "Updated Certification: " + saved.getTitle(), "Issued by " + saved.getIssuer(), saved.getCredentialUrl());
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "certifications", allEntries = true)
    public void deleteCertification(UUID id) {
        Certification c = certificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", id));
        deleteCareerActivity("CERT_" + c.getId());
        certificationRepository.delete(c);
    }
}

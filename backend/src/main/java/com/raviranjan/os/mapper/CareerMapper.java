package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.*;
import com.raviranjan.os.dto.career.*;
import org.springframework.stereotype.Component;

@Component
public class CareerMapper {

    public ExperienceDto toDto(Experience entity) {
        if (entity == null) return null;
        ExperienceDto dto = new ExperienceDto();
        dto.setId(entity.getId());
        dto.setCompany(entity.getCompany());
        dto.setRole(entity.getRole());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setLocation(entity.getLocation());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setCurrent(entity.isCurrent());
        dto.setDescription(entity.getDescription());
        dto.setResponsibilities(entity.getResponsibilities());
        dto.setTechnologies(entity.getTechnologies());
        dto.setAchievements(entity.getAchievements());
        dto.setOfferLetterUrl(entity.getOfferLetterUrl());
        dto.setCertificateUrl(entity.getCertificateUrl());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }

    public EducationDto toDto(Education entity) {
        if (entity == null) return null;
        EducationDto dto = new EducationDto();
        dto.setId(entity.getId());
        dto.setInstitution(entity.getInstitution());
        dto.setDegree(entity.getDegree());
        dto.setField(entity.getField());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setGrade(entity.getGrade());
        dto.setDescription(entity.getDescription());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }

    public AchievementDto toDto(Achievement entity) {
        if (entity == null) return null;
        AchievementDto dto = new AchievementDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCategory(entity.getCategory());
        dto.setOrganization(entity.getOrganization());
        dto.setDate(entity.getDate());
        dto.setDescription(entity.getDescription());
        dto.setProofUrl(entity.getProofUrl());
        dto.setCertificate(entity.getCertificate());
        dto.setFeatured(entity.isFeatured());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }

    public CertificationDto toDto(Certification entity) {
        if (entity == null) return null;
        CertificationDto dto = new CertificationDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setIssuer(entity.getIssuer());
        dto.setIssueDate(entity.getIssueDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setCredentialId(entity.getCredentialId());
        dto.setCredentialUrl(entity.getCredentialUrl());
        dto.setFeatured(entity.isFeatured());
        dto.setDisplayOrder(entity.getDisplayOrder());
        return dto;
    }
}

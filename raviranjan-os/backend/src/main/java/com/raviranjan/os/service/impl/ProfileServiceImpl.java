package com.raviranjan.os.service.impl;

import com.raviranjan.os.domain.entity.AuditLog;
import com.raviranjan.os.domain.entity.Profile;
import com.raviranjan.os.domain.entity.SocialLink;
import com.raviranjan.os.dto.profile.ProfileDto;
import com.raviranjan.os.dto.profile.SocialLinkDto;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ProfileMapper;
import com.raviranjan.os.repository.AuditLogRepository;
import com.raviranjan.os.repository.ProfileRepository;
import com.raviranjan.os.service.ProfileService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProfileMapper mapper;

    public ProfileServiceImpl(ProfileRepository profileRepository, AuditLogRepository auditLogRepository, ProfileMapper mapper) {
        this.profileRepository = profileRepository;
        this.auditLogRepository = auditLogRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "profile")
    public ProfileDto getProfile() {
        Profile profile = profileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Profile not initialized"));
        return mapper.toDto(profile);
    }

    @Override
    @CacheEvict(value = "profile", allEntries = true)
    public ProfileDto updateProfile(ProfileDto dto) {
        Profile profile = profileRepository.findAll().stream().findFirst()
                .orElseGet(Profile::new);

        String oldState = String.format(
                "{\"fullName\":\"%s\",\"headline\":\"%s\",\"email\":\"%s\",\"location\":\"%s\",\"resumeUrl\":\"%s\",\"availableForHire\":%s}",
                safeJson(profile.getFullName()),
                safeJson(profile.getHeadline()),
                safeJson(profile.getEmail()),
                safeJson(profile.getLocation()),
                safeJson(profile.getResumeUrl()),
                profile.isAvailableForHire()
        );

        if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
        if (dto.getHeadline() != null) profile.setHeadline(dto.getHeadline());
        if (dto.getBio() != null) profile.setBio(dto.getBio());
        if (dto.getAvatarUrl() != null) profile.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getLocation() != null) profile.setLocation(dto.getLocation());
        if (dto.getEmail() != null) profile.setEmail(dto.getEmail());
        if (dto.getPhone() != null) profile.setPhone(dto.getPhone());
        profile.setAvailableForHire(dto.isAvailableForHire());
        if (dto.getCurrentFocus() != null) profile.setCurrentFocus(dto.getCurrentFocus());
        if (dto.getResumeUrl() != null) profile.setResumeUrl(dto.getResumeUrl());
        if (dto.getSystemStatus() != null) profile.setSystemStatus(dto.getSystemStatus());

        if (dto.getSocialLinks() != null) {
            if (profile.getSocialLinks() == null) {
                profile.setSocialLinks(new ArrayList<>());
            } else {
                profile.getSocialLinks().clear();
            }
            int order = 0;
            for (SocialLinkDto slDto : dto.getSocialLinks()) {
                if (slDto == null || slDto.getPlatform() == null || slDto.getUrl() == null) continue;
                SocialLink link = new SocialLink();
                link.setProfile(profile);
                link.setPlatform(slDto.getPlatform());
                link.setUrl(slDto.getUrl());
                link.setHandle(slDto.getHandle() != null ? slDto.getHandle() : slDto.getPlatform());
                link.setIconKey(slDto.getIconKey() != null ? slDto.getIconKey() : slDto.getPlatform().toLowerCase());
                link.setDisplayOrder(slDto.getDisplayOrder() > 0 ? slDto.getDisplayOrder() : order++);
                link.setVisible(true);
                profile.getSocialLinks().add(link);
            }
        }

        Profile saved = profileRepository.save(profile);

        try {
            String newState = String.format(
                    "{\"fullName\":\"%s\",\"headline\":\"%s\",\"email\":\"%s\",\"location\":\"%s\",\"resumeUrl\":\"%s\",\"availableForHire\":%s}",
                    safeJson(saved.getFullName()),
                    safeJson(saved.getHeadline()),
                    safeJson(saved.getEmail()),
                    safeJson(saved.getLocation()),
                    safeJson(saved.getResumeUrl()),
                    saved.isAvailableForHire()
            );
            AuditLog log = new AuditLog();
            log.setPrincipal("raviranjan");
            log.setAction("UPDATE");
            log.setEntityName("Profile & Resume Link");
            log.setEntityId(saved.getId() != null ? saved.getId().toString() : "primary");
            log.setOldState(oldState);
            log.setNewState(newState);
            log.setClientIp("127.0.0.1");
            log.setCreatedAt(Instant.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {}

        return mapper.toDto(saved);
    }

    private String safeJson(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

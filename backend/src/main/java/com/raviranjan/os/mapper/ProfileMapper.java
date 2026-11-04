package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.Profile;
import com.raviranjan.os.domain.entity.SocialLink;
import com.raviranjan.os.dto.profile.ProfileDto;
import com.raviranjan.os.dto.profile.SocialLinkDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProfileMapper {

    public ProfileDto toDto(Profile entity) {
        if (entity == null) return null;
        ProfileDto dto = new ProfileDto();
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setHeadline(entity.getHeadline());
        dto.setBio(entity.getBio());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setLocation(entity.getLocation());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAvailableForHire(entity.isAvailableForHire());
        dto.setCurrentFocus(entity.getCurrentFocus());
        dto.setResumeUrl(entity.getResumeUrl());
        dto.setSystemStatus(entity.getSystemStatus());
        if (entity.getSocialLinks() != null) {
            dto.setSocialLinks(entity.getSocialLinks().stream().map(this::toSocialLinkDto).collect(Collectors.toList()));
        }
        return dto;
    }

    public SocialLinkDto toSocialLinkDto(SocialLink entity) {
        if (entity == null) return null;
        SocialLinkDto dto = new SocialLinkDto();
        dto.setId(entity.getId());
        dto.setPlatform(entity.getPlatform());
        dto.setUrl(entity.getUrl());
        dto.setHandle(entity.getHandle());
        dto.setIconKey(entity.getIconKey());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setVisible(entity.isVisible());
        return dto;
    }
}

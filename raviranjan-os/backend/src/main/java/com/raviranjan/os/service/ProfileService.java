package com.raviranjan.os.service;

import com.raviranjan.os.dto.profile.ProfileDto;

public interface ProfileService {
    ProfileDto getProfile();
    ProfileDto updateProfile(ProfileDto dto);
}

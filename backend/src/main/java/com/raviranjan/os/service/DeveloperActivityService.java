package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.ActivityType;
import com.raviranjan.os.dto.integration.DeveloperActivityDto;
import org.springframework.data.domain.Pageable;

public interface DeveloperActivityService {
    PageResponse<DeveloperActivityDto> getUnifiedActivity(ActivityType type, String sourcePlatform, Pageable pageable);
}

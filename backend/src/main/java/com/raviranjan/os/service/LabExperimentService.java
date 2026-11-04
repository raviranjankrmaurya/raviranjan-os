package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.LabStatus;
import com.raviranjan.os.dto.lab.LabExperimentDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LabExperimentService {
    LabExperimentDto createLab(LabExperimentDto dto);
    LabExperimentDto updateLab(UUID id, LabExperimentDto dto);
    LabExperimentDto getLabBySlug(String slug);
    PageResponse<LabExperimentDto> getLabs(LabStatus status, Pageable pageable);
    void deleteLab(UUID id);
}

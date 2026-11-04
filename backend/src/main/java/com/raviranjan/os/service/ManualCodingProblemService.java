package com.raviranjan.os.service;

import com.raviranjan.os.dto.dsa.ManualProblemCreateRequest;
import com.raviranjan.os.dto.dsa.ManualProblemResponse;

import java.util.List;
import java.util.UUID;

public interface ManualCodingProblemService {
    ManualProblemResponse createManualProblem(ManualProblemCreateRequest request);
    ManualProblemResponse updateManualProblem(UUID id, ManualProblemCreateRequest request);
    void deleteManualProblem(UUID id);
    List<ManualProblemResponse> getAllManualProblems();
}

package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.dsa.ManualProblemCreateRequest;
import com.raviranjan.os.dto.dsa.ManualProblemResponse;
import com.raviranjan.os.service.ManualCodingProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Manual Coding Knowledge & DSA", description = "Manual problem solutions and algorithmic pattern notes")
public class ManualCodingProblemController {

    private final ManualCodingProblemService problemService;

    public ManualCodingProblemController(ManualCodingProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping({"/api/admin/dsa/manual", "/api/admin/dsa", "/api/admin/coding/problems"})
    @Operation(summary = "Manually record problem solution, approach, and complexity (Admin)")
    public ResponseEntity<ApiResponse<ManualProblemResponse>> createManualProblem(
            @RequestBody ManualProblemCreateRequest request
    ) {
        ManualProblemResponse response = problemService.createManualProblem(request);
        return new ResponseEntity<>(ApiResponse.success("Coding problem saved to Neon DB", response), HttpStatus.CREATED);
    }

    @RequestMapping(value = {"/api/admin/dsa/{id}", "/api/admin/coding/problems/{id}"}, method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update manual coding problem note in Neon DB (Admin)")
    public ResponseEntity<ApiResponse<ManualProblemResponse>> updateManualProblem(
            @PathVariable UUID id,
            @RequestBody ManualProblemCreateRequest request
    ) {
        ManualProblemResponse response = problemService.updateManualProblem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Coding problem updated in Neon DB", response));
    }

    @DeleteMapping({"/api/admin/dsa/{id}", "/api/admin/coding/problems/{id}"})
    @Operation(summary = "Delete manual coding problem note from Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteManualProblem(@PathVariable UUID id) {
        problemService.deleteManualProblem(id);
        return ResponseEntity.ok(ApiResponse.success("Coding problem deleted from Neon DB", null));
    }

    @GetMapping({"/api/dsa/problems", "/api/coding/problems", "/api/admin/dsa", "/api/admin/dsa/problems"})
    @Operation(summary = "List all manually authored coding problems and solutions")
    public ResponseEntity<ApiResponse<List<ManualProblemResponse>>> getPublicProblems() {
        return ResponseEntity.ok(ApiResponse.success(problemService.getAllManualProblems()));
    }
}

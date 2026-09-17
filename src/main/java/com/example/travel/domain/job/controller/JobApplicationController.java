package com.example.travel.domain.job.controller;

import com.example.travel.domain.job.dto.*;
import com.example.travel.domain.job.service.JobApplicationService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs/applications")
@Validated
public class JobApplicationController {
    private final JobApplicationService applicationService;

    public JobApplicationController(JobApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobApplicationItem>> add(
            Authentication authentication,
            @Valid @RequestBody CreateJobApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                applicationService.add((Long) authentication.getPrincipal(), request),
                "지원한 공고로 등록했습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<JobApplicationListResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.findAll((Long) authentication.getPrincipal(), page, size)));
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<JobApplicationItem>> updateStatus(
            Authentication authentication,
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateJobApplicationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.updateStatus(
                (Long) authentication.getPrincipal(), applicationId, request)));
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                       @PathVariable Long applicationId) {
        applicationService.delete((Long) authentication.getPrincipal(), applicationId);
        return ResponseEntity.noContent().build();
    }
}

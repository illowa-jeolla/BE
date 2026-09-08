package com.example.travel.domain.job.controller;

import com.example.travel.domain.job.dto.CreateJobFavoriteRequest;
import com.example.travel.domain.job.dto.JobFavoriteItem;
import com.example.travel.domain.job.dto.JobFavoriteListResponse;
import com.example.travel.domain.job.service.JobFavoriteService;
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
@RequestMapping("/api/v1/jobs/favorites")
@Validated
public class JobFavoriteController {
    private final JobFavoriteService favoriteService;

    public JobFavoriteController(JobFavoriteService favoriteService) { this.favoriteService = favoriteService; }

    @PostMapping
    public ResponseEntity<ApiResponse<JobFavoriteItem>> add(
            Authentication authentication, @Valid @RequestBody CreateJobFavoriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                favoriteService.add((Long) authentication.getPrincipal(), request), "일자리를 찜했습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<JobFavoriteListResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                favoriteService.findAll((Long) authentication.getPrincipal(), page, size)));
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long favoriteId) {
        favoriteService.delete((Long) authentication.getPrincipal(), favoriteId);
        return ResponseEntity.noContent().build();
    }
}

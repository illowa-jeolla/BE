package com.example.travel.domain.location.controller;

import com.example.travel.domain.location.dto.LocationSearchResponse;
import com.example.travel.domain.location.service.LocationSearchService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private final LocationSearchService locationSearchService;

    public LocationController(LocationSearchService locationSearchService) {
        this.locationSearchService = locationSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<LocationSearchResponse>> search(
            @RequestParam @Positive Long regionId,
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                locationSearchService.search(regionId, query, size)));
    }

    @GetMapping("/route-points/search")
    public ResponseEntity<ApiResponse<LocationSearchResponse>> searchRoutePoints(
            @RequestParam @Positive Long regionId,
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(15) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                locationSearchService.searchRoutePoints(regionId, query, size)));
    }
}

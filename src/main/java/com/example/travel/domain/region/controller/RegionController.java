package com.example.travel.domain.region.controller;

import com.example.travel.domain.region.dto.RegionResponse;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionController {
    private final RegionRepository regionRepository;

    public RegionController(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<RegionResponse>>> activeRegions() {
        return ResponseEntity.ok(ApiResponse.success(regionRepository
                .findAllByActiveTrueOrderByNameAsc().stream().map(RegionResponse::from).toList()));
    }
}

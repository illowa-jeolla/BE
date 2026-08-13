package com.example.travel.domain.gathering.controller;

import com.example.travel.domain.gathering.dto.CreateGatheringRequest;
import com.example.travel.domain.gathering.dto.CreateGatheringResponse;
import com.example.travel.domain.gathering.dto.GatheringSearchRequest;
import com.example.travel.domain.gathering.dto.GatheringSearchResponse;
import com.example.travel.domain.gathering.service.GatheringSearchService;
import com.example.travel.domain.gathering.service.GatheringService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gatherings")
public class GatheringController {
    private final GatheringService gatheringService;
    private final GatheringSearchService gatheringSearchService;

    public GatheringController(GatheringService gatheringService,
                               GatheringSearchService gatheringSearchService) {
        this.gatheringService = gatheringService;
        this.gatheringSearchService = gatheringSearchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<GatheringSearchResponse>> search(
            Authentication authentication,
            @Valid @ModelAttribute GatheringSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gatheringSearchService.search(
                (Long) authentication.getPrincipal(), request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateGatheringResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateGatheringRequest request) {
        CreateGatheringResponse response = gatheringService.create(
                (Long) authentication.getPrincipal(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "게더링이 생성되었습니다."));
    }
}

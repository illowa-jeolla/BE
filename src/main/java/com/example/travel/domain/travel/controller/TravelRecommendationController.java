package com.example.travel.domain.travel.controller;

import com.example.travel.domain.travel.dto.request.CreateTravelRecommendationRequest;
import com.example.travel.domain.travel.dto.response.CreateTravelRecommendationResponse;
import com.example.travel.domain.travel.service.TravelRecommendationService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.travel.domain.travel.dto.response.TravelRecommendationStatusResponse;
import com.example.travel.domain.travel.service.TravelRecommendationQueryService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/travel-recommendations")
public class TravelRecommendationController {
    private final TravelRecommendationService travelRecommendationService;
    private final TravelRecommendationQueryService queryService;

    public TravelRecommendationController(TravelRecommendationService travelRecommendationService,
                                          TravelRecommendationQueryService queryService) {
        this.travelRecommendationService = travelRecommendationService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateTravelRecommendationResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateTravelRecommendationRequest request) {
        CreateTravelRecommendationResponse response = travelRecommendationService.create(
                (Long) authentication.getPrincipal(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "여행 추천 요청을 접수했습니다."));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<TravelRecommendationStatusResponse>> status(
            Authentication authentication, @PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                queryService.status((Long) authentication.getPrincipal(), requestId)));
    }
}

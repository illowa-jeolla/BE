package com.example.travel.domain.travel.controller;

import com.example.travel.domain.travel.dto.request.CreateManualTravelGuideRequest;
import com.example.travel.domain.travel.dto.response.CreateTravelRecommendationResponse;
import com.example.travel.domain.travel.dto.response.DeletedSavedTravelGuideResponse;
import com.example.travel.domain.travel.dto.response.SavedTravelGuideResponse;
import com.example.travel.domain.travel.dto.response.TravelGuideResponse;
import com.example.travel.domain.travel.dto.response.TravelGuideSaveResponse;
import com.example.travel.domain.travel.dto.response.ManualTravelPlacePageResponse;
import com.example.travel.domain.travel.service.ManualTravelPlaceService;
import com.example.travel.domain.travel.service.ManualTravelGuideService;
import com.example.travel.domain.travel.service.SavedTravelGuideService;
import com.example.travel.domain.travel.service.TravelGuideDraftQueryService;
import com.example.travel.domain.travel.service.TravelRecommendationQueryService;
import com.example.travel.domain.travel.service.TravelRecommendationService;
import com.example.travel.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/travel-guides")
@Validated
public class TravelGuideController {
    private final TravelRecommendationQueryService queryService;
    private final SavedTravelGuideService savedGuideService;
    private final TravelRecommendationService recommendationService;
    private final TravelGuideDraftQueryService draftQueryService;
    private final ManualTravelPlaceService manualTravelPlaceService;
    private final ManualTravelGuideService manualTravelGuideService;

    public TravelGuideController(TravelRecommendationQueryService queryService,
                                 SavedTravelGuideService savedGuideService,
                                 TravelRecommendationService recommendationService,
                                 TravelGuideDraftQueryService draftQueryService,
                                 ManualTravelPlaceService manualTravelPlaceService,
                                 ManualTravelGuideService manualTravelGuideService) {
        this.queryService = queryService;
        this.savedGuideService = savedGuideService;
        this.recommendationService = recommendationService;
        this.draftQueryService = draftQueryService;
        this.manualTravelPlaceService = manualTravelPlaceService;
        this.manualTravelGuideService = manualTravelGuideService;
    }

    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<TravelGuideResponse>> createManualGuide(
            Authentication authentication,
            @Valid @RequestBody CreateManualTravelGuideRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Long draftId = manualTravelGuideService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                draftQueryService.find(userId, draftId)));
    }

    @GetMapping("/manual/places/nearby")
    public ResponseEntity<ApiResponse<ManualTravelPlacePageResponse>> manualPlaces(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo) {
        return ResponseEntity.ok(ApiResponse.success(
                manualTravelPlaceService.findPlaces(latitude, longitude, pageNo)));
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponse<TravelGuideResponse>> draft(
            Authentication authentication, @PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.success(
                draftQueryService.find((Long) authentication.getPrincipal(), draftId)));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedTravelGuideResponse>>> savedGuides(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                savedGuideService.findAll((Long) authentication.getPrincipal())));
    }

    @GetMapping("/saved/deleted")
    public ResponseEntity<ApiResponse<List<DeletedSavedTravelGuideResponse>>> deletedSavedGuides(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                savedGuideService.findDeleted((Long) authentication.getPrincipal())));
    }

    @PostMapping("/drafts/{draftId}/save")
    public ResponseEntity<ApiResponse<TravelGuideSaveResponse>> saveDraft(
            Authentication authentication, @PathVariable Long draftId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                savedGuideService.saveDraft((Long) authentication.getPrincipal(), draftId)));
    }

    @PostMapping("/saved/{guideId}/restore")
    public ResponseEntity<ApiResponse<TravelGuideSaveResponse>> restore(
            Authentication authentication, @PathVariable Long guideId) {
        return ResponseEntity.ok(ApiResponse.success(
                savedGuideService.restore((Long) authentication.getPrincipal(), guideId)));
    }

    @DeleteMapping("/saved/{guideId}")
    public ResponseEntity<ApiResponse<TravelGuideSaveResponse>> cancelSave(
            Authentication authentication, @PathVariable Long guideId) {
        return ResponseEntity.ok(ApiResponse.success(
                savedGuideService.cancel((Long) authentication.getPrincipal(), guideId)));
    }

    @PostMapping("/drafts/{draftId}/alternatives")
    public ResponseEntity<ApiResponse<CreateTravelRecommendationResponse>> refreshDraft(
            Authentication authentication, @PathVariable Long draftId) {
        CreateTravelRecommendationResponse response = recommendationService.refreshDraft(
                (Long) authentication.getPrincipal(), draftId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "새로운 여행 추천 요청이 접수되었습니다."));
    }

    @GetMapping("/{guideId}")
    public ResponseEntity<ApiResponse<TravelGuideResponse>> guide(
            Authentication authentication, @PathVariable Long guideId) {
        return ResponseEntity.ok(ApiResponse.success(
                queryService.guide((Long) authentication.getPrincipal(), guideId)));
    }
}

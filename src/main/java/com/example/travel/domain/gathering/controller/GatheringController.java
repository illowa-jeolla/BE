package com.example.travel.domain.gathering.controller;

import com.example.travel.domain.gathering.dto.request.CreateGatheringRequest;
import com.example.travel.domain.gathering.dto.response.CreateGatheringResponse;
import com.example.travel.domain.gathering.dto.request.GatheringSearchRequest;
import com.example.travel.domain.gathering.dto.response.GatheringSearchResponse;
import com.example.travel.domain.gathering.dto.response.GatheringDetailResponse;
import com.example.travel.domain.gathering.dto.response.JoinGatheringResponse;
import com.example.travel.domain.gathering.dto.response.CancelGatheringParticipationResponse;
import com.example.travel.domain.gathering.dto.request.MyGatheringRequest;
import com.example.travel.domain.gathering.dto.response.MyGatheringResponse;
import com.example.travel.domain.gathering.dto.response.GatheringParticipantListResponse;
import com.example.travel.domain.gathering.dto.request.UpdateGatheringRequest;
import com.example.travel.domain.gathering.dto.response.UpdateGatheringResponse;
import com.example.travel.domain.gathering.service.GatheringParticipationService;
import com.example.travel.domain.gathering.service.MyGatheringService;
import com.example.travel.domain.gathering.service.GatheringParticipantQueryService;
import com.example.travel.domain.gathering.service.GatheringDetailService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gatherings")
public class GatheringController {
    private final GatheringService gatheringService;
    private final GatheringSearchService gatheringSearchService;
    private final GatheringDetailService gatheringDetailService;
    private final GatheringParticipationService gatheringParticipationService;
    private final MyGatheringService myGatheringService;
    private final GatheringParticipantQueryService gatheringParticipantQueryService;

    public GatheringController(GatheringService gatheringService,
                               GatheringSearchService gatheringSearchService,
                               GatheringDetailService gatheringDetailService,
                               GatheringParticipationService gatheringParticipationService,
                               MyGatheringService myGatheringService,
                               GatheringParticipantQueryService gatheringParticipantQueryService) {
        this.gatheringService = gatheringService;
        this.gatheringSearchService = gatheringSearchService;
        this.gatheringDetailService = gatheringDetailService;
        this.gatheringParticipationService = gatheringParticipationService;
        this.myGatheringService = myGatheringService;
        this.gatheringParticipantQueryService = gatheringParticipantQueryService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyGatheringResponse>> myGatherings(
            Authentication authentication,
            @Valid @ModelAttribute MyGatheringRequest request) {
        return ResponseEntity.ok(ApiResponse.success(myGatheringService.findMine(
                (Long) authentication.getPrincipal(), request)));
    }

    @PatchMapping("/{gatheringId}")
    public ResponseEntity<ApiResponse<UpdateGatheringResponse>> update(
            Authentication authentication,
            @PathVariable Long gatheringId,
            @Valid @RequestBody UpdateGatheringRequest request) {
        UpdateGatheringResponse response = gatheringService.update(
                gatheringId, (Long) authentication.getPrincipal(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "게더링을 수정했습니다."));
    }

    @DeleteMapping("/{gatheringId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long gatheringId) {
        gatheringService.delete(gatheringId, (Long) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gatheringId}/participants")
    public ResponseEntity<ApiResponse<GatheringParticipantListResponse>> participants(
            Authentication authentication,
            @PathVariable Long gatheringId) {
        return ResponseEntity.ok(ApiResponse.success(
                gatheringParticipantQueryService.findParticipants(
                        gatheringId, (Long) authentication.getPrincipal())));
    }

    @PostMapping("/{gatheringId}/participants")
    public ResponseEntity<ApiResponse<JoinGatheringResponse>> join(
            Authentication authentication,
            @PathVariable Long gatheringId) {
        JoinGatheringResponse response = gatheringParticipationService.join(
                gatheringId, (Long) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "게더링에 참여했습니다."));
    }

    @PatchMapping("/{gatheringId}/participants/me")
    public ResponseEntity<ApiResponse<CancelGatheringParticipationResponse>> cancelParticipation(
            Authentication authentication,
            @PathVariable Long gatheringId) {
        CancelGatheringParticipationResponse response =
                gatheringParticipationService.cancel(
                        gatheringId, (Long) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(response, "게더링 참여를 취소했습니다."));
    }

    @GetMapping("/{gatheringId}")
    public ResponseEntity<ApiResponse<GatheringDetailResponse>> detail(
            Authentication authentication,
            @PathVariable Long gatheringId) {
        return ResponseEntity.ok(ApiResponse.success(gatheringDetailService.findDetail(
                gatheringId, (Long) authentication.getPrincipal())));
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

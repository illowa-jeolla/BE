package com.example.travel.domain.community.controller;

import com.example.travel.domain.community.dto.request.UpdateTravelPostDraftRequest;
import com.example.travel.domain.community.dto.response.CreateTravelPostDraftResponse;
import com.example.travel.domain.community.dto.response.TravelPostDraftResponse;
import com.example.travel.domain.community.dto.response.TravelPostDetailResponse;
import com.example.travel.domain.community.dto.response.TravelPostImageResponse;
import com.example.travel.domain.community.service.TravelPostDraftService;
import com.example.travel.domain.community.service.TravelPostImageService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/community/travel-posts/drafts")
public class TravelPostDraftController {
    private final TravelPostDraftService travelPostDraftService;
    private final TravelPostImageService imageService;

    public TravelPostDraftController(TravelPostDraftService travelPostDraftService,
                                     TravelPostImageService imageService) {
        this.travelPostDraftService = travelPostDraftService;
        this.imageService = imageService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateTravelPostDraftResponse>> create(
            Authentication authentication) {
        CreateTravelPostDraftResponse response = travelPostDraftService.open(
                (Long) authentication.getPrincipal());
        return ResponseEntity.status(response.resumed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.resumed()
                        ? "작성 중인 임시 글을 불러왔습니다."
                        : "임시 글을 생성했습니다."));
    }

    @PatchMapping("/{draftId}")
    public ResponseEntity<ApiResponse<TravelPostDraftResponse>> update(
            Authentication authentication,
            @PathVariable Long draftId,
            @Valid @RequestBody UpdateTravelPostDraftRequest request) {
        TravelPostDraftResponse response = travelPostDraftService.update(
                (Long) authentication.getPrincipal(), draftId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "임시 글을 저장했습니다."));
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> deleteDraft(Authentication authentication,
                                            @PathVariable Long draftId) {
        travelPostDraftService.delete((Long) authentication.getPrincipal(), draftId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{draftId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TravelPostImageResponse>> addImage(
            Authentication authentication, @PathVariable Long draftId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                imageService.upload((Long) authentication.getPrincipal(), draftId, file),
                "이미지를 추가했습니다."));
    }

    @DeleteMapping("/{draftId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(Authentication authentication,
                                            @PathVariable Long draftId,
                                            @PathVariable Long imageId) {
        imageService.delete((Long) authentication.getPrincipal(), draftId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{draftId}/publish")
    public ResponseEntity<ApiResponse<TravelPostDetailResponse>> publish(
            Authentication authentication, @PathVariable Long draftId) {
        return ResponseEntity.ok(ApiResponse.success(travelPostDraftService.publish(
                (Long) authentication.getPrincipal(), draftId), "게시글을 등록했습니다."));
    }
}

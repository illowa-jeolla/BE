package com.example.travel.domain.community.controller;

import com.example.travel.domain.community.dto.request.TravelPostSearchRequest;
import com.example.travel.domain.community.dto.request.UpdateTravelPostRequest;
import com.example.travel.domain.community.dto.response.TravelPostDetailResponse;
import com.example.travel.domain.community.dto.response.TravelPostListResponse;
import com.example.travel.domain.community.dto.response.TravelPostImageResponse;
import com.example.travel.domain.community.service.TravelPostImageService;
import com.example.travel.domain.community.service.TravelPostService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/community/travel-posts")
public class TravelPostController {
    private final TravelPostService travelPostService;
    private final TravelPostImageService imageService;

    public TravelPostController(TravelPostService travelPostService,
                                TravelPostImageService imageService) {
        this.travelPostService = travelPostService;
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TravelPostListResponse>> list(
            @Valid @ModelAttribute TravelPostSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(travelPostService.findAll(request)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<TravelPostDetailResponse>> detail(
            Authentication authentication, @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(travelPostService.findDetail(
                postId, (Long) authentication.getPrincipal())));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<TravelPostDetailResponse>> update(
            Authentication authentication, @PathVariable Long postId,
            @Valid @RequestBody UpdateTravelPostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(travelPostService.update(
                postId, (Long) authentication.getPrincipal(), request),
                "게시글을 수정했습니다."));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                       @PathVariable Long postId) {
        travelPostService.delete(postId, (Long) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TravelPostImageResponse>> addImage(
            Authentication authentication, @PathVariable Long postId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.success(imageService.uploadPublished(
                (Long) authentication.getPrincipal(), postId, file), "이미지를 추가했습니다."));
    }

    @DeleteMapping("/{postId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(Authentication authentication,
                                            @PathVariable Long postId,
                                            @PathVariable Long imageId) {
        imageService.deletePublished((Long) authentication.getPrincipal(), postId, imageId);
        return ResponseEntity.noContent().build();
    }
}

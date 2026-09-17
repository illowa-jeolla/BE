package com.example.travel.domain.community.controller;

import com.example.travel.domain.community.dto.request.CreateTravelPostCommentRequest;
import com.example.travel.domain.community.dto.request.UpdateTravelPostCommentRequest;
import com.example.travel.domain.community.dto.response.TravelPostCommentResponse;
import com.example.travel.domain.community.service.TravelPostCommentService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community/travel-posts/{postId}/comments")
public class TravelPostCommentController {
    private final TravelPostCommentService commentService;

    public TravelPostCommentController(TravelPostCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelPostCommentResponse>>> list(
            Authentication authentication, @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(commentService.findAll(
                postId, (Long) authentication.getPrincipal())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TravelPostCommentResponse>> create(
            Authentication authentication, @PathVariable Long postId,
            @Valid @RequestBody CreateTravelPostCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                commentService.create(postId, (Long) authentication.getPrincipal(), request),
                "댓글을 등록했습니다."));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<TravelPostCommentResponse>> update(
            Authentication authentication, @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateTravelPostCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(commentService.update(postId, commentId,
                (Long) authentication.getPrincipal(), request), "댓글을 수정했습니다."));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(Authentication authentication,
                                       @PathVariable Long postId,
                                       @PathVariable Long commentId) {
        commentService.delete(postId, commentId, (Long) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}

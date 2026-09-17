package com.example.travel.domain.user.controller;

import com.example.travel.domain.user.dto.MyProfileResponse;
import com.example.travel.domain.user.dto.UpdateNicknameRequest;
import com.example.travel.domain.user.service.UserProfileService;
import com.example.travel.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MyProfileResponse>> profile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                userProfileService.findMine((Long) authentication.getPrincipal())));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateNickname(
            Authentication authentication,
            @Valid @RequestBody UpdateNicknameRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.updateNickname(
                (Long) authentication.getPrincipal(), request), "닉네임을 변경했습니다."));
    }
}

package com.example.travel.domain.auth.dto;

import com.example.travel.domain.user.policy.NicknamePolicy;
import com.example.travel.global.validation.TrimmedSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @NotBlank @TrimmedSize(max = NicknamePolicy.MAX_LENGTH) String nickname
) {}

package com.example.travel.domain.user.dto;

import com.example.travel.domain.user.policy.NicknamePolicy;
import com.example.travel.global.validation.TrimmedSize;
import jakarta.validation.constraints.NotBlank;

public record UpdateNicknameRequest(
        @NotBlank @TrimmedSize(max = NicknamePolicy.MAX_LENGTH) String nickname
) {}

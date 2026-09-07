package com.example.travel.domain.user.dto;

import com.example.travel.domain.user.policy.NicknamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank @Size(max = NicknamePolicy.MAX_LENGTH) String nickname
) {}

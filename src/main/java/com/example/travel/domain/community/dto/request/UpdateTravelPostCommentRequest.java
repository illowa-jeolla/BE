package com.example.travel.domain.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTravelPostCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 2000, message = "댓글은 2000자 이하여야 합니다.") String content,
        @NotNull Boolean secret
) {
}

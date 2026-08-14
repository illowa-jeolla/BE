package com.example.travel.domain.gathering.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MyGatheringRequest(
        @NotBlank(message = "조회 유형은 필수입니다.")
        @Pattern(regexp = "^(hosted|joined)$",
                message = "조회 유형은 hosted 또는 joined여야 합니다.")
        String type,

        @Min(value = 0, message = "페이지는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
}

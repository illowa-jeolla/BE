package com.example.travel.domain.community.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReorderTravelPostImagesRequest(
        @NotNull(message = "이미지 ID 목록은 필수입니다.")
        @Size(max = 5, message = "이미지는 최대 5장까지 정렬할 수 있습니다.")
        List<@NotNull Long> imageIds
) {
}

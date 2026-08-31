package com.example.travel.domain.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTravelPostRequest(
        @NotNull(message = "지역은 필수입니다.") Long regionId,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.") String title,
        @Size(max = 100, message = "콘셉트는 100자 이하여야 합니다.") String concept,
        @NotBlank(message = "본문은 필수입니다.") String content,
        @Size(max = 5, message = "이미지는 최대 5장까지 정렬할 수 있습니다.")
        List<Long> imageIds
) {
}

package com.example.travel.domain.gathering.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateGatheringRequest(
        @NotBlank(message = "지역은 필수입니다.")
        @Size(max = 80, message = "지역은 80자 이하여야 합니다.")
        String region,

        @NotBlank(message = "모임 이름은 필수입니다.")
        @Size(max = 150, message = "모임 이름은 150자 이하여야 합니다.")
        String title,

        @NotNull(message = "정원은 필수입니다.")
        @Min(value = 2, message = "정원은 2명 이상이어야 합니다.")
        @Max(value = 100, message = "정원은 100명 이하여야 합니다.")
        Integer capacity,

        @NotBlank(message = "만날 장소는 필수입니다.")
        @Size(max = 255, message = "만날 장소는 255자 이하여야 합니다.")
        String meetingPlace,

        @NotNull(message = "날짜와 시간은 필수입니다.")
        @Future(message = "날짜와 시간은 현재보다 미래여야 합니다.")
        OffsetDateTime startsAt,

        @NotBlank(message = "콘셉트는 필수입니다.")
        @Size(max = 100, message = "콘셉트는 100자 이하여야 합니다.")
        String concept,

        @NotBlank(message = "모임 설명은 필수입니다.")
        String description
) {
}

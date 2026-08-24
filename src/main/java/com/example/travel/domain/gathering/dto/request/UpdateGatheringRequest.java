package com.example.travel.domain.gathering.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record UpdateGatheringRequest(
        @Size(max = 150, message = "제목은 150자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        @Pattern(regexp = "(?s).*\\S.*", message = "설명은 공백일 수 없습니다.")
        String description,

        @Size(max = 100, message = "콘셉트는 100자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "콘셉트는 공백일 수 없습니다.")
        String concept,

        @Size(max = 255, message = "장소는 255자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "장소는 공백일 수 없습니다.")
        String meetingPlace,

        OffsetDateTime startsAt,

        @Min(value = 2, message = "정원은 최소 2명이어야 합니다.")
        @Max(value = 100, message = "정원은 최대 100명이어야 합니다.")
        Integer capacity
) {
    public boolean hasChanges() {
        return title != null || description != null || concept != null
                || meetingPlace != null || startsAt != null || capacity != null;
    }
}

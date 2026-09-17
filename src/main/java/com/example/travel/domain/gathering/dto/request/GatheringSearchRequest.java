package com.example.travel.domain.gathering.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GatheringSearchRequest(
        @Size(max = 80, message = "지역은 80자 이하여야 합니다.")
        String region,

        LocalDate startsOn,

        LocalDate endsOn,

        @Pattern(regexp = "^\\s*$|^(?:[01]\\d|2[0-3]):[0-5]\\d$",
                message = "시간은 HH:mm 형식이어야 합니다.")
        String time,

        @Size(max = 100, message = "컨셉은 100자 이하여야 합니다.")
        String concept,

        @Size(max = 255, message = "구체적인 장소는 255자 이하여야 합니다.")
        String meetingPlace,

        @Min(value = 0, message = "페이지는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
        Integer size
) {
}

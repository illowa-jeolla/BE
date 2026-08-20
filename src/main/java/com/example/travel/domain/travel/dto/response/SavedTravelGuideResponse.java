package com.example.travel.domain.travel.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SavedTravelGuideResponse(
        Long guideId,
        String title,
        String summary,
        String regionName,
        LocalDate startsOn,
        LocalDate endsOn,
        boolean generatedByAi,
        OffsetDateTime savedAt
) {
}

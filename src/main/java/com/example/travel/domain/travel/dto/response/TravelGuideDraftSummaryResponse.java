package com.example.travel.domain.travel.dto.response;

import java.time.LocalDate;

public record TravelGuideDraftSummaryResponse(
        Long draftId,
        String title,
        String regionName,
        LocalDate startsOn,
        LocalDate endsOn,
        boolean generatedByAi
) {}

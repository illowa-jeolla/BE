package com.example.travel.domain.travel.dto.response;

import com.example.travel.domain.travel.enums.RecommendationStatus;

public record TravelRecommendationStatusResponse(
        Long requestId,
        RecommendationStatus status,
        Long draftId,
        Boolean generatedByAi
) {
}

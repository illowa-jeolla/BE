package com.example.travel.domain.travel.dto.response;

import com.example.travel.domain.travel.enums.RecommendationStatus;

import java.util.List;

public record CreateTravelRecommendationResponse(
        Long requestId,
        RecommendationStatus status,
        int candidateCount,
        List<TravelCandidateItem> candidates
) {
}

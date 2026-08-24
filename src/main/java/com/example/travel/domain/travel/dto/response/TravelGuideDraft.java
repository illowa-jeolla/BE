package com.example.travel.domain.travel.dto.response;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.route.PlannedRouteSegment;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;

import java.util.List;

public record TravelGuideDraft(
        Long requestId,
        Long userId,
        TravelRecommendationRequest request,
        AiTravelGuideResult result,
        List<TravelCandidateItem> candidates,
        List<PlannedRouteSegment> routes,
        boolean generatedByAi,
        boolean refreshResult
) {
    public TravelGuideDraft {
        candidates = List.copyOf(candidates);
        routes = List.copyOf(routes);
    }
}

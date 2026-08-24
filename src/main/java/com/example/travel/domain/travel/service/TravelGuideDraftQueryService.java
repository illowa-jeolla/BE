package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.dto.response.TravelGuideResponse;
import com.example.travel.domain.travel.enums.GuideStatus;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.route.PlannedRouteSegment;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TravelGuideDraftQueryService {
    private final TravelGuideDraftCacheService draftCacheService;

    public TravelGuideDraftQueryService(TravelGuideDraftCacheService draftCacheService) {
        this.draftCacheService = draftCacheService;
    }

    public TravelGuideResponse find(Long userId, Long draftId) {
        TravelGuideDraft draft = draftCacheService.find(draftId);
        if (!draft.userId().equals(userId)) {
            throw new TravelRecommendationException(TravelRecommendationErrorCode.DRAFT_NOT_FOUND);
        }
        Map<String, TravelCandidateItem> candidates = draft.candidates().stream()
                .collect(Collectors.toMap(TravelCandidateItem::contentId, Function.identity()));

        var days = draft.result().days().stream().map(day -> {
            var items = day.items().stream().map(item -> {
                TravelCandidateItem candidate = candidates.get(item.contentId());
                if (candidate == null) {
                    throw new TravelRecommendationException(
                            TravelRecommendationErrorCode.INVALID_AI_PLACE_ID);
                }
                Integer travelMinutes = draft.routes().stream()
                        .filter(route -> route.dayNumber() == day.dayNumber()
                                && route.segmentOrder() == item.order())
                        .map(route -> route.route().durationMinutes())
                        .findFirst().orElse(null);
                return new TravelGuideResponse.Item((short) item.order(), item.contentId(),
                        candidate.title(), item.reason(), item.recommendedTime() == null
                                ? null : LocalTime.parse(item.recommendedTime()),
                        item.stayMinutes(), travelMinutes, candidate.latitude(),
                        candidate.longitude(), candidate.thumbnailUrl());
            }).toList();
            var routes = draft.routes().stream()
                    .filter(route -> route.dayNumber() == day.dayNumber())
                    .map(this::routeResponse)
                    .toList();
            return new TravelGuideResponse.Day((short) day.dayNumber(), items, routes);
        }).toList();

        boolean refreshAvailable = !draft.refreshResult()
                && !draftCacheService.isRefreshUsed(draftId);
        return new TravelGuideResponse(draftId, null, draft.result().title(), draft.result().summary(),
                draft.result().travelTip(), draft.generatedByAi(), refreshAvailable,
                GuideStatus.READY, days);
    }

    private TravelGuideResponse.RouteSegment routeResponse(PlannedRouteSegment route) {
        var path = route.route().path().stream()
                .map(point -> new TravelGuideResponse.Coordinate(
                        point.latitude(), point.longitude()))
                .toList();
        return new TravelGuideResponse.RouteSegment((short) route.segmentOrder(),
                route.from().name(), route.to().name(), route.route().distanceMeters(),
                route.route().durationMinutes(), route.route().estimated(), path);
    }
}

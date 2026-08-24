package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.dto.response.TravelGuideResponse;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.dto.response.TravelRecommendationStatusResponse;
import com.example.travel.domain.travel.entity.TravelGuide;
import com.example.travel.domain.travel.entity.TravelGuideItem;
import com.example.travel.domain.travel.entity.TravelGuideRouteSegment;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.repository.TravelGuideItemRepository;
import com.example.travel.domain.travel.repository.TravelGuideRepository;
import com.example.travel.domain.travel.repository.TravelGuideRouteSegmentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class TravelRecommendationQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final TravelGuideRepository guideRepository;
    private final TravelGuideItemRepository guideItemRepository;
    private final TravelGuideRouteSegmentRepository routeSegmentRepository;
    private final TravelGuideDraftCacheService draftCacheService;
    private final TravelRecommendationRequestCacheService requestCacheService;

    public TravelRecommendationQueryService(
            TravelGuideRepository guideRepository,
            TravelGuideItemRepository guideItemRepository,
            TravelGuideRouteSegmentRepository routeSegmentRepository,
            TravelGuideDraftCacheService draftCacheService,
            TravelRecommendationRequestCacheService requestCacheService) {
        this.guideRepository = guideRepository;
        this.guideItemRepository = guideItemRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.draftCacheService = draftCacheService;
        this.requestCacheService = requestCacheService;
    }

    public TravelRecommendationStatusResponse status(Long userId, Long requestId) {
        var request = requestCacheService.find(requestId);
        if (!request.getUserId().equals(userId)) throw notFound();
        TravelGuideDraft draft = draftCacheService.findOptional(requestId).orElse(null);
        return new TravelRecommendationStatusResponse(request.getId(), request.getStatus(),
                draft == null ? null : draft.requestId(),
                draft == null ? null : draft.generatedByAi());
    }

    public TravelGuideResponse guide(Long userId, Long guideId) {
        TravelGuide guide = guideRepository.findByIdAndUserId(guideId, userId)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.GUIDE_NOT_FOUND));
        List<TravelGuideItem> items = guideItemRepository
                .findByGuideIdOrderByDayNumberAscItemOrderAsc(guideId);
        Map<Short, List<TravelGuideResponse.Item>> grouped = new LinkedHashMap<>();
        Map<Short, List<TravelGuideResponse.RouteSegment>> groupedRoutes = new LinkedHashMap<>();
        for (TravelGuideItem item : items) {
            grouped.computeIfAbsent(item.getDayNumber(), ignored -> new ArrayList<>())
                    .add(new TravelGuideResponse.Item(item.getItemOrder(),
                            item.getTourContentId(), item.getTitle(), item.getDescription(),
                            item.getStartsAt(), item.getStayMinutes(), item.getTravelMinutes(),
                            item.getLatitude(), item.getLongitude(), item.getThumbnailUrl()));
        }
        for (TravelGuideRouteSegment segment : routeSegmentRepository
                .findByGuideIdOrderByDayNumberAscSegmentOrderAsc(guideId)) {
            groupedRoutes.computeIfAbsent(segment.getDayNumber(), ignored -> new ArrayList<>())
                    .add(new TravelGuideResponse.RouteSegment(segment.getSegmentOrder(),
                            segment.getFromName(), segment.getToName(), segment.getDistanceMeters(),
                            segment.getDurationMinutes(), segment.isEstimated(),
                            readPath(segment.getPathJson())));
        }
        List<TravelGuideResponse.Day> days = grouped.entrySet().stream()
                .map(entry -> new TravelGuideResponse.Day(entry.getKey(),
                        List.copyOf(entry.getValue()), List.copyOf(groupedRoutes.getOrDefault(
                        entry.getKey(), List.of()))))
                .toList();
        return new TravelGuideResponse(null, guide.getId(), guide.getTitle(), guide.getSummary(),
                guide.getTravelTip(), guide.isGeneratedByAi(), false,
                guide.getStatus(), days);
    }

    private List<TravelGuideResponse.Coordinate> readPath(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private TravelRecommendationException notFound() {
        return new TravelRecommendationException(TravelRecommendationErrorCode.REQUEST_NOT_FOUND);
    }
}

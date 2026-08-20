package com.example.travel.domain.travel.service;

import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelGuide;
import com.example.travel.domain.travel.entity.TravelGuideItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.example.travel.domain.travel.entity.TravelGuideRouteSegment;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.repository.TravelGuideItemRepository;
import com.example.travel.domain.travel.repository.TravelGuideRepository;
import com.example.travel.domain.travel.repository.TravelGuideRouteSegmentRepository;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.travel.route.PlannedRouteSegment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TravelRecommendationPersistenceService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final TravelGuideRepository guideRepository;
    private final TravelGuideItemRepository guideItemRepository;
    private final TravelGuideRouteSegmentRepository routeSegmentRepository;
    private final Clock clock;
    private final UserRepository userRepository;

    public TravelRecommendationPersistenceService(
            TravelGuideRepository guideRepository,
            TravelGuideItemRepository guideItemRepository,
            TravelGuideRouteSegmentRepository routeSegmentRepository, Clock clock,
            UserRepository userRepository) {
        this.guideRepository = guideRepository;
        this.guideItemRepository = guideItemRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.clock = clock;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long saveGuide(TravelRecommendationRequest request, AiTravelGuideResult result,
                          List<TravelCandidateItem> candidates,
                          List<PlannedRouteSegment> routes, boolean generatedByAi) {
        var existing = guideRepository.findBySourceRequestId(request.getId());
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        var user = userRepository.findByIdAndStatus(request.getUserId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new TravelRecommendationException(
                        TravelRecommendationErrorCode.USER_NOT_FOUND));
        TravelGuide guide = guideRepository.save(TravelGuide.ready(request, user, result.title(),
                result.summary(), result.travelTip(), generatedByAi, OffsetDateTime.now(clock)));
        Map<String, TravelCandidateItem> byId = candidates.stream().collect(
                Collectors.toMap(TravelCandidateItem::contentId, Function.identity()));

        List<TravelGuideItem> items = result.days().stream()
                .flatMap(day -> day.items().stream().map(item -> {
                    TravelCandidateItem candidate = byId.get(item.contentId());
                    if (candidate == null) {
                        throw new TravelRecommendationException(
                                TravelRecommendationErrorCode.INVALID_AI_PLACE_ID);
                    }
                    TravelGuideItem guideItem = TravelGuideItem.create(guide, candidate.contentId(),
                            (short) day.dayNumber(), (short) item.order(), candidate.title(),
                            item.reason(), LocalTime.parse(item.recommendedTime()),
                            item.stayMinutes(), candidate.latitude(), candidate.longitude(),
                            candidate.thumbnailUrl());
                    routes.stream().filter(route -> route.dayNumber() == day.dayNumber()
                                    && route.segmentOrder() == item.order())
                            .findFirst().ifPresent(route -> guideItem.setTravelMinutes(
                                    route.route().durationMinutes()));
                    return guideItem;
                })).toList();
        guideItemRepository.saveAll(items);
        routeSegmentRepository.saveAll(routes.stream().map(route ->
                TravelGuideRouteSegment.create(guide, (short) route.dayNumber(),
                        (short) route.segmentOrder(), route.from().name(), route.to().name(),
                        route.from().latitude(), route.from().longitude(),
                        route.to().latitude(), route.to().longitude(),
                        route.route().distanceMeters(), route.route().durationMinutes(),
                        route.route().estimated(), pathJson(route))).toList());
        return guide.getId();
    }

    private String pathJson(PlannedRouteSegment route) {
        try { return OBJECT_MAPPER.writeValueAsString(route.route().path()); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("경로 좌표 저장에 실패했습니다.", exception);
        }
    }

}

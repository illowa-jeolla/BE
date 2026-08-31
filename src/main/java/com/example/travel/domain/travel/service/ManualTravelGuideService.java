package com.example.travel.domain.travel.service;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.request.CreateManualTravelGuideRequest;
import com.example.travel.domain.travel.dto.request.ManualTravelDayRequest;
import com.example.travel.domain.travel.dto.request.ManualTravelPlaceRequest;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.route.PlannedRouteSegment;
import com.example.travel.domain.travel.route.TravelRouteService;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ManualTravelGuideService {
    private static final long MAX_TRIP_DAYS = 7;
    private static final double REGION_SCOPE_METERS = 40_000;
    private static final double PLACE_RADIUS_METERS = 20_000;

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final TravelRecommendationRequestCacheService requestCacheService;
    private final TravelRouteService routeService;
    private final TravelGuideDraftCacheService draftCacheService;
    private final Clock clock;

    public ManualTravelGuideService(UserRepository userRepository,
                                    RegionRepository regionRepository,
                                    TravelRecommendationRequestCacheService requestCacheService,
                                    TravelRouteService routeService,
                                    TravelGuideDraftCacheService draftCacheService,
                                    Clock clock) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.requestCacheService = requestCacheService;
        this.routeService = routeService;
        this.draftCacheService = draftCacheService;
        this.clock = clock;
    }

    public Long create(Long userId, CreateManualTravelGuideRequest request) {
        validateDates(request);
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> error(TravelRecommendationErrorCode.USER_NOT_FOUND));
        Region region = regionRepository.findActiveById(request.regionId())
                .orElseThrow(() -> error(TravelRecommendationErrorCode.REGION_NOT_FOUND));
        validateLocationScope(region, request.accommodation().latitude(),
                request.accommodation().longitude(), TravelRecommendationErrorCode.LODGING_OUTSIDE_REGION);
        validateDays(request);

        Long draftId = requestCacheService.nextId();
        TravelRecommendationContext context = context(draftId, userId, region, request);
        List<TravelCandidateItem> candidates = candidates(request);
        AiTravelGuideResult result = result(region, request);
        List<PlannedRouteSegment> routes = routeService.plan(context, result, candidates);
        draftCacheService.replaceManual(new TravelGuideDraft(draftId, userId, context, result,
                candidates, routes, false, true));
        return draftId;
    }

    private void validateDates(CreateManualTravelGuideRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw error(TravelRecommendationErrorCode.INVALID_DATE_RANGE);
        }
        if (request.startDate().isBefore(java.time.LocalDate.now(clock))) {
            throw error(TravelRecommendationErrorCode.PAST_START_DATE);
        }
        long tripDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (tripDays > MAX_TRIP_DAYS) {
            throw error(TravelRecommendationErrorCode.TRIP_TOO_LONG);
        }
        if (request.dailyPlaceCounts().size() != tripDays
                || request.days().size() != tripDays) {
            throw error(TravelRecommendationErrorCode.INVALID_MANUAL_DAYS);
        }
    }

    private void validateDays(CreateManualTravelGuideRequest request) {
        Set<String> contentIds = new HashSet<>();
        for (int index = 0; index < request.days().size(); index++) {
            ManualTravelDayRequest day = request.days().get(index);
            if (day.dayNumber() != index + 1) {
                throw error(TravelRecommendationErrorCode.INVALID_MANUAL_DAYS);
            }
            if (day.places().size() > request.dailyPlaceCounts().get(index)) {
                throw error(TravelRecommendationErrorCode.MANUAL_PLACE_COUNT_EXCEEDED);
            }
            for (ManualTravelPlaceRequest place : day.places()) {
                String contentId = place.contentId().trim();
                if (!contentIds.add(contentId)) {
                    throw error(TravelRecommendationErrorCode.DUPLICATE_MANUAL_PLACE);
                }
                double distance = haversineMeters(request.accommodation().latitude(),
                        request.accommodation().longitude(), place.latitude(), place.longitude());
                if (distance > PLACE_RADIUS_METERS) {
                    throw error(TravelRecommendationErrorCode.MANUAL_PLACE_OUTSIDE_SEARCH_RADIUS);
                }
            }
        }
    }

    private TravelRecommendationContext context(Long id, Long userId, Region region,
                                                CreateManualTravelGuideRequest request) {
        var lodging = request.accommodation();
        var start = request.startLocation();
        var end = request.endLocation();
        String[] themes = request.themes().stream().map(Enum::name).sorted().toArray(String[]::new);
        return TravelRecommendationContext.create(id, userId, region.getId(), region.getName(),
                lodging.kakaoPlaceId().trim(), lodging.name().trim(), lodging.address().trim(),
                lodging.latitude(), lodging.longitude(), start.kakaoPlaceId().trim(),
                start.name().trim(), start.address().trim(), start.latitude(), start.longitude(),
                end.kakaoPlaceId().trim(), end.name().trim(), end.address().trim(),
                end.latitude(), end.longitude(), request.startDate(), request.endDate(), themes,
                request.dailyPlaceCounts().toArray(Integer[]::new), request.transportType(),
                request.companionType());
    }

    private List<TravelCandidateItem> candidates(CreateManualTravelGuideRequest request) {
        List<TravelCandidateItem> candidates = new ArrayList<>();
        for (ManualTravelDayRequest day : request.days()) {
            for (ManualTravelPlaceRequest place : day.places()) {
                candidates.add(new TravelCandidateItem(place.contentId().trim(), place.title().trim(),
                        place.address(), place.thumbnailUrl(), place.latitude(), place.longitude(),
                        null, 0));
            }
        }
        return List.copyOf(candidates);
    }

    private AiTravelGuideResult result(Region region, CreateManualTravelGuideRequest request) {
        var days = request.days().stream().map(day -> new AiTravelGuideResult.Day(
                day.dayNumber(), day.dayNumber() + "일차",
                java.util.stream.IntStream.range(0, day.places().size())
                        .mapToObj(index -> new AiTravelGuideResult.Item(
                                day.places().get(index).contentId().trim(), index + 1,
                                null, 0, null))
                        .toList())).toList();
        String title = request.title() == null || request.title().isBlank()
                ? region.getName() + " 사용자 지정 여행" : request.title().trim();
        return new AiTravelGuideResult(title, "사용자가 직접 구성한 여행 일정입니다.",
                days, null);
    }

    private void validateLocationScope(Region region, BigDecimal latitude, BigDecimal longitude,
                                       TravelRecommendationErrorCode errorCode) {
        if (region.getLatitude() == null || region.getLongitude() == null
                || haversineMeters(region.getLatitude(), region.getLongitude(),
                latitude, longitude) > REGION_SCOPE_METERS) {
            throw error(errorCode);
        }
    }

    private double haversineMeters(BigDecimal latitude1, BigDecimal longitude1,
                                   BigDecimal latitude2, BigDecimal longitude2) {
        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double value = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(clamped), Math.sqrt(1 - clamped));
    }

    private TravelRecommendationException error(TravelRecommendationErrorCode code) {
        return new TravelRecommendationException(code);
    }
}

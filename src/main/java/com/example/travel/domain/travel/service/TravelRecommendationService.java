package com.example.travel.domain.travel.service;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.travel.dto.request.AccommodationRequest;
import com.example.travel.domain.travel.dto.request.CreateTravelRecommendationRequest;
import com.example.travel.domain.travel.dto.request.RouteLocationRequest;
import com.example.travel.domain.travel.dto.response.CreateTravelRecommendationResponse;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.travel.enums.TravelTheme;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TravelRecommendationService {
    private static final double REGION_SCOPE_METERS = 40_000;
    private static final long MAX_TRIP_DAYS = 7;

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final TravelCandidateService candidateService;
    private final TravelCandidateCacheService candidateCacheService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final TravelGuideDraftCacheService draftCacheService;
    private final TravelRecommendationRequestCacheService requestCacheService;

    public TravelRecommendationService(UserRepository userRepository,
                                       RegionRepository regionRepository,
                                       TravelCandidateService candidateService,
                                       TravelCandidateCacheService candidateCacheService,
                                       Clock clock,
                                       ApplicationEventPublisher eventPublisher,
                                       TravelGuideDraftCacheService draftCacheService,
                                       TravelRecommendationRequestCacheService requestCacheService) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.candidateService = candidateService;
        this.candidateCacheService = candidateCacheService;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
        this.draftCacheService = draftCacheService;
        this.requestCacheService = requestCacheService;
    }

    @Transactional
    public CreateTravelRecommendationResponse create(
            Long userId, CreateTravelRecommendationRequest request) {
        validateDates(request.startDate(), request.endDate());
        validateDailyPlaceCounts(request);
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> error(TravelRecommendationErrorCode.USER_NOT_FOUND));
        Region region = regionRepository.findActiveById(request.regionId())
                .orElseThrow(() -> error(TravelRecommendationErrorCode.REGION_NOT_FOUND));
        validateLodgingScope(region, request.accommodation());

        int requestedPlaces = request.dailyPlaceCounts().stream().mapToInt(Integer::intValue).sum();
        List<TravelCandidateItem> candidates = candidateService.findCandidates(
                request.accommodation().latitude(), request.accommodation().longitude(),
                request.themes(), request.transportType(), requestedPlaces, Set.of());
        if (candidates.isEmpty()) {
            throw error(TravelRecommendationErrorCode.NO_CANDIDATES);
        }
        if (candidates.size() < requestedPlaces) {
            throw error(TravelRecommendationErrorCode.INSUFFICIENT_CANDIDATES);
        }

        AccommodationRequest lodging = request.accommodation();
        RouteLocationRequest start = request.startLocation();
        RouteLocationRequest end = request.endLocation();
        String[] themes = request.themes().stream().map(Enum::name).sorted().toArray(String[]::new);
        Long requestId = requestCacheService.nextId();
        TravelRecommendationContext saved = TravelRecommendationContext.create(
                        requestId, userId, region.getId(), region.getName(),
                        lodging.kakaoPlaceId().trim(),
                        lodging.name().trim(),
                        lodging.address().trim(), lodging.latitude(), lodging.longitude(),
                        start.kakaoPlaceId().trim(), start.name().trim(), start.address().trim(),
                        start.latitude(), start.longitude(), end.kakaoPlaceId().trim(),
                        end.name().trim(), end.address().trim(), end.latitude(), end.longitude(),
                        request.startDate(), request.endDate(), themes,
                        request.dailyPlaceCounts().toArray(Integer[]::new),
                        request.transportType(), request.companionType());
        requestCacheService.save(saved);
        candidateCacheService.save(saved.getId(), candidates);
        eventPublisher.publishEvent(new TravelRecommendationCreatedEvent(saved.getId()));

        return new CreateTravelRecommendationResponse(saved.getId(), saved.getStatus(),
                candidates.size(), candidates);
    }

    @Transactional
    public CreateTravelRecommendationResponse refreshDraft(Long userId, Long draftId) {
        var draft = draftCacheService.find(draftId);
        if (!draft.userId().equals(userId)) {
            throw error(TravelRecommendationErrorCode.DRAFT_NOT_FOUND);
        }
        if (draft.refreshResult() || draftCacheService.isRefreshUsed(draftId)) {
            throw error(TravelRecommendationErrorCode.REFRESH_ALREADY_USED);
        }
        TravelRecommendationContext original = draft.request();
        Set<TravelTheme> themes = Arrays.stream(original.getThemes())
                .map(TravelTheme::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> excludedContentIds = draft.result().days().stream()
                .flatMap(day -> day.items().stream())
                .map(item -> item.contentId())
                .collect(Collectors.toUnmodifiableSet());
        int requestedPlaces = Arrays.stream(original.getDailyPlaceCounts())
                .mapToInt(Integer::intValue)
                .sum();
        List<TravelCandidateItem> candidates = candidateService.findCandidates(
                original.getLodgingLatitude(), original.getLodgingLongitude(), themes,
                original.getTransportType(), requestedPlaces, excludedContentIds);
        if (candidates.size() < requestedPlaces) {
            throw error(TravelRecommendationErrorCode.REFRESH_INSUFFICIENT_CANDIDATES);
        }
        if (!draftCacheService.useRefresh(draftId)) {
            throw error(TravelRecommendationErrorCode.REFRESH_ALREADY_USED);
        }

        TravelRecommendationContext refreshed = original.createRefreshRequest(
                requestCacheService.nextId());
        requestCacheService.save(refreshed);
        candidateCacheService.save(refreshed.getId(), candidates);
        eventPublisher.publishEvent(new TravelRecommendationCreatedEvent(refreshed.getId()));
        return new CreateTravelRecommendationResponse(refreshed.getId(), refreshed.getStatus(),
                candidates.size(), candidates);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw error(TravelRecommendationErrorCode.INVALID_DATE_RANGE);
        }
        if (startDate.isBefore(LocalDate.now(clock))) {
            throw error(TravelRecommendationErrorCode.PAST_START_DATE);
        }
        long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (inclusiveDays > MAX_TRIP_DAYS) {
            throw error(TravelRecommendationErrorCode.TRIP_TOO_LONG);
        }
    }

    private void validateDailyPlaceCounts(CreateTravelRecommendationRequest request) {
        long tripDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (tripDays > 0 && request.dailyPlaceCounts().size() != tripDays) {
            throw error(TravelRecommendationErrorCode.DAILY_PLACE_COUNTS_MISMATCH);
        }
    }

    private void validateLodgingScope(Region region, AccommodationRequest lodging) {
        if (region.getLatitude() == null || region.getLongitude() == null) {
            throw error(TravelRecommendationErrorCode.REGION_NOT_FOUND);
        }
        double distance = haversineMeters(region.getLatitude(), region.getLongitude(),
                lodging.latitude(), lodging.longitude());
        if (distance > REGION_SCOPE_METERS) {
            throw error(TravelRecommendationErrorCode.LODGING_OUTSIDE_REGION);
        }
    }

    private double haversineMeters(BigDecimal latitude1, BigDecimal longitude1,
                                   BigDecimal latitude2, BigDecimal longitude2) {
        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private TravelRecommendationException error(TravelRecommendationErrorCode errorCode) {
        return new TravelRecommendationException(errorCode);
    }
}

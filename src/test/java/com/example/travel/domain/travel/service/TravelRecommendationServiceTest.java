package com.example.travel.domain.travel.service;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.travel.dto.request.AccommodationRequest;
import com.example.travel.domain.travel.dto.request.CreateTravelRecommendationRequest;
import com.example.travel.domain.travel.dto.request.RouteLocationRequest;
import com.example.travel.domain.travel.dto.response.CreateTravelRecommendationResponse;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.enums.CompanionType;
import com.example.travel.domain.travel.enums.RecommendationStatus;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.TravelTheme;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelRecommendationServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private TravelRecommendationRequestCacheService requestCacheService;
    @Mock private TravelCandidateService candidateService;
    @Mock private TravelCandidateCacheService candidateCacheService;
    @Mock private User user;
    @Mock private Region region;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TravelGuideDraftCacheService draftCacheService;

    private TravelRecommendationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC);
        service = new TravelRecommendationService(userRepository, regionRepository,
                candidateService, candidateCacheService, clock, eventPublisher, draftCacheService,
                requestCacheService);
    }

    @Test
    void createsPendingRequestWithNearbyCandidates() {
        CreateTravelRecommendationRequest request = validRequest();
        when(userRepository.findByIdAndStatus(1L, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(regionRepository.findActiveById(9L)).thenReturn(Optional.of(region));
        when(region.getLatitude()).thenReturn(new BigDecimal("34.3110"));
        when(region.getLongitude()).thenReturn(new BigDecimal("126.7551"));
        TravelCandidateItem candidate = new TravelCandidateItem("100", "완도타워", "주소",
                null, new BigDecimal("34.31"), new BigDecimal("126.75"), 1000, 80);
        List<TravelCandidateItem> candidates = List.of(candidate,
                new TravelCandidateItem("101", "청해포구", "주소", null,
                        new BigDecimal("34.32"), new BigDecimal("126.76"), 2000, 70),
                new TravelCandidateItem("102", "완도수목원", "주소", null,
                        new BigDecimal("34.33"), new BigDecimal("126.77"), 3000, 60));
        when(candidateService.findCandidates(any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(candidates);
        when(requestCacheService.nextId()).thenReturn(10L);

        CreateTravelRecommendationResponse response = service.create(1L, request);

        assertThat(response.requestId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(response.candidates()).containsExactlyElementsOf(candidates);
        ArgumentCaptor<TravelRecommendationContext> requestCaptor =
                ArgumentCaptor.forClass(TravelRecommendationContext.class);
        verify(requestCacheService).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLodgingKakaoPlaceId()).isEqualTo("kakao-1");
        assertThat(requestCaptor.getValue().getStartPlaceName()).isEqualTo("완도항");
        assertThat(requestCaptor.getValue().getEndPlaceName()).isEqualTo("완도공용버스터미널");
        assertThat(requestCaptor.getValue().getDailyPlaceCounts()).containsExactly(1, 1, 1);
        verify(candidateCacheService).save(10L, candidates);
        verify(eventPublisher).publishEvent(new TravelRecommendationCreatedEvent(10L));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        CreateTravelRecommendationRequest request = request(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 19));

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.INVALID_DATE_RANGE.code());
    }

    @Test
    void rejectsTripsLongerThanSevenDays() {
        CreateTravelRecommendationRequest request = request(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 27));

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.TRIP_TOO_LONG.code());
    }

    @Test
    void rejectsDailyPlaceCountsThatDoNotMatchTripDays() {
        CreateTravelRecommendationRequest original = validRequest();
        CreateTravelRecommendationRequest request = new CreateTravelRecommendationRequest(
                original.regionId(), original.startDate(), original.endDate(),
                original.accommodation(), original.startLocation(), original.endLocation(),
                original.themes(), List.of(3, 4),
                original.transportType(), original.companionType());

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.DAILY_PLACE_COUNTS_MISMATCH.code());
    }

    @Test
    void refreshesRedisDraftFromNewTourCandidatesOnlyOnce() {
        TravelRecommendationContext original = org.mockito.Mockito.mock(
                TravelRecommendationContext.class);
        TravelRecommendationContext refreshed = org.mockito.Mockito.mock(
                TravelRecommendationContext.class);
        AiTravelGuideResult result = new AiTravelGuideResult("기존", "요약", List.of(
                new AiTravelGuideResult.Day(1, "1일차", List.of(
                        new AiTravelGuideResult.Item("old-1", 1, "10:00", 60, "기존")))), "팁");
        TravelGuideDraft draft = new TravelGuideDraft(10L, 1L, original, result, List.of(),
                List.of(), true, false);
        when(draftCacheService.find(10L)).thenReturn(draft);
        when(original.getThemes()).thenReturn(new String[]{TravelTheme.NATURE_HEALING.name()});
        when(original.getLodgingLatitude()).thenReturn(new BigDecimal("34.3150"));
        when(original.getLodgingLongitude()).thenReturn(new BigDecimal("126.7600"));
        when(original.getTransportType()).thenReturn(TransportType.CAR);
        when(original.getDailyPlaceCounts()).thenReturn(new Integer[]{1});
        when(requestCacheService.nextId()).thenReturn(20L);
        when(original.createRefreshRequest(20L)).thenReturn(refreshed);
        when(candidateService.findCandidates(any(), any(), any(), any(), anyInt(), any())).thenReturn(
                List.of(candidate("new-1")));
        when(draftCacheService.useRefresh(10L)).thenReturn(true);
        when(refreshed.getId()).thenReturn(20L);
        when(refreshed.getStatus()).thenReturn(RecommendationStatus.PENDING);

        var response = service.refreshDraft(1L, 10L);

        assertThat(response.candidates()).extracting(TravelCandidateItem::contentId)
                .containsExactly("new-1");
        verify(candidateService).findCandidates(any(), any(), any(), any(), eq(1),
                eq(Set.of("old-1")));
        verify(draftCacheService).useRefresh(10L);
        verify(candidateCacheService).save(20L, response.candidates());
    }

    private TravelCandidateItem candidate(String contentId) {
        return new TravelCandidateItem(contentId, contentId, "주소", null,
                new BigDecimal("34.31"), new BigDecimal("126.75"), 1000, 80);
    }

    private CreateTravelRecommendationRequest validRequest() {
        return request(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22));
    }

    private CreateTravelRecommendationRequest request(LocalDate startDate, LocalDate endDate) {
        AccommodationRequest lodging = new AccommodationRequest("kakao-1", "완도수엘라펜션",
                "전남 완도군", new BigDecimal("34.3150"), new BigDecimal("126.7600"));
        RouteLocationRequest start = new RouteLocationRequest("start-1", "완도항", "전남 완도군",
                new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        RouteLocationRequest end = new RouteLocationRequest("end-1", "완도공용버스터미널", "전남 완도군",
                new BigDecimal("35.1796"), new BigDecimal("129.0756"));
        int tripDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return new CreateTravelRecommendationRequest(9L, startDate, endDate, lodging, start, end,
                Set.of(TravelTheme.NATURE_HEALING), java.util.Collections.nCopies(
                Math.max(tripDays, 1), 1), TransportType.CAR, CompanionType.COUPLE);
    }
}

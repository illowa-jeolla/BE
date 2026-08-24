package com.example.travel.domain.travel.service;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.travel.dto.request.AccommodationRequest;
import com.example.travel.domain.travel.dto.request.CreateManualTravelGuideRequest;
import com.example.travel.domain.travel.dto.request.ManualTravelDayRequest;
import com.example.travel.domain.travel.dto.request.ManualTravelPlaceRequest;
import com.example.travel.domain.travel.dto.request.RouteLocationRequest;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.enums.CompanionType;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.TravelTheme;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.route.TravelRouteService;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ManualTravelGuideServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private TravelRecommendationRequestCacheService requestCacheService;
    @Mock private TravelRouteService routeService;
    @Mock private TravelGuideDraftCacheService draftCacheService;
    @Mock private User user;
    @Mock private Region region;

    private ManualTravelGuideService service;

    @BeforeEach
    void setUp() {
        service = new ManualTravelGuideService(userRepository, regionRepository,
                requestCacheService, routeService, draftCacheService,
                Clock.fixed(Instant.parse("2026-08-21T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void preservesUserDayAndPlaceOrderAndCreatesNonAiDraft() {
        CreateManualTravelGuideRequest request = validRequest();
        stubOwnerAndRegion();
        when(requestCacheService.nextId()).thenReturn(50L);
        when(routeService.plan(any(), any(), any())).thenReturn(List.of());

        Long draftId = service.create(1L, request);

        assertThat(draftId).isEqualTo(50L);
        ArgumentCaptor<TravelGuideDraft> captor = ArgumentCaptor.forClass(TravelGuideDraft.class);
        verify(draftCacheService).replaceManual(captor.capture());
        TravelGuideDraft draft = captor.getValue();
        assertThat(draft.generatedByAi()).isFalse();
        assertThat(draft.refreshResult()).isTrue();
        assertThat(draft.result().days().get(0).items())
                .extracting(item -> item.contentId())
                .containsExactly("100", "101");
        assertThat(draft.result().days().get(1).items())
                .extracting(item -> item.contentId())
                .containsExactly("102");
    }

    @Test
    void rejectsMorePlacesThanTheOriginalDailyLimit() {
        CreateManualTravelGuideRequest original = validRequest();
        CreateManualTravelGuideRequest request = new CreateManualTravelGuideRequest(
                original.regionId(), original.startDate(), original.endDate(),
                original.accommodation(), original.startLocation(), original.endLocation(),
                original.themes(), List.of(1, 1), original.transportType(),
                original.companionType(), original.title(), original.days());
        stubOwnerAndRegion();

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.MANUAL_PLACE_COUNT_EXCEEDED.code());
    }

    @Test
    void rejectsTheSamePlaceAcrossDifferentDays() {
        CreateManualTravelGuideRequest original = validRequest();
        var duplicateDay = new ManualTravelDayRequest(2, List.of(place("100", "중복 장소", "34.746")));
        CreateManualTravelGuideRequest request = new CreateManualTravelGuideRequest(
                original.regionId(), original.startDate(), original.endDate(),
                original.accommodation(), original.startLocation(), original.endLocation(),
                original.themes(), original.dailyPlaceCounts(), original.transportType(),
                original.companionType(), original.title(),
                List.of(original.days().get(0), duplicateDay));
        stubOwnerAndRegion();

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.DUPLICATE_MANUAL_PLACE.code());
    }

    @Test
    void rejectsNonSequentialDayNumbers() {
        CreateManualTravelGuideRequest original = validRequest();
        CreateManualTravelGuideRequest request = new CreateManualTravelGuideRequest(
                original.regionId(), original.startDate(), original.endDate(),
                original.accommodation(), original.startLocation(), original.endLocation(),
                original.themes(), original.dailyPlaceCounts(), original.transportType(),
                original.companionType(), original.title(),
                List.of(original.days().get(0),
                        new ManualTravelDayRequest(3, original.days().get(1).places())));
        stubOwnerAndRegion();

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.INVALID_MANUAL_DAYS.code());
    }

    @Test
    void rejectsAPlaceOutsideTheAccommodationSearchRadius() {
        CreateManualTravelGuideRequest original = validRequest();
        ManualTravelPlaceRequest farAway = new ManualTravelPlaceRequest(
                "999", "먼 관광지", "순천", null, bd("35.100"), bd("127.750"));
        CreateManualTravelGuideRequest request = new CreateManualTravelGuideRequest(
                original.regionId(), original.startDate(), original.endDate(),
                original.accommodation(), original.startLocation(), original.endLocation(),
                original.themes(), original.dailyPlaceCounts(), original.transportType(),
                original.companionType(), original.title(),
                List.of(new ManualTravelDayRequest(1, List.of(farAway)),
                        original.days().get(1)));
        stubOwnerAndRegion();

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.MANUAL_PLACE_OUTSIDE_SEARCH_RADIUS.code());
    }

    private void stubOwnerAndRegion() {
        when(userRepository.findByIdAndStatus(1L, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(regionRepository.findActiveById(1L)).thenReturn(Optional.of(region));
        lenient().when(region.getId()).thenReturn(1L);
        lenient().when(region.getName()).thenReturn("여수");
        when(region.getLatitude()).thenReturn(bd("34.7604"));
        when(region.getLongitude()).thenReturn(bd("127.6622"));
    }

    private CreateManualTravelGuideRequest validRequest() {
        var lodging = new AccommodationRequest("lodging", "숙소", "여수", bd("34.7452"), bd("127.7515"));
        var start = new RouteLocationRequest("start", "여수엑스포역", "여수", bd("34.7531"), bd("127.7462"));
        var end = new RouteLocationRequest("end", "여수엑스포역", "여수", bd("34.7531"), bd("127.7462"));
        return new CreateManualTravelGuideRequest(1L, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2), lodging, start, end,
                Set.of(TravelTheme.NATURE_HEALING), List.of(2, 1), TransportType.CAR,
                CompanionType.FRIENDS, "나의 여수 여행",
                List.of(new ManualTravelDayRequest(1, List.of(
                                place("100", "오동도", "34.744"),
                                place("101", "해상케이블카", "34.739"))),
                        new ManualTravelDayRequest(2, List.of(
                                place("102", "아쿠아플라넷", "34.748")))));
    }

    private ManualTravelPlaceRequest place(String id, String title, String latitude) {
        return new ManualTravelPlaceRequest(id, title, "여수", null,
                bd(latitude), bd("127.750"));
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}

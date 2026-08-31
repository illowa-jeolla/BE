package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.repository.projection.GatheringSearchProjection;
import com.example.travel.domain.gathering.dto.request.GatheringSearchRequest;
import com.example.travel.domain.gathering.dto.response.GatheringSearchResponse;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.calculator.GatheringRelevanceCalculator;
import com.example.travel.domain.gathering.service.GatheringSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatheringSearchServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T03:00:00Z");

    private GatheringRepository gatheringRepository;
    private GatheringSearchService searchService;

    @BeforeEach
    void setUp() {
        gatheringRepository = mock(GatheringRepository.class);
        searchService = new GatheringSearchService(gatheringRepository,
                new GatheringRelevanceCalculator(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void searchesOpenGatheringsWithinInclusiveDateRange() {
        when(gatheringRepository.findSearchCandidates(
                eq(7L), eq("여수"), any(), any(),
                eq(GatheringStatus.OPEN), eq(ParticipantStatus.JOINED)))
                .thenReturn(List.of());

        searchService.search(7L, request(null, null, 0, 20));

        ArgumentCaptor<OffsetDateTime> startsAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> endsAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(gatheringRepository).findSearchCandidates(
                eq(7L), eq("여수"), startsAt.capture(), endsAt.capture(),
                eq(GatheringStatus.OPEN), eq(ParticipantStatus.JOINED));
        assertThat(startsAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-08-13T03:00:00Z"));
        assertThat(endsAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-08-15T00:00:00+09:00"));
    }

    @Test
    void searchesAllRegionsAndDatesWhenFiltersAreMissing() {
        GatheringSearchRequest request = new GatheringSearchRequest(
                null, null, null, null, null, null, 0, 20);
        when(gatheringRepository.findSearchCandidates(
                eq(7L), eq(""), any(), any(),
                eq(GatheringStatus.OPEN), eq(ParticipantStatus.JOINED)))
                .thenReturn(List.of());

        GatheringSearchResponse response = searchService.search(7L, request);

        verify(gatheringRepository).findSearchCandidates(
                eq(7L), eq(""), any(), any(),
                eq(GatheringStatus.OPEN), eq(ParticipantStatus.JOINED));
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void optionalTimeAndConceptPrioritizeBetterMatchWithoutFilteringCandidates() {
        GatheringSearchProjection exact = candidate(1L, "펍투어", "2026-08-13T19:00:00+09:00");
        GatheringSearchProjection other = candidate(2L, "해변 산책", "2026-08-13T18:00:00+09:00");
        when(gatheringRepository.findSearchCandidates(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(other, exact));

        GatheringSearchResponse response = searchService.search(
                7L, request("19:00", "펍투어", 0, 20));

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).extracting(item -> item.id())
                .containsExactly(1L, 2L);
        assertThat(response.content().get(0).relevanceScore())
                .isGreaterThan(response.content().get(1).relevanceScore());
    }

    @Test
    void missingOptionalConditionsSortsByStartTime() {
        GatheringSearchProjection later = candidate(1L, "펍투어", "2026-08-13T20:00:00+09:00");
        GatheringSearchProjection earlier = candidate(2L, "산책", "2026-08-13T18:00:00+09:00");
        when(gatheringRepository.findSearchCandidates(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(later, earlier));

        GatheringSearchResponse response = searchService.search(
                7L, request(" ", " ", 0, 20));

        assertThat(response.content()).extracting(item -> item.id())
                .containsExactly(2L, 1L);
    }

    @Test
    void optionalMeetingPlacePrioritizesSimilarStoredMeetingPlace() {
        GatheringSearchProjection exact = candidate(
                1L, "산책", "여수 낭만포차 입구", "2026-08-13T19:00:00+09:00");
        GatheringSearchProjection other = candidate(
                2L, "산책", "오동도 주차장", "2026-08-13T19:00:00+09:00");
        when(gatheringRepository.findSearchCandidates(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(other, exact));

        GatheringSearchResponse response = searchService.search(
                7L, request(null, null, "낭만포차", 0, 20));

        assertThat(response.content()).extracting(item -> item.id())
                .containsExactly(1L, 2L);
        assertThat(response.content().get(0).meetingPlaceScore())
                .isGreaterThan(response.content().get(1).meetingPlaceScore());
    }

    @Test
    void appliesPaginationAfterRelevanceSorting() {
        when(gatheringRepository.findSearchCandidates(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        candidate(1L, "산책", "2026-08-13T18:00:00+09:00"),
                        candidate(2L, "미식", "2026-08-13T19:00:00+09:00"),
                        candidate(3L, "전시", "2026-08-13T20:00:00+09:00")));

        GatheringSearchResponse response = searchService.search(
                7L, request(null, null, 1, 2));

        assertThat(response.content()).extracting(item -> item.id()).containsExactly(3L);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        GatheringSearchRequest request = new GatheringSearchRequest(
                "여수", LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 13),
                null, null, null, 0, 20);

        assertThatThrownBy(() -> searchService.search(7L, request))
                .isInstanceOfSatisfying(GatheringException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("GATHERING_400_INVALID_DATE_RANGE"));
        verify(gatheringRepository, never()).findSearchCandidates(
                any(), any(), any(), any(), any(), any());
    }

    private GatheringSearchRequest request(String time, String concept, int page, int size) {
        return request(time, concept, null, page, size);
    }

    private GatheringSearchRequest request(String time, String concept, String meetingPlace,
                                           int page, int size) {
        return new GatheringSearchRequest(" 여수 ", LocalDate.of(2026, 8, 13),
                LocalDate.of(2026, 8, 14), time, concept, meetingPlace, page, size);
    }

    private GatheringSearchProjection candidate(Long id, String concept, String startsAt) {
        return candidate(id, concept, "만남 장소", startsAt);
    }

    private GatheringSearchProjection candidate(Long id, String concept, String meetingPlace,
                                                String startsAt) {
        return new GatheringSearchProjection(id, "게더링 " + id, 10L, "여수", concept,
                meetingPlace, OffsetDateTime.parse(startsAt), (short) 4,
                GatheringStatus.OPEN, 15L, "남도산책", 2L, 0L);
    }
}

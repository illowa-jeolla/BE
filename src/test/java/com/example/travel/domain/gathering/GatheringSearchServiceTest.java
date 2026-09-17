package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.dto.request.GatheringSearchRequest;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.repository.projection.GatheringSearchProjection;
import com.example.travel.domain.gathering.service.GatheringSearchService;
import com.example.travel.domain.gathering.service.calculator.GatheringRelevanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

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
    void usesNowAsStartWhenDateFiltersAreMissing() {
        GatheringSearchRequest request = new GatheringSearchRequest(
                null, null, null, null, null, null, 0, 20);

        searchService.search(7L, request);

        ArgumentCaptor<OffsetDateTime> startsAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(gatheringRepository).findSearchCandidates(
                eq(7L), eq(""), startsAt.capture(), any(), eq(GatheringStatus.OPEN),
                eq(ParticipantStatus.JOINED), any(Pageable.class));
        assertThat(startsAt.getValue()).isEqualTo(OffsetDateTime.parse("2026-08-13T03:00:00Z"));
    }

    @Test
    void usesLaterRequestedStartAndInclusiveEndDate() {
        GatheringSearchRequest request = new GatheringSearchRequest(
                "여수", LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 20),
                null, null, null, 0, 20);

        searchService.search(7L, request);

        ArgumentCaptor<OffsetDateTime> startsAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> endsAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(gatheringRepository).findSearchCandidates(
                eq(7L), eq("여수"), startsAt.capture(), endsAt.capture(),
                eq(GatheringStatus.OPEN), eq(ParticipantStatus.JOINED), any(Pageable.class));
        assertThat(startsAt.getValue()).isEqualTo(
                OffsetDateTime.parse("2026-08-14T00:00:00+09:00"));
        assertThat(endsAt.getValue()).isEqualTo(
                OffsetDateTime.parse("2026-08-21T00:00:00+09:00"));
    }

    @Test
    void appliesRepositoryPaginationWithoutRelevanceConditions() {
        when(gatheringRepository.countSearchCandidates(any(), any(), any(), any()))
                .thenReturn(45L);
        when(gatheringRepository.findSearchCandidates(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(candidate(21L, "산책", "2026-08-14T18:00:00+09:00")));

        var response = searchService.search(7L, request(null, null, 1, 20));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(gatheringRepository).findSearchCandidates(
                any(), any(), any(), any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(45);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void boundsCandidatesAndSortsByRelevanceWhenRequested() {
        when(gatheringRepository.countSearchCandidates(any(), any(), any(), any()))
                .thenReturn(2L);
        when(gatheringRepository.findSearchCandidates(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        candidate(2L, "저녁 식사", "2026-08-14T18:00:00+09:00"),
                        candidate(1L, "야간 산책", "2026-08-14T19:00:00+09:00")));

        var response = searchService.search(7L, request("19:00", "야간 산책", 0, 20));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(gatheringRepository).findSearchCandidates(
                any(), any(), any(), any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1000);
        assertThat(response.content()).extracting(item -> item.id()).containsExactly(1L, 2L);
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
                any(), any(), any(), any(), any(), any(), any());
    }

    private GatheringSearchRequest request(String time, String concept, int page, int size) {
        return new GatheringSearchRequest("여수", LocalDate.of(2026, 8, 13),
                LocalDate.of(2026, 8, 14), time, concept, null, page, size);
    }

    private GatheringSearchProjection candidate(Long id, String concept, String startsAt) {
        return new GatheringSearchProjection(id, "게더링 " + id, 10L, "여수", concept,
                "여수역", OffsetDateTime.parse(startsAt), (short) 4,
                GatheringStatus.OPEN, 15L, "호스트", 2L, 0L);
    }
}

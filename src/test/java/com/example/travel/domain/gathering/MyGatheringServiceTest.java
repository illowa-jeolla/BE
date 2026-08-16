package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.repository.projection.MyGatheringProjection;
import com.example.travel.domain.gathering.dto.request.MyGatheringRequest;
import com.example.travel.domain.gathering.dto.response.MyGatheringResponse;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.GatheringTiming;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.MyGatheringService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyGatheringServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");
    private final GatheringRepository repository = mock(GatheringRepository.class);
    private final MyGatheringService service = new MyGatheringService(
            repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void returnsHostedGatheringsWithUpcomingAndPastDistinction() {
        when(repository.findHostedGatherings(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(ParticipantStatus.JOINED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        candidate(1L, NOW.minusSeconds(60)),
                        candidate(2L, NOW.plusSeconds(60)))));

        MyGatheringResponse response = service.findMine(
                3L, new MyGatheringRequest("hosted", null, null));

        assertThat(response.type()).isEqualTo("hosted");
        assertThat(response.content()).extracting(item -> item.timing())
                .containsExactly(GatheringTiming.PAST, GatheringTiming.UPCOMING);
        verify(repository).findHostedGatherings(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(ParticipantStatus.JOINED), any(Pageable.class));
    }

    @Test
    void joinedTypeQueriesOnlyJoinedMembers() {
        when(repository.findJoinedGatherings(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(ParticipantRole.MEMBER),
                org.mockito.ArgumentMatchers.eq(ParticipantStatus.JOINED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(candidate(4L, NOW.plusSeconds(60)))));

        MyGatheringResponse response = service.findMine(
                7L, new MyGatheringRequest("joined", 0, 10));

        assertThat(response.content()).hasSize(1);
        verify(repository).findJoinedGatherings(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(ParticipantRole.MEMBER),
                org.mockito.ArgumentMatchers.eq(ParticipantStatus.JOINED), any(Pageable.class));
    }

    private MyGatheringProjection candidate(Long id, Instant startsAt) {
        return new MyGatheringProjection(id, "여수 모임", 1L, "여수", "산책", "여수역",
                OffsetDateTime.ofInstant(startsAt, ZoneOffset.UTC), (short) 5,
                GatheringStatus.OPEN, 2L);
    }
}

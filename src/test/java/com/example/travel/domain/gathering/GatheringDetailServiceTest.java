package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.dto.GatheringDetailCandidate;
import com.example.travel.domain.gathering.dto.GatheringDetailResponse;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.GatheringDetailService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatheringDetailServiceTest {
    private final GatheringRepository gatheringRepository = mock(GatheringRepository.class);
    private final GatheringDetailService service = new GatheringDetailService(gatheringRepository);

    @Test
    void returnsGatheringDetailWithParticipationAndHostState() {
        OffsetDateTime startsAt = OffsetDateTime.parse("2026-08-20T19:00:00+09:00");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-14T09:00:00+09:00");
        GatheringDetailCandidate candidate = new GatheringDetailCandidate(
                10L, "여수 펍 투어", "함께 둘러봐요", 1L, "여수", "펍투어",
                "여수역", new BigDecimal("34.7527000"),
                new BigDecimal("127.7485000"), startsAt, (short) 6,
                GatheringStatus.OPEN, 3L, "여행자", "https://example.com/avatar.png",
                createdAt, createdAt, 2L, 1L);
        when(gatheringRepository.findDetail(
                10L, 3L, ParticipantStatus.JOINED)).thenReturn(Optional.of(candidate));

        GatheringDetailResponse response = service.findDetail(10L, 3L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.participantCount()).isEqualTo(2L);
        assertThat(response.joined()).isTrue();
        assertThat(response.host()).isTrue();
        assertThat(response.creator().avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(response.description()).isEqualTo("함께 둘러봐요");
    }

    @Test
    void throwsNotFoundWhenGatheringDoesNotExist() {
        when(gatheringRepository.findDetail(
                999L, 2L, ParticipantStatus.JOINED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail(999L, 2L))
                .isInstanceOf(GatheringException.class)
                .satisfies(exception -> assertThat(((GatheringException) exception).getCode())
                        .isEqualTo("GATHERING_404_NOT_FOUND"));
    }
}

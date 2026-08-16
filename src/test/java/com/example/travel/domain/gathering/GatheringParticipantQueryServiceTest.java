package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.repository.projection.GatheringParticipantProjection;
import com.example.travel.domain.gathering.dto.response.GatheringParticipantListResponse;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.GatheringParticipantQueryService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatheringParticipantQueryServiceTest {
    private final GatheringRepository gatheringRepository = mock(GatheringRepository.class);
    private final GatheringParticipantRepository participantRepository =
            mock(GatheringParticipantRepository.class);
    private final GatheringParticipantQueryService service =
            new GatheringParticipantQueryService(gatheringRepository, participantRepository);

    @Test
    void returnsCurrentParticipantsWithLimitedProfileFields() {
        OffsetDateTime joinedAt = OffsetDateTime.parse("2026-08-14T09:00:00+09:00");
        when(gatheringRepository.existsActiveById(1L)).thenReturn(true);
        when(participantRepository.existsJoinedParticipant(
                1L, 3L, ParticipantStatus.JOINED)).thenReturn(true);
        when(participantRepository.findCurrentParticipants(1L, ParticipantStatus.JOINED))
                .thenReturn(List.of(
                        new GatheringParticipantProjection(3L, "방장", "host.png",
                                ParticipantRole.HOST, ParticipantStatus.JOINED, joinedAt),
                        new GatheringParticipantProjection(7L, "참여자", null,
                                ParticipantRole.MEMBER, ParticipantStatus.JOINED, joinedAt)));

        GatheringParticipantListResponse response = service.findParticipants(1L, 3L);

        assertThat(response.participantCount()).isEqualTo(2L);
        assertThat(response.participants()).extracting(item -> item.role())
                .containsExactly(ParticipantRole.HOST, ParticipantRole.MEMBER);
        assertThat(response.participants().get(0).nickname()).isEqualTo("방장");
    }

    @Test
    void rejectsNonParticipant() {
        when(gatheringRepository.existsActiveById(1L)).thenReturn(true);
        when(participantRepository.existsJoinedParticipant(
                1L, 9L, ParticipantStatus.JOINED)).thenReturn(false);

        assertThatThrownBy(() -> service.findParticipants(1L, 9L))
                .isInstanceOfSatisfying(GatheringException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("GATHERING_403_PARTICIPANT_LIST_FORBIDDEN"));
        verify(participantRepository, never()).findCurrentParticipants(
                1L, ParticipantStatus.JOINED);
    }

    @Test
    void rejectsUnknownGathering() {
        when(gatheringRepository.existsActiveById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.findParticipants(999L, 3L))
                .isInstanceOfSatisfying(GatheringException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("GATHERING_404_NOT_FOUND"));
    }
}

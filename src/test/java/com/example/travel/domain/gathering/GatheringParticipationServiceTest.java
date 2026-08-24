package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.dto.response.JoinGatheringResponse;
import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.GatheringParticipationService;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatheringParticipationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");

    private GatheringRepository gatheringRepository;
    private GatheringParticipantRepository participantRepository;
    private UserRepository userRepository;
    private GatheringParticipationService service;

    @BeforeEach
    void setUp() {
        gatheringRepository = mock(GatheringRepository.class);
        participantRepository = mock(GatheringParticipantRepository.class);
        userRepository = mock(UserRepository.class);
        service = new GatheringParticipationService(gatheringRepository, participantRepository,
                userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void joinsAsMember() {
        Gathering gathering = gathering((short) 4, GatheringStatus.OPEN);
        User user = user(7L);
        prepare(gathering, user);
        when(participantRepository.countByGatheringAndStatus(
                1L, ParticipantStatus.JOINED)).thenReturn(2L);

        JoinGatheringResponse response = service.join(1L, 7L);

        ArgumentCaptor<GatheringParticipant> captor =
                ArgumentCaptor.forClass(GatheringParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipantRole()).isEqualTo(ParticipantRole.MEMBER);
        assertThat(captor.getValue().getStatus()).isEqualTo(ParticipantStatus.JOINED);
        assertThat(response.participantCount()).isEqualTo(3L);
        assertThat(response.gatheringStatus()).isEqualTo(GatheringStatus.OPEN);
    }

    @Test
    void rejectsDuplicateParticipant() {
        Gathering gathering = gathering((short) 4, GatheringStatus.OPEN);
        User user = user(7L);
        prepare(gathering, user);
        GatheringParticipant participant = mock(GatheringParticipant.class);
        when(participant.getStatus()).thenReturn(ParticipantStatus.JOINED);
        when(participantRepository.findParticipant(1L, 7L))
                .thenReturn(Optional.of(participant));

        assertCode(() -> service.join(1L, 7L), "GATHERING_409_ALREADY_JOINED");

        verify(participantRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRejoinAfterCancellationWithSpecificMessage() {
        Gathering gathering = gathering((short) 4, GatheringStatus.OPEN);
        User user = user(7L);
        prepare(gathering, user);
        GatheringParticipant participant = mock(GatheringParticipant.class);
        when(participant.getStatus()).thenReturn(ParticipantStatus.CANCELLED);
        when(participantRepository.findParticipant(1L, 7L))
                .thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> service.join(1L, 7L))
                .isInstanceOfSatisfying(GatheringException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo("GATHERING_409_REJOIN_NOT_ALLOWED");
                    assertThat(exception.getMessage())
                            .isEqualTo("이미 참여한 게더링은 재참여할 수 없습니다.");
                });

        verify(participantRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void marksGatheringFullWhenLastSeatIsTaken() {
        Gathering gathering = gathering((short) 3, GatheringStatus.OPEN);
        User user = user(7L);
        prepare(gathering, user);
        when(participantRepository.countByGatheringAndStatus(
                1L, ParticipantStatus.JOINED)).thenReturn(2L);
        when(gathering.getStatus()).thenReturn(GatheringStatus.OPEN, GatheringStatus.FULL);

        JoinGatheringResponse response = service.join(1L, 7L);

        verify(gathering).markFull();
        assertThat(response.participantCount()).isEqualTo(3L);
        assertThat(response.gatheringStatus()).isEqualTo(GatheringStatus.FULL);
    }

    @Test
    void rejectsWhenCapacityIsAlreadyFull() {
        Gathering gathering = gathering((short) 3, GatheringStatus.OPEN);
        User user = user(7L);
        prepare(gathering, user);
        when(participantRepository.countByGatheringAndStatus(
                1L, ParticipantStatus.JOINED)).thenReturn(3L);

        assertCode(() -> service.join(1L, 7L), "GATHERING_409_CAPACITY_FULL");

        verify(participantRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelsMemberParticipationAndReopensFullGathering() {
        Gathering gathering = gathering((short) 3, GatheringStatus.FULL);
        GatheringParticipant participant = mock(GatheringParticipant.class);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(participantRepository.findParticipant(1L, 7L))
                .thenReturn(Optional.of(participant));
        when(participant.getParticipantRole()).thenReturn(ParticipantRole.MEMBER);
        when(participant.getStatus()).thenReturn(ParticipantStatus.JOINED);
        when(participantRepository.countByGatheringAndStatus(
                1L, ParticipantStatus.JOINED)).thenReturn(3L);
        when(gathering.getStatus()).thenReturn(GatheringStatus.FULL, GatheringStatus.OPEN);

        var response = service.cancel(1L, 7L);

        verify(participant).cancel();
        verify(gathering).reopen();
        assertThat(response.participantStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(response.participantCount()).isEqualTo(2L);
        assertThat(response.gatheringStatus()).isEqualTo(GatheringStatus.OPEN);
    }

    @Test
    void rejectsHostParticipationCancellation() {
        Gathering gathering = gathering((short) 3, GatheringStatus.OPEN);
        GatheringParticipant participant = mock(GatheringParticipant.class);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(participantRepository.findParticipant(1L, 7L))
                .thenReturn(Optional.of(participant));
        when(participant.getParticipantRole()).thenReturn(ParticipantRole.HOST);

        assertCode(() -> service.cancel(1L, 7L),
                "GATHERING_409_HOST_CANNOT_CANCEL_PARTICIPATION");

        verify(participant, never()).cancel();
    }

    @Test
    void rejectsRepeatedParticipationCancellation() {
        Gathering gathering = gathering((short) 3, GatheringStatus.OPEN);
        GatheringParticipant participant = mock(GatheringParticipant.class);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(participantRepository.findParticipant(1L, 7L))
                .thenReturn(Optional.of(participant));
        when(participant.getParticipantRole()).thenReturn(ParticipantRole.MEMBER);
        when(participant.getStatus()).thenReturn(ParticipantStatus.CANCELLED);

        assertCode(() -> service.cancel(1L, 7L), "GATHERING_409_ALREADY_CANCELLED");

        verify(participant, never()).cancel();
    }

    @Test
    void rejectsCancellationWhenParticipationDoesNotExist() {
        Gathering gathering = gathering((short) 3, GatheringStatus.OPEN);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(participantRepository.findParticipant(1L, 7L)).thenReturn(Optional.empty());

        assertCode(() -> service.cancel(1L, 7L),
                "GATHERING_404_PARTICIPANT_NOT_FOUND");
    }

    private void prepare(Gathering gathering, User user) {
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(participantRepository.findParticipant(1L, 7L)).thenReturn(Optional.empty());
    }

    private Gathering gathering(short capacity, GatheringStatus status) {
        Gathering gathering = mock(Gathering.class);
        when(gathering.getId()).thenReturn(1L);
        when(gathering.getCapacity()).thenReturn(capacity);
        when(gathering.getStatus()).thenReturn(status);
        when(gathering.getStartsAt()).thenReturn(
                OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC));
        return gathering;
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
                            String code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(GatheringException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
